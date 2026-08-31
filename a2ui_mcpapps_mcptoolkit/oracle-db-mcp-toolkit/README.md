# Oracle Database MCP Java Toolkit integration

This directory contains the standalone startup scripts, supply-chain exchange configuration, write contract, and wallet compatibility patches. It deliberately does not copy Oracle's Toolkit source.

`run.sh` invokes `agent-service/prepare-mcp-toolkit.sh`, which downloads and builds a pinned revision of [oracle/mcp](https://github.com/oracle/mcp) into the ignored `.runtime/` directory. It then runs the Toolkit independently over authenticated Streamable HTTP with TLS and the selected toolset.

The YAML datasource resolves `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the Toolkit process environment. Credentials are never stored in this file. Local development uses generated TLS and bearer tokens; production should use a managed certificate and OAuth 2.0.

Do not use `-Dtools=*` for this demo: it would enable unrestricted `write-query`, table-management, and other broad tools.

The five enabled business tools are:

1. `find-stockout-transfer-recommendations`
2. `get-stockout-transfer-details`
3. `reserve-inventory-transfer-id`
4. `approve-inventory-transfer`
5. `count-inventory-transfers`

The Toolkit YAML schema cannot register a callable OUT parameter. The demo avoids a custom Toolkit fork: `reserve-inventory-transfer-id` obtains a sequence value, then `approve-inventory-transfer` passes it to the input-only `APPROVE_INVENTORY_TRANSFER_MCP` procedure. That one statement locks the source and target inventory rows in deterministic order, recomputes current transfer feasibility, inserts the audit record, and reserves the source quantity atomically. Failed or abandoned workflows may leave normal sequence gaps but no partial business record.

At startup, the Java agent connects to the Toolkit URL, verifies both the server identity and the exact five-tool allowlist, and only then serves requests. The independently managed endpoint can be reused by other authorized agents and MCP clients.

## Run

```bash
# Terminal 1: full read/write allowlist
./run.sh full

# Optional terminal 2: separate read-only database identity and allowlist
# First set ORACLE_MCP_ENABLE_READ_PROFILE=true in the ignored .env.
./run.sh read

# Terminal 3
cd ../agent-service
./run.sh
```
