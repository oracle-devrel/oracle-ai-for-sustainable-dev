[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Region = "us-east4",
    [string]$ServiceName = "oracle-supply-chain-a2ui",
    [string]$Repository = "a2ui-agents",
    [string]$RuntimeServiceAccount = "a2ui-gemini-runner",
    [string]$DatabaseServiceName = "paulparkdb_tp",
    [string]$DatabaseUsername = "FINANCIAL",
    [string]$RestrictedDatabaseUsername = "ENVIRONMENTAL_PLANNER",
    [string]$WalletSecret = "a2ui-paulparkdb-wallet",
    [string]$DatabasePasswordSecret =
        "a2ui-paulparkdb-financial-password",
    [string]$RestrictedDatabasePasswordSecret =
        "a2ui-paulparkdb-environmental-planner-password",
    [switch]$AllowUnauthenticated
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$image = (
    "$Region-docker.pkg.dev/$ProjectId/$Repository/" +
    "$ServiceName`:latest")
$serviceAccountEmail = "$RuntimeServiceAccount@$ProjectId.iam.gserviceaccount.com"

function Invoke-Gcloud {
    & $gcloud @args
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: $($args -join ' ')"
    }
}

function Test-GcloudResource {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $gcloud @args *> $null
        return $LASTEXITCODE -eq 0
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }
}

Invoke-Gcloud config set account paul.parkinson@oracle.com
Invoke-Gcloud config set project $ProjectId
Invoke-Gcloud services enable `
    artifactregistry.googleapis.com `
    cloudbuild.googleapis.com `
    run.googleapis.com `
    secretmanager.googleapis.com `
    --project=$ProjectId

if (-not (Test-GcloudResource artifacts repositories describe $Repository `
        --project=$ProjectId --location=$Region)) {
    Invoke-Gcloud artifacts repositories create $Repository `
        --project=$ProjectId `
        --location=$Region `
        --repository-format=docker `
        --description="A2A and A2UI agent images"
}

if (-not (Test-GcloudResource iam service-accounts describe `
        $serviceAccountEmail --project=$ProjectId)) {
    Invoke-Gcloud iam service-accounts create $RuntimeServiceAccount `
        --project=$ProjectId `
        --display-name="Gemini Enterprise A2UI runtime"
}

foreach ($secret in @(
        $WalletSecret,
        $DatabasePasswordSecret,
        $RestrictedDatabasePasswordSecret)) {
    if (-not (Test-GcloudResource secrets versions describe latest `
            --secret=$secret --project=$ProjectId)) {
        throw (
            "Secret $secret has no accessible latest version. Run " +
            "prepare-secrets.ps1 first.")
    }
    Invoke-Gcloud secrets add-iam-policy-binding $secret `
        --project=$ProjectId `
        --member="serviceAccount:$serviceAccountEmail" `
        --role=roles/secretmanager.secretAccessor
}

Push-Location $projectRoot
try {
    Invoke-Gcloud builds submit `
        --project=$ProjectId `
        --config=deploy/gcp/cloudbuild.yaml `
        --substitutions="_IMAGE=$image" `
        .
}
finally {
    Pop-Location
}

$accessFlag = if ($AllowUnauthenticated) {
    "--allow-unauthenticated"
}
else {
    "--no-allow-unauthenticated"
}

$commonArguments = @(
    "run", "deploy", $ServiceName,
    "--project=$ProjectId",
    "--region=$Region",
    "--platform=managed",
    "--image=$image",
    "--service-account=$serviceAccountEmail",
    "--cpu=2",
    "--memory=2Gi",
    "--concurrency=10",
    "--max-instances=1",
    "--timeout=300",
    "--port=8080",
    "--network=default",
    "--subnet=default",
    "--vpc-egress=private-ranges-only",
    "--set-env-vars=DB_SERVICE_NAME=$DatabaseServiceName,DB_USERNAME=$DatabaseUsername,DB_USERNAME2=$RestrictedDatabaseUsername,AGENT_PORT=8081,AGENT_SERVICE_URL=http://127.0.0.1:8081,PUBLIC_A2A_URL=https://pending.invalid",
    "--set-secrets=/var/run/secrets/oracle-wallet/wallet.zip=$WalletSecret`:latest,DB_PASSWORD=$DatabasePasswordSecret`:latest,DB_PASSWORD2=$RestrictedDatabasePasswordSecret`:latest",
    $accessFlag
)
Invoke-Gcloud @commonArguments

$serviceUri = & $gcloud run services describe $ServiceName `
    --project=$ProjectId --region=$Region --format="value(status.url)"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($serviceUri)) {
    throw "Cloud Run did not return a service URL."
}

Invoke-Gcloud run services update $ServiceName `
    --project=$ProjectId `
    --region=$Region `
    --update-env-vars="PUBLIC_A2A_URL=$serviceUri"

Write-Host "Cloud Run service: $serviceUri"
Write-Host "Agent card: $serviceUri/.well-known/agent-card.json"
