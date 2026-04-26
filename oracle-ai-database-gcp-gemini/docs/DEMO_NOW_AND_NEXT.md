# Demo Now And Next

This doc captures the practical guidance for the current live demo, the current A2A and multi-agent state, how to incorporate Deep Research right now, and what the end-goal architecture should be.

## Best To Use Right Now

For the current live demo, the most reliable approach is:

- Use `oracle_select_ai_agent` with self-contained prompts.
- Use `oracle_spatial_agent` for the hotspot map.
- Use `oracle_graph_agent` for the supply-chain dependency image.
- Use `Deep Research` manually as the external-intelligence step.
- Use `oracle_inventory_action_agent` last to tell the "insight to action" story.

## Best Current Prompts

Use these prompts as-is for the live demo:

### Select AI

Use a self-contained prompt instead of short follow-ups like `Now summarize the result in business language.`

Recommended:

```text
Using only the Oracle inventory risk demo tables, list the top products at risk of stockouts next quarter, including stockout probability, projected revenue impact, and primary region.
```

Good drill-down:

```text
For SKU-500, summarize in business language which warehouses are driving the risk and why.
```

Avoid for now:

- `Now summarize the result...`
- `Based on that...`
- `Tell me more about the previous answer...`

The current Select AI agent is much more reliable as a single-turn agent than as a memory-driven multi-turn analyst.

### Spatial

```text
Show that on a map for SKU-500 and highlight the warehouse hotspots plus the best relief route.
```

### Graph

```text
Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.
```

### Inventory Action

```text
What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.
```

## What A2A Is Doing Right Now

A2A is actively being used today, but mainly at the Gemini Enterprise integration boundary.

What is live today:

- Gemini Enterprise imports the Oracle agents from their A2A agent-card URLs.
- Gemini Enterprise invokes those agents over A2A JSON-RPC.
- The graph, spatial, Select AI, and inventory-action agents are all real A2A agent surfaces.

What is not happening yet:

- The custom Oracle agents are not yet calling each other over A2A at runtime.
- The inventory-action flow is exposed as an A2A agent, but its internal orchestration is currently in-process Java.

So the honest current wording is:

- `A2A-exposed specialist agents inside Gemini Enterprise`
- `human-in-the-loop multi-agent flow`

Avoid saying:

- `fully automated agent-to-agent runtime orchestration`

## Do We Have Multi-Agent Flows Right Now

Yes, in two practical forms:

- A manual multi-agent flow in Gemini Enterprise:
  1. Select AI identifies risk.
  2. Spatial visualizes hotspots.
  3. Graph explains dependency/root cause.
  4. Deep Research adds external context.
  5. Inventory Action recommends what to do.

- An in-process orchestrated flow in `oracle_inventory_action_agent`:
  - it gathers graph evidence
  - it gathers spatial evidence
  - it adds external-signal context
  - it drafts an action recommendation

## How To Use Deep Research In The Demo Right Now

The simplest and best current use of Deep Research is as a manual external-factors step.

Use it after the internal Oracle-grounded steps:

1. Ask Select AI what is at risk.
2. Ask Spatial where the pressure is.
3. Ask Graph why the risk exists.
4. Ask Deep Research what external factors could worsen the risk.
5. Ask Inventory Action what to do given all of the above.

Recommended Deep Research prompt:

```text
Research external factors over the next 90 days that could worsen supply risk for SKU-500, especially around Newark, Northeast distribution, upstream ports, and supplier lanes. Include weather, port congestion, labor actions, trade policy, and geopolitical issues. Return the top risks with citations and likely operational impact.
```

This completes the intended storyboard without requiring us to automate the Deep Research handoff yet.

## Best End Goal

The best long-term shape is:

- A grounded Oracle data agent for internal facts.
- A real spatial specialist.
- A real graph specialist.
- A real external-intelligence step, likely Deep Research or another tool-backed agent.
- A final action coordinator that can combine all of that evidence.

The best final architecture would include:

- explicit cross-agent handoff of structured evidence
- real runtime A2A or tool-based downstream specialist calls
- shared session state across turns
- approval and policy gates before execution
- a hybrid direct-SQL plus NL2SQL strategy for graph and spatial where appropriate

## Current Gaps

Current major gaps are:

- Select AI multi-turn memory is not yet robust.
- Graph and spatial do not yet use Select AI/NL2SQL for richer dynamic query interpretation.
- Deep Research is not yet wired into the action flow automatically.
- Inventory action still falls back when VM ADC is stale.

## Minimal Live Demo Flow

If you need to demo in the next few minutes, use this exact story:

1. `oracle_select_ai_agent`
   - Ask which products are at risk next quarter.
2. `oracle_spatial_agent`
   - Show the hotspot map for `SKU-500`.
3. `oracle_graph_agent`
   - Show the dependency graph for `SKU-500`.
4. `Deep Research`
   - Research external factors for `SKU-500`.
5. `oracle_inventory_action_agent`
   - Ask what action to take based on the internal and external evidence.

That is the cleanest current "from insight to action" demo.

## Related Docs

- [GEMINI_ENTERPRISE_AGENT_SETUP.md](./GEMINI_ENTERPRISE_AGENT_SETUP.md)
- [TODO.md](./TODO.md)
- [oracle_agent_java/README.md](../oracle_agent_java/README.md)
- [oracle_agent_java/MULTI_AGENT_GRAPH_FLOW.md](../oracle_agent_java/MULTI_AGENT_GRAPH_FLOW.md)
