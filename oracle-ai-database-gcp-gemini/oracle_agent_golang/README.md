# Oracle Inventory Action Go Agent for Gemini Enterprise

This directory holds a Go implementation of the inventory-action coordinator using the official Google ADK for Go and its A2A launcher.

Current design:

- uses Go ADK `llmagent`, `functiontool`, `parallelagent`, and `sequentialagent`
- exposes the agent over A2A using the official Go ADK A2A launcher
- mirrors the Java and Python inventory-action flow: graph evidence, spatial evidence, external signals, policy check, and draft transfer
- falls back to a deterministic local response when live Gemini model initialization is disabled or unavailable
- uses seeded inventory data today, while the shared database setup and seed assets live in `../sql/`

Important files:

- `main.go` starts the Go ADK A2A server
- `inventory_action.go` contains the Go ADK agent construction, tools, seeded data, and deterministic fallback
- `run.sh` starts the service with the shared repo `.env`
- `test.sh` hits the generated A2A card and JSON-RPC endpoint

## Setup Instructions

1. Install Go 1.24.4 or newer.
2. From this directory, fetch dependencies:
   ```bash
   go mod tidy
   ```
3. Make sure the shared repo `.env` contains the local runtime values you want.
4. Run the agent:
   ```bash
   ./run.sh
   ```
5. Test the A2A endpoint:
   ```bash
   ./test.sh
   ```

## Shared Environment

Typical local runtime values:

```bash
PUBLIC_PROTOCOL="http"
PUBLIC_HOST="localhost"
ACTION_AGENT_PORT="8080"
```

For live ADK model calls later, provide either Gemini API or Vertex-style settings:

```bash
GOOGLE_API_KEY="..."
# or GEMINI_API_KEY="..."

GOOGLE_CLOUD_PROJECT="..."
GOOGLE_CLOUD_LOCATION="..."
# or GOOGLE_CLOUD_REGION / GCP_REGION
```

Set this if you want to force the deterministic fallback path:

```bash
ACTION_DISABLE_ADK="true"
```

## Notes

- The Go agent uses the official package path `google.golang.org/adk`.
- The generated A2A card is created dynamically by the Go ADK A2A launcher.
- This implementation was written to match the shared Java/Python inventory-action behavior, but it was not compiled locally here because the current machine does not have `go` installed.
