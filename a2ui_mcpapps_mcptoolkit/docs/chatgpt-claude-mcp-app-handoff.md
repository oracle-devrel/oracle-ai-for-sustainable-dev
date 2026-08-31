# ChatGPT and Claude MCP App validation handoff

This runbook is the complete handoff for validating the supply-chain MCP App in
ChatGPT and/or Claude from another machine and account. It was prepared on
2026-07-28. Product labels and settings can change, so check the linked official
documentation before changing application code.

## Goal

Prove that the existing standards-based MCP App can:

1. connect to ChatGPT and/or Claude through a remote MCP connection;
2. invoke `show-inventory-transfer-dashboard`;
3. render the `ui://oracle-supply-chain/inventory-exchange-v2` dashboard;
4. display current Oracle Database MCP Java Toolkit-governed recommendations;
5. send a selected recommendation back to the host conversation;
6. explicitly approve one exact transfer through an app-only tool and verify
   its audited result;
7. cancel a second review and verify that no write occurs; and
8. produce publication-quality evidence for a later update to `blog.html`.

Do not redesign the application or rewrite the blog during this validation.
Record verified host behavior and capture evidence. The Oracle blog-writing
session will use that evidence to finish the article.

## Repository and implementation

- Repository:
  `https://github.com/paulparkinson/oracle-ai-for-sustainable-dev`
- Branch: `main`
- Project: `a2ui_mcpapps_mcptoolkit`
- Main application: Java service and web client on port `8080`
- MCP App server: TypeScript Streamable HTTP server on port `3001`
- Local account-free MCP Apps host: port `8082`
- MCP tool: `show-inventory-transfer-dashboard`
- App-only tools: `approve-inventory-transfer` and
  `reject-inventory-transfer-review`
- UI resource:
  `ui://oracle-supply-chain/inventory-exchange-v2`
- UI MIME type: the MCP Apps `RESOURCE_MIME_TYPE`
- Governed review adapter:
  `POST http://127.0.0.1:8080/api/reviews`
- Read-only diagnostic adapter:
  `GET http://127.0.0.1:8080/api/recommendations`
- Database path:
  MCP App → Java adapter → Oracle Database MCP Java Toolkit → Oracle AI Database

The relevant files are:

- `mcp-app/server.ts`: tool, resource, Streamable HTTP endpoint, validation
- `mcp-app/src/mcp-app.ts`: dashboard and host-context interaction
- `mcp-app/mcp-app.html`: dashboard markup and styles
- `mcp-app/run.sh`: install, typecheck, build, and server startup
- `mcp-app/run-basic-host.sh`: pinned official local MCP Apps host
- `agent-service/run.sh`: Java service and Toolkit startup
- `oracle-db-mcp-toolkit/config/tools.yaml`: governed database tool definitions
- `blog.html`: publication article; leave its final prose update for the
  Oracle blog-writing session

The port-8080 web application demonstrates AG-UI and A2UI. It does not embed
the MCP App. ChatGPT, Claude, or the official basic host renders the separate
MCP App returned by the port-3001 MCP server.

At handoff on 2026-07-28, the local baseline was verified: Java tests passed,
the TypeScript package typechecked and built, and the A2UI v0.8 payload tests
passed. The server registers one model-visible dashboard tool plus two
app-only action tools and the `ui://` resource. The remote ChatGPT and Claude
host checks are the intentionally outstanding work.

## What must be available on the other machine

- Git
- JDK 21 or newer
- Maven 3.9 or newer
- Node.js 20.19+, 22.12+, or a newer compatible release
- npm
- `curl`
- Network access to GitHub and package repositories for the first build
- Access to the Oracle `financialdb_high` database
- The Autonomous Database wallet copied by a secure channel
- The `FINANCIAL` database password supplied locally
- For ChatGPT: an account/workspace with Developer mode enabled
- For Claude: an account allowed to add a custom connector
- A public HTTPS development tunnel, or OpenAI Secure MCP Tunnel for ChatGPT

Never commit the wallet, `.env`, passwords, API keys, OAuth secrets, tunnel
credentials, or database output containing sensitive data.

## Clone and configure

```bash
git clone https://github.com/paulparkinson/oracle-ai-for-sustainable-dev.git
cd oracle-ai-for-sustainable-dev
git switch main
git pull --ff-only

cd a2ui_mcpapps_mcptoolkit
cp .env.example .env
```

Edit `.env` locally:

```text
TNS_ADMIN=/absolute/path/to/Wallet_financialdb
DB_USERNAME=FINANCIAL
DB_PASSWORD=<local-secret>
```

The `.env` file and `.runtime/` directory are ignored. Do not copy the current
machine's ignored runtime directories to the other machine; allow the scripts
to build clean local runtimes.

## Start and verify the Oracle-backed application

Run the database setup once:

```bash
cd a2ui_mcpapps_mcptoolkit/agent-service
./setup-database.sh
./test.sh

cd ../oracle-db-mcp-toolkit
./run.sh full

cd ../agent-service
./run.sh
```

Keep both processes running. The Java service connects to the standalone
Toolkit over authenticated Streamable HTTP, verifies the exact five-tool
allowlist, and exposes the application on port `8080`.

In another terminal:

```bash
curl --fail --silent --show-error \
  http://127.0.0.1:8080/api/health

curl --fail --silent --show-error \
  'http://127.0.0.1:8080/api/recommendations?minimumStockoutRisk=70&maximumRows=10'
```

The recommendation response must contain:

```json
{
  "source": "oracle-db-mcp-java-toolkit"
}
```

The exact recommendations can change after approved transfers. That is
expected; they are live database results, not static fixture data.

## Start and preflight the MCP App

In a second long-running terminal:

```bash
cd a2ui_mcpapps_mcptoolkit/mcp-app
./run.sh
```

The script installs pinned dependencies, typechecks, builds the single-file UI,
and starts:

```text
http://127.0.0.1:3001/mcp
```

Verify both services:

```bash
curl --fail --silent --show-error \
  http://127.0.0.1:3001/health

curl --fail --silent --show-error \
  'http://127.0.0.1:8080/api/recommendations?minimumStockoutRisk=70&maximumRows=10'
```

Optionally run the official local host before involving a remote product:

```bash
cd a2ui_mcpapps_mcptoolkit/mcp-app
./run-basic-host.sh
```

Open `http://127.0.0.1:8082`, select
`show-inventory-transfer-dashboard`, and invoke it with:

```json
{
  "minimumStockoutRisk": 70,
  "maximumRows": 10
}
```

Do not proceed to ChatGPT or Claude until the local host renders the dashboard
with the message “Live governed results from the Oracle Database MCP Java
Toolkit.”

The official MCP Inspector is another useful transport check:

```bash
npx @modelcontextprotocol/inspector@latest
```

Configure Streamable HTTP with `http://127.0.0.1:3001/mcp`, list the tools, and
invoke `show-inventory-transfer-dashboard`. Inspector should list three tools:
the dashboard tool with model-and-app visibility, plus approve and reject tools
with app-only visibility.

If MCP Inspector is unavailable, this stateless Streamable HTTP request checks
tool discovery directly:

```bash
curl --fail --silent --show-error \
  -X POST http://127.0.0.1:3001/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  --data '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

The response must contain `show-inventory-transfer-dashboard`,
`approve-inventory-transfer`, `reject-inventory-transfer-review`, their
visibility metadata, and `ui://oracle-supply-chain/inventory-exchange-v2`.
ChatGPT's model-facing discovery should expose only the dashboard tool; the
rendered app can call the two app-only tools.

## Make the MCP endpoint reachable

ChatGPT and Claude cloud connections cannot call `127.0.0.1`. Choose one
approach and keep it running throughout registration and testing.

### Option A: OpenAI Secure MCP Tunnel for ChatGPT

This is the preferred ChatGPT option when the account has the required
permissions because the local server remains private.

OpenAI currently requires:

- a `tunnel_id` created in Platform tunnel settings;
- a runtime API key for `tunnel-client`;
- Tunnels Read + Manage to create or edit the tunnel;
- Tunnels Read + Use to run or select it; and
- separate ChatGPT Developer mode access.

Associate the tunnel with the target ChatGPT workspace and Platform
organization. Follow the generated quickstart in Platform tunnel settings and
use the local HTTP MCP URL:

```text
http://127.0.0.1:3001/mcp
```

Run `tunnel-client doctor --profile <profile> --explain`, then keep
`tunnel-client run --profile <profile>` running. Do not commit the tunnel ID,
runtime API key, profile, or generated credentials.

### Option B: temporary public HTTPS tunnel

Use a trusted development tunnel such as ngrok or Cloudflare Tunnel to forward:

```text
http://127.0.0.1:3001
```

The URL registered in either product must include the MCP path:

```text
https://<temporary-hostname>/mcp
```

Before registration, verify the public endpoint with MCP Inspector. Treat this
unauthenticated demo tunnel as temporary: start it only for the validation,
do not share the URL, and stop it immediately afterward.

## ChatGPT validation

Current official OpenAI steps:

1. Open ChatGPT **Settings → Security and login**.
2. Enable **Developer mode**. Availability depends on account and workspace
   policy.
3. Open `https://chatgpt.com/plugins`.
4. Select the plus button.
5. Use a clear name such as `Oracle Supply-Chain Inventory Exchange`.
6. Add a concise description explaining that it displays Oracle Database MCP
   Java Toolkit-governed inventory-transfer recommendations.
7. For a public tunnel, enter the HTTPS URL ending in `/mcp`. For OpenAI Secure
   MCP Tunnel, choose **Tunnel** and select the associated tunnel.
8. Review the discovered tool and metadata. The server should expose
   `show-inventory-transfer-dashboard`.
9. Start a new chat and enable the new plugin from the tools menu.

Use this direct prompt:

```text
Show the inventory transfer dashboard for products with a minimum stockout
risk of 70, limited to 10 recommendations.
```

Expected result:

- ChatGPT calls `show-inventory-transfer-dashboard`.
- The tool arguments are `minimumStockoutRisk: 70` and `maximumRows: 10`.
- The MCP App renders inline in a sandboxed iframe.
- The source message identifies the Oracle Database MCP Java Toolkit.
- Recommendation cards show SKU, route, transfer quantity, risk, and rationale.
- Clicking **Review this transfer** adds a structured selection such as SKU,
  source, target, and quantity to the conversation.
- Clicking **Approve and execute transfer** invokes the app-only approval tool
  and returns an audited transfer ID.
- Clicking **Cancel review** in a fresh review invokes the app-only rejection
  tool and performs no database write.

Then run:

```text
Show only recommendations with a minimum stockout risk of 85, limited to 3.
```

```text
Select one recommendation in the dashboard and explain why its proposed
source-to-target transfer is feasible.
```

```text
Write a poem about warehouses. Do not use the inventory exchange.
```

The first prompt tests different arguments, the second tests host-context
follow-up, and the third checks that an unrelated request does not invoke the
tool.

Record the selected tool, arguments, result, errors, and UI behavior. If server
metadata changes, restart the server, open its connection at
`https://chatgpt.com/plugins`, select **Refresh**, and start a new chat.

## Claude validation

Anthropic currently documents custom remote MCP connectors for Claude web,
Claude Desktop, Cowork, and mobile. The connection originates from Anthropic's
cloud even when using Claude Desktop, so the remote HTTPS endpoint must be
publicly reachable. Individual accounts can add custom connectors; Team and
Enterprise workspaces can require an Owner or Primary Owner to add one.

For an individual account:

1. Open **Customize → Connectors**.
2. Select **+ → Add custom connector**.
3. Name it `Oracle Supply-Chain Inventory Exchange`.
4. Enter the public HTTPS URL ending in `/mcp`.
5. Leave OAuth fields empty for this temporary unauthenticated local demo.
6. Add the connector.
7. Start a new conversation.
8. Use the lower-left **+ → Connectors** menu to enable it for that
   conversation.

For Team or Enterprise, an owner first adds it under
**Organization settings → Connectors → Add → Custom → Web**; the user then
connects it under **Customize → Connectors**.

Use the same direct, bounded prompt:

```text
Show the inventory transfer dashboard for products with a minimum stockout
risk of 70, limited to 10 recommendations.
```

Expected result:

- Claude invokes `show-inventory-transfer-dashboard`.
- The interactive connector renders the MCP App inline or in its supported
  expanded presentation.
- The dashboard shows Toolkit-governed results.
- In an anonymous read-only validation deployment, the dashboard displays an
  explicit read-only banner and a disabled **Read-only preview** control.
- In a separately authenticated write-enabled deployment, **Review this
  transfer** can pass the selected transfer into host context and the app-only
  approval or rejection tools can be tested.

If the UI does not appear, confirm that the connector is connected and enabled,
start a new conversation, and verify the public endpoint again with MCP
Inspector. Disconnecting and reconnecting can refresh connector state. If an
installed Chrome-app/PWA surface fails, open the same Claude conversation in a
regular browser tab and compare behavior before changing the server.

## Evidence to capture

Capture screenshots at native resolution. Crop only irrelevant browser chrome;
do not crop tool names, product labels, source attribution, or the selected
recommendation.

Use these repository paths:

- `images/chatgpt-mcp-app-dashboard.png`
- `images/chatgpt-mcp-app-selection.png`
- `images/claude-mcp-app-dashboard.png`
- `images/claude-mcp-app-selection.png`

Only add files for hosts actually tested. Each dashboard screenshot should
show:

- the ChatGPT or Claude host;
- the rendered inventory-transfer dashboard;
- at least one recommendation;
- the Toolkit source label; and
- no secrets, wallet paths, tunnel credentials, or private URLs.

Each selection screenshot should show the dashboard action and the resulting
conversation context. Use synthetic/demo inventory only.

Complete `docs/mcp-app-host-validation-results.md` with:

- date and timezone;
- operating system and browser/desktop client;
- host and account type without an email address;
- transport type without credentials or private identifiers;
- prompts;
- tool arguments;
- observed results;
- screenshot filenames;
- failures and fixes; and
- anything the blog must describe differently from this runbook.

## Definition of done

For each tested host:

- [ ] Java service health passes.
- [ ] Recommendation response is labeled
      `oracle-db-mcp-java-toolkit`.
- [ ] MCP App health passes.
- [ ] MCP Inspector discovers and invokes the tool.
- [ ] The remote product connects to the `/mcp` endpoint.
- [ ] The expected tool and arguments are visible.
- [ ] The interactive MCP App renders.
- [ ] Toolkit-governed recommendation data appears.
- [ ] Selection returns structured transfer context to the conversation.
- [ ] Explicit MCP App approval returns an audited transfer ID.
- [ ] A separate cancellation performs no database write.
- [ ] Unrelated prompts do not invoke the tool.
- [ ] Evidence contains no secrets.
- [ ] Validation results and screenshots are committed and pushed to `main`.

Stop the public tunnel and remove or disable the temporary connector after
capturing the evidence.

## Copy/paste prompt for the other ChatGPT session

```text
Clone or update
https://github.com/paulparkinson/oracle-ai-for-sustainable-dev on branch main.
Read
a2ui_mcpapps_mcptoolkit/docs/chatgpt-claude-mcp-app-handoff.md completely
and follow it. Validate the existing supply-chain MCP App in ChatGPT and, if
available, Claude. Do not redesign the app and do not rewrite blog.html.
Diagnose and make only the minimal compatibility fixes required for the
standards-based MCP App to connect and render. Never commit .env, wallets,
passwords, API keys, tunnel IDs, tunnel URLs, OAuth secrets, or sensitive logs.
Capture the requested sanitized screenshots, complete
docs/mcp-app-host-validation-results.md, run the documented local checks, then
commit and push the verified evidence and any necessary compatibility fixes to
main. In your final response, report the commit hash, tested hosts, successful
prompts, screenshot paths, and any remaining limitation for the Oracle
blog-writing session.
```

## Official references

- [OpenAI: Add UI to your MCP server](https://developers.openai.com/plugins/build/chatgpt-ui)
- [OpenAI: Connect and test your plugin](https://developers.openai.com/plugins/deploy/connect-chatgpt)
- [OpenAI: Secure MCP Tunnel](https://developers.openai.com/api/docs/guides/secure-mcp-tunnels)
- [MCP Apps specification](https://modelcontextprotocol.io/extensions/apps/overview)
- [Anthropic: Custom connectors using remote MCP](https://support.claude.com/en/articles/11175166-get-started-with-custom-connectors-using-remote-mcp)
- [Anthropic: Interactive connectors in Claude](https://support.claude.com/en/articles/13454812-use-interactive-connectors-in-claude)
- [Anthropic: Troubleshoot MCP Apps](https://claude.com/docs/connectors/building/mcp-apps/troubleshooting)
- [Anthropic: Cross-platform MCP Apps](https://claude.com/docs/connectors/building/mcp-apps/cross-compatibility)
