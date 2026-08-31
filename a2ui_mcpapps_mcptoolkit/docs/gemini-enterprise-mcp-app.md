# Gemini Enterprise MCP App deployment

This is the repository-specific runbook for the workflow described in Romain
Lancelot's June 12, 2026 guide, *Running Interactive MCP Apps in Gemini
Enterprise*. It adds an MCP App path alongside the existing Gemini Enterprise
A2A/A2UI path; it does not replace that native integration.

## Architecture and security boundary

Gemini Enterprise connects to a dedicated private Cloud Run service. The
service packages the TypeScript MCP server, its self-contained `ui://` HTML
resource, the Java agent service, the Oracle Database MCP Java Toolkit, and the
wallet-backed connection to `paulparkdb_tp`.

The deployment deliberately differs from the temporary ChatGPT and Claude
validation endpoint:

- Cloud Run anonymous invocation is disabled.
- Only the project's Discovery Engine service agent receives
  `roles/run.invoker` on this service.
- `MCP_WRITES_ENABLED=false`, so the service advertises one bounded read-only
  dashboard tool and never returns an approval handle.
- The existing ChatGPT/Claude service is left unchanged.

## 1. Deploy the private service

Authenticate `gcloud` as `paul.parkinson@oracle.com`, then run:

```powershell
cd a2ui_mcpapps_mcptoolkit
.\deploy\gcp\deploy-gemini-enterprise-mcp.ps1
```

The script builds a distinct image, deploys
`oracle-supply-chain-mcp-gemini` in `us-east4`, and grants Cloud Run invocation
to:

```text
service-<PROJECT_NUMBER>@gcp-sa-discoveryengine.iam.gserviceaccount.com
```

Record the final HTTPS URL ending in `/mcp`.

### Verified deployment record

On August 7, 2026, the script created Cloud Build
`15851fc0-fb2f-4f82-88b5-b1287b01f494` and deployed revision
`oracle-supply-chain-mcp-gemini-00001-fkv` at:

```text
https://oracle-supply-chain-mcp-gemini-54d5grsrkq-uk.a.run.app/mcp
```

Anonymous health access returned HTTP 403, an authenticated health request
returned HTTP 200, and the service IAM policy contained one invoker:
`service-1093068138027@gcp-sa-discoveryengine.iam.gserviceaccount.com`. Startup
logs confirmed that the Oracle Database MCP Java Toolkit initialized all five
governed tools and that the MCP App server started on the Cloud Run port.

## 2. Create the OAuth client

In the `adb-pm-prod` Google Cloud project:

1. Open **APIs & Services > Credentials**.
2. Select **Create credentials > OAuth client ID**.
3. Choose **Web application**.
4. Add this exact authorized redirect URI:

   ```text
   https://vertexaisearch.cloud.google.com/oauth-redirect
   ```

5. For this developer setup, put the client ID and secret only in the ignored
   local `.env` placeholders. Never put the secret in `.env.example`, Git, a
   screenshot, this runbook, or the blog. Use Secret Manager for production.

## 3. Create the Custom MCP Server data store

Open **Gemini Enterprise / Agent Builder > Data stores**, choose **Create data
store**, and select **Custom MCP Server**. Enter:

| Field | Value |
|---|---|
| MCP Server URL | The private Cloud Run URL ending in `/mcp` |
| Authorization URL | `https://accounts.google.com/o/oauth2/auth` |
| Authorization URL parameters | `&access_type=offline` |
| Token URL | `https://oauth2.googleapis.com/token` |
| Client ID / secret | The OAuth web client from the previous step |
| Scopes | `openid email profile https://www.googleapis.com/auth/cloud-platform` |
| PKCE support | Enabled |

Select **Login**, complete the Google authorization prompt, and use these
repository-specific values:

**Description**

> An interactive Oracle-governed supply-chain inventory exchange. It returns
> bounded stockout-risk and source-to-target transfer recommendations from
> Oracle AI Database through the Oracle Database MCP Java Toolkit and renders
> them as a sandboxed MCP App dashboard.

**Agent instructions**

> Use this server when a user asks about stockout exposure, inventory
> rebalancing, or transfer recommendations. Call
> `show-inventory-transfer-dashboard` with the user's minimum risk and row
> limit. Tell the user that the interactive Supply-Chain Inventory Exchange
> dashboard has loaded. Do not claim that the preview executed a transfer: this
> deployment is read-only and exposes no approval or rejection action.

### Organization-policy prerequisite

Custom MCP Server creation can be disabled centrally by the managed constraint
`discoveryengine.managed.disableCustomMcpServerConnector`. The first creation
attempt in `adb-pm-prod` was denied because this constraint was effectively
enforced and inherited from an Oracle parent folder or organization; there was
no project-level policy. The account could read the effective project result
but did not have `orgpolicy.policy.get` on the parents.

An Organization Policy Administrator at an Oracle parent folder or organization
must create a project-scoped exception that sets this constraint to **Not
enforced** for `adb-pm-prod`, or place the project under an approved parent
policy. The project Owner role is insufficient, and Google does not allow
`roles/orgpolicy.policyAdmin` to be granted on the project itself. Do not
silently bypass a corporate control. Supply the administrator with project
number `1093068138027`, organization `168803584848`, constraint name, business
purpose, and the fact that the target Cloud Run service is private, read-only,
and invokable only by the Discovery Engine service agent. The checked-in
`deploy/gcp/gemini-custom-mcp-org-policy.yaml` is the exact project-scoped
override for an authorized administrator to apply.

## 4. Enable the action

1. Wait for the new data store status to become **Active**.
2. Open its **Actions** tab.
3. Select **Reload custom actions**.
4. Select `show-inventory-transfer-dashboard` and choose **Enable actions**.
5. Connect the data store to the `Inventory System` Gemini Enterprise app if
   the creation flow did not do so automatically.

## 5. Validate the interactive result

In the Gemini Enterprise web app, submit:

```text
Show the inventory transfer dashboard for products with a minimum stockout
risk of 70, limited to 3 recommendations.
```

The model-visible tool result and the widget should agree on the deterministic
sample row: `WATER-SENSE`, `PHX-DC` to `SEA-FC`, 42 units, stockout risk 74.6.
The widget must show its read-only banner and disabled **Read-only preview**
control.

Capture sanitized screenshots of the data-store configuration, enabled action,
and rendered widget. Do not include the OAuth secret, access tokens, wallet
contents, or database password.

## References

- Romain Lancelot, *Running Interactive MCP Apps in Gemini Enterprise*, June
  12, 2026 (the guide supplied with this project update)
- [Official MCP Apps examples](https://github.com/modelcontextprotocol/ext-apps)
- [MCP Apps overview](https://modelcontextprotocol.io/extensions/apps/overview)
- [Cloud Run service-to-service authentication](https://cloud.google.com/run/docs/authenticating/service-to-service)
