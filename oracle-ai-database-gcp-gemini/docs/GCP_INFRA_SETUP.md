# GCP Infrastructure Setup

This runbook is for setting up the Oracle AI Database demo infrastructure in a different GCP project.

Current migration target:

- source project: `adb-pm-prod`
- target project: `oracle-public-488519`

This document starts with the compute instance because that is the most important infrastructure dependency for the current demo.

## Scope

This is a setup and migration guide, not a record of work already completed in the target project.

The immediate goal is:

- stand up a new compute instance
- deploy the Java runtime there
- expose the HTTPS agent endpoints
- keep the same demo shape working in Gemini Enterprise

## Can This Project Be Set Up In `oracle-public-488519`?

Yes.

The current demo is portable as long as the new project has:

- a Compute Engine VM
- an external IP
- firewall rules for `22`, `80`, and `443`
- the repo checked out on the VM
- Java and build tooling installed
- the Oracle wallet and database connectivity configured
- HTTPS configured with a trusted certificate
- any required Google ADC and service account access configured

## Phase 1: Compute Instance

### Recommended VM Shape

Use a VM sized for a Java service plus build tooling:

- OS: Ubuntu LTS
- machine type: `n2-standard-8` or similarly powerful, clearly larger than `e2-standard-4`
- boot disk: `500GB`
- external IP: static if possible
- network tags:
  - `allow-ssh`
  - `allow-http`
  - `allow-https`

### Recommended Firewall Rules

Allow ingress for:

- TCP `22` for SSH
- TCP `80` for Let's Encrypt issuance and renewal
- TCP `443` for the live HTTPS agent endpoints

Source ranges:

- `0.0.0.0/0` for public demo access to `443`
- preferably your public IP `/32` for `22` if you want a tighter SSH posture

### Enable Required APIs

At minimum:

- Compute Engine API
- IAM API
- Service Usage API
- Cloud Resource Manager API
- Logging API

If you want the ADK path later:

- Vertex AI API

### Suggested Service Account

Create a dedicated VM service account instead of relying only on user ADC.

Minimum likely roles:

- logging writer
- monitoring metric writer
- vertex ai user, if the ADK path should run on the VM

Keep Oracle DB secrets and the wallet out of the repo.

## Example Compute Setup Commands

These commands are a template and still require your exact region, zone, subnet, and naming choices.

```bash
gcloud config set project oracle-public-488519

gcloud compute addresses create oracle-demo-agent-ip \
  --region=YOUR_REGION

gcloud compute instances create oracle-demo-agent-vm \
  --project=oracle-public-488519 \
  --zone=YOUR_ZONE \
  --machine-type=n2-standard-8 \
  --subnet=default \
  --address=oracle-demo-agent-ip \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=500GB \
  --tags=allow-ssh,allow-http,allow-https \
  --service-account=YOUR_VM_SERVICE_ACCOUNT \
  --scopes=https://www.googleapis.com/auth/cloud-platform
```

Firewall example:

```bash
gcloud compute firewall-rules create allow-oracle-demo-https \
  --project=oracle-public-488519 \
  --network=default \
  --direction=INGRESS \
  --action=ALLOW \
  --rules=tcp:443 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=allow-https
```

Repeat similarly for `tcp:80` and `tcp:22`.

## VM Bootstrap Checklist

Once SSH access works, install:

- `git`
- `curl`
- `unzip`
- `jq`
- `openjdk-17-jdk`
- `maven`
- `python3`
- `python3-venv`
- `certbot`

Important note from the current VM:

- the remote build failed when `mvn` was not on `PATH`
- so in the new project, install Maven explicitly instead of assuming it is present

Suggested bootstrap:

```bash
sudo apt-get update
sudo apt-get install -y git curl unzip jq openjdk-17-jdk maven python3 python3-venv certbot
java -version
mvn -version
```

## Repo And Runtime Setup

On the new VM:

1. clone the repo
2. create the shared `.env`
3. copy the Oracle wallet into a secure path outside the repo if preferred
4. verify DB connectivity
5. build the Java runtime
6. configure HTTPS
7. run the Spring Boot service under `systemd`

Suggested paths:

- repo: `/home/YOUR_VM_SSH_USER/oracle-ai-for-sustainable-dev`
- runtime dir: `/home/YOUR_VM_SSH_USER/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-gemini/oracle_agent_java`

## Required Secret And Config Inputs

You will need to provide these on the new VM:

- shared `.env`
- Oracle DB connect string or DSN
- Oracle DB username
- Oracle DB password
- wallet directory
- public host or public IP
- Let's Encrypt certificate path or issuance flow
- `SELECT_AI_PROFILE` if Select AI should be live
- ADC or service account access if the ADK path should be live

## HTTPS Setup

For the new VM, reuse the same pattern already documented in:

- [oracle_agent_java/HTTPS_SETUP.md](../oracle_agent_java/HTTPS_SETUP.md)

High-level flow:

1. ensure inbound `80` works
2. issue a trusted cert
3. sync the cert into a user-readable location if needed
4. start the Java service on `443`
5. verify the agent-card URLs publicly

## Systemd Deployment

The Java runtime should be run as a long-lived service, not just a background shell process.

Use the same deployment pattern already described in:

- [oracle_agent_java/README.md](../oracle_agent_java/README.md)

Recommended verification after deployment:

- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`
- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`
- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`
- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`

## Phase 2 And Beyond

After the compute instance is working, the next infrastructure layers are:

- Oracle wallet and DB connectivity
- shared `.env` population
- Select AI profile verification
- ADC or service-account configuration for ADK
- Gemini Enterprise agent re-import in the new environment

## Open Migration Questions

Before doing the full move, confirm:

- target region and zone
- whether the new VM should use a static public IP
- whether a new DNS name should be used instead of direct IP HTTPS
- whether the Oracle wallet should stay on disk or move into Secret Manager plus local materialization
- whether the VM should use user ADC or a proper service account for Vertex/ADK
