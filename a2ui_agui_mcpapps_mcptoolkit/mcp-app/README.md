# Supply-chain inventory-transfer MCP App

This dashboard is a separate MCP App that uses the running Java service as its governed-data adapter. The Java service invokes `find-stockout-transfer-recommendations` through the Oracle Database MCP Java Toolkit; the MCP App server validates that Toolkit-labeled response and returns it as structured tool content. The sandboxed `ui://oracle-supply-chain/inventory-exchange-v1` resource receives those rows through the MCP Apps host bridge. It never receives database credentials or connects directly to Oracle Database.

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
5. Start a new chat, select the app from **+ → More**, and prompt: `Show the inventory transfer dashboard for products with a minimum stockout risk of 70, limited to 10 recommendations.`
6. Confirm that ChatGPT calls `show-inventory-transfer-dashboard`, renders the dashboard, and that selecting a recommendation sends its structured source, target, SKU, and quantity back to the conversation.

See OpenAI's [MCP Apps compatibility](https://developers.openai.com/apps-sdk/mcp-apps-in-chatgpt), [Connect from ChatGPT](https://developers.openai.com/apps-sdk/deploy/connect-chatgpt), and [testing](https://developers.openai.com/apps-sdk/deploy/testing) guidance. Developer Mode availability and permissions depend on the account and workspace policy.

Claude web and Claude Desktop remain optional host demonstrations. Expose port 3001 through an HTTPS tunnel and add the resulting `/mcp` URL as a custom connector. Neither ChatGPT nor Claude is required for the local basic-host walkthrough.

For production, use HTTPS, authentication, an explicit origin policy, service-to-service authorization between the MCP App server and agent service, and durable user identity rather than the local loopback trust boundary.
