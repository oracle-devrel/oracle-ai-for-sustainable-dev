# MCP App Developer Walkthrough Script

This is the 29-step developer walkthrough for the MCP App path. It explains how the Gemini Enterprise, ChatGPT, or Claude host discovers the MCP server, calls the dashboard tool, receives the `ui://` resource reference, and renders the governed supply-chain dashboard while Oracle AI Database remains the transaction authority.

| Step | What to show | What to say | Source |
| --- | --- | --- | --- |
| 1 | Open the Gemini Enterprise Inventory System and confirm the connector is active. | The host is the conversational application. In Gemini Enterprise, the Oracle Supply Chain MCP App connector appears as a connected data store. | [Gemini MCP App screenshot](../images/gemini-enterprise-mcp-app-dashboard.png) |
| 2 | Show the connector registration. | The connector registration points the host at the remote MCP server endpoint. This registration lives in Gemini Enterprise, not in application source. | [Gemini connector screenshot](../images/gemini-enterprise-mcp-app-enable-action.png) |
| 3 | Show the MCP tool registration. | `show-inventory-transfer-dashboard` is the model-visible MCP tool that the host can choose when the prompt asks for a dashboard. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 4 | Show `_meta.ui.resourceUri`. | The tool result advertises the MCP App resource URI, `ui://oracle-supply-chain/inventory-exchange-v2`. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 5 | Show resource registration. | The MCP server serves the compiled app HTML when the host reads the `ui://` resource. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 6 | Show CSP metadata. | The MCP App declares host-enforced CSP boundaries such as allowed resource domains. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 7 | Show the iframe app client. | The iframe app connects to the MCP Apps host bridge and waits for the tool result. | [mcp-app/src/mcp-app.ts](../mcp-app/src/mcp-app.ts) |
| 8 | Show connector authorization. | The user authorizes the connector before the host can call the remote MCP server. | [Gemini authorization screenshot](../images/gemini-enterprise-mcp-app-authorize.png) |
| 9 | Show prompt controls. | After discovery, the connector appears in the host’s connector menu and can be enabled for the chat. | [Gemini authorization screenshot](../images/gemini-enterprise-mcp-app-authorize.png) |
| 10 | Submit the dashboard prompt. | The prompt asks for an inventory transfer dashboard with stockout risk and a row limit, which matches the MCP tool description. | [Gemini MCP App screenshot](../images/gemini-enterprise-mcp-app-dashboard.png) |
| 11 | Show `tools/call`. | The host calls `show-inventory-transfer-dashboard` with parsed arguments. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 12 | Show the service call. | The MCP server posts to `/api/reviews` on the application service. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 13 | Show the controller endpoint. | The Spring Boot service receives `/api/reviews` and delegates to the runtime. | [AgentController.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AgentController.java) |
| 14 | Show review creation. | The runtime creates an approval context and requests governed recommendations. | [AgentRuntime.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AgentRuntime.java) |
| 15 | Show the Toolkit gateway. | The service calls the standalone Oracle Database MCP Java Toolkit through the gateway. | [McpToolkitSupplyChainGateway.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/McpToolkitSupplyChainGateway.java) |
| 16 | Show the Toolkit runtime. | The Oracle Database MCP Java Toolkit runs as a separate process and owns the database tool contract. | [oracle-db-mcp-toolkit/run.sh](../oracle-db-mcp-toolkit/run.sh) |
| 17 | Show the YAML tool. | `tools.yaml` maps the MCP tool name to bounded SQL, not arbitrary model-generated SQL. | [oracle-db-mcp-toolkit/config/tools.yaml](../oracle-db-mcp-toolkit/config/tools.yaml) |
| 18 | Show the database view. | Oracle AI Database returns governed rows from the recommendation view. | [database/04-views.sql](../database/04-views.sql) |
| 19 | Show the return path. | Structured data returns through Toolkit, gateway, service, MCP server, and host. | [MCP App sequence](../images/mcp-app-chatgpt-sequence.svg) |
| 20 | Show `ui://` loading. | The host reads the app resource associated with the tool result. | [MCP App sequence](../images/mcp-app-chatgpt-sequence.svg) |
| 21 | Show iframe rendering. | The host renders the developer-built app inside a sandboxed iframe or equivalent host surface. | [Gemini MCP App screenshot](../images/gemini-enterprise-mcp-app-dashboard.png) |
| 22 | Show metrics rendering. | The iframe computes metrics from `structuredContent`, such as recommendations and units to rebalance. | [mcp-app/src/mcp-app.ts](../mcp-app/src/mcp-app.ts) |
| 23 | Show recommendation cards. | The iframe renders route, risk, rationale, and action state for each governed recommendation. | [mcp-app/src/mcp-app.ts](../mcp-app/src/mcp-app.ts) |
| 24 | Show read-only mode. | If no approval handle is available, the MCP App shows read-only preview and exposes no write button. | [Gemini MCP App screenshot](../images/gemini-enterprise-mcp-app-dashboard.png) |
| 25 | Show optional write tools. | When writes are enabled, the MCP server registers app-only approval and rejection tools. | [mcp-app/server.ts](../mcp-app/server.ts) |
| 26 | Show app-only approval call. | The iframe asks the host bridge to call `approve-inventory-transfer`; it does not call Oracle AI Database directly. | [mcp-app/src/mcp-app.ts](../mcp-app/src/mcp-app.ts) |
| 27 | Show approval validation. | The service validates approval ID, recommendation ID, notes, actor, and expiration. | [ApprovalService.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/ApprovalService.java) |
| 28 | Show the write tool. | The Toolkit executes one approved stored procedure call, and Oracle AI Database records the audited result. | [oracle-db-mcp-toolkit/config/tools.yaml](../oracle-db-mcp-toolkit/config/tools.yaml) |
| 29 | Recap the boundary. | The host presents the experience, the MCP App renders the dashboard, and Oracle AI Database keeps data and transaction authority. | [Architecture diagram](../images/interactive-ai-architecture.svg) |

Generated companion assets:

- [mcp-app-developer-walkthrough.mp4](mcp-app-developer-walkthrough.mp4)
- [mcp-app-developer-walkthrough.vtt](mcp-app-developer-walkthrough.vtt)
- [mcp-app-developer-walkthrough.srt](mcp-app-developer-walkthrough.srt)
- [mcp-app-developer-walkthrough-poster.png](mcp-app-developer-walkthrough-poster.png)

