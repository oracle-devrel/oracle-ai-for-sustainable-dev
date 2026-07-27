# Implementation plan and compatibility record

Originally verified on 2026-07-20 against the upstream projects linked below; the supply-chain use case was implemented on 2026-07-27 without changing the protocol baselines.

## Version decisions

| Layer | Baseline | Decision |
|---|---|---|
| Oracle Database MCP Java Toolkit | `com.oracle.database.mcptoolkit:oracle-db-mcp-toolkit:1.0.0`; pinned upstream commit `5bb406b5b70e109a749cd16cc026422134a117b2`; JDK 17+, Maven 3.9+ | Build the official source into ignored `.runtime/`, apply the one-line stdio registration patch, and launch it with only `supply-chain-exchange`. Use Streamable HTTP with TLS/OAuth for a remote deployment. |
| MCP protocol client | MCP `2025-03-26`, matching the Toolkit's `io.modelcontextprotocol.sdk:mcp:0.12.1` baseline | Keep the agent adapter small and dependency-light: newline-delimited JSON-RPC over stdio, initialization, tool discovery, tool calls, 30-second timeouts, server identity checks, and an exact allowlist. |
| AG-UI | Current standardized lifecycle, text, tool-call, state, and `CUSTOM` events | Emit the official event names over SSE. Carry each A2UI envelope in a `CUSTOM` event named `a2ui.message`. Approval is application state, not an invented AG-UI event type. |
| A2UI | v0.9.1, current production baseline | Emit `createSurface`, `updateComponents`, and `updateDataModel` envelopes using the Basic Catalog ID. The browser renderer accepts only a small catalog and component allowlist. |
| MCP Apps | Stable 2026-01-26 extension; `@modelcontextprotocol/ext-apps` 1.7.4; `ui://` resource; `text/html;profile=mcp-app` | Keep the recommendation dashboard in a separate TypeScript package. It receives structured Toolkit-governed results through the host bridge and never connects to Oracle Database. |

## Business decision: a governed recommendation, not an arbitrary action menu

The database view computes each recommendation from current inventory, reservations, inbound quantities, seven-day forecast, safety stock, and active supply lanes. It chooses the best feasible source by transit time, available surplus, and unit cost. The UI does not invent actions such as “review” or “freeze changes”; it renders source, target, product, and quantity returned by the governed query.

The short-lived approval token holds an immutable copy of each returned recommendation. The browser sends only its recommendation ID and notes. The service recovers the exact bound product, locations, and quantity, then the stored procedure locks both positions and recomputes feasibility. This gives two safeguards against stale or manipulated transfers.

## Toolkit write compatibility

Current Toolkit YAML parameters describe `name`, `type`, `description`, and `required`; they do not describe JDBC/PLSQL OUT-parameter modes. The integration avoids a Toolkit fork by reserving an ID with `INVENTORY_TRANSFER_MCP_SEQ`, then passing that ID into the input-only `APPROVE_INVENTORY_TRANSFER_MCP` procedure. The procedure performs validation, deterministic row locking, current-stock revalidation, audit insertion, and source reservation in one MCP tool statement. Failure creates no partial business record; sequence gaps remain valid Oracle behavior.

## Runtime architecture

```text
web-client
  |  POST run / approval + SSE response
  v
agent-service
  |  MCP 2025-03-26 over stdio, exact tool allowlist
  v
Oracle Database MCP Java Toolkit
  |  YAML business tools + JDBC/UCP + binds
  v
Oracle AI Database
  |  inventory + forecast + lanes -> recommendation -> locked transfer

MCP-compatible host
  |  ui:// resource + postMessage bridge
  v
mcp-app (recommendation dashboard; structured data only)
```

## Delivered phases

1. **Foundation:** supply-chain schema, deterministic data, tool YAML, AG-UI SSE, allowlisted A2UI renderer, MCP App dashboard, tests, and blog.
2. **Oracle schema bootstrap:** guarded one-time UCP setup using the financial database defaults, recommendation view, sequence, input-only procedure, and deterministic sample records. This installer is separate from application runtime access.
3. **MCP integration:** pinned official Toolkit bootstrap, synchronous stdio client, exact allowlist verification, YAML reads, and a sequence-backed procedure write. Every application database operation uses this path.
4. **Production path:** replace in-memory approval state with durable hashed nonce/idempotency records; use external identity, VPD/data-role policies, centralized audit export, authenticated Streamable HTTP, and host compatibility testing.

## Compatibility risks

- The Java AG-UI SDK is community-maintained and has moved within the upstream monorepo. The baseline therefore uses the stable wire contract without taking a hard dependency on a volatile Java artifact.
- A2UI v1.0 changes require a deliberate migration. The current renderer and envelopes are fixed at v0.9.1.
- MCP Apps is an MCP extension and host support varies. The base web app works without it.
- YAML cannot currently express a callable OUT parameter. The sequence-plus-input-only-procedure design avoids that requirement, but sequence gaps are expected and the exact Toolkit commit remains pinned and tested.
- The pinned Toolkit/SDK combination can emit tool-list change notifications before stdio is ready when several tools register at startup. The checked-in patch disables only those dynamic stdio notifications; static discovery and calls remain enabled, and the agent checks the exact list before serving.
- Oracle bind support in `FETCH FIRST :maximumRows ROWS ONLY` can vary by execution path. The demo uses `ROWNUM <= :maximumRows` in an outer query.
- The recommendation formula is intentionally transparent and deterministic. A production planner may add lead-time uncertainty, service-level targets, capacity, shelf-life, shipment consolidation, and optimization across multiple simultaneous moves.

## Primary sources

- [Oracle Database MCP Java Toolkit](https://github.com/oracle/mcp/tree/main/src/oracle-db-mcp-java-toolkit)
- [AG-UI events](https://docs.ag-ui.com/sdk/js/core/events)
- [A2UI v0.9.1 specification](https://a2ui.org/specification/v0.9.1-a2ui/)
- [MCP Apps overview](https://modelcontextprotocol.io/extensions/apps/overview)
- [MCP Apps build guide](https://modelcontextprotocol.io/extensions/apps/build)
