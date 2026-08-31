[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Region = "us-east4",
    [string]$ServiceName = "oracle-supply-chain-mcp-app",
    [string]$Repository = "a2ui-agents",
    [string]$RuntimeServiceAccount = "a2ui-gemini-runner",
    [string]$DatabaseServiceName = "paulparkdb_tp",
    [string]$DatabaseUsername = "FINANCIAL",
    [string]$WalletSecret = "a2ui-paulparkdb-wallet",
    [string]$DatabasePasswordSecret =
        "a2ui-paulparkdb-financial-password",
    [ValidateRange(0, 10)]
    [int]$MinInstances = 0,
    [switch]$EnableWriteActions,
    [switch]$AllowUnauthenticated
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$image = (
    "$Region-docker.pkg.dev/$ProjectId/$Repository/" +
    "$ServiceName`:latest")
$serviceAccountEmail = "$RuntimeServiceAccount@$ProjectId.iam.gserviceaccount.com"
$writesEnabled = if ($EnableWriteActions) { "true" } else { "false" }
$runtimeEnvironment = (
    "DB_SERVICE_NAME=$DatabaseServiceName,DB_USERNAME=$DatabaseUsername," +
    "AGENT_PORT=8081,AGENT_SERVICE_URL=http://127.0.0.1:8081," +
    "MCP_BIND_HOST=0.0.0.0,MCP_WRITES_ENABLED=$writesEnabled")

if ($AllowUnauthenticated -and $EnableWriteActions) {
    throw (
        "Refusing to expose Oracle-backed write actions anonymously. " +
        "Configure OAuth 2.1 before enabling both options.")
}

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

if (-not (Test-GcloudResource artifacts repositories describe $Repository `
        --project=$ProjectId --location=$Region)) {
    throw "Artifact Registry repository $Repository does not exist in $Region."
}
if (-not (Test-GcloudResource iam service-accounts describe `
        $serviceAccountEmail --project=$ProjectId)) {
    throw "Runtime service account $serviceAccountEmail does not exist."
}
foreach ($secret in @($WalletSecret, $DatabasePasswordSecret)) {
    if (-not (Test-GcloudResource secrets versions describe latest `
            --secret=$secret --project=$ProjectId)) {
        throw "Secret $secret has no accessible latest version."
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
        --config=deploy/gcp/cloudbuild.mcp-app.yaml `
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

Invoke-Gcloud run deploy $ServiceName `
    --project=$ProjectId `
    --region=$Region `
    --platform=managed `
    --image=$image `
    --service-account=$serviceAccountEmail `
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
    $accessFlag

$serviceUri = & $gcloud run services describe $ServiceName `
    --project=$ProjectId --region=$Region --format="value(status.url)"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($serviceUri)) {
    throw "Cloud Run did not return a service URL."
}

Write-Host "Cloud Run MCP service: $serviceUri"
Write-Host "Health endpoint: $serviceUri/health"
Write-Host "ChatGPT MCP endpoint: $serviceUri/mcp"
if (-not $AllowUnauthenticated) {
    Write-Host "The endpoint is private. Keep it private until OAuth is configured or a time-bounded developer-mode validation is ready."
}
elseif (-not $EnableWriteActions) {
    Write-Host "The public endpoint is read-only; approval and rejection tools are not registered."
}
