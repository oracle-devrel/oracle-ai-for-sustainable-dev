# Demo Import Cards

This directory is for import-ready Gemini Enterprise card JSON examples with a real public host filled in.

Use the checked-in examples as snapshots. If you generate private local variants with environment-specific hosts or IP addresses, save them with a `.local.json` suffix so they stay ignored.

To generate the concrete card files for the current demo host:

```bash
./render_demo_import_cards.sh YOUR_PUBLIC_AGENT_HOST
```

After running the script, use these files for copy/paste imports:

- `agent-cards/demo-import/agent-card-graph.local.json`
- `agent-cards/demo-import/agent-card-spatial.local.json`
- `agent-cards/demo-import/agent-card-select-ai.local.json`
- `agent-cards/demo-import/agent-card-action.local.json`
- `agent-cards/demo-import/agent-card-inventory-system.local.json`

Current note for the live April 2026 demo VM:

- The live public graph runtime that Gemini Enterprise is successfully invoking still appears to expect the legacy root execution URL.
- If the imported graph card points to `/graph` and Gemini Enterprise returns `404 Not Found` for `https://34.186.79.96/graph`, use:
  - `agent-cards/demo-import/agent-card-graph-live-root.json`
- Keep `agent-cards/demo-import/agent-card-graph.json` as the canonical `/graph` version for the newer runtime contract.
