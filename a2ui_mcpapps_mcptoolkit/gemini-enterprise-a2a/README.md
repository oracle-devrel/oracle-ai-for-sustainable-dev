# Gemini Enterprise A2A/A2UI adapter

This native integration does not consume the MCP App `ui://` resource. It uses
an A2A endpoint carrying A2UI messages. Gemini Enterprise can separately load
the sibling MCP App through a Custom MCP Server data store; see
[`../docs/gemini-enterprise-mcp-app.md`](../docs/gemini-enterprise-mcp-app.md).
As of 2026-07-28, Gemini Enterprise supports A2A v0.3 streaming semantics and
A2UI v0.8.

This adapter presents the same governed inventory-transfer workflow through
that host contract:

```text
Gemini Enterprise
  -> A2A v0.3 JSON-RPC
  -> this adapter
  -> Java agent-service review/approval API
  -> Oracle Database MCP Java Toolkit
  -> Oracle AI Database
```

The adapter does not add a second set of business rules. It retrieves a
short-lived review from the Java service, converts the exact recommendations
to deterministic A2UI v0.8 `beginRendering`, `surfaceUpdate`, and
`dataModelUpdate` messages, and handles `userAction` messages for approval or
rejection. Oracle still revalidates the selected transfer under locks.

## Run locally

Keep the Java service running on port 8080, then:

```bash
./run.sh
```

The default A2A endpoint is `http://127.0.0.1:3002`. For remote registration,
set `PUBLIC_A2A_URL` to the public HTTPS base URL and host the adapter where
Gemini Enterprise can reach it.

Run only the deterministic payload tests with:

```bash
python3 -m unittest discover -s tests
```

## Register with Gemini Enterprise

1. Deploy the Java service, Toolkit, and this adapter behind authenticated
   service boundaries.
2. Set `AGENT_SERVICE_URL` to the private Java-service URL.
3. Set `PUBLIC_A2A_URL` to the adapter's public HTTPS URL.
4. Verify the agent card at `/.well-known/agent-card.json`.
5. In Gemini Enterprise, register a custom agent via A2A using that agent card.
6. Ask for stockout-risk recommendations and verify the native A2UI review.

For the `adb-pm-prod` implementation, use the checked-in Cloud Run build,
Secret Manager, deployment, and registration scripts in `../deploy/gcp/`.
The complete runbook is
[`../docs/gemini-enterprise-gcp-deployment.md`](../docs/gemini-enterprise-gcp-deployment.md).

Gemini Enterprise A2UI support is currently Preview and limited to v0.8. The
standalone web client intentionally remains on A2UI v0.9.1. The two payload
builders are separate adapters over one domain API; do not silently downgrade
the browser protocol or send v0.9.1 envelopes to Gemini Enterprise.
For production, configure Model Armor in this A2A application path; Gemini
Enterprise's console-level Model Armor settings do not automatically protect
separately hosted A2A agents.

Current Google documentation:

- [Register A2UI/A2A agents](https://docs.cloud.google.com/gemini/enterprise/docs/a2ui-agents/register-and-manage-an-a2ui-agent)
- [Register A2A agents](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
- [Gemini Enterprise and A2UI integration guide](https://cloud.google.com/blog/topics/developers-practitioners/guide-to-gemini-enterprise-and-a2ui-integration)
