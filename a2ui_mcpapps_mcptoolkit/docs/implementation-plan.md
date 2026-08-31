# Implementation plan and compatibility record

Originally verified on 2026-07-20 against the upstream projects linked below; the supply-chain use case was implemented on 2026-07-27 without changing the protocol baselines.

## Version decisions

| Layer | Baseline | Decision |
|---|---|---|
| Oracle Database MCP Java Toolkit | `com.oracle.database.mcptoolkit:oracle-db-mcp-toolkit:1.0.0`; pinned upstream commit `5bb406b5b70e109a749cd16cc026422134a117b2`; JDK 17+, Maven 3.9+ | Build the official source into ignored `.runtime/` and run it independently with TLS, authentication, and only `supply-chain-exchange`. |
| MCP protocol client | MCP `2025-03-26`, matching the Toolkit's `io.modelcontextprotocol.sdk:mcp:0.12.1` baseline | Use Streamable HTTP for initialization, tool discovery, calls, 30-second timeouts, server identity checks, and an exact allowlist. |
| AG-UI | Current standardized lifecycle, text, tool-call, state, and `CUSTOM` events | Emit the official event names over SSE. Carry each A2UI envelope in a `CUSTOM` event named `a2ui.message`. Approval is application state, not an invented AG-UI event type. |
| A2UI browser | v0.9.1 | Emit `createSurface`, `updateComponents`, and `updateDataModel` envelopes using the Basic Catalog ID. The browser renderer accepts only a small catalog and component allowlist. |
| MCP Apps / ChatGPT | Stable 2026-01-26 extension; `@modelcontextprotocol/ext-apps` 1.7.4; `ui://` resource; `text/html;profile=mcp-app` | Keep the dashboard in a separate TypeScript package. The model-visible tool is read-only; approve and reject are app-only tools invoked after explicit interaction. |
| Gemini Enterprise native UI | Preview; A2A v0.3 compatibility; A2UI v0.8 only as of 2026-07-28 | Serve a separate A2A adapter with deterministic `beginRendering`, `surfaceUpdate`, and `dataModelUpdate` messages. Reuse the Java domain API and Oracle transaction. |
| Gemini Enterprise MCP App | Custom MCP Server data store; private Cloud Run invocation by the Discovery Engine service agent; Google OAuth web client with PKCE | Reuse the portable MCP App through a dedicated private service. Keep the model-visible tool read-only until actor-bound OAuth write semantics are tested. |

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
  |  MCP 2025-03-26 over authenticated Streamable HTTP
  v
Oracle Database MCP Java Toolkit
  |  YAML business tools + JDBC/UCP + binds
  v
Oracle AI Database
  |  inventory + forecast + lanes -> recommendation -> locked transfer

MCP-compatible host
  |  ui:// resource + postMessage bridge
  v
mcp-app (dashboard + app-only approval/rejection tools)

Gemini Enterprise
  |  A2A v0.3 JSON-RPC + A2UI v0.8 DataParts
  v
gemini-enterprise-a2a
  |  same review / approval / rejection API
  v
agent-service -> Toolkit -> Oracle AI Database
```

## Delivered phases

1. **Foundation:** supply-chain schema, deterministic data, tool YAML, AG-UI SSE, allowlisted A2UI renderer, MCP App dashboard, tests, and blog.
2. **Oracle schema bootstrap:** guarded one-time UCP setup using the financial database defaults, recommendation view, sequence, input-only procedure, and deterministic sample records. This installer is separate from application runtime access.
3. **MCP integration:** pinned official Toolkit bootstrap, standalone Streamable HTTP service, exact allowlist verification, YAML reads, and a sequence-backed procedure write. Every application database operation uses this path.
4. **Host adapters:** MCP Apps approval inside ChatGPT-compatible hosts and
   A2A/A2UI v0.8 delivery for Gemini Enterprise, both over the same
   short-lived approval and Oracle transaction boundary.
5. **Production path:** replace in-memory approval state with durable hashed
   nonce/idempotency records; use external identity, VPD/data-role policies,
   centralized audit export, authenticated Streamable HTTP/A2A, and host
   compatibility testing.

## Compatibility risks

- The Java AG-UI SDK is community-maintained and has moved within the upstream monorepo. The baseline therefore uses the stable wire contract without taking a hard dependency on a volatile Java artifact.
- A2UI v1.0 changes require a deliberate migration. The standalone renderer is fixed at v0.9.1, while Gemini Enterprise currently requires a separately tested v0.8 payload builder.
- MCP Apps is an MCP extension and host support varies. The base web app works without it.
- Gemini Enterprise A2UI support is Preview and its supported A2UI/A2A revisions can change independently. Its Custom MCP Server path can separately consume the MCP App resource in a sandboxed iframe.
- YAML cannot currently express a callable OUT parameter. The sequence-plus-input-only-procedure design avoids that requirement, but sequence gaps are expected and the exact Toolkit commit remains pinned and tested.
- Oracle bind support in `FETCH FIRST :maximumRows ROWS ONLY` can vary by execution path. The demo uses `ROWNUM <= :maximumRows` in an outer query.
- The recommendation formula is intentionally transparent and deterministic. A production planner may add lead-time uncertainty, service-level targets, capacity, shelf-life, shipment consolidation, and optimization across multiple simultaneous moves.

## Primary sources

- [Oracle Database MCP Java Toolkit](https://github.com/oracle/mcp/tree/main/src/oracle-db-mcp-java-toolkit)
- [AG-UI events](https://docs.ag-ui.com/sdk/js/core/events)
- [A2UI v0.9.1 specification](https://a2ui.org/specification/v0.9.1-a2ui/)
- [MCP Apps overview](https://modelcontextprotocol.io/extensions/apps/overview)
- [MCP Apps build guide](https://modelcontextprotocol.io/extensions/apps/build)
- [OpenAI: add UI to an MCP server](https://developers.openai.com/plugins/build/chatgpt-ui)
- [Gemini Enterprise A2UI/A2A registration](https://docs.cloud.google.com/gemini/enterprise/docs/a2ui-agents/register-and-manage-an-a2ui-agent)
- [Gemini Enterprise A2A registration](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
