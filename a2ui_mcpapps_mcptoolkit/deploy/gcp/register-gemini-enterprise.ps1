[CmdletBinding()]
param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Location = "global",
    [string]$AppId = "inventory-system_1775523930395",
    [Parameter(Mandatory = $true)]
    [string]$AgentBaseUrl,
    [string]$DisplayName = "Oracle Supply-Chain A2UI"
)

$ErrorActionPreference = "Stop"
$gcloud = "C:\Users\paulp\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
$endpointLocation = if ($Location -eq "global") { "global" } else { $Location }
$agentsUrl = (
    "https://$endpointLocation-discoveryengine.googleapis.com/v1alpha/" +
    "projects/$ProjectId/locations/$Location/collections/" +
    "default_collection/engines/$AppId/assistants/" +
    "default_assistant/agents")

$identityToken = & $gcloud auth print-identity-token
if ($LASTEXITCODE -ne 0) {
    throw "Unable to obtain a Google Cloud identity token."
}
$agentCard = Invoke-RestMethod -Method Get -Uri (
    $AgentBaseUrl.TrimEnd("/") + "/.well-known/agent-card.json") `
    -Headers @{ Authorization = "Bearer $identityToken" }
if ($agentCard.protocolVersion -ne "0.3.0") {
    throw "Gemini Enterprise requires the A2A v0.3 compatibility card."
}

$token = & $gcloud auth print-access-token
if ($LASTEXITCODE -ne 0) {
    throw "Unable to obtain a Google Cloud access token."
}
$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
    "X-Goog-User-Project" = $ProjectId
}

$existing = Invoke-RestMethod -Method Get -Uri $agentsUrl -Headers $headers
$duplicate = @($existing.agents) |
    Where-Object { $_.displayName -eq $DisplayName }
if ($duplicate) {
    throw (
        "An agent named '$DisplayName' is already registered. " +
        "Update or remove it explicitly rather than creating a duplicate.")
}

$payload = @{
    name = "oracle-supply-chain-a2ui"
    displayName = $DisplayName
    description = (
        "Oracle-governed inventory recommendations and explicit " +
        "A2UI approval before a database write.")
    a2aAgentDefinition = @{
        jsonAgentCard = ($agentCard | ConvertTo-Json -Depth 30 -Compress)
    }
} | ConvertTo-Json -Depth 30

$created = Invoke-RestMethod -Method Post -Uri $agentsUrl `
    -Headers $headers -Body $payload
Write-Host "Registered Gemini Enterprise agent: $($created.name)"
