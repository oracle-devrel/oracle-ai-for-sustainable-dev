$ErrorActionPreference = "Stop"

$ProviderVersion = if ($env:OJDBC_PROVIDER_VERSION) { $env:OJDBC_PROVIDER_VERSION } else { "1.1.0" }
$ProviderCoordinate = "com.oracle.database.jdbc:ojdbc-provider-spring:$ProviderVersion"
$ExtensionsRepoUrl = if ($env:OJDBC_EXTENSIONS_REPO_URL) { $env:OJDBC_EXTENSIONS_REPO_URL } else { "https://github.com/oracle/ojdbc-extensions.git" }
$ExtensionsRef = if ($env:OJDBC_EXTENSIONS_REF) { $env:OJDBC_EXTENSIONS_REF } else { "main" }
$TempRoot = if ($env:TEMP) { $env:TEMP } else { "/tmp" }
$ExtensionsWorkdir = if ($env:OJDBC_EXTENSIONS_WORKDIR) { $env:OJDBC_EXTENSIONS_WORKDIR } else { Join-Path $TempRoot "ojdbc-extensions-deepsec" }

mvn -q dependency:get "-Dartifact=$ProviderCoordinate" "-Dtransitive=false" *> $null
if ($LASTEXITCODE -eq 0) {
    exit 0
}

Write-Host "ojdbc-provider-spring $ProviderVersion was not found in Maven Central/local cache."
Write-Host "Installing it from $ExtensionsRepoUrl ($ExtensionsRef) into the local Maven cache."

$GitProxyArgs = @()
if ($env:OJDBC_EXTENSIONS_USE_CONFIGURED_GIT_PROXY -ne "true") {
    $GitProxyArgs = @("-c", "http.proxy=", "-c", "https.proxy=")
}

if (Test-Path (Join-Path $ExtensionsWorkdir ".git")) {
    git @GitProxyArgs -C $ExtensionsWorkdir fetch --depth 1 origin $ExtensionsRef
    git @GitProxyArgs -C $ExtensionsWorkdir checkout FETCH_HEAD
}
else {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ExtensionsWorkdir) | Out-Null
    git @GitProxyArgs clone --depth 1 --branch $ExtensionsRef $ExtensionsRepoUrl $ExtensionsWorkdir
}

$ExtensionsPom = Join-Path $ExtensionsWorkdir "pom.xml"

mvn -f $ExtensionsPom `
    -pl ojdbc-provider-spring `
    -am `
    -DskipTests `
    -Dmaven.javadoc.skip=true `
    install

mvn -q dependency:get "-Dartifact=$ProviderCoordinate" "-Dtransitive=false" *> $null
