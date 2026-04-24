# GCP Setup Progress

This log records the actual migration work toward recreating the Oracle AI Database demo in the `oracle-public-488519` GCP project.

## 2026-04-12

### Completed locally

- Moved the Oracle demo runbooks into `oracle-ai-database-gcp-vertex-ai/docs/` so the project README can act as the main documentation hub.
- Added a first-class `/graph` A2A controller to the shared Java runtime so the graph agent now has a canonical path that matches the other specialist agents.
- Updated the graph card builder so the graph agent advertises `/graph` as its execution URL.
- Kept the legacy root graph surface available as a backward-compatibility fallback.
- Rebuilt the Java runtime locally and confirmed the project still compiles successfully.
- Backed up the current shared environment file to:
  - `oracle-ai-database-gcp-vertex-ai/.env_adb=pm=prod`

### GCP access checks performed from this workstation

- Confirmed `gcloud` is installed.
- Confirmed the current local `gcloud` config project is still `adb-pm-prod`.
- Hit a local permissions issue when trying to read or update some `gcloud` auth state inside the sandboxed environment because `~/.config/gcloud/credentials.db` was not writable in that mode.
- Re-ran the `gcloud` checks with elevated permissions and confirmed the configured account is `paul.parkinson@oracle.com`.
- Confirmed the active login token is stale right now because `gcloud projects describe oracle-public-488519` returned `invalid_grant`.
- Completed a fresh `gcloud auth login --no-launch-browser` flow and restored local project access successfully.

### Source-project baseline captured

- Source project used for the existing live demo: `adb-pm-prod`
- Current demo VM used as the closest baseline: `paulpark-instance`
- Source VM zone: `us-east4-a`
- Source VM network: `default`
- Source VM tags: `http-server`, `https-server`

### Target resources created in `oracle-public-488519`

- Static public IP:
  - name: `oracle-agent-public-ip`
  - region: `us-east4`
  - address: `34.186.79.96`
  - status: `IN_USE`

- Firewall rules:
  - `allow-oracle-agent-http`
    - network: `default`
    - ingress: `tcp:80`
    - source ranges: `0.0.0.0/0`
    - target tag: `http-server`
  - `allow-oracle-agent-https`
    - network: `default`
    - ingress: `tcp:443`
    - source ranges: `0.0.0.0/0`
    - target tag: `https-server`
  - Existing SSH rule already present in the project:
    - `default-allow-ssh`
    - ingress: `tcp:22`

- Compute instance:
  - name: `oracle-agent-vm`
  - project: `oracle-public-488519`
  - zone: `us-east4-a`
  - machine type: `n2-standard-8`
  - boot disk: `500GB`
  - internal IP: `10.150.0.2`
  - external IP: `34.186.79.96`
  - status: `RUNNING`
  - tags: `http-server`, `https-server`
  - service account: `329920210012-compute@developer.gserviceaccount.com`
  - OAuth scope: `cloud-platform`

### SSH/bootstrap readiness

- Verified SSH access from this workstation to `oracle-agent-vm`.
- Verified the remote OS account resolved to `pparkins`.
- Verified the root filesystem is already expanded and available at roughly `485G`, so the `500GB` boot disk is effectively usable without an extra manual resize step.

### Bootstrap progress completed on the VM

- Installed baseline packages:
  - `git`
  - `curl`
  - `unzip`
  - `jq`
  - `openjdk-17-jdk`
  - `maven`
  - `python3`
  - `python3-venv`
  - `certbot`
- Verified runtime tools:
  - Java: `17.0.18`
  - Maven: `3.6.3`
  - Apt-packaged Certbot: `1.21.0`
- Noted that the Ubuntu-packaged Certbot was too old for the newer IP-certificate flow used by this demo.
- Installed the newer isolated Certbot layout under `/opt/certbot` and linked it into `/usr/local/bin/certbot`.
- Verified upgraded Certbot version:
  - `certbot 5.5.0`

### Repo bootstrap completed on the VM

- Cloned the repo to:
  - `/home/pparkins/oracle-ai-for-sustainable-dev`
- Verified cloned revision:
  - `1869946`

### Oracle AI Database 26ai provisioning started

- Switched the local `gcloud` default project to:
  - `oracle-public-488519`
- Verified Oracle Database@Google Cloud entitlement is active in the target project.
- Verified `26ai` Autonomous Database versions are available in `us-east4`, including the `DW` workload.
- Issued a create request for a new Autonomous AI Database:
  - resource id: `oracle-agent-26ai`
  - display name: `oracle-agent-26ai`
  - region: `us-east4`
  - workload: `DW` (lakehouse-style warehouse shape)
  - database version: `26ai`
  - compute count: `2`
  - storage size: `1 TiB`
  - network: `projects/oracle-public-488519/global/networks/default`
  - client CIDR: `192.168.100.0/24`
- Confirmed the database resource now exists and is currently in:
  - `PROVISIONING`
- Observed that the service auto-created an ODB network for the new database:
  - `projects/oracle-public-488519/locations/us-east4/odbNetworks/odbnetwork-oracle-agent-26ai-default-968fc0`

### Oracle DB secret staging on the VM

- Created private staging directories on the new VM:
  - `/home/pparkins/.oracle-agent-secrets`
  - `/home/pparkins/wallets`
- Saved the generated ADMIN password to a private file on the new VM:
  - `/home/pparkins/.oracle-agent-secrets/oracle-agent-26ai-admin-password`
- Kept the password out of repo files and out of this progress document.

### Resume after local interruption

- Re-checked the database state and confirmed `oracle-agent-26ai` is now:
  - `AVAILABLE`
- Confirmed the wallet had already been generated on the VM under:
  - `/home/pparkins/wallets/oracle-agent-26ai`
- Verified the wallet directory contains the expected files, including:
  - `cwallet.sso`
  - `ewallet.p12`
  - `tnsnames.ora`
  - `sqlnet.ora`
  - `wallet.zip`
- Created the shared VM environment file at:
  - `/home/pparkins/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/.env`
- Pointed that VM `.env` at the new target resources:
  - `GOOGLE_CLOUD_PROJECT=oracle-public-488519`
  - `DB_DSN=oracleagent26ai_medium`
  - `DB_WALLET_DIR=/home/pparkins/wallets/oracle-agent-26ai`
  - `TNS_ADMIN=/home/pparkins/wallets/oracle-agent-26ai`
  - `GRAPH_DATA_MODE=database`
- Ran the graph schema setup wrapper on the VM:
  - `sql/run_supply_chain_graph_setup.sh`
- Confirmed the graph objects now exist in the new database, including:
  - `SUPPLY_CHAIN_GRAPH`
  - the `SC_*` demo tables
- Ran the graph seed wrapper on the VM:
  - `sql/run_supply_chain_graph_seed.sh`
- Verified the seeded graph path for `SKU-500` in the new database:
  - `Vertex Plastics -> Columbus Final Pack -> Houston -> Newark Inventory Hub`
- Ran the inventory-risk schema and seed SQL on the VM for the spatial and Select AI demo tables.
- Built the Java runtime successfully on the VM:
  - build result: `BUILD SUCCESS`
  - note: the first VM build took about 64 minutes because Maven had to populate a cold dependency cache
- Started the Java runtime locally on the VM on port:
  - `8081`
- Verified local graph-agent discovery and action successfully against the new database:
  - discovery URL: `http://127.0.0.1:8081/graph/.well-known/agent-card.json`
  - test harness: `GRAPH_AGENT_URL=http://127.0.0.1:8081 ./test.sh`
  - outcome: completed successfully with a database-backed response for `SKU-500`
- Saved a rendered verification artifact on the VM at:
  - `/home/pparkins/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/test-output/supply-chain-graph.png`
- Enabled the `networkmanagement.googleapis.com` API during SSH troubleshooting to confirm network reachability to the VM.

### Public HTTPS cutover completed

- Updated the VM `.env` from local validation mode to the final public HTTPS settings:
  - `PUBLIC_PROTOCOL=https`
  - `PUBLIC_HOST=34.186.79.96`
  - `PORT=443`
  - `GRAPH_AGENT_PORT=443`
  - `SPATIAL_AGENT_PORT=443`
- Issued a trusted IP certificate for `34.186.79.96` with the newer Certbot flow:
  - certificate name: `oracle-graph-agent-ip`
  - live cert path: `/etc/letsencrypt/live/oracle-graph-agent-ip/fullchain.pem`
  - live key path: `/etc/letsencrypt/live/oracle-graph-agent-ip/privkey.pem`
  - observed expiry after issuance: `2026-04-19`
- Started the shared Java runtime as a transient `systemd` service on:
  - `443`
- Stopped the temporary validation-only `8081` process after the public HTTPS service was up.
- Verified the public card surfaces respond over HTTPS:
  - `https://34.186.79.96/graph/.well-known/agent-card.json`
  - `https://34.186.79.96/agent-card-graph.json`
  - `https://34.186.79.96/agent-card-spatial.json`
  - `https://34.186.79.96/agent-card-select-ai.json`
  - `https://34.186.79.96/agent-card-action.json`

### Public agent verification after cutover

- Verified the public graph agent end to end with:
  - `GRAPH_AGENT_URL=https://34.186.79.96 ./test.sh`
- Confirmed the graph agent returns a live database-backed result for `SKU-500` over public HTTPS and writes a rendered PNG artifact on the VM at:
  - `/home/pparkins/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/test-output/supply-chain-graph-2.png`

### Inventory-action recovery after the retired Gemini model

- Observed that the first public inventory-action run failed on the old default model:
  - `gemini-2.0-flash`
- Confirmed the old model path returned a Vertex AI 404 in the service logs for:
  - `projects/oracle-public-488519/locations/us-central1/publishers/google/models/gemini-2.0-flash`
- Updated the VM runtime model to:
  - `MODEL_NAME=gemini-2.5-flash`
- After that model change, the inventory-action task no longer failed fast, but the A2A task remained stuck in `working`.
- Identified the next issue as an inventory-action controller task-lifecycle problem: the background ADK completion was racing with the request-scoped A2A event queue, so the final task state was not being persisted reliably for polling clients.
- Reworked `InventoryActionA2AController` to keep its own in-memory task map and update the task when the background ADK run finishes.
- Rebuilt the Java runtime locally, synced the controller fix to the VM, rebuilt on the VM, and restarted the HTTPS service.
- Re-ran the public inventory-action flow and confirmed it now completes successfully over public HTTPS:
  - final task state: `completed`
  - metadata: `coordinator=adk`
  - metadata: `traceCount=14`
- Verified the live recommendation for `SKU-500` is now an ADK-generated transfer recommendation:
  - move `500` units
  - source: `Warehouse: DFW Hub`
  - destination: `Warehouse: Newark Inventory Hub`
  - approval required: `Yes`
  - draft action id: `draft-transfer-sku-500`

### Public URLs ready for Gemini Enterprise import

- Graph:
  - `https://34.186.79.96/agent-card-graph.json`
- Spatial:
  - `https://34.186.79.96/agent-card-spatial.json`
- Select AI:
  - `https://34.186.79.96/agent-card-select-ai.json`
- Inventory action:
  - `https://34.186.79.96/agent-card-action.json`

### OpenAI Select AI profile and runtime recovery

- Stored the provided OpenAI API key in the VM private secrets area at:
  - `/home/pparkins/.oracle-agent-secrets/openai-api-key`
- Updated the Select AI setup template in the repo to improve compatibility with this database:
  - disabled SQL verify/echo to avoid substitution noise during setup
  - removed the `JSON_OBJECT(... RETURNING CLOB)` form that this 26ai environment rejected in PL/SQL
- Created the database-side OpenAI credential and Select AI profile:
  - credential: `OPENAI_CRED`
  - profile: `OPENAI_INVENTORY_DEMO`
  - model: `gpt-4o-mini`
- Updated the VM `.env` to keep the Select AI profile active and to expose the OpenAI secret to the Java runtime without putting the key in git:
  - `SELECT_AI_PROFILE=OPENAI_INVENTORY_DEMO`
  - `OPENAI_API_KEY_FILE=/home/pparkins/.oracle-agent-secrets/openai-api-key`
  - `SELECT_AI_OPENAI_MODEL=gpt-4o-mini`
- Confirmed the same OpenAI key works directly:
  - from this workstation against `https://api.openai.com/v1/models/gpt-4o-mini`
  - from this workstation against `https://api.openai.com/v1/chat/completions`
  - from the GCP VM against `https://api.openai.com/v1/chat/completions`
- Confirmed that the remaining failure is specific to Oracle `DBMS_CLOUD_AI.GENERATE` with this OpenAI profile in this database environment:
  - direct database test still returns `ORA-20401`
  - failing URI reported by Oracle: `bearer://api.openai.com/v1/chat/completions`
- Added a service-side OpenAI recovery path in:
  - `oracle_agent_java/src/main/java/oracleai/SelectAiService.java`
- The new Select AI behavior is:
  - try `DBMS_CLOUD_AI.GENERATE` first when `SELECT_AI_PROFILE` is configured
  - if Oracle returns the current OpenAI authorization failure, call OpenAI `chat.completions` directly from the Java runtime
  - ground the OpenAI answer in live Oracle table context from the demo schema instead of falling back immediately to a deterministic canned summary
- Rebuilt and redeployed the Java runtime on the VM with that recovery path.
- Verified the public Select AI endpoint now completes successfully over HTTPS with OpenAI-backed output:
  - discovery URL: `https://34.186.79.96/select-ai/.well-known/agent-card.json`
  - execution mode: `openai-direct-fallback`
  - source detail: `OpenAI chat.completions direct after DBMS_CLOUD_AI.GENERATE failed for profile OPENAI_INVENTORY_DEMO`

### Next steps

- Import the four public card URLs above into Gemini Enterprise.
- If you want true in-database Select AI rather than the new OpenAI service-side recovery path, open the next troubleshooting step on the Oracle side:
  - investigate why `DBMS_CLOUD_AI.GENERATE` is rejecting this working OpenAI key with `ORA-20401`
  - likely paths are a newer Oracle patch level, a different OpenAI credential format, or an Oracle support ticket
- Optionally revisit the graph card URL presentation if the alias card should advertise `/graph` more explicitly in every client surface, even though the dedicated `/graph/.well-known/agent-card.json` endpoint is already live and working.
