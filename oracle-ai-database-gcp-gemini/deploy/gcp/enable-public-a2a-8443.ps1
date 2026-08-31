param(
    [string]$ProjectId = "adb-pm-prod",
    [string]$Zone = "us-east4-a",
    [string]$InstanceName = "paulpark-instance",
    [string]$NetworkTag = "oracle-gemini-a2a",
    [string]$FirewallRule = "allow-oracle-gemini-a2a-8443"
)

$ErrorActionPreference = "Stop"

$instance = gcloud compute instances describe $InstanceName `
    --project=$ProjectId `
    --zone=$Zone `
    --format=json | ConvertFrom-Json

if ($instance.name -ne $InstanceName -or $instance.zone -notlike "*/$Zone") {
    throw "Resolved instance does not match the requested name and zone."
}

$networkName = ($instance.networkInterfaces[0].network -split "/")[-1]

gcloud compute instances add-tags $InstanceName `
    --project=$ProjectId `
    --zone=$Zone `
    --tags=$NetworkTag

gcloud compute firewall-rules describe $FirewallRule `
    --project=$ProjectId `
    --format=json 2>$null | Out-Null

if ($LASTEXITCODE -ne 0) {
    gcloud compute firewall-rules create $FirewallRule `
        --project=$ProjectId `
        --network=$networkName `
        --direction=INGRESS `
        --action=ALLOW `
        --rules=tcp:8443 `
        --source-ranges=0.0.0.0/0 `
        --target-tags=$NetworkTag `
        --description="Public TLS A2A endpoints on paulpark-instance only"
}

gcloud compute firewall-rules describe $FirewallRule `
    --project=$ProjectId `
    --format="yaml(name,network,direction,sourceRanges,allowed,targetTags,disabled)"
