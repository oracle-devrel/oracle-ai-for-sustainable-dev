[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$WalletDirectory =
        "C:\Users\paulp\Downloads\Wallet_PAULPARKDB",
    [string]$WalletSecret = "a2ui-paulparkdb-wallet",
    [string]$DatabasePasswordSecret = "a2ui-paulparkdb-financial-password",
    [string]$RestrictedDatabasePasswordSecret =
        "a2ui-paulparkdb-environmental-planner-password"
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"

if (-not (Test-Path -LiteralPath "$WalletDirectory\tnsnames.ora")) {
    throw "Wallet directory does not contain tnsnames.ora: $WalletDirectory"
}

function Invoke-Gcloud {
    & $gcloud @args
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: $($args -join ' ')"
    }
}

function Ensure-Secret([string]$Name) {
    & $gcloud secrets describe $Name --project=$ProjectId *> $null
    if ($LASTEXITCODE -ne 0) {
        Invoke-Gcloud secrets create $Name `
            --project=$ProjectId `
            --replication-policy=automatic
    }
}

Invoke-Gcloud config set account paul.parkinson@oracle.com
Invoke-Gcloud config set project $ProjectId
Invoke-Gcloud services enable secretmanager.googleapis.com --project=$ProjectId

Ensure-Secret $WalletSecret
Ensure-Secret $DatabasePasswordSecret
Ensure-Secret $RestrictedDatabasePasswordSecret

$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) (
    "a2ui-gcp-secrets-" + [guid]::NewGuid().ToString("N"))
$walletArchive = Join-Path $temporaryRoot "wallet.zip"
$passwordFile = Join-Path $temporaryRoot "db-password.txt"

try {
    New-Item -ItemType Directory -Path $temporaryRoot | Out-Null
    Compress-Archive -Path (Join-Path $WalletDirectory "*") `
        -DestinationPath $walletArchive
    Invoke-Gcloud secrets versions add $WalletSecret `
        --project=$ProjectId `
        --data-file=$walletArchive

    $securePassword = Read-Host `
        "Enter the FINANCIAL password for paulparkdb" -AsSecureString
    $credential = [pscredential]::new("FINANCIAL", $securePassword)
    [IO.File]::WriteAllText(
        $passwordFile,
        $credential.GetNetworkCredential().Password,
        [Text.UTF8Encoding]::new($false))
    Invoke-Gcloud secrets versions add $DatabasePasswordSecret `
        --project=$ProjectId `
        --data-file=$passwordFile

    $restrictedSecurePassword = Read-Host `
        "Enter the ENVIRONMENTAL_PLANNER password for paulparkdb" `
        -AsSecureString
    $restrictedCredential = [pscredential]::new(
        "ENVIRONMENTAL_PLANNER", $restrictedSecurePassword)
    [IO.File]::WriteAllText(
        $passwordFile,
        $restrictedCredential.GetNetworkCredential().Password,
        [Text.UTF8Encoding]::new($false))
    Invoke-Gcloud secrets versions add $RestrictedDatabasePasswordSecret `
        --project=$ProjectId `
        --data-file=$passwordFile
}
finally {
    if (Test-Path -LiteralPath $passwordFile) {
        [IO.File]::WriteAllText(
            $passwordFile,
            "",
            [Text.UTF8Encoding]::new($false))
    }
    if (Test-Path -LiteralPath $temporaryRoot) {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
}

Write-Host "Wallet and both database password versions are staged in Secret Manager."
