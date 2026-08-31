# Recreate the Gemini Enterprise database and specialist agents on `adb-pm-prod`

This runbook reconciles the existing implementation with the shared demo environment:

- Google Cloud project: `adb-pm-prod`
- Oracle AI Database: `paulparkdb` (`paulparkdb_tp`)
- Compute host: the existing shared VM in `us-east4-a`
- Gemini Enterprise app: the existing Inventory System application
- Runtime: the managed Oracle AI Database Agent for inventory analysis, plus the Java service in `oracle_agent_java` for graph, spatial, inventory-system, and inventory-action A2A surfaces

It does **not** create another VM or database. It also does not depend on the separate Custom MCP Server organization-policy exception.

## Target flow

1. A user asks the Oracle AI Database Agent which products are at risk of stockout and which regions drive that risk.
2. The database-managed agent runs governed NL2SQL under the user's Oracle database identity.
3. The inventory-system A2A agent routes follow-up dependency questions to the graph specialist.
4. The spatial specialist renders warehouse hotspots for a map request.
5. The inventory-action specialist proposes a reviewable transfer rather than silently writing to the database.

The demo's core prompts are:

```text
Which products are at risk of stockouts next quarter, and which regions are driving that risk?

Which upstream suppliers and downstream warehouses are connected to SKU-500?

Show that on a map.

Suggest actions to take for the SKU in question.
```

## Authoritative references

- [Oracle walkthrough: Unlocking Data Insights with the Oracle AI Database Agent in Gemini Enterprise, Part 1](https://blogs.oracle.com/developers/unlocking-data-insights-with-the-oracle-ai-database-agent-in-gemini-enterprise-part-1)
- [Oracle documentation: Use Oracle AI Database Agent with Google Gemini Enterprise](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/use-oracle-ai-database-agent-google-gemini-enterprise-application.html)
- [Oracle documentation: Expose in-database agents through A2A](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/expose-database-ai-agents-agent2agent-protocol.html)
- [Oracle's official agent installer samples](https://github.com/oracle-devrel/oracle-autonomous-database-samples/tree/main/google-gemini-marketplace-agents/oracle_ai_database_agent)
- [Google documentation: Register and manage A2A agents](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
- [Demo video: Oracle AI Database Agent for Gemini Enterprise](https://www.youtube.com/watch?v=lU8UAwmBMeQ)

The official Oracle installer is fetched at pinned commit `7c61fc86ffb7f0f548bdba32ae53ce46ea876fa2` and verified with SHA-256 checksums by `sql/fetch_official_oracle_ai_database_agent.sh`.

## Safety rules

- Audit before applying DDL.
- Keep the existing A2UI and MCP services running while the new runtime is staged on a separate port.
- Use the existing database wallet and Secret Manager values; never commit them.
- Select one schema owner after the audit and use it consistently for `DB_SCHEMA_OWNER`, `INVENTORY_SCHEMA_OWNER`, and `SELECT_AI_OBJECT_OWNER`.
- Keep the Select AI profile object list narrow. Do not expose the complete `ADMIN` schema.
- The seed scripts use deterministic `MERGE` statements. The schema scripts only create missing objects.
- Back up any working Select AI profile before extending it.
- Inventory-action remains a review flow until an explicit write-enabled deployment is separately approved.

## Phase 1: audit the shared environment

After authenticating `gcloud`, inspect the existing VM, machine shape, services, ports, repository checkout, Java/Maven/SQLcl versions, wallet path, and TLS certificate. Do not resize or restart it until capacity and collision checks are complete.

Run the read-only database preflight on the VM:

```bash
cd /path/to/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-gemini
bash sql/run_paulparkdb_demo_audit.sh
```

The audit reports connection identity, relevant schema names, `SC_*` object ownership, Select AI profile names, Select AI agent-team names, and required package visibility. It does not print credentials or business rows and makes no changes.

## Phase 2: reconcile demo data

Choose the schema owner from Phase 1. Then run, in this order, only after reviewing the preflight:

1. `sql/setup_supply_chain_graph_schema.sql`
2. `sql/seed_supply_chain_graph_data.sql`
3. `sql/setup_inventory_risk_demo_schema.sql`
4. `sql/seed_inventory_risk_demo_data.sql`

The resulting object boundary is:

- relational inventory/risk: `SC_PRODUCTS`, `SC_WAREHOUSES`, `SC_INVENTORY_RISK_SUMMARY`, `SC_WAREHOUSE_RISK_SNAPSHOT`, `SC_INVENTORY_RISK_DEMO_V`
- graph: supplier, plant, port, warehouse, product, and alert tables plus `SUPPLY_CHAIN_GRAPH`
- spatial: `SC_WAREHOUSE_GEO` with warehouse coordinates and regional attributes
- action: recommendation data remains read-only until the user explicitly approves a separate write path

### `adb-pm-prod` installation note (August 8, 2026)

The relational inventory, risk, graph-edge, and spatial tables are installed in
`FINANCIAL` and contain the deterministic SKU-500/700/900 demo data. Creating
`SUPPLY_CHAIN_GRAPH` requires the schema-level `CREATE PROPERTY GRAPH`
privilege. Run `sql/admin_prepare_paulparkdb_demo.sql` once as `ADMIN`, then
rerun `sql/setup_supply_chain_graph_schema.sql` as `FINANCIAL`. Until that grant
is applied, the Java graph agent deliberately executes the equivalent governed
relational joins and identifies its source as `database-relational-fallback`.

## Phase 3: create the narrow Select AI profile

Reuse a working provider credential already owned by the selected schema. Prefer creating a dedicated profile named `PAULPARK_SUPPLY_CHAIN_DEMO`; if the Oracle Marketplace agent is already bound to another working profile, use `configure_select_ai_demo_profile.sql` to clone its provider settings and add the demo objects.

The `FINANCIAL` audit found no existing Select AI profile to clone. Run
`sql/create_google_select_ai_credential.sql` as `FINANCIAL` with a paid Google
AI Studio API key, then run `sql/create_paulparkdb_select_ai_profile.sql`. Its
object list contains only the demo tables, view, and property graph. The scripts
do not echo the API key and refuse to replace an existing credential/profile, so
a working configuration cannot be silently destroyed.

Do not run `extend_sales_data_profile_with_inventory.sql` until the audit confirms that `SALES_DATA_PROFILE` exists and is the profile actually bound to the managed agent. That script creates `SALES_DATA_PROFILE_BEFORE_SC` as a rollback snapshot before rebuilding the original profile.

Verify the profile using the stockout question and inspect the generated SQL before proceeding.

## Phase 4: install the Oracle in-database agent team

Fetch the pinned official scripts:

```bash
cd /path/to/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-gemini
bash sql/fetch_official_oracle_ai_database_agent.sh
```

Connect as `ADMIN` or as the target schema when it already has `EXECUTE` on
`DBMS_CLOUD_AI` and `DBMS_CLOUD_AI_AGENT`, then run the two official scripts in
this order:

```text
oracle_ai_database_agent_tool.sql
oracle_ai_database_agent.sql
```

Supply the selected schema name and its narrow supply-chain profile when prompted. The official installer creates the reusable tools plus `ORACLE_AI_DATABASE_TASK`, `ORACLE_AI_DATABASE_AGENT_ROLE`, and `ORACLE_AI_DATABASE_AGENT` team.

Run `sql/verify_oracle_ai_database_agent.sql` as the target schema, verify the
team in `USER_AI_AGENT_TEAMS`, and enable it if needed before exposing it
through A2A. The verification script creates a real conversation ID before
calling `DBMS_CLOUD_AI_AGENT.RUN_TEAM`; an arbitrary string is not a valid
conversation ID.

## Phase 5: enable the `paulparkdb` managed A2A server

In the OCI view of the Oracle Database@Google Cloud database, add this free-form tag:

```text
Tag name:  adb$feature
Tag value: {"name":"a2a_server","enable":true}
```

Allow up to five minutes for propagation. This is an Oracle database-resource setting, not the Gemini Enterprise Custom MCP organization policy.

Register an OAuth client using the database registration endpoint and HTTP Basic authentication with the database administrator account. Use this exact Gemini Enterprise redirect URI:

```text
https://vertexaisearch.cloud.google.com/oauth-redirect
```

Store the returned client ID and one-time client secret in an ignored `.env` file or Secret Manager. Never write them to logs or Git.

## Phase 6: add the Marketplace agent

In the existing Gemini Enterprise application:

1. Open **Agents** and choose **Add agent**.
2. Select **Agents via Marketplace**.
3. Choose **Oracle AI Database Agent**.
4. Supply the OAuth client ID and secret from Phase 5.
5. Grant the intended users the Agent User permission.
6. In Gemini Enterprise chat, authorize with the individual Oracle database username and password.

The user identity is enforced in Oracle Database. Gemini credentials do not grant additional database privileges.

## Phase 7: deploy the custom specialists on the shared VM

Build and stage the Java runtime without replacing the existing A2UI/MCP processes:

```bash
cd /path/to/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-gemini/oracle_agent_java
./build.sh
./run.sh
```

The launcher loads the complete database, Select AI, downstream Oracle A2A, model, graph, and rendering environment from the ignored repository-level `.env` file. Set the shared schema owner consistently so delegated prompts reference the real owner instead of a hard-coded `SALES_USER` schema.

After HTTPS and health checks pass, register these cards in the Gemini Enterprise Inventory System app:

- `https://34.48.146.146:8443/inventory-system/.well-known/agent-card.json`
- `https://34.48.146.146:8443/graph/.well-known/agent-card.json`
- `https://34.48.146.146:8443/spatial/.well-known/agent-card.json`
- `https://34.48.146.146:8443/inventory-action/.well-known/agent-card.json`

Gemini Enterprise currently expects A2A v0.3 streaming behavior for custom A2A agents. Preserve the existing SSE-compatible controller behavior when upgrading libraries.

The isolated deployment uses `https://34.48.146.146:8443` so the pre-existing
service on port 443 remains untouched. Before public registration, add a unique
`oracle-gemini-a2a` network tag to `paulpark-instance` and allow TCP 8443 only to
that tag. Do not attach the rule to the shared `https-server` tag.

## Verification record

Fill this table during deployment rather than assuming old project state:

| Check | Result |
|---|---|
| Shared VM name, zone, and machine type | `paulpark-instance`, `us-east4-a`, `e2-medium` |
| Existing services and free port/memory capacity | Existing 443 service retained; new service stable on 8443 at about 180-250 MiB; no resize required |
| Demo schema owner | `FINANCIAL` |
| Existing `SC_*` objects and row counts | Installed: 11 graph base/edge tables plus risk/spatial objects; three deterministic demo entities per primary domain and nine warehouse snapshots |
| Existing Select AI profiles | `PAULPARK_SUPPLY_CHAIN_OPENAI` is enabled in `FINANCIAL` and retains a narrow `SC_*` object allowlist |
| `ORACLE_AI_DATABASE_AGENT` installed and enabled | Installed and enabled in `FINANCIAL` on August 16, 2026, with `SQL_TOOL`, `DISTINCT_VALUES_CHECK`, `RANGE_VALUES_CHECK`, and `GENERATE_CHART`; package and installer procedure are valid |
| `adb$feature` A2A tag configured | Configured in OCI on August 16, 2026; paulparkdb returned to `AVAILABLE`, but the documented managed agents-list endpoint still returns HTTP 404 and requires Oracle back-end activation verification |
| Gemini OAuth client registered | Registered on August 16, 2026; the one-time credentials are stored only in the ignored local `.runtime` directory |
| Marketplace database agent verified | Pending |
| Inventory-system/graph/spatial/action cards verified | All six discovery cards return HTTP 200 locally over TLS on port 8443 |
| Four-prompt demo verified end to end | Custom specialists verified. On August 16, 2026, `DBMS_CLOUD_AI_AGENT.RUN_TEAM` returned SKU-500, SKU-700, and SKU-900 with the expected risk, revenue, and region values. Remote A2A and Gemini Marketplace validation remain pending until the OCI A2A tag and OAuth registration are completed |

The current certificate is valid from August 8 through August 15, 2026. The
`certbot-oracle-graph-agent-ip.timer` unit is enabled and checks renewal twice
daily. Public reachability was verified from outside the VM after applying
`allow-oracle-gemini-a2a-8443`, which permits TCP 8443 only on instances carrying
the unique `oracle-gemini-a2a` tag. All four registration cards return HTTP 200
over trusted HTTPS.
