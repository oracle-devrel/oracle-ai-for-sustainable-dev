# Project: Interactive Supply-Chain Applications with A2UI, AG-UI, MCP Apps, and Oracle AI Database

## Objective

Create a runnable reference application and technical blog showing Oracle AI Database as the governed data and execution layer for an interactive inventory-transfer exchange built with:

- Oracle Database MCP Java Toolkit
- AG-UI for streaming agent events and application state
- A2UI for declarative, client-rendered user interfaces
- MCP Apps for rich interfaces inside an MCP-compatible host

The application must explain where the technologies complement one another and where their responsibilities overlap.

## Core message

Oracle AI Database owns trusted retrieval, transfer feasibility rules, transactions, business logic, authorization, and auditing. The Oracle Database MCP Java Toolkit exposes safe, deterministic, bounded database capabilities as MCP tools. AG-UI carries agent progress, messages, tool activity, and state. A2UI describes trusted native controls. MCP Apps provide richer sandboxed dashboards.

## Reference use case

> Show inventory at risk of stockout, explain the demand and safety-stock factors, and let me approve a feasible transfer from another location.

1. The user submits a stockout-risk request.
2. The agent invokes a bounded MCP database tool.
3. Oracle AI Database joins inventory positions, forecasts, safety stock, products, locations, and transfer lanes to return governed recommendations.
4. AG-UI streams run, message, tool, and state events.
5. A2UI renders filters, recommendation cards, explanations, and approval controls.
6. An optional MCP App renders the same recommendations as a richer supply-chain dashboard.
7. The user selects one exact source-to-target recommendation and explicitly approves it.
8. A bounded MCP tool calls a stored procedure that revalidates current stock under locks and records the transfer in one transaction.
9. The UI displays the audited transfer result.

## Responsibility boundaries

Do not place AG-UI and A2UI inside the Oracle Database MCP Java Toolkit.

- **Oracle Database MCP Java Toolkit:** database-facing MCP tools
- **Agent service:** orchestration, approval state, MCP client, and AG-UI events
- **Web client:** AG-UI consumption and allowlisted A2UI rendering
- **MCP App:** optional embedded recommendation dashboard
- **Oracle AI Database:** system of record, feasibility rules, transactions, policies, and auditing

Extend the Toolkit only where database-specific behavior cannot be expressed safely through YAML-defined tools.

## Security requirements

- Never commit passwords, wallets, tokens, client secrets, or API keys.
- Use bind variables for user-provided values.
- Do not expose unrestricted SQL tools to the model.
- Enable only the five business tools required by the demo.
- Require explicit approval before a write.
- Bind approval to the exact database recommendation, not a client-supplied arbitrary action.
- Put the write behind a stored procedure that locks and revalidates inventory.
- Validate tool inputs and A2UI envelopes.
- Allowlist A2UI components; never execute generated JavaScript.
- Treat MCP App content as sandboxed, untrusted web content.
- Log actor, tool, timestamp, input classification, and result status without logging secrets or sensitive records.
- Roll back failed actions and enforce limits and timeouts.

## Definition of done

- Reproducible locations, products, inventory positions, transfer lanes, recommendations, and transfer audit data
- Five bounded business-tool contracts
- AG-UI event streaming for a stockout-recommendation run
- A2UI recommendation results and approval controls
- Separate MCP App inventory-transfer dashboard
- Transactional approved transfer; rejection causes no write
- Tests for reads, approval binding, rejection, invalid input, and single-use approval
- Secret-free local setup and a followable blog

Compatibility research and deliberate implementation choices are recorded in `docs/implementation-plan.md`.
