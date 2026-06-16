# Multi-Agent Graph Flow

This document explains how the Java graph agent now supports both direct Oracle Database lookups and upstream-agent-provided graph payloads.

## Goal

The graph agent needs to work in two different operating modes:

1. As a standalone graph specialist that receives a product request and queries Oracle Database directly.
2. As a downstream renderer in a larger Gemini Enterprise or A2A workflow, where another agent already collected or assembled the graph data.

Instead of forcing one model, the agent now supports both.

## Resolution Modes

The active behavior is controlled by `GRAPH_DATA_MODE`.

- `database`
  The graph agent resolves `productId`, connects to Oracle Database, runs the `GRAPH_TABLE` query, and renders the returned graph path.
- `payload`
  The graph agent requires a structured JSON graph payload and renders it without making a database call.
- `auto`
  The graph agent prefers a valid structured payload when one is present. If no structured payload is present, it falls back to the Oracle Database lookup.

Recommended default:

- `GRAPH_DATA_MODE=auto`

That gives the agent a clean direct-call path for humans and a structured handoff path for upstream agents.

## Why This Helps

This split lets us keep retrieval responsibility and rendering responsibility separate when we want to.

- In a simple flow, Gemini Enterprise or a user prompt can send `SKU-500` and let the graph agent own the database query.
- In a multi-agent flow, an upstream agent can query Oracle, merge other signals, normalize the graph, and hand the final graph payload to this renderer agent.

This keeps the graph agent focused on:

- validating graph structure
- rendering a consistent PNG artifact
- returning an A2A-friendly text summary

## Supported Inputs

The parser currently accepts:

- plain text only
- a raw JSON object as the text body
- a fenced JSON block using ```json ... ```

Accepted JSON root shapes:

- the graph payload itself
- `{ "graph": { ... } }`
- `{ "graphPayload": { ... } }`

## Canonical Payload Contract

Current schema version:

- `1.0`

Example:

```json
{
  "schemaVersion": "1.0",
  "productId": "SKU-500",
  "nodes": [
    {
      "id": "supplier",
      "type": "SUPPLIER",
      "label": "Supplier: Vertex Plastics",
      "detail": "Tier 1 | Busan",
      "metric": "On-time 92%"
    },
    {
      "id": "plant",
      "type": "PLANT",
      "label": "Plant: Columbus Final Pack",
      "detail": "Cycle 4.3 days",
      "metric": "Utilization 78%"
    },
    {
      "id": "port",
      "type": "PORT",
      "label": "Port: Houston",
      "detail": "ETA 54 hrs",
      "metric": "Delay risk 0.31"
    },
    {
      "id": "warehouse",
      "type": "WAREHOUSE",
      "label": "Warehouse: Newark Inventory Hub",
      "detail": "Inventory 4226 units",
      "metric": "Fill rate 95%"
    },
    {
      "id": "product",
      "type": "PRODUCT",
      "label": "Product: SKU-500",
      "detail": "Demand +8%",
      "metric": "Margin 30%"
    },
    {
      "id": "alert",
      "type": "ALERT",
      "label": "Alert: Weather Delay",
      "detail": "Pacific lane",
      "metric": "Risk 0.60"
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
```

## Validation Rules

The graph agent validates payloads before rendering. That is how we protect the renderer from inconsistent upstream calls.

Required:

- `nodes[]`
- `edges[]`
- each node must have `id`, `type`, and `label`
- each edge must have `from`, `to`, and `label`
- at least one `PRODUCT` node must exist

Allowed node types:

- `SUPPLIER`
- `PLANT`
- `PORT`
- `WAREHOUSE`
- `PRODUCT`
- `ALERT`

Allowed edge labels:

- `SUPPLIES`
- `SHIPS_VIA`
- `ROUTES_TO`
- `STOCKS`
- `AFFECTS`

Other checks:

- duplicate node ids are rejected
- edges must reference known node ids
- unsupported schema versions are rejected
- conflicting text `productId` and payload `productId` are rejected

Important `auto` mode rule:

- if a structured payload is present but malformed, the request fails fast

That is intentional. We do not want a silent fallback to database mode to hide bad multi-agent contracts.

## Source Reporting

Every successful response includes provenance in both the text summary and artifact metadata.

Current values:

- `sourceMode=database`
- `sourceMode=payload`

The rendered PNG and text summary also reflect the chosen source so it is easier to debug chained-agent behavior.

## Oracle Database Path

In `database` mode, the graph agent expects:

- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DSN`
- `TNS_ADMIN` or `DB_WALLET_DIR`

It then:

1. resolves the requested `productId`
2. connects through the Oracle wallet-backed JDBC config
3. runs the property-graph query
4. maps the returned row into the node and edge structure used by the renderer

The sample schema, seed data, and query patterns live under the shared repo-level [`sql/`](/path/to/repo-root/oracle-ai-database-gcp-gemini/sql).

## Upstream Agent Guidance

If another agent is calling this graph agent, the safest contract is:

1. upstream agent owns retrieval and normalization
2. upstream agent sends canonical JSON matching the schema above
3. graph agent validates and renders

Suggested upstream responsibilities:

- map source data into the canonical node and edge vocabulary
- keep ids stable within a graph payload
- set `schemaVersion`
- include `productId`
- avoid free-form structural variations unless the graph agent explicitly adds support for them

## Handling Different Content and Formats

The graph agent is intentionally permissive about where the JSON appears, but strict about the graph structure itself.

That means:

- it can accept plain text or embedded JSON in the message body
- it does not try to guess arbitrary graph schemas
- it normalizes only the contract it understands today

This is the tradeoff that keeps multi-agent integrations debuggable.

## Live Testing Notes

Current public deployment target:

- `https://YOUR_PUBLIC_AGENT_HOST/graph/.well-known/agent-card.json`
- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`
- `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`

Recommended verification pattern:

1. test plain text and confirm `sourceMode=database`
2. test a structured payload and confirm `sourceMode=payload`
3. test one malformed payload and confirm validation fails clearly

## Current Recommendation

Use:

- `GRAPH_DATA_MODE=auto`

That gives us the most flexibility while still keeping the contract explicit enough for Gemini Enterprise and A2A multi-agent flows.
