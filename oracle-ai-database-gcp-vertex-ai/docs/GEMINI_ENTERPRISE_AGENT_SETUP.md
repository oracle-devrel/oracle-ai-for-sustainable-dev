# Gemini Enterprise Agent Setup

This runbook describes which Oracle demo agents are ready to import into Gemini Enterprise right now, what each one does today, and how to test the end-to-end flow.

Replace placeholder values such as `YOUR_PUBLIC_AGENT_HOST`, `YOUR_VM_SSH_USER`, `YOUR_SELECT_AI_PROFILE_NAME`, and `/path/to/repo-root` with your own environment-specific values before running commands.

## Current Agent List

There are currently four importable agent cards for the Oracle AI Database demo:

1. Graph agent
   Import URL:
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`

   What it does:
   - queries the Oracle Database property graph for seeded products such as `SKU-500`
   - can also render a validated upstream payload for products such as `SKU-777`
   - returns a PNG graph artifact plus a short text summary

   Current live card:
   - `name`: `oracle_graph_agent`
   - `version`: `0.0.1`

2. Spatial agent
   Import URL:
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`

   What it does:
   - renders a hotspot PNG map for warehouse pressure and transfer direction
   - uses Oracle-backed inventory-risk tables when available
   - falls back to seeded demo data when those tables are not yet present

   Current live card:
   - `name`: `oracle_spatial_agent`
   - `version`: `0.0.1`

3. Select AI agent
   Import URL:
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`

   What it does:
   - answers natural-language inventory questions
   - uses `DBMS_CLOUD_AI.GENERATE` through the live `YOUR_SELECT_AI_PROFILE_NAME` profile
   - falls back to direct Oracle SQL summaries only if the profile is unavailable
   - now uses a generic schema-aware prompt, so it is not limited to the stockout-summary demo question
   - is currently most reliable when you use self-contained prompts instead of short conversational follow-ups

   Current live card:
   - `name`: `oracle_select_ai_agent`
   - `version`: `0.0.1`

4. Inventory action agent
   Import URL:
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`

   What it does:
   - coordinates the final action stage
   - gathers graph, spatial, and external-signal evidence
   - recommends an action such as transfer, expedite, substitute, or hold
   - drafts a move but does not execute it

   Current live card:
   - `name`: `oracle_inventory_action_agent`
   - `version`: `0.0.1`

## Important Current Caveats

- The graph agent is production-demo ready for the current story and is the main path to keep imported.
- The graph payload path is supported, but Gemini Enterprise may still rewrite pasted JSON in surprising ways. The parser now tolerates inline JSON, fenced JSON, and markdown-style escaped brackets such as `\[` and `\]`.
- The spatial agent is now a real separate hotspot-map implementation in the shared Java runtime.
- The Select AI agent is live and currently configured for real database-side generation through `DBMS_CLOUD_AI.GENERATE`.
- The Select AI agent is currently more reliable as a single-turn agent than as a memory-driven multi-turn analyst. Prompts like `Now summarize the result in business language.` should be replaced with a fresh self-contained question.
- The inventory action agent is live and working, but the ADK model path on the VM is currently falling back to deterministic local orchestration because the VM-side ADC refresh is stale.

## ADC Note

For the ADK-based inventory action agent, `gcloud auth application-default login` matters directly.

What that command does:
- creates or refreshes `~/.config/gcloud/application_default_credentials.json`

What matters for this demo:
- running it on your laptop refreshes only your laptop ADC
- the live Java runtime is running on the GCP VM
- the VM must also have valid ADC if you want the ADK model path to run live instead of fallback mode

Current behavior:
- graph agent does not depend on live Gemini model credentials
- spatial agent does not depend on live Gemini model credentials
- Select AI fallback mode does not depend on live Gemini model credentials
- inventory action agent does try the ADK path first
- if ADC cannot refresh on the VM, the service falls back to deterministic orchestration instead of returning `500`

## Recommended Gemini Enterprise Imports

If you want the cleanest current demo setup in Gemini Enterprise, import these four:

1. Graph agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`

2. Spatial agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`

3. Select AI agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`

4. Inventory action agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`

If the graph agent is already imported in Gemini Enterprise and you want the latest runtime behavior, re-import or update it from:

`https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`

Canonical graph runtime path:

`https://YOUR_PUBLIC_AGENT_HOST/graph`

Note:
- the live cards still report `version: 0.0.1`
- if you want explicit semantic version bumps in the cards themselves, that should be a separate follow-up change

## Gemini Enterprise Test Steps

### 1. Import The Agents

In Gemini Enterprise:

1. Open `Agents`
2. Choose `Add agent`
3. Import each of these four URLs:
   - `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`
   - `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`
   - `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`
   - `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`
4. Leave auth empty for now because these endpoints are publicly reachable over HTTPS

### 1a. How To Actually Talk To A Specific Agent

After the agents are installed in Gemini Enterprise:

1. Open `Agents` in the left navigation
2. Click the specific agent you want to test
3. Start a new chat for that agent
4. Paste one of the prompts from the sections below

Use these names in the UI:

- `oracle_graph_agent`
- `oracle_spatial_agent`
- `oracle_select_ai_agent`
- `oracle_inventory_action_agent`

If Gemini Enterprise shows a general chat with multiple agents available, make sure the intended agent is the one currently selected before you send the prompt.

### 2. Test The Graph Agent

Select:
- `oracle_graph_agent`

Use this prompt:

```text
Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.
```

Expected result:
- a completed response
- a PNG graph image
- text summary mentioning `SKU-500`

This path should use the database.

### 3. Test The Payload Graph Path

Select:
- `oracle_graph_agent`

Use this prompt:

````text
Render this supply-chain dependency graph exactly from the structured payload below.
Do not query the database.
Use the payload as the authoritative graph input.

```json
{
  "graphPayload": {
    "schemaVersion": "1.0",
    "productId": "SKU-777",
    "nodes": [
      {
        "id": "supplier",
        "type": "SUPPLIER",
        "label": "Supplier: Blue Ocean Resins",
        "detail": "Tier 2 | Singapore",
        "metric": "On-time 97%"
      },
      {
        "id": "plant",
        "type": "PLANT",
        "label": "Plant: Austin Assembly",
        "detail": "Cycle 3.2 days",
        "metric": "Utilization 81%"
      },
      {
        "id": "port",
        "type": "PORT",
        "label": "Port: Long Beach",
        "detail": "ETA 22 hrs",
        "metric": "Delay risk 0.18"
      },
      {
        "id": "warehouse",
        "type": "WAREHOUSE",
        "label": "Warehouse: Reno DC",
        "detail": "Inventory 8120 units",
        "metric": "Fill rate 98%"
      },
      {
        "id": "product",
        "type": "PRODUCT",
        "label": "Product: SKU-777",
        "detail": "Demand +12%",
        "metric": "Margin 34%"
      },
      {
        "id": "alert",
        "type": "ALERT",
        "label": "Alert: Customs Review",
        "detail": "West coast lane",
        "metric": "Risk 0.27"
      }
    ],
    "edges": [
      { "from": "supplier", "to": "plant", "label": "SUPPLIES" },
      { "from": "plant", "to": "port", "label": "SHIPS_VIA" },
      { "from": "port", "to": "warehouse", "label": "ROUTES_TO" },
      { "from": "warehouse", "to": "product", "label": "STOCKS" },
      { "from": "alert", "to": "port", "label": "AFFECTS" }
    ]
  }
}
```
````

Expected result:
- PNG graph image
- text summary for `SKU-777`
- this path should not use the database

If Gemini Enterprise keeps mutating your paste, try sending only the JSON object or check whether the UI inserted markdown escapes such as `\[` and `\]`.

### 4. Test The Spatial Agent

Select:
- `oracle_spatial_agent`

Use this prompt:

```text
Show that on a map for SKU-500 and highlight the warehouse hotspots plus the best relief route.
```

Expected result:
- a completed response
- a PNG hotspot map
- text summary mentioning Newark as the primary pressure node and DFW as the relief source

Current local verification uses the seeded hotspot rows when the live Oracle hotspot tables are not yet present on the host.

### 5. Test The Select AI Agent

Select:
- `oracle_select_ai_agent`

Best live prompt right now:

```text
Using only the Oracle inventory risk demo tables, list the top products at risk of stockouts next quarter, including stockout probability, projected revenue impact, and primary region.
```

Other good prompts to try:

```text
For SKU-500, summarize in business language which warehouses are driving the risk and why.
```

```text
For SKU-700, which warehouse has the lowest coverage days and what hotspot score and revenue impact does it have?
```

```text
What is the projected revenue impact for each product in the current quarter?
```

```text
Show SQL for the warehouse hotspot metrics for SKU-500.
```

Avoid for now:

- `Now summarize the result in business language.`
- `Based on that...`
- `Tell me more about the previous answer...`

Those conversational follow-ups do not yet carry enough prior result context through the current Select AI agent implementation.

Expected result today:
- a completed text response
- next-quarter risk summary for `SKU-500`, `SKU-700`, and `SKU-900`
- regional-driver detail for `SKU-500`
- metadata showing `executionMode=select-ai`
- metadata showing `sourceDetail=DBMS_CLOUD_AI.GENERATE via profile YOUR_SELECT_AI_PROFILE_NAME`

This path is already wired to the live database profile, so the answer should be database-backed rather than fallback.

### 6. Test The Inventory Action Agent

Select:
- `oracle_inventory_action_agent`

Use this prompt:

```text
What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.
```

Expected result today:
- completed text response
- recommendation to transfer `500` units from `Warehouse: DFW Hub` to `Warehouse: Newark Inventory Hub`
- explicit note that approval is required
- current response may mention deterministic fallback because VM ADC refresh is stale

Latest verified local response shape:
- recommended move: transfer `500` units
- source: `Warehouse: DFW Hub`
- destination: `Warehouse: Newark Inventory Hub`
- approval: required
- draft action id: `draft-transfer-sku-500`

### 7. Suggested Demo Order

For the current state of the demo, this is the best Gemini Enterprise order:

1. Ask the Select AI agent:
   `Using only the Oracle inventory risk demo tables, list the top products at risk of stockouts next quarter, including stockout probability, projected revenue impact, and primary region.`

2. Ask the Select AI agent again for drill-down:
   `For SKU-500, summarize in business language which warehouses are driving the risk and why.`

3. Ask the spatial agent:
   `Show that on a map for SKU-500 and highlight the warehouse hotspots plus the best relief route.`

4. Ask the graph agent:
   `Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.`

5. Ask Deep Research:
   `Research external factors over the next 90 days that could worsen supply risk for SKU-500, especially around Newark, Northeast distribution, upstream ports, and supplier lanes. Include weather, port congestion, labor actions, trade policy, and geopolitical issues. Return the top risks with citations and likely operational impact.`

6. Ask the inventory action agent:
   `What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.`

## Naming Note

For Gemini Enterprise imports, use the four matching alias-style URLs:

- `agent-card-graph.json`
- `agent-card-spatial.json`
- `agent-card-select-ai.json`
- `agent-card-action.json`

The legacy root path `/.well-known/agent-card.json` still exists for backward compatibility and still points to the graph agent, but the alias URLs plus the canonical `/graph` runtime path are the clearest and most consistent way to think about the four-agent setup.

## Next Recommended Follow-Up

If you want the inventory action agent to use live ADK reasoning instead of fallback mode on the VM, the next step is:

1. SSH to the VM as `YOUR_VM_SSH_USER`
2. Run:

```bash
gcloud auth application-default login
```

3. Restart the Java service

The Select AI agent is already using the live database-side profile:

1. Oracle DB credential and profile created from the pattern in:
   [configure_select_ai_openai_profile.sql](/path/to/repo-root/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/sql/configure_select_ai_openai_profile.sql)
2. Shared environment set to `SELECT_AI_PROFILE=YOUR_SELECT_AI_PROFILE_NAME`
3. Java service restarted on the GCP VM

If you ever need to recreate it, repeat those same three steps.
