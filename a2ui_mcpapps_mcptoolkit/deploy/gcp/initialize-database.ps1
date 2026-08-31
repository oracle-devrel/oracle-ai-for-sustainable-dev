[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Region = "us-east4",
    [string]$Image =
        "us-east4-docker.pkg.dev/adb-pm-prod/a2ui-agents/" +
        "oracle-supply-chain-a2ui:latest",
    [string]$RuntimeServiceAccount = "a2ui-gemini-runner",
    [string]$DatabaseServiceName = "paulparkdb_tp",
    [string]$ApplicationUsername = "FINANCIAL",
    [string]$WalletSecret = "a2ui-paulparkdb-wallet",
    [string]$ApplicationPasswordSecret =
        "a2ui-paulparkdb-financial-password",
    [string]$AdminBootstrapSecret =
        "a2ui-paulparkdb-admin-bootstrap"
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$environmentFile = Join-Path $projectRoot ".env"
$serviceAccountEmail =
    "$RuntimeServiceAccount@$ProjectId.iam.gserviceaccount.com"
$bootstrapJob = "oracle-supply-chain-user-bootstrap"
$setupJob = "oracle-supply-chain-db-setup"

function Invoke-Gcloud {
    & $gcloud @args
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: $($args -join ' ')"
    }
}

function Invoke-GcloudWithRetry {
    $arguments = @($args)
    for ($attempt = 1; $attempt -le 12; $attempt++) {
        & $gcloud @arguments
        if ($LASTEXITCODE -eq 0) {
            return
        }
        if ($attempt -lt 12) {
            Start-Sleep -Seconds 5
        }
    }
    throw "gcloud command failed after retries: $($arguments -join ' ')"
}

function Test-GcloudResource {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $gcloud @args 2> $null | Out-Null
        return $LASTEXITCODE -eq 0
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Ensure-Secret([string]$Name) {
    if (-not (Test-GcloudResource `
            secrets describe $Name --project=$ProjectId)) {
        Invoke-Gcloud secrets create $Name `
            --project=$ProjectId `
            --replication-policy=automatic
    }
}

function Read-EnvironmentFile([string]$Path) {
    $result = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match "^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$") {
            $value = $matches[2].Trim()
            if ($value.Length -ge 2) {
                $first = $value[0]
                $last = $value[$value.Length - 1]
                if (($first -eq '"' -and $last -eq '"') -or
                        ($first -eq "'" -and $last -eq "'")) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
            }
            $result[$matches[1]] = $value
        }
    }
    return $result
}

$configured = Read-EnvironmentFile $environmentFile
$adminUsername = $configured["DB_USERNAME"]
$adminPassword = $configured["DB_PASSWORD"]
if ([string]::IsNullOrWhiteSpace($adminUsername) -or
        [string]::IsNullOrWhiteSpace($adminPassword)) {
    throw "DB_USERNAME and DB_PASSWORD must be configured in .env."
}
if ($adminUsername.ToUpperInvariant() -ne "ADMIN") {
    throw (
        "The one-time bootstrap credential must be ADMIN; configured user is " +
        "$adminUsername.")
}

$passwordBytes = [byte[]]::new(36)
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($passwordBytes)
}
finally {
    $random.Dispose()
}
$applicationPassword = (
    [Convert]::ToBase64String($passwordBytes)
).Replace("+", "A").Replace("/", "b").TrimEnd("=")

$temporaryRoot = Join-Path $projectRoot (
    ".tmp-gcp-db-bootstrap-" + [guid]::NewGuid().ToString("N"))
$adminPasswordFile = Join-Path $temporaryRoot "admin-password.txt"
$applicationPasswordFile =
    Join-Path $temporaryRoot "application-password.txt"
$bootstrapJobCreated = $false

try {
    New-Item -ItemType Directory -Path $temporaryRoot | Out-Null
    [IO.File]::WriteAllText(
        $adminPasswordFile,
        $adminPassword,
        [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText(
        $applicationPasswordFile,
        $applicationPassword,
        [Text.UTF8Encoding]::new($false))

    Ensure-Secret $AdminBootstrapSecret
    Ensure-Secret $ApplicationPasswordSecret
    Invoke-Gcloud secrets versions add $AdminBootstrapSecret `
        --project=$ProjectId --data-file=$adminPasswordFile
    Invoke-Gcloud secrets versions add $ApplicationPasswordSecret `
        --project=$ProjectId --data-file=$applicationPasswordFile

    if (-not (Test-GcloudResource `
            iam service-accounts describe $serviceAccountEmail `
            --project=$ProjectId)) {
        Invoke-Gcloud iam service-accounts create $RuntimeServiceAccount `
            --project=$ProjectId `
            --display-name="Gemini Enterprise A2UI runtime"
    }

    foreach ($secret in @(
            $WalletSecret,
            $AdminBootstrapSecret,
            $ApplicationPasswordSecret)) {
        Invoke-GcloudWithRetry secrets add-iam-policy-binding $secret `
            --project=$ProjectId `
            --member="serviceAccount:$serviceAccountEmail" `
            --role=roles/secretmanager.secretAccessor
    }

    Invoke-Gcloud run jobs deploy $bootstrapJob `
        --project=$ProjectId `
        --region=$Region `
        --image=$Image `
        --service-account=$serviceAccountEmail `
        --command=/usr/local/bin/database-job-entrypoint `
        "--args=java,-cp,/opt/app/interactive-ai-agent-service.jar,com.oracle.demo.interactiveai.DatabaseUserBootstrap" `
        --set-env-vars="DB_SERVICE_NAME=$DatabaseServiceName,DB_USERNAME=$adminUsername,DB_APPLICATION_USERNAME=$ApplicationUsername,DB_POOL_NAME=InteractiveAiBootstrapUcpPool" `
        --set-secrets="/var/run/secrets/oracle-wallet/wallet.zip=$WalletSecret`:latest,DB_PASSWORD=$AdminBootstrapSecret`:latest,DB_APPLICATION_PASSWORD=$ApplicationPasswordSecret`:latest" `
        --network=default `
        --subnet=default `
        --vpc-egress=private-ranges-only `
        --max-retries=0 `
        --task-timeout=300
    $bootstrapJobCreated = $true
    Invoke-Gcloud run jobs execute $bootstrapJob `
        --project=$ProjectId --region=$Region --wait

    Invoke-Gcloud run jobs deploy $setupJob `
        --project=$ProjectId `
        --region=$Region `
        --image=$Image `
        --service-account=$serviceAccountEmail `
        --command=/usr/local/bin/database-job-entrypoint `
        "--args=java,-Ddatabase.root=/opt/app/database,-cp,/opt/app/interactive-ai-agent-service.jar,com.oracle.demo.interactiveai.DatabaseSetup" `
        --set-env-vars="DB_SERVICE_NAME=$DatabaseServiceName,DB_USERNAME=$ApplicationUsername,DB_POOL_NAME=InteractiveAiSetupUcpPool" `
        --set-secrets="/var/run/secrets/oracle-wallet/wallet.zip=$WalletSecret`:latest,DB_PASSWORD=$ApplicationPasswordSecret`:latest" `
        --network=default `
        --subnet=default `
        --vpc-egress=private-ranges-only `
        --max-retries=0 `
        --task-timeout=300
    Invoke-Gcloud run jobs execute $setupJob `
        --project=$ProjectId --region=$Region --wait

    $environmentText = Get-Content -LiteralPath $environmentFile -Raw
    $environmentText = [regex]::Replace(
        $environmentText,
        "(?m)^DB_USERNAME=.*$",
        "DB_USERNAME=$ApplicationUsername")
    $environmentText = [regex]::Replace(
        $environmentText,
        "(?m)^DB_PASSWORD=.*$",
        "DB_PASSWORD=$applicationPassword")
    [IO.File]::WriteAllText(
        $environmentFile,
        $environmentText,
        [Text.UTF8Encoding]::new($false))

    Write-Host (
        "Created $ApplicationUsername, installed the supply-chain schema, " +
        "and updated the ignored local .env.")
}
finally {
    $applicationPassword = $null
    $adminPassword = $null
    foreach ($file in @($adminPasswordFile, $applicationPasswordFile)) {
        if (Test-Path -LiteralPath $file) {
            [IO.File]::WriteAllText(
                $file,
                "",
                [Text.UTF8Encoding]::new($false))
        }
    }
    if (Test-Path -LiteralPath $temporaryRoot) {
        $resolvedTemporary = (Resolve-Path -LiteralPath $temporaryRoot).Path
        if (-not $resolvedTemporary.StartsWith(
                $projectRoot.Path +
                [IO.Path]::DirectorySeparatorChar)) {
            throw "Refusing to remove a temporary path outside the project."
        }
        Remove-Item -LiteralPath $resolvedTemporary -Recurse -Force
    }
    if ($bootstrapJobCreated) {
        & $gcloud run jobs delete $bootstrapJob `
            --project=$ProjectId --region=$Region --quiet
    }
    if (Test-GcloudResource `
            secrets describe $AdminBootstrapSecret --project=$ProjectId) {
        & $gcloud secrets delete $AdminBootstrapSecret `
            --project=$ProjectId --quiet
    }
}
