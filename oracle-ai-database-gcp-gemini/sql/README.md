# Shared SQL Assets

This directory holds the shared Oracle Database setup, seed, and SQLcl helper scripts used across the Java, Python, and Go agent implementations.

Current contents:

- schema and seed SQL for the supply-chain graph demo
- schema and seed SQL for the inventory-risk and hotspot demo tables
- Select AI profile helper SQL
- SQLcl wrapper scripts that load the repo-level `.env` and run the shared SQL from one canonical location
- region-scoped Deep Data Security setup and direct database verification for
  the inventory-analysis data

Two mutually exclusive manager-identity configurations are available. Do not
run both for the same users without intentionally switching modes.

For ordinary schema users that can authenticate directly to the managed Oracle
AI Database Agent, set distinct `DB_PASSWORD_NA` and `DB_PASSWORD_APAC` values
in the ignored `.env`, then run:

```bash
./sql/run_supply_chain_manager_schema_users.sh
```

This replaces any same-named local Deep Sec end users with conventional
`CREATE USER` schemas, gives both managers identical read access to the
`FINANCIAL.SC_*` objects, and creates an equivalent Select AI profile and
official Oracle AI Database Agent team in each schema. It reuses the encrypted
`FINANCIAL.OPENAI_CRED` database credential without exposing its secret.

For the separate regional Deep Sec database-only demonstration, keep
`FINANCIAL` as the schema and agent-team owner and run:

```bash
./sql/run_inventory_risk_deepsec_regions.sh
```

That runner adds synthetic APAC risk rows, creates the two local Deep Sec end
users, assigns NA and APAC data roles, enables mandatory data-grant enforcement
on `FINANCIAL.SC_INVENTORY_RISK_DEMO_V`, and verifies each identity sees only
its authorized region.

`FINANCIAL` intentionally remains the one shared application schema and Select
AI agent-team owner. `SUPPLYCHAIN_NA_MGR` and `SUPPLYCHAIN_APAC_MGR` follow the
Oracle Deep Data Security direct-logon pattern: they are password-authenticated
local end users, not separate schema owners. Their assigned data roles and data
grants restrict the rows returned from the same protected `FINANCIAL` view.
No OCI IAM or Microsoft Entra ID identity propagation is required for this
database-side demonstration, and separate Select AI profiles or agent teams are
not required per end user.

Use this directory as the canonical home for database assets instead of treating them as Java-specific files.

For the `adb-pm-prod` / `paulparkdb` deployment, the controlled completion
sequence is:

1. Run `admin_prepare_paulparkdb_demo.sql` as `ADMIN`.
2. Run `setup_supply_chain_graph_schema.sql` as `FINANCIAL` to create the
   property graph over the already-installed relational tables.
3. Choose one or both provider paths:
   - OpenAI: run `create_openai_select_ai_credential.sql`, then
     `create_paulparkdb_openai_select_ai_profile.sql` as `FINANCIAL`.
   - Google: obtain a paid Google AI Studio API key and run
     `create_google_select_ai_credential.sql`, then
     `create_paulparkdb_select_ai_profile.sql` as `FINANCIAL`.
   Both credential scripts accept their key with input echo disabled, and the
   profiles have different names so they can coexist.
   On the GCP VM, `run_paulparkdb_openai_profile_setup.sh` performs the ADMIN
   preparation and both OpenAI steps using the ignored repo-level `.env`.
   For later API-key rotation, run
   `update_openai_select_ai_credential.sql` as `FINANCIAL`; it updates the
   encrypted credential in place and does not recreate the profile.
4. Verify the selected profile with its included `showsql` query before making
   it the active managed-agent profile.
5. Fetch and run Oracle's pinned official `oracle_ai_database_agent_tool.sql`
   and `oracle_ai_database_agent.sql` installers as described in
   `../docs/ADB_PM_PROD_REDEPLOYMENT.md`.
6. Run `verify_oracle_ai_database_agent.sql` as the application schema. It
   creates a one-day verification conversation, executes the stockout prompt
   through `DBMS_CLOUD_AI_AGENT.RUN_TEAM`, and lists the enabled team and its
   four expected tools.

The ADMIN preparation script grants only `CREATE PROPERTY GRAPH`, execute on the
two Select AI packages, and outbound HTTP access to the documented Google and
OpenAI endpoints. Both provider profiles are intentionally narrow and refuse to
overwrite an existing credential or profile.

On Oracle AI Database 26ai, `DBMS_CLOUD` can be a public synonym for a
patch-versioned package object. The ADMIN script discovers that synonym target
and grants the real package rather than hard-coding a patch-specific name. The
runner feeds database passwords and the provider key to SQLcl over standard
input so they do not appear in SQLcl process arguments.
