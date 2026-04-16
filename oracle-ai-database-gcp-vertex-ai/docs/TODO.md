# TODO

This file tracks the main follow-up work for the Oracle AI Database GCP Vertex AI demo.

## Highest Priority

- Improve the Select AI agent's multi-turn behavior so follow-up prompts can safely use prior answer context.
- Replace the seeded external-signal step in the inventory-action flow with a real Deep Research or external-intelligence handoff.
- Refresh VM ADC for the ADK path so the inventory-action agent can use live ADK reasoning instead of deterministic fallback.

## Graph And Spatial NL2SQL

Deferred by request for later:

- Add hybrid NL2SQL to the graph agent.
- Add hybrid NL2SQL to the spatial agent.
- Keep the current direct SQL path as fallback.

Recommended implementation shape:

- `GRAPH_QUERY_MODE=direct|nl2sql|auto`
- `SPATIAL_QUERY_MODE=direct|nl2sql|auto`
- use Select AI `showsql`
- validate SQL in Java before execution
- allow only `SELECT`
- allow only approved flattened views
- reject unsafe or malformed SQL

## Deep Research Integration

- Document and demo the manual Deep Research handoff clearly.
- Add a normalized external-risk payload format for the action agent.
- Optionally turn Deep Research into a downstream tool or agent call later.
- Capture cited external risks into shared coordinator state.

## Multi-Agent Orchestration

- Decide whether the end goal should be:
  - in-process orchestration with specialist tools
  - runtime A2A calls between specialist agents
  - or a hybrid of both
- Add stronger evidence provenance in the action agent response.
- Add approval-state persistence and draft-action persistence.

## Select AI Hardening

- Improve prompt shaping for broad risk-summary questions.
- Add a safer deterministic fallback trigger when the live Select AI result is clearly poor.
- Add regression tests around prompts like:
  - `Which products are at risk of stockouts next quarter?`
  - `Now summarize the result in business language.`
  - `For SKU-500, which warehouses are driving the risk and why?`

## Demo Hardening

- Add a concise "safe prompts only" section to all live demo docs.
- Add a quick pre-demo verification checklist.
- Add a screenshot or saved artifact directory for the latest known-good graph and spatial outputs.

## Infrastructure

- Document and reproduce the GCP deployment in a second project.
- Start with a new compute instance in `oracle-public-488519`.
- Move HTTPS, service, wallet, env, and repo setup into a repeatable runbook.
- Optionally automate the VM setup with startup scripts or Terraform later.
