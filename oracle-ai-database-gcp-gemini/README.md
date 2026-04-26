# Develop A2A Agentic AI with Oracle AI Database and Google Gemini including Oracle AI Database Agent in Gemini Enterprise

This project contains the Oracle AI Database and Google Gemini A2A demo shown in Google Next presentations. It demonstrates how Gemini Enterprise can call A2A agents backed by Oracle AI Database, including graph, spatial, Select AI, inventory-risk, and action-recommendation flows.

The Java agent runtime is the implementation used in the live demo. The Python and Go agents are included as work in progress and reference implementations while they continue to evolve.

## Watch The Demo

[![Oracle AI Database Agent in Gemini Enterprise demo](https://img.youtube.com/vi/lU8UAwmBMeQ/hqdefault.jpg)](https://www.youtube.com/watch?v=lU8UAwmBMeQ)

[Watch on YouTube](https://www.youtube.com/watch?v=lU8UAwmBMeQ)

## LiveLabs Workshop

For the guided hands-on version, use the Oracle LiveLabs workshop:

[Develop A2A Agentic AI with Oracle AI Database and GCP Gemini](https://livelabs.oracle.com/ords/r/dbpm/livelabs/view-workshop?wid=4330)

## What This Repo Demonstrates

This repo is set up for multiple A2A-style agents. The common pattern is:

1. A caller, such as Gemini Enterprise, sends a user message to an agent over A2A.
2. The agent runtime interprets the message with deterministic routing, Gemini-backed reasoning, Oracle AI Database, or a combination of those pieces.
3. The agent calls local tools or database-backed services.
4. The tool returns structured data, text, or generated image artifacts.
5. The agent returns a final A2A response to Gemini Enterprise.

The current demo flow is:

1. Gemini Enterprise asks an Oracle inventory analyst which products are at risk of stockouts.
2. The Oracle AI Database-backed flow identifies risk drivers by product, warehouse, county, and region.
3. A spatial specialist renders hotspot maps for warehouse and regional risk.
4. A graph specialist renders Oracle Database property graph supply-chain dependencies.
5. The Oracle AI Database Agent can be used from Gemini Enterprise for database-grounded answers.
6. An inventory-action coordinator gathers the graph, spatial, and inventory evidence and recommends the safest next move.

## Agent Implementations

- [oracle_agent_java](./oracle_agent_java/README.md): the Java/Spring Boot A2A runtime used in the demo. It serves the graph, spatial, Select AI, inventory-system, and inventory-action agent surfaces from one process.
- [oracle_agent_python](./oracle_agent_python/README.md): Python agent work in progress and earlier reference implementation.
- [oracle_agent_golang](./oracle_agent_golang/README.md): Go agent work in progress.

Use the Java runtime when recreating the Google Next demo.

## Java Runtime Quick Start

From this directory:

```bash
cd oracle_agent_java
./build.sh
./run.sh
```

In another shell, test the local A2A endpoint:

```bash
cd oracle_agent_java
./test.sh
```

For Gemini Enterprise, the agent must be reachable over HTTPS. The full setup, certificate, import, and test details are in:

- [oracle_agent_java/README.md](./oracle_agent_java/README.md)
- [oracle_agent_java/HTTPS_SETUP.md](./oracle_agent_java/HTTPS_SETUP.md)
- [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](./docs/GEMINI_ENTERPRISE_AGENT_SETUP.md)

## Gemini Enterprise Agent Cards

The Java process serves these demo agent cards:

- Graph agent: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`
- Spatial agent: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`
- Select AI agent: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-select-ai.json`
- Inventory-system gateway: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-inventory-system.json`
- Inventory-action coordinator: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-action.json`

The inventory-system gateway can delegate general inventory and database-style questions to the Oracle AI Database Agent, while keeping graph and spatial routing local to this demo runtime.

## Suggested Gemini Enterprise Prompts

Select AI:

```text
Which products are at risk of stockouts next quarter, and which regions are driving that risk?
```

Spatial:

```text
Show that on a map for SKU-500 and highlight the warehouse hotspots plus the best relief route.
```

Graph:

```text
Use the Oracle Database property graph to show supply chain dependencies for SKU-500 and render the graph as an image.
```

Inventory action:

```text
What inventory action should we take for SKU-500 given the current supply risk? Gather graph, spatial, and external evidence first, then recommend the safest next move and say whether approval is required.
```

## Configuration

This README uses placeholders such as `YOUR_PUBLIC_AGENT_HOST`, `YOUR_VM_SSH_USER`, `YOUR_SELECT_AI_PROFILE_NAME`, and `/path/to/repo-root`. Replace them with your own values before running the demo.

The repo-level `.env` file holds shared settings used by the agent scripts:

```bash
GOOGLE_GENAI_USE_VERTEXAI=true
GOOGLE_CLOUD_PROJECT="your-gcp-project"
GOOGLE_CLOUD_LOCATION="us-central1"
PUBLIC_HOST="YOUR_PUBLIC_AGENT_HOST"
PUBLIC_PROTOCOL="https"
GRAPH_AGENT_PORT="443"
MODEL_NAME="gemini-2.0-flash"
```

If you use Vertex AI credentials, authenticate Application Default Credentials with:

```bash
./auth.sh
```

If you use a Gemini API key instead, set:

```bash
GOOGLE_API_KEY="your-api-key"
```

The graph and spatial paths are deterministic and image-first. Select AI depends on a database-side `DBMS_CLOUD_AI` profile. The inventory-action coordinator uses Google ADK Java when credentials are available and falls back to deterministic recommendations when the model path is unavailable.

## Documentation Index

Start here:

- [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](./docs/GEMINI_ENTERPRISE_AGENT_SETUP.md): Gemini Enterprise import URLs, tested prompts, caveats, and expected behavior.
- [oracle_agent_java/README.md](./oracle_agent_java/README.md): Java runtime, local build/run commands, HTTPS deployment, and agent card URLs.
- [docs/DEMO_NOW_AND_NEXT.md](./docs/DEMO_NOW_AND_NEXT.md): current demo status and next steps.
- [docs/GCP_INFRA_SETUP.md](./docs/GCP_INFRA_SETUP.md): GCP setup and migration guide.
- [docs/GCP_SETUP_PROGRESS.md](./docs/GCP_SETUP_PROGRESS.md): live migration notes and setup progress.
- [docs/TODO.md](./docs/TODO.md): follow-up work, hardening, and roadmap items.

Reference docs:

- [docs/README.md](./docs/README.md)
- [docs/ADK_AGENT_README.md](./docs/ADK_AGENT_README.md)
- [docs/ADK_RAG_IMPLEMENTATION.md](./docs/ADK_RAG_IMPLEMENTATION.md)
- [docs/AGENT_FILES_COMPARISON.md](./docs/AGENT_FILES_COMPARISON.md)
- [docs/AGENT_README.md](./docs/AGENT_README.md)
- [docs/API_README.md](./docs/API_README.md)
- [docs/DIRECTORY_STRUCTURE.md](./docs/DIRECTORY_STRUCTURE.md)
- [docs/EMBEDDINGS_COMPARISON.md](./docs/EMBEDDINGS_COMPARISON.md)
- [docs/MCP_IMPLEMENTATION_SUMMARY.md](./docs/MCP_IMPLEMENTATION_SUMMARY.md)
- [docs/MCP_TOOLBOX_README.md](./docs/MCP_TOOLBOX_README.md)
- [docs/QUICK_START_ADK_MCP.md](./docs/QUICK_START_ADK_MCP.md)
- [docs/QUICK_START_SUMMARY.md](./docs/QUICK_START_SUMMARY.md)
- [docs/TESTING_RESULTS.md](./docs/TESTING_RESULTS.md)
- [docs/WORKSHOP.md](./docs/WORKSHOP.md)
- [docs/oracle_ai_database_adk_agent.md](./docs/oracle_ai_database_adk_agent.md)

## Notes

- The Java runtime is the one to use for the recorded and presented demo.
- Python and Go agents are under development.
- The YouTube thumbnail is loaded from YouTube so no separate image asset is required in this repo.
- Use [docs/GEMINI_ENTERPRISE_AGENT_SETUP.md](./docs/GEMINI_ENTERPRISE_AGENT_SETUP.md) for the most complete Gemini Enterprise import and test runbook.
