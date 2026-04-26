# Demo Import Cards

This directory is for local, import-ready Gemini Enterprise card JSON files with a real public host filled in.

The generated `*.json` files in this directory are intentionally ignored by git because they can contain environment-specific hosts or IP addresses.

To generate the concrete card files for the current demo host:

```bash
./render_demo_import_cards.sh 34.186.79.96
```

After running the script, use these files for copy/paste imports:

- `demo-import/agent-card-graph.json`
- `demo-import/agent-card-spatial.json`
- `demo-import/agent-card-select-ai.json`
- `demo-import/agent-card-action.json`
- `demo-import/agent-card-inventory-system.json`

Current note for the live April 2026 demo VM:

- The live public graph runtime that Gemini Enterprise is successfully invoking still appears to expect the legacy root execution URL.
- If the imported graph card points to `/graph` and Gemini Enterprise returns `404 Not Found` for `https://34.186.79.96/graph`, use:
  - `demo-import/agent-card-graph-live-root.json`
- Keep `demo-import/agent-card-graph.json` as the canonical `/graph` version for the newer runtime contract.
