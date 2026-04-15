# Oracle AI Database As A Downstream Agent

This note captures the setup that lets `oracle_inventory_system_agent` delegate database questions to the Oracle-hosted Oracle AI Database agent, while still routing graph requests to the property graph service and map requests to the spatial service.

## What this enables

- DB questions go to the Oracle AI Database agent over A2A JSON-RPC.
- Graph questions stay local.
- Spatial questions stay local.
- The inventory-system gateway can answer from Oracle AI Database even when Gemini Enterprise does not forward a usable Oracle bearer token.

## Working approach used for the demo

The stable demo path uses an Oracle service identity configured on the gateway VM.

The gateway checks auth in this order:

1. `ORACLE_AI_DATABASE_AGENT_AUTHORIZATION_HEADER`
2. `ORACLE_AI_DATABASE_AGENT_BEARER_TOKEN`
3. `ORACLE_AI_DATABASE_AGENT_CLIENT_ID` + `ORACLE_AI_DATABASE_AGENT_CLIENT_SECRET` + `ORACLE_AI_DATABASE_AGENT_REFRESH_TOKEN`
4. inbound caller `Authorization` header

That ordering is intentional so the VM can keep using the known-good Oracle demo identity even if Gemini sends a different caller token.

## One-time Oracle setup

You need two OAuth client registrations:

1. Gemini Enterprise import client
   - redirect URI: `https://vertexaisearch.cloud.google.com/oauth-redirect`
   - used when importing the agent card into Gemini Enterprise

2. Gateway demo client
   - redirect URI you control locally, for example:
     `http://127.0.0.1:8765/oracle-oauth/callback`
   - used once to mint a refresh token for the gateway

Oracle registration endpoint:

`POST https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/databases/<AUTONOMOUS_DB_OCID>/register`

## Minting the gateway refresh token

Use the helper in this folder:

```bash
python3 mint_oracle_refresh_token.py \
  --client-id "<gateway-client-id>" \
  --client-secret "<gateway-client-secret>" \
  --output-json /tmp/oracle_inventory_gateway_tokens.json \
  --write-env /tmp/oracle_inventory_gateway_oauth.env
```

Then open the printed Oracle authorize URL in a browser, sign in as the Oracle demo DB user, and let the callback complete on `127.0.0.1`.

The generated env file should contain:

```bash
ORACLE_AI_DATABASE_AGENT_CLIENT_ID=...
ORACLE_AI_DATABASE_AGENT_CLIENT_SECRET=...
ORACLE_AI_DATABASE_AGENT_REFRESH_TOKEN=...
```

## VM runtime configuration

Copy that env file to the VM and start the service so it sources the file before launching `run.sh`.

The deployed VM flow used for the demo was:

- copy env file to `/home/pparkins/oracle_inventory_gateway_oauth.env`
- launch `oracle-graph-agent.service` with:
  - normal HTTPS/public host env
  - `bash -lc 'set -a; source /home/pparkins/oracle_inventory_gateway_oauth.env; set +a; exec .../run.sh'`

## Important implementation details

- Oracle AI Database agent is asynchronous.
- `message/send` usually returns a task in `submitted` state with `Task has been submitted.`
- The gateway must poll the same Oracle agent endpoint with JSON-RPC `tasks/get`.
- The final answer may come back in `artifacts[].parts[].text`, not only in `status.message.parts`.
- JSON-RPC `id` should be a string when calling the Oracle agent.

Those behaviors are handled in:

- [OracleAiDatabaseAgentClient.java](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/src/main/java/oracleai/OracleAiDatabaseAgentClient.java)

## Quick verification

Public card:

`https://34.186.79.96/agent-card-inventory-system.json`

Good DB test prompt:

`For year 2025, how did total sales revenue break down across sales channels?`

Expected metadata on success:

- `delegatedTo = oracle-ai-database-agent`
- `executionMode = remote-a2a`

## If it fails

- `No Oracle OAuth bearer token was available`
  - gateway has no usable Oracle credential configured

- `Task has failed`
  - usually means Oracle accepted the request but the token/user/profile/data access was not valid for that query

- answer stops at `Task has been submitted.`
  - polling is wrong or final text extraction is incomplete
