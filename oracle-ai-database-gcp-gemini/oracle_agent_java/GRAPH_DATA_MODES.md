# Graph Data Modes

This document explains how the Java graph agent can now resolve graph data in three different ways:

- directly from Oracle Database
- from a validated structured payload supplied by another agent
- automatically choosing between those two paths

## Why This Exists

The graph agent has two legitimate deployment patterns:

1. A standalone graph agent that owns the Oracle query and renders the result.
2. A downstream renderer agent in a multi-agent workflow, where an upstream agent has already gathered or transformed the graph data.

To support both, the agent now has a configurable resolution mode controlled by `GRAPH_DATA_MODE`.

## Configuration

Supported values:

- `GRAPH_DATA_MODE=database`
- `GRAPH_DATA_MODE=payload`
- `GRAPH_DATA_MODE=auto`

Recommended default:

- `GRAPH_DATA_MODE=auto`

Behavior:

- `database`: ignore upstream graph structure and query Oracle Database using the resolved `productId`
- `payload`: require a valid structured payload and do not query Oracle Database
- `auto`: use a valid structured payload when present, otherwise query Oracle Database

## How Product Resolution Works

The agent can receive product information from:

- plain text such as `show me supply chain dependencies for SKU-500`
- a structured payload containing `"productId": "SKU-500"`

Conflict rule:

- if both are present and they disagree, the request fails with a validation error

This is intentional. Silent precedence rules make multi-agent debugging harder.

Fallback rule:

- if no explicit product is found, the current fallback remains `SKU-500`

## Supported Input Formats

The parser currently accepts:

- plain text only
- a raw JSON object as the text body
- a fenced JSON block such as:

```json
{
  "productId": "SKU-500",
  "nodes": [],
  "edges": []
}
```

It also accepts these top-level JSON shapes:

- the payload itself
- `{ "graph": { ... } }`
- `{ "graphPayload": { ... } }`

## Structured Payload Contract

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

The graph agent validates payload input before rendering.

Required:

- top-level object
- `nodes[]`
- `edges[]`
- every node must have `id`, `type`, and `label`
- every edge must have `from`, `to`, and `label`
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

Other validation:

- duplicate node ids are rejected
- edges must reference known node ids
- unsupported `schemaVersion` values are rejected
- malformed payloads in `auto` mode fail fast rather than silently falling back to the database

That last rule is important: if a caller tried to send structured graph data and got the shape wrong, we want to know immediately.

## Database Mode

In `database` mode, the graph agent:

1. resolves the requested `productId`
2. connects to Oracle Database using `DB_USERNAME`, `DB_PASSWORD`, `DB_DSN`, and `TNS_ADMIN` or `DB_WALLET_DIR`
3. runs the `GRAPH_TABLE` query against `SUPPLY_CHAIN_GRAPH`
4. converts the result row into the node/edge shape used by the renderer

This mode is the best fit when:

- the graph agent owns the supply-chain query
- you want the renderer tightly coupled to the database model
- the upstream caller only knows a product or question, not the full graph payload

## Payload Mode

In `payload` mode, the graph agent:

1. requires a valid structured payload
2. validates it strictly
3. renders it without making a database call

This mode is the best fit when:

- another agent already queried Oracle Database
- another agent merged multiple data sources
- you want the graph agent to be a pure renderer

## Auto Mode

In `auto` mode, the graph agent:

1. checks whether a structured payload is present
2. if the payload is valid, renders it
3. if no structured payload is present, queries the database
4. if a structured payload is present but invalid, returns an error instead of silently switching to DB mode

This is the best default for mixed direct-call and multi-agent usage.

## How This Helps Multi-Agent Flows

An upstream agent can now do either of these:

- pass only `productId` and let the graph agent query Oracle directly
- pass a fully materialized graph payload and let the graph agent only validate and render

That separation is useful because it lets you decide where responsibility lives:

- retrieval responsibility in one agent
- visualization responsibility in another agent

## Current Recommendation

Use:

- `GRAPH_DATA_MODE=auto`

And have the upstream caller follow this rule:

- if you have reliable structured graph data, send it as JSON
- if you only know the product, send plain text or a product id and let the graph agent query Oracle

That gives us strong validation without making the graph agent brittle.
