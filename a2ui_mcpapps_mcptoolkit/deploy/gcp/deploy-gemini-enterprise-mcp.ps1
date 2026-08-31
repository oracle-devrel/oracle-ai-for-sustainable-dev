[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Region = "us-east4",
    [string]$ServiceName = "oracle-supply-chain-mcp-gemini",
    [string]$Repository = "a2ui-agents",
    [string]$RuntimeServiceAccount = "a2ui-gemini-runner",
    [string]$DatabaseServiceName = "paulparkdb_tp",
    [string]$DatabaseUsername = "FINANCIAL",
    [string]$WalletSecret = "a2ui-paulparkdb-wallet",
    [string]$DatabasePasswordSecret =
        "a2ui-paulparkdb-financial-password",
    [ValidateRange(0, 10)]
    [int]$MinInstances = 0
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$image = (
    "$Region-docker.pkg.dev/$ProjectId/$Repository/" +
    "$ServiceName`:latest")
$runtimeServiceAccountEmail =
    "$RuntimeServiceAccount@$ProjectId.iam.gserviceaccount.com"
$runtimeEnvironment = (
    "DB_SERVICE_NAME=$DatabaseServiceName,DB_USERNAME=$DatabaseUsername," +
    "AGENT_PORT=8081,AGENT_SERVICE_URL=http://127.0.0.1:8081," +
    "MCP_BIND_HOST=0.0.0.0,MCP_WRITES_ENABLED=false")

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

$projectNumber = & $gcloud projects describe $ProjectId `
    --format="value(projectNumber)"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($projectNumber)) {
    throw "Could not resolve the project number for $ProjectId."
}
$discoveryEngineServiceAgent = (
    "service-$projectNumber@gcp-sa-discoveryengine.iam.gserviceaccount.com")

if (-not (Test-GcloudResource artifacts repositories describe $Repository `
        --project=$ProjectId --location=$Region)) {
    throw "Artifact Registry repository $Repository does not exist in $Region."
}
if (-not (Test-GcloudResource iam service-accounts describe `
        $runtimeServiceAccountEmail --project=$ProjectId)) {
    throw "Runtime service account $runtimeServiceAccountEmail does not exist."
}
foreach ($secret in @($WalletSecret, $DatabasePasswordSecret)) {
    if (-not (Test-GcloudResource secrets versions describe latest `
            --secret=$secret --project=$ProjectId)) {
        throw "Secret $secret has no accessible latest version."
    }
    Invoke-Gcloud secrets add-iam-policy-binding $secret `
        --project=$ProjectId `
        --member="serviceAccount:$runtimeServiceAccountEmail" `
        --role=roles/secretmanager.secretAccessor
}

Push-Location $projectRoot
try {
    Invoke-Gcloud builds submit `
        --project=$ProjectId `
        --config=deploy/gcp/cloudbuild.mcp-app.yaml `
        --substitutions="_IMAGE=$image" `
        .
}
finally {
    Pop-Location
}

Invoke-Gcloud run deploy $ServiceName `
    --project=$ProjectId `
    --region=$Region `
    --platform=managed `
    --image=$image `
    --service-account=$runtimeServiceAccountEmail `
    --cpu=2 `
    --memory=2Gi `
    --concurrency=10 `
    --min-instances=$MinInstances `
    --max-instances=1 `
    --timeout=300 `
    --port=8080 `
    --network=default `
    --subnet=default `
    --vpc-egress=private-ranges-only `
    --set-env-vars=$runtimeEnvironment `
    --set-secrets="/var/run/secrets/oracle-wallet/wallet.zip=$WalletSecret`:latest,DB_PASSWORD=$DatabasePasswordSecret`:latest" `
    --no-allow-unauthenticated

$iamPolicy = & $gcloud run services get-iam-policy $ServiceName `
    --project=$ProjectId `
    --region=$Region `
    --format=json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect the Cloud Run IAM policy."
}
$anonymousBinding = $iamPolicy.bindings | Where-Object {
    $_.role -eq "roles/run.invoker" -and $_.members -contains "allUsers"
}
if ($anonymousBinding) {
    Invoke-Gcloud run services remove-iam-policy-binding $ServiceName `
        --project=$ProjectId `
        --region=$Region `
        --member=allUsers `
        --role=roles/run.invoker
}

Invoke-Gcloud run services add-iam-policy-binding $ServiceName `
    --project=$ProjectId `
    --region=$Region `
    --member="serviceAccount:$discoveryEngineServiceAgent" `
    --role=roles/run.invoker

$serviceUri = & $gcloud run services describe $ServiceName `
    --project=$ProjectId --region=$Region --format="value(status.url)"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($serviceUri)) {
    throw "Cloud Run did not return a service URL."
}

Write-Host "Gemini Enterprise private MCP service: $serviceUri"
Write-Host "Gemini Enterprise MCP endpoint: $serviceUri/mcp"
Write-Host "Discovery Engine invoker: $discoveryEngineServiceAgent"
Write-Host "Writes remain disabled; only the dashboard tool is registered."
Write-Host "Continue with docs/gemini-enterprise-mcp-app.md."
