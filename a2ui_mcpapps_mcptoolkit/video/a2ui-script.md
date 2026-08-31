# A2UI Developer Walkthrough Script

This is the companion developer walkthrough for the A2UI path. It shows how the same Oracle AI Database-backed recommendation workflow can render native review controls through A2UI rather than loading a full MCP App iframe.

| Step | What to show | What to say | Source |
| --- | --- | --- | --- |
| 1 | Start with the architecture map. | A2UI lets an agent return a declarative UI recipe that the host renders with approved native components. | [Architecture diagram](../images/interactive-ai-architecture.svg) |
| 2 | Show the version compatibility note. | This project has two A2UI paths: standalone browser A2UI v0.9.1 over AG-UI and Gemini Enterprise A2UI v0.8 over A2A DataParts. | [blog.html](../blog.html) |
| 3 | Show the standalone web page. | The local browser example demonstrates A2UI alongside AG-UI event streaming. | [web-client/index.html](../web-client/index.html) |
| 4 | Start the local Toolkit. | The Oracle Database MCP Java Toolkit runs as its own process and exposes governed tools. | [oracle-db-mcp-toolkit/run.sh](../oracle-db-mcp-toolkit/run.sh) |
| 5 | Start the application service. | The Spring Boot service is the orchestration layer between UI protocols and database tools. | [agent-service/run.sh](../agent-service/run.sh) |
| 6 | Submit the local review request. | The browser asks for stockout transfer recommendations and opens a streaming run. | [web-client/app.js](../web-client/app.js) |
| 7 | Show AG-UI event handling. | The browser receives AG-UI events and listens for custom A2UI messages. | [web-client/app.js](../web-client/app.js) |
| 8 | Show the SSE endpoint. | The service streams AG-UI events from `/api/runs/stream`. | [AgentController.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AgentController.java) |
| 9 | Show official AG-UI event names. | The service emits run, text, tool-call, state, and custom events. | [AguiRunService.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AguiRunService.java) |
| 10 | Show the custom A2UI event. | A2UI messages are carried in AG-UI `CUSTOM` events named `a2ui.message`. | [AguiRunService.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AguiRunService.java) |
| 11 | Show A2UI surface creation. | The standalone path creates an A2UI v0.9.1 surface with a basic catalog and theme. | [A2uiPayloads.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/A2uiPayloads.java) |
| 12 | Show approved components. | The service declares Image, Text, List, Card, TextField, Row, and Button components. | [A2uiPayloads.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/A2uiPayloads.java) |
| 13 | Show data binding. | Recommendation values from Oracle AI Database are bound into the A2UI data model. | [A2uiPayloads.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/A2uiPayloads.java) |
| 14 | Show the Toolkit gateway. | The service asks the Toolkit for bounded transfer recommendations. | [McpToolkitSupplyChainGateway.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/McpToolkitSupplyChainGateway.java) |
| 15 | Show the YAML tool definition. | The Toolkit maps `find-stockout-transfer-recommendations` to bounded SQL. | [oracle-db-mcp-toolkit/config/tools.yaml](../oracle-db-mcp-toolkit/config/tools.yaml) |
| 16 | Show the recommendation view. | Oracle AI Database calculates feasible source-to-target transfer candidates. | [database/04-views.sql](../database/04-views.sql) |
| 17 | Show the rendered local A2UI surface. | The host renders a native review form from the recipe rather than executing generated frontend code. | [local app screenshot](../images/local-end-to-end-application.png) |
| 18 | Show approval state. | The data model carries the approval ID, selected recommendation, write permission, and approval notes. | [A2uiPayloads.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/A2uiPayloads.java) |
| 19 | Click approve in the local app. | The user explicitly approves one exact transfer from the rendered controls. | [web-client/app.js](../web-client/app.js) |
| 20 | Show approval endpoint. | The service receives approval and validates it before any database write. | [AgentController.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/AgentController.java) |
| 21 | Show approval validation. | The approval service prevents arbitrary transfer substitution after review. | [ApprovalService.java](../agent-service/src/main/java/com/oracle/demo/interactiveai/ApprovalService.java) |
| 22 | Show the write tool. | The Toolkit executes the approved write through a stored procedure. | [oracle-db-mcp-toolkit/config/tools.yaml](../oracle-db-mcp-toolkit/config/tools.yaml) |
| 23 | Switch to Gemini Enterprise A2A. | Gemini Enterprise receives A2UI as DataParts instead of AG-UI custom events. | [gemini-enterprise-a2a/main.py](../gemini-enterprise-a2a/main.py) |
| 24 | Show the agent card. | The A2A adapter advertises the A2UI extension and supported catalog. | [gemini-enterprise-a2a/main.py](../gemini-enterprise-a2a/main.py) |
| 25 | Show DataPart creation. | The adapter wraps each A2UI message with `create_a2ui_part`. | [gemini-enterprise-a2a/main.py](../gemini-enterprise-a2a/main.py) |
| 26 | Show Gemini A2UI payloads. | The Gemini path builds v0.8 `beginRendering`, `surfaceUpdate`, and `dataModelUpdate` messages. | [gemini-enterprise-a2a/a2ui_payloads.py](../gemini-enterprise-a2a/a2ui_payloads.py) |
| 27 | Show Gemini rendered review. | Gemini Enterprise renders the review surface from the A2UI DataParts. | [Gemini A2UI screenshot](../images/gemini-enterprise-a2ui-review.png) |
| 28 | Show the graph A2UI extension. | The graph agent now returns portable A2UI cards and inspect buttons, plus its deterministic PNG. | [GraphA2uiPayloads.java](../../oracle-ai-database-gcp-gemini/oracle_agent_java/src/main/java/oracleai/GraphA2uiPayloads.java) |
| 29 | Recap the boundary. | A2UI gives controlled native UI, while Oracle AI Database remains the governed data and transaction layer. | [A2UI host boundary diagram](../images/a2ui-host-process-boundary.svg) |

Generated companion assets:

- `a2ui-developer-walkthrough.mp4`
- `a2ui-developer-walkthrough.vtt`
- `a2ui-developer-walkthrough.srt`
- `a2ui-developer-walkthrough-poster.png`

