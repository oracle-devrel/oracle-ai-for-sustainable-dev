# MCP App host validation results

This record captures the completed ChatGPT and Claude read-only validations.
Do not record emails, passwords, API keys, wallet paths,
tunnel IDs, tunnel credentials, private endpoints, or other secrets.

## Test environment

- Date and timezone: August 3, 2026, America/New_York
- Git commit tested: Cloud Run deployment based on `c6a3a1e`
- Host and account type: ChatGPT with Developer mode
- Transport: temporary public HTTPS Cloud Run endpoint, immediately returned to private

## Local preflight

- Java service health:
- Recommendation source equals `oracle-db-mcp-java-toolkit`:
- MCP App health:
- Official basic-host result:
- MCP Inspector discovery and invocation:

## ChatGPT

- Tested: yes, read-only phase
- Developer mode available: yes
- Connection created: yes, `Supply-Chain Inventory Exchange`
- Discovered tool: `show-inventory-transfer-dashboard` only
- Direct prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 3 recommendations.`
- Tool arguments: `minimumStockoutRisk=70`, `maximumRows=3`
- Dashboard rendered: yes, inline
- Toolkit source label visible: yes
- Live result: `WATER-SENSE`, `PHX-DC` to `SEA-FC`, 42 units, risk 74.6
- App-only approval returned audited transfer ID: not tested; writes disabled and no approval handle issued
- Separate cancellation performed no write: not tested in this read-only phase
- Dashboard screenshot: `images/chatgpt-mcp-app-dashboard.png`
- Setup screenshots: `images/chatgpt-developer-mode.png`, `images/chatgpt-plugin-connection.png`, `images/chatgpt-plugin-tool-discovery.png`
- Errors and minimal fixes: first load reported `Failed to fetch template`; retry rendered successfully. CSP enforcement was off. A later revision added portable CSP metadata while omitting host-specific `ui.domain`; a CSP-enforced ChatGPT retest remains pending.

## Claude

- Tested: yes, read-only phase on August 6, 2026
- Connector type and account category: custom web connector in Claude; no account identifier recorded
- Connection created: yes, `Supply-Chain Inventory Exchange`
- Discovered tool: `show-inventory-transfer-dashboard` only
- Direct prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 3 recommendations.`
- Tool arguments: bounded by the prompt to `minimumStockoutRisk=70`, `maximumRows=3`
- Dashboard rendered: yes in a regular Chrome tab; the installed Chrome-app/PWA surface returned a generic reachability error
- Toolkit source label visible: yes
- Live result: `WATER-SENSE`, `PHX-DC` to `SEA-FC`, 42 units, risk 74.6
- Selection returned to conversation: not tested; public validation mode intentionally returned no approval handle
- App-only approval returned audited transfer ID: not tested; writes disabled and action tools not registered
- Separate cancellation performed no write: not tested in this read-only phase
- Follow-up prompt result: not tested
- Negative prompt avoided tool invocation: not tested
- Dashboard screenshot: `images/claude-mcp-app-dashboard.png`
- Setup screenshots: `images/claude-add-custom-connector.png`, `images/claude-custom-connector-endpoint.png`, `images/claude-connector-connected.png`, `images/claude-connector-tool-permissions.png`, `images/claude-chat-enable-connector.png`
- Errors and minimal fixes: removed host-specific optional `ui.domain`, retained an explicit self-contained CSP, awaited `app.connect()`, and changed the resource URI to `inventory-exchange-v2` to bypass cached markup. The final UI also replaces the misleading review action with an explicit read-only banner and disabled preview control whenever the server returns no approval handle.

## Blog handback

- Confirmed statements the blog can make: Claude in a regular browser discovered the one interactive tool, received live Toolkit data, and rendered the Oracle-backed MCP App inline.
- Product-specific wording or navigation that changed: `Settings > Connectors > Add > Add custom connector`; enable the connector from the chat plus menu.
- Limitations or caveats: the installed Chrome-app/PWA surface failed while the regular browser succeeded; no OAuth-protected write action was tested.
- Suggested screenshot captions and alt text: use the descriptive filenames and captions now included in `blog.html`.

