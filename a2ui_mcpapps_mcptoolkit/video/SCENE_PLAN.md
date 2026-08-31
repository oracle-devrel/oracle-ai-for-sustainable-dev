# MCP App Developer Walkthrough Scene Plan

This silent, captioned video follows the developer call flow for the Oracle Supply-Chain MCP App.

| Step | Focus | Visual |
| --- | --- | --- |
| 1 | Open Gemini Enterprise Inventory System and confirm connector | Gemini MCP App dashboard |
| 2 | Register the custom MCP server connector | Gemini connector setup |
| 3 | Expose `show-inventory-transfer-dashboard` | `mcp-app/server.ts` |
| 4 | Advertise `_meta.ui.resourceUri` | `mcp-app/server.ts` |
| 5 | Serve the `ui://` app resource | `mcp-app/server.ts` |
| 6 | Declare iframe CSP domains | `mcp-app/server.ts` |
| 7 | Connect iframe to MCP Apps bridge | `mcp-app/src/mcp-app.ts` |
| 8 | Authorize connector | Gemini authorization screen |
| 9 | Enable discovered connector tool | Gemini prompt controls |
| 10 | Submit dashboard prompt | Gemini MCP App dashboard |
| 11 | Host calls `tools/call` | `mcp-app/server.ts` |
| 12 | MCP server calls `/api/reviews` | `mcp-app/server.ts` |
| 13 | Spring Boot receives `/api/reviews` | `AgentController.java` |
| 14 | Runtime creates approval handle | `AgentRuntime.java` |
| 15 | Gateway calls the Toolkit MCP tool | `McpToolkitSupplyChainGateway.java` |
| 16 | Toolkit runs standalone | `oracle-db-mcp-toolkit/run.sh` |
| 17 | Tool name maps to bounded SQL | `oracle-db-mcp-toolkit/config/tools.yaml` |
| 18 | Oracle AI Database returns governed rows | database view snippet |
| 19 | Structured data returns along the same path | sequence diagram |
| 20 | Host reads the `ui://` resource | sequence diagram |
| 21 | Host renders sandboxed iframe | Gemini MCP App dashboard |
| 22 | Iframe populates metrics | `mcp-app/src/mcp-app.ts` |
| 23 | Iframe renders cards | `mcp-app/src/mcp-app.ts` |
| 24 | Read-only mode is visible | Gemini MCP App dashboard |
| 25 | Optional write tools are registered | `mcp-app/server.ts` |
| 26 | Iframe calls app-only approval tool | `mcp-app/src/mcp-app.ts` |
| 27 | Approval is validated | `ApprovalService.java` |
| 28 | One governed write runs | Toolkit tool config |
| 29 | Recap the responsibility boundary | architecture diagram |

Generated assets:

- `mcp-app-developer-walkthrough.mp4`
- `mcp-app-developer-walkthrough-poster.png`
- `mcp-app-developer-walkthrough.srt`
- `mcp-app-developer-walkthrough.vtt`
