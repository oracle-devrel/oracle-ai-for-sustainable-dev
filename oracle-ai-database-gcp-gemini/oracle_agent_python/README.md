# Oracle Inventory Action Agent for Gemini Enterprise

This is a Python A2A agent for the inventory-action coordinator flow that already works in `oracle_agent_java`.
It mirrors the Java behavior in a standalone Python service:

- it gathers graph, spatial, and external evidence for a product such as `SKU-500`
- it checks whether a transfer requires approval
- it drafts a non-executed inventory-transfer action
- it tries Google ADK orchestration first and falls back to deterministic local orchestration when model access is unavailable
- it can optionally query Oracle Database later through `oracledb` using the same wallet-style env vars as the Java agent, but it also ships with seeded fallback data so local bring-up does not require live infra

Important files:

- `main.py` runs the standalone A2A service
- `inventory_action_service.py` contains the ported Java-style coordinator, tool logic, seeded data, and optional Oracle DB lookup paths
- `test.sh` and `test.py` are callers that send A2A messages to the agent
- shared database setup and seed assets now live in `../sql/`

## Setup Instructions

1. **Install Dependencies:**
   ```bash
   ./setup_venv.sh
   ```

2. **Python Version:**
   Use Python 3.10 or newer. The current `google-adk` releases require Python `>=3.10`.

3. **Enter the Project Environment:**
   ```bash
   ./enter_venv.sh
   ```

4. **Run the Agent:**
   ```bash
   ./run.sh
   ```

5. **Test the A2A Endpoint:**
   ```bash
   ./test.sh
   ```

6. **Configure the Shared Repo `.env`:**
   The repo root now has a shared `.env` file at `../.env`.
   The shell helpers and Python entrypoints load it automatically.

   Shared local runtime settings:
   ```bash
    PUBLIC_PROTOCOL="http"
    PUBLIC_HOST="localhost"
    ACTION_AGENT_PORT="8080"
    ```

   Optional later for live Oracle Database lookups:
   ```bash
   DB_USERNAME="..."
   DB_PASSWORD="..."
   DB_DSN="..."
   TNS_ADMIN="/path/to/wallet"
   # or DB_WALLET_DIR="/path/to/wallet"
   DB_WALLET_PASSWORD="..."
   ```

## What Success Looks Like

When `./test.sh` succeeds, discovery should show:

- `name: oracle_inventory_action_agent`
- `defaultOutputModes: ["application/json", "text/plain"]`
- `url: http://localhost:8080`

The action response should show:

- `status: completed`
- a text recommendation in the status message
- a structured `data` part when the deterministic fallback drafts an action locally
- status-message metadata such as `coordinator`, `traceCount`, and usually `draftActionId` in fallback mode

Example summarized action output:

```json
{
  "status": "completed",
  "statusMessage": {
    "metadata": {
      "coordinator": "deterministic-fallback",
      "traceCount": 5,
      "actionType": "INVENTORY_TRANSFER",
      "draftActionId": "draft-transfer-sku-500"
    },
    "parts": [
      {
        "kind": "text",
        "text": "Fallback recommendation for SKU-500: transfer 500 units ..."
      },
      {
        "kind": "data",
        "data": {
          "action": {
            "draftActionId": "draft-transfer-sku-500"
          },
          "policy": {
            "requiresApproval": true
          }
        }
      }
    ]
  }
}
```

## Moving This Directory

If you move this agent under another repo:

- keep the shared `.env` one directory above this folder, or update the scripts and `main.py`
- keep `ACTION_AGENT_PORT` defined in that new parent `.env`
- rerun `./setup_venv.sh` after the move so the local `.venv` is recreated in the new location

## Notes

- The agent advertises `application/json` and `text/plain` output modes.
- The final A2A response is a text recommendation plus optional structured action/policy data.
- The app advertises its A2A URL from `PUBLIC_HOST`, `PUBLIC_PROTOCOL`, and `ACTION_AGENT_PORT`. By default that is `http://localhost:8080`.
- `enter_venv.sh`, `setup_venv.sh`, `run.sh`, `test.sh`, `main.py`, and `test.py` all load the shared repo `.env`.
- Set `ACTION_DISABLE_ADK=true` if you want to force the deterministic local path while iterating.
