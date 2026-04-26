# Oracle AI Database GCP Gemini Demo

This README now uses placeholders such as `YOUR_PUBLIC_AGENT_HOST`, `YOUR_VM_SSH_USER`, `YOUR_SELECT_AI_PROFILE_NAME`, and `/path/to/repo-root` so it can be shared more safely. Replace them with your environment-specific values when you run the demo.

## Doc Index

Use this section as the main entry point for the project documentation.

Primary runbooks and status:

- [README.md](./README.md): main project overview, shared mental model, demo flow, and top-level architecture.
- [docs/DEMO_NOW_AND_NEXT.md](./docs/DEMO_NOW_AND_NEXT.md): what is best to use right now in the live demo, what is not reliable yet, how Deep Research fits today, and the end-goal architecture.
- [docs/TODO.md](./docs/TODO.md): deferred work items, near-term hardening, and roadmap items such as NL2SQL for graph and spatial.
- [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](./docs/GEMINI_ENTERPRISE_AGENT_SETUP.md): Gemini Enterprise imports, tested prompts, caveats, and expected behavior.
- [docs/GCP_INFRA_SETUP.md](./docs/GCP_INFRA_SETUP.md): GCP setup and migration guide, starting with the compute-instance plan for moving this demo to another project.
- [docs/GCP_SETUP_PROGRESS.md](./docs/GCP_SETUP_PROGRESS.md): live migration notes and step-by-step progress toward recreating this demo in a second GCP project.

Java runtime docs:

- [oracle_agent_java/README.md](./oracle_agent_java/README.md): shared Java runtime, live agent surfaces, HTTPS, and runtime behavior.
- [oracle_agent_java/GRAPH_DATA_MODES.md](./oracle_agent_java/GRAPH_DATA_MODES.md): graph-agent `database|payload|auto` behavior and payload contract.
- [oracle_agent_java/MULTI_AGENT_GRAPH_FLOW.md](./oracle_agent_java/MULTI_AGENT_GRAPH_FLOW.md): graph handoff patterns for A2A and multi-agent flows.
- [oracle_agent_java/HTTPS_SETUP.md](./oracle_agent_java/HTTPS_SETUP.md): Let's Encrypt and HTTPS deployment notes.

Python runtime reference:

- [oracle_agent_python/README.md](./oracle_agent_python/README.md): earlier Python A2A spatial-style agent reference.

Additional project docs:

- [docs/README.md](./docs/README.md)
- [docs/ADK_AGENT_README.md](./docs/ADK_AGENT_README.md)
- [docs/ADK_RAG_IMPLEMENTATION.md](./docs/ADK_RAG_IMPLEMENTATION.md)
- [docs/AGENT_FILES_COMPARISON.md](./docs/AGENT_FILES_COMPARISON.md)
- [docs/AGENT_README.md](./docs/AGENT_README.md)
- [docs/API_README.md](./docs/API_README.md)
- [docs/BUG_REPORT_ADK_MCP.md](./docs/BUG_REPORT_ADK_MCP.md)
- [docs/BUG_REPORT_ADK_MCP_ONEOF.md](./docs/BUG_REPORT_ADK_MCP_ONEOF.md)
- [docs/DIRECTORY_STRUCTURE.md](./docs/DIRECTORY_STRUCTURE.md)
- [docs/EMBEDDINGS_COMPARISON.md](./docs/EMBEDDINGS_COMPARISON.md)
- [docs/MCP_IMPLEMENTATION_SUMMARY.md](./docs/MCP_IMPLEMENTATION_SUMMARY.md)
- [docs/MCP_TOOLBOX_README.md](./docs/MCP_TOOLBOX_README.md)
- [docs/QUICK_START_ADK_MCP.md](./docs/QUICK_START_ADK_MCP.md)
- [docs/QUICK_START_SUMMARY.md](./docs/QUICK_START_SUMMARY.md)
- [docs/TESTING_RESULTS.md](./docs/TESTING_RESULTS.md)
- [docs/WORKSHOP.md](./docs/WORKSHOP.md)
- [docs/oracle_ai_database_adk_agent.md](./docs/oracle_ai_database_adk_agent.md)

This repo is set up for multiple A2A-style agents. A common pattern in this repo is:

1. A caller sends a user message to an agent over A2A.
2. The agent runtime interprets that message, either with a Gemini model or with deterministic application logic.
3. The agent decides whether to call one of its local tools.
4. The tool runs locally and returns structured data.
5. The agent returns the final A2A response, often as a text message, an image artifact, or both.

## Shared Mental Model

There are three separate pieces involved in every agent:

- Client or orchestrator: Sends the user's message to the agent. In local development this is usually `test.sh` or `test.py`. In production this could be Gemini Enterprise or another A2A caller.
- Agent runtime: The service that receives the message and runs either deterministic routing or a model-driven reasoning loop.
- Tool code: Local functions such as `generate_warehouse_map(...)` that do the specialized work.

That means the local test harness is **not** acting as Gemini itself. It is acting as the caller. When an agent is model-driven, the Gemini or Vertex-backed model call happens inside the running agent service.

## Why Credentials Are Needed

The local tool functions do not need a Gemini API key by themselves. A running model-driven agent does need Google model credentials, because the agent is still LLM-driven:

- The model reads the incoming user message.
- The model decides whether a tool should be called.
- The model incorporates the tool result into the final response.

Without Gemini API or Vertex AI credentials, a model-driven flow fails before tool selection, even if the tool logic is completely local.

If you build a purely deterministic agent endpoint that directly calls local code without an LLM reasoning step, then that endpoint would not need Gemini/Vertex model credentials.

The current sample agents in this repo are now mostly deterministic and image-first:

- the Java graph agent returns a PNG graph artifact
- the Java spatial agent returns a PNG hotspot-map artifact
- the Java Select AI agent now uses a live Oracle `DBMS_CLOUD_AI` profile for database-side natural-language generation
- the Java inventory-action agent tries ADK first and falls back cleanly when host ADC is stale

So the current local demo does not require live Vertex AI or Gemini API auth for the graph or spatial paths. The Select AI path now depends on the database-side `DBMS_CLOUD_AI` profile instead of local model credentials. The shared Vertex settings are still useful repo infrastructure for the ADK-backed action stage and for future model-driven agents.

## Shared Repo Configuration

The repo root has a shared config file at [`.env`](/path/to/repo-root/oracle-ai-database-gcp-gemini/.env). Agent scripts can source this so we only maintain credentials, ports, model names, and related settings in one place.

This repo now defaults to Vertex AI in that file:

- `GOOGLE_GENAI_USE_VERTEXAI=true`
- `GOOGLE_CLOUD_PROJECT`
- `GOOGLE_CLOUD_LOCATION`

Important shared values:

- `GOOGLE_API_KEY`
- `GOOGLE_GENAI_USE_VERTEXAI`
- `GOOGLE_CLOUD_PROJECT`
- `GOOGLE_CLOUD_LOCATION`
- `GOOGLE_IMPERSONATE_SERVICE_ACCOUNT`
- `PUBLIC_PROTOCOL`
- `PUBLIC_HOST`
- `BIND_HOST`
- `PORT`
- `SPATIAL_AGENT_PORT`
- `GRAPH_AGENT_PORT`
- `A2A_URL`
- `SPATIAL_AGENT_URL`
- `GRAPH_AGENT_URL`
- `MODEL_NAME`

## Credentials

You have two common options:

### Gemini API

If you use this path, you need a Gemini API key.

Create or manage a Gemini API key in Google AI Studio, then place it in:

```bash
GOOGLE_API_KEY="your-api-key"
```

Official docs:

- Gemini API keys: https://ai.google.dev/gemini-api/docs/api-key
- Gemini quickstart: https://ai.google.dev/gemini-api/docs/quickstart

### Vertex AI

If you use this path, you do not need a Gemini API key. Your local app uses
Application Default Credentials from `gcloud auth application-default login`.

Use Vertex AI project settings instead:

```bash
GOOGLE_GENAI_USE_VERTEXAI=true
GOOGLE_CLOUD_PROJECT="your-gcp-project"
GOOGLE_CLOUD_LOCATION="us-central1"
```

Authenticate ADC with the repo helper:

```bash
./auth.sh
```

`auth.sh` sources the repo [`.env`](/path/to/repo-root/oracle-ai-database-gcp-gemini/.env), runs `gcloud auth application-default login`, and then runs `gcloud auth application-default set-quota-project` for `GOOGLE_CLOUD_PROJECT`.

If you want to use service account impersonation, set:

```bash
GOOGLE_IMPERSONATE_SERVICE_ACCOUNT="service-account@your-project.iam.gserviceaccount.com"
```

Official docs:

- Vertex AI auth setup: https://cloud.google.com/vertex-ai/generative-ai/docs/start/gcp-auth
- Vertex AI Gemini quickstart: https://cloud.google.com/vertex-ai/generative-ai/docs/start/quickstarts/try-gen-ai

Inference from Google's docs: the Vertex AI ADC path is the better fit for this repo if you want shared GCP auth across multiple agents.

## Agent Directories

- [oracle_agent_python](./oracle_agent_python/README.md): earlier Python A2A agent for warehouse map workflows. It is now mostly a reference implementation.
- [oracle_agent_java](./oracle_agent_java/README.md): shared Java/Spring Boot A2A runtime serving the graph agent, spatial agent, Select AI agent, and inventory-action coordinator from one process.
- [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](./docs/GEMINI_ENTERPRISE_AGENT_SETUP.md): current Gemini Enterprise import URLs, which four agents to create, test prompts, expected behavior, payload-path notes, and the ADC caveat for the ADK action coordinator.

## Demo Flow

The current demo storyboard is:

1. Gemini Enterprise calls a Select AI-style Oracle inventory analyst to identify inventory risk, such as likely stockouts next quarter.
2. The same conversation drills into which warehouses, counties, or regions are driving the risk while preserving conversational context.
3. A spatial specialist renders the hotspot map so the user can see where the pressure is concentrated.
4. A graph specialist renders the supplier dependency chain so the user can see why the risk exists.
5. A Deep Research or external-intel step layers on weather, geopolitics, or other outside factors that may worsen or reduce the risk.
6. A final multi-agent action step proposes an inventory move, such as shifting units from one warehouse to another, and turns insight into an operational recommendation.

## Recommended Java Orchestration

The best use of the Java ADK pieces here is as an orchestrator around the existing specialist agents, not as a replacement for the deterministic graph renderer.

Recommended shape:

- Keep the current A2A specialists focused and deterministic. The Java runtime can own graph rendering, hotspot-map rendering, and Java-heavy business logic, while the Python spatial sample remains a useful reference.
- Add a Java ADK coordinator for the final action stage. Use an `LlmAgent` for intent interpretation and recommendation synthesis, `ParallelAgent` for concurrent evidence gathering, `SequentialAgent` for the overall decision pipeline, and `FunctionTool` wrappers for policy checks and action execution.
- Treat the existing specialist agents as downstream tools or A2A calls. The coordinator should gather risk rows, map output, graph output, and external research, then write normalized fields into shared session state before making a recommendation.
- Put the actual inventory move behind a human or policy gate. The right last step for the demo is not immediate execution; it is proposal, validation, approval, and only then execution.

A good final-stage pipeline would be:

1. Intake agent: convert the live conversation into a structured inventory case with SKU, affected warehouses, service-level risk, and business impact.
2. Parallel evidence step: fan out to graph, spatial, and external-intel specialists and collect their outputs.
3. Decision agent: rank options such as transfer, expedite, substitute, or hold based on cost, coverage days, and operational risk.
4. Policy or approval tool: check business rules, thresholds, and whether a human approval is required.
5. Execution tool: create the transfer or replenishment action in the target system only after the approval result is positive.

That is the point where ADK Java becomes especially appropriate: it gives us a clean way to maintain shared state across the conversation, compose specialized agents, and add a human-in-the-loop approval stage for the "what should we do about inventory?" moment.

## Current Java ADK Implementation

The first cut of that final-stage action flow now lives in the shared Java runtime at [oracle_agent_java](/path/to/repo-root/oracle-ai-database-gcp-gemini/oracle_agent_java/README.md).

What is implemented today:

- a dedicated inventory-action A2A agent surface served by the same Spring Boot process at `/inventory-action`
- a separate action card import URL at `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`
- a Google ADK Java coordinator that uses a `ParallelAgent` for evidence gathering and a final `LlmAgent` for recommendation synthesis
- tool-backed graph evidence from the live Oracle graph path
- a real in-process spatial hotspot tool plus seeded external evidence so we can demonstrate the full multi-agent arc without splitting the runtime yet
- a policy check and draft transfer tool so the final answer can explicitly say whether approval is required

What still comes next:

- swap the seeded external-intel tool for a real downstream A2A or MCP-backed call
- persist richer shared state across multi-turn conversations
- optionally separate the action coordinator into its own runtime once the demo shape settles

## Gemini Enterprise Right Now

If you want the current Gemini Enterprise setup, create these four agents:

1. Graph agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`

2. Spatial agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`

3. Select AI agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`

4. Inventory action agent
   `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`

Current status:

- the graph agent is the main production-demo path and is working against the Oracle graph database
- the graph renderer still uses custom Java2D layout and image generation
- the spatial agent is now a real in-process spatial implementation that uses the JTS Topology Suite for geometry work and Java2D for the rendered PNG
- the Select AI agent is live in the same Java process and now answers through `DBMS_CLOUD_AI.GENERATE` using the `YOUR_SELECT_AI_PROFILE_NAME` database profile
- the inventory action agent is live and working, but it currently returns deterministic fallback recommendations because the VM-side ADC refresh is stale for the ADK model path

Quick Gemini Enterprise test prompts:

- Select AI:
  `Which products are at risk of stockouts next quarter, and which regions are driving that risk?`

- Spatial:
  `Show that on a map for SKU-500 and highlight the warehouse hotspots plus the best relief route.`

- Graph:
  `Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.`

- Graph payload:
  `Render this supply-chain dependency graph exactly from the structured payload below. Do not query the database. Use the payload as the authoritative graph input.`

- Inventory action:
  `What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.`

The full import and test runbook is in [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](/path/to/repo-root/oracle-ai-database-gcp-gemini/docs/GEMINI_ENTERPRISE_AGENT_SETUP.md).
