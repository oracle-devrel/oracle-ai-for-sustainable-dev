# Gemini Enterprise deployment on Google Cloud

This runbook deploys the supply-chain A2A/A2UI adapter and its private Java
service as one Cloud Run container. It targets the existing Google Cloud and
Oracle Database@Google Cloud resources validated on 2026-08-01:

- Google Cloud project: `adb-pm-prod`
- region: `us-east4`
- Autonomous Database: `paulparkdb` (`26ai` in OCI, `OLTP`)
- wallet service: `paulparkdb_tp`
- Gemini Enterprise app: `Inventory System`
- Gemini Enterprise app ID: `inventory-system_1775523930395`
- Gemini Enterprise location: `global`

The existing `paulpark-instance` VM remains separate from this deployment.

## Why Cloud Run instead of another VM

Gemini Enterprise requires a reachable HTTPS A2A endpoint, not a VM. Cloud Run
provides managed HTTPS and an IAM-protected service URL for agent discovery.
The selected `paulparkdb` private endpoint is reachable through Cloud Run
Direct VPC egress on the project's `default` network and subnet, so this
deployment does not need a new VM or Serverless VPC Access connector.

Oracle Database@Google Cloud requires the ODB network and associated VPC to be
in the same project. A VM in that VPC remains an alternative hosting model,
but is not required for this service.

## Runtime and secret boundaries

The container exposes only the Python A2A adapter on Cloud Run's `PORT`.
The Java service listens on loopback port 8081 and reaches the independently
running Oracle Database MCP Java Toolkit through authenticated Streamable HTTP:

```text
Gemini Enterprise
  -> Cloud Run HTTPS / A2A v0.3 / A2UI v0.8
  -> Python adapter :8080
  -> Java review and approval service 127.0.0.1:8081
  -> Oracle Database MCP Java Toolkit over authenticated Streamable HTTP
  -> paulparkdb_tp over wallet-authenticated TCPS
```

The wallet ZIP, `FINANCIAL` password, and local Deep Data Security
`ENVIRONMENTAL_PLANNER` password are separate Secret Manager secrets. They are excluded from the Cloud Build upload by
`.gcloudignore`. The runtime service account receives Secret Manager access;
no database secret is built into the image or stored in Git.

The deployment deliberately sets `--max-instances=1` because approval handles
are currently in memory. Production should move approvals and idempotency
records to a durable store before enabling multiple instances.

## Prerequisites

1. Authenticate `gcloud` as `paul.parkinson@oracle.com`.
2. Activate the dedicated `a2ui-adb-pm-prod` gcloud configuration.
3. Put the one-time `ADMIN` credential in the ignored local `.env` without
   surrounding quote characters.
4. Run `deploy/gcp/initialize-database.ps1`. It creates the dedicated
   `FINANCIAL` user, installs the schema, stores its generated password in
   Secret Manager, updates the ignored local `.env`, and deletes the temporary
   ADMIN secret and bootstrap job.

Do not deploy with the `ADMIN` database user.

## Stage secrets

From PowerShell:

```powershell
cd a2ui_mcpapps_mcptoolkit
.\deploy\gcp\prepare-secrets.ps1
```

The script packages `Wallet_PAULPARKDB`, prompts securely for the `FINANCIAL`
passwords, creates secret versions, and erases its temporary plaintext file.

## Build and deploy

Deploy with Cloud Run IAM authentication (the script's default):

```powershell
.\deploy\gcp\deploy-cloud-run.ps1
```

Grant `roles/run.invoker` on this service only to Gemini Enterprise's Discovery
Engine service agent, `service-1093068138027@gcp-sa-discoveryengine.iam.gserviceaccount.com`.
OAuth client credentials are not required because the agent does not access
Google resources on behalf of an end user. No general SQL tool, wallet,
password, Java port, or Toolkit transport is public.

Verify:

```powershell
$serviceUrl = gcloud run services describe oracle-supply-chain-a2ui `
  --project=adb-pm-prod --region=us-east4 `
  --format="value(status.url)"
$identityToken = gcloud auth print-identity-token
Invoke-RestMethod "$serviceUrl/.well-known/agent-card.json" `
  -Headers @{ Authorization = "Bearer $identityToken" }
```

## Register in Gemini Enterprise

After the live recommendation and approval paths pass:

```powershell
.\deploy\gcp\register-gemini-enterprise.ps1 `
  -AgentBaseUrl $serviceUrl
```

The registration script uses the official Discovery Engine
`assistants/default_assistant/agents` REST resource. It reads the deployed
agent card, checks A2A protocol `0.3.0`, refuses to create a duplicate display
name, and registers it in the existing `Inventory System` app.

Validated deployment evidence on 2026-08-01:

- Cloud Run service: `oracle-supply-chain-a2ui`
- active tested endpoint: `https://oracle-supply-chain-a2ui-54d5grsrkq-uk.a.run.app`
- Gemini Enterprise agent ID: `10914982377308290813`
- anonymous agent-card request: HTTP `403`
- authenticated agent-card request: HTTP `200`
- registered state: `ENABLED`
- read-only A2A smoke test: result returned with A2UI v0.8 content and no error

## Bind authenticated users to Deep Data Security profiles

Do not select a database profile from prompt text. Deploy the same image as two
private Cloud Run services and set an immutable profile on each service:

- `TRUSTED_ACCESS_PROFILE=full` for the full planner
- `TRUSTED_ACCESS_PROFILE=environmental` for the read-only environmental planner

Register each service as a separate Gemini Enterprise A2A agent. In the Gemini
Enterprise console, open each agent's **User permissions** tab. Grant the full
agent only to the privileged user or group and grant the environmental agent to
the restricted user or group. The adapter ignores `accessProfile` values from
prompts and A2UI actions. The selected, permissioned endpoint chooses the
Toolkit session, and Oracle Deep Data Security performs the row filtering.

## Source material

- [Google Cloud: Gemini Enterprise and A2UI integration guide](https://cloud.google.com/blog/topics/developers-practitioners/guide-to-gemini-enterprise-and-a2ui-integration)
- [Google Cloud: register and manage an A2UI agent](https://docs.cloud.google.com/gemini/enterprise/docs/a2ui-agents/register-and-manage-an-a2ui-agent)
- [Google Cloud: register and manage an A2A agent](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
- [Google Cloud: deploy A2A agents to Cloud Run](https://docs.cloud.google.com/run/docs/deploy-a2a-agents)
- [Google Cloud: host an A2UI agent with Cloud Run](https://docs.cloud.google.com/gemini/enterprise/docs/a2ui-agents/tutorial-host-agent-cloud-run)
- [Google A2UI source and examples](https://github.com/google/A2UI)
- [A2UI reference implementation linked by Google](https://github.com/wadave/agent-a2ui-demo)
- [Google Cloud: Direct VPC egress](https://docs.cloud.google.com/run/docs/configuring/vpc-direct-vpc)
- [Oracle: create an ODB network for Oracle Database@Google Cloud](https://docs.oracle.com/en-us/iaas/Content/database-at-gcp-autonomous/ogadb-task-1-creating-odb-network-oracle-databasegoogle.html)
- [Oracle: Oracle Database@Google Cloud network topologies](https://docs.oracle.com/en-us/iaas/Content/database-at-gcp/network-topologies.htm)
