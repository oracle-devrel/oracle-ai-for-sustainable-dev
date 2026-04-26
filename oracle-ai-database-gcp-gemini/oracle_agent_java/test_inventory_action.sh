#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
ENV_HELPER="$REPO_ROOT/load_env_defaults.sh"

if [[ -f "$ENV_HELPER" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_HELPER"
  load_env_defaults "$ENV_FILE" \
    ACTION_AGENT_URL \
    GRAPH_AGENT_URL \
    A2A_URL \
    PUBLIC_PROTOCOL \
    PUBLIC_HOST \
    GRAPH_AGENT_PORT \
    PORT
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

BASE_URL="${ACTION_AGENT_URL:-${GRAPH_AGENT_URL:-${A2A_URL:-${PUBLIC_PROTOCOL:-http}://${PUBLIC_HOST:-localhost}:${GRAPH_AGENT_PORT:-${PORT:-8080}}}}}"
TARGET_URL="${BASE_URL%/}/inventory-action"
PROMPT="${1:-What inventory action should we take for SKU-500 given the current supply risk?}"

echo "-----------------------------------------------"
echo "STEP 1: Testing Discovery"
echo "-----------------------------------------------"
curl -sS "$TARGET_URL/.well-known/agent-card.json" | python3 -m json.tool
echo -e "\n"

echo "-----------------------------------------------"
echo "STEP 2: Testing Action Coordinator (A2A JSON-RPC)"
echo "-----------------------------------------------"
curl -sS -X POST "$TARGET_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"method\": \"message/send\",
    \"params\": {
      \"message\": {
        \"kind\": \"message\",
        \"messageId\": \"inventory-action-test-1\",
        \"role\": \"user\",
        \"parts\": [
          {
            \"kind\": \"text\",
            \"text\": \"$PROMPT\"
          }
        ]
      }
    },
    \"id\": 1
  }" | python3 -m json.tool
echo
