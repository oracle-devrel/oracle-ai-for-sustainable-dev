# Oracle Database MCP Java Toolkit integration

This directory contains only the supply-chain exchange configuration, write contract, and compatibility patch. It deliberately does not copy Oracle's Toolkit source.

`agent-service/run.sh` invokes `prepare-mcp-toolkit.sh`, which downloads and builds a pinned revision of [oracle/mcp](https://github.com/oracle/mcp) into the ignored `.runtime/` directory. It applies `patches/stdio-tool-registration.patch`, then starts the Toolkit over stdio with this configuration and `-Dtools=supply-chain-exchange`.

The YAML datasource resolves `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from the Toolkit child-process environment. Credentials are never stored in this file. For a remote deployment, run Streamable HTTP with TLS and OAuth 2.0 as documented by the Toolkit.

Do not use `-Dtools=*` for this demo: it would enable unrestricted `write-query`, table-management, and other broad tools.

The five enabled business tools are:

1. `find-stockout-transfer-recommendations`
2. `get-stockout-transfer-details`
3. `reserve-inventory-transfer-id`
4. `approve-inventory-transfer`
5. `count-inventory-transfers`

The Toolkit YAML schema cannot register a callable OUT parameter. The demo avoids a custom Toolkit fork: `reserve-inventory-transfer-id` obtains a sequence value, then `approve-inventory-transfer` passes it to the input-only `APPROVE_INVENTORY_TRANSFER_MCP` procedure. That one statement locks the source and target inventory rows in deterministic order, recomputes current transfer feasibility, inserts the audit record, and reserves the source quantity atomically. Failed or abandoned workflows may leave normal sequence gaps but no partial business record.

At startup, the Java agent verifies both the server identity and the exact five-tool allowlist before serving requests. This lets the same governed tool definitions be reused by other agents and MCP-compatible clients.

### Why the stdio patch exists

The pinned Toolkit revision declares dynamic tool-list change notifications while registering tools immediately after constructing its stdio server. With several startup tools, the underlying SDK can reject a notification before the transport is ready. The one-line patch changes only the stdio declaration from `tools(true)` to `tools(false)`. Tool listing and calls still work; only dynamic list-change notifications are disabled. The HTTP transport is unchanged.
