# Supply-chain inventory-transfer MCP App

This dashboard is a separate MCP App that uses the running Java service as its
governed-data and approval adapter. The Java service invokes
`find-stockout-transfer-recommendations` through the Oracle Database MCP Java
Toolkit, binds a short-lived approval handle to the exact returned rows, and
returns Toolkit-labeled data. The sandboxed
`ui://oracle-supply-chain/inventory-exchange-v2` resource receives the rows and
the widget-only handle through the MCP Apps host bridge. It never receives
database credentials or connects directly to Oracle Database.

The MCP server exposes:

- `show-inventory-transfer-dashboard` to the model and app; it is read-only.
- `approve-inventory-transfer` only to the app; it executes one exact reviewed
  recommendation.
- `reject-inventory-transfer-review` only to the app; it invalidates the
  handle without a write.

The model cannot call the approve or reject tools. The person must select a
card, inspect the route and quantity, review the notes, and click the explicit
approval button inside the MCP App.

Requires Node.js 20.19+ or 22.12+:

```bash
./run.sh
```

Keep the Java service running at `http://127.0.0.1:8080`. The MCP App server defaults to that address; set `AGENT_SERVICE_URL` only when it differs.

For a no-account local demonstration, start the pinned official MCP Apps basic host in a third terminal:

```bash
./run-basic-host.sh
```

Open `http://127.0.0.1:8082`, choose `show-inventory-transfer-dashboard`, set the stockout-risk inputs, and call the tool. The official host fetches the `ui://` resource, renders it inside its two-level sandbox, and passes the live Toolkit-governed recommendations to the dashboard.

The wrapper pins the official host source and applies one checked-in local-runtime compatibility patch: it serves the already-built sandbox HTML with a direct file read because Express `sendFile` can return `NotFoundError` for that generated asset in some macOS runtime combinations. The host, sandbox, bridge, and MCP App implementation remain the pinned official code.

ChatGPT can render this same portable MCP App without a separate UI implementation. ChatGPT supports the standard `_meta.ui.resourceUri` metadata and `ui/*` host bridge used here. To demonstrate it:

1. Keep the Java service and `./run.sh` running.
2. Make port 3001 reachable through HTTPS. Use OpenAI Secure MCP Tunnel when available, or an HTTPS development tunnel such as ngrok or Cloudflare Tunnel. ChatGPT cannot connect directly to `127.0.0.1`.
3. In ChatGPT, enable **Developer mode** under **Settings → Security and login**. If the setting is unavailable, the account or workspace administrator must allow it.
4. Open **Settings → Plugins** or `https://chatgpt.com/plugins`, select **+**, and create a developer-mode app using the tunnel URL ending in `/mcp`.
5. Start a new chat, select the app from **+ → More**, and prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 3 recommendations.`
6. Confirm that ChatGPT calls `show-inventory-transfer-dashboard`, renders the
   dashboard, and that selecting a recommendation sends its structured source,
   target, SKU, and quantity back to the conversation.
7. For an anonymous synthetic-data test, verify that the server exposes no
   approval handle or action tool, then revoke public invocation. Test approval
   and cancellation only after MCP-compatible OAuth is installed and writes
   are explicitly enabled.

See OpenAI's [MCP Apps compatibility](https://developers.openai.com/apps-sdk/mcp-apps-in-chatgpt), [Connect from ChatGPT](https://developers.openai.com/apps-sdk/deploy/connect-chatgpt), and [testing](https://developers.openai.com/apps-sdk/deploy/testing) guidance. Developer Mode availability and permissions depend on the account and workspace policy.

Claude web and Claude Desktop can use the same remote `/mcp` endpoint as a
custom interactive connector. Remote connector traffic originates from
Anthropic's cloud, so a private or loopback-only endpoint is insufficient.
Neither host is required for the local basic-host walkthrough.

To continue the host validation on another machine or account, follow
[`../docs/chatgpt-claude-mcp-app-handoff.md`](../docs/chatgpt-claude-mcp-app-handoff.md)
and complete the checked-in
[`../docs/mcp-app-host-validation-results.md`](../docs/mcp-app-host-validation-results.md)
template. The runbook includes prerequisites, database startup, tunnel choices,
current ChatGPT and Claude navigation, prompts, expected results, screenshot
names, security rules, and a copy/paste prompt for the next ChatGPT session.

Gemini Enterprise supports this same `ui://` application through a Custom MCP
Server data store. Use a dedicated private Cloud Run service and grant its
Discovery Engine service agent `roles/run.invoker`; do not make that service
anonymous. The sibling `../gemini-enterprise-a2a/` adapter remains the
alternative native path, exposing the workflow over A2A and producing A2UI
v0.8 controls. See
[`../docs/gemini-enterprise-mcp-app.md`](../docs/gemini-enterprise-mcp-app.md).

For production, use HTTPS, authentication, an explicit origin policy,
service-to-service authorization between the MCP App server and agent service,
durable approval/idempotency storage, and durable user identity rather than
the local loopback trust boundary.

## Cloud Run deployment

The repository now includes a separate Cloud Run image and deployment script:

```powershell
.\deploy\gcp\deploy-chatgpt-mcp.ps1
```

For Gemini Enterprise, deploy an independent private service:

```powershell
.\deploy\gcp\deploy-gemini-enterprise-mcp.ps1
```

This service packages the MCP App, Java service, Oracle Database MCP Java
Toolkit, and wallet mount in one container. It uses the same Direct VPC egress
and `paulparkdb_tp` secrets as the Gemini deployment, but it does not modify or
proxy the Gemini A2A service.

The resource deliberately omits optional `_meta.ui.domain`: Claude and ChatGPT
validate that stable sandbox origin using different host-specific formats, and
this app does not run an iframe-local OAuth flow that needs a stable origin.
It does advertise an explicit CSP. This self-contained single-file UI uses
empty connection and resource allowlists because it loads no external content.

The safe default is private and read-only. Set `MCP_WRITES_ENABLED=true` only
behind an MCP-compatible OAuth 2.1 resource server. The deployment script
refuses `-AllowUnauthenticated -EnableWriteActions` because app-only visibility
is host metadata, not an authorization boundary. For a time-bounded
developer-mode rendering test with synthetic data, `-AllowUnauthenticated`
keeps write tools unregistered. Remove public invocation immediately after the
test.

When a host result contains no approval handle, the widget makes that boundary
visible with a read-only mode banner and a disabled **Read-only preview**
control. It shows **Review this transfer** and the approval panel only when the
authenticated server has issued a review-bound approval handle.

See
[`../docs/chatgpt-cloud-run-deployment.md`](../docs/chatgpt-cloud-run-deployment.md)
for the verified preflight, sizing decision, commands, and access choices.
