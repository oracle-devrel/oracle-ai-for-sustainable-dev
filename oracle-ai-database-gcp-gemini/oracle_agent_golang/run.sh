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
    A2A_URL \
    PUBLIC_PROTOCOL \
    PUBLIC_HOST \
    ACTION_AGENT_PORT \
    PORT \
    BIND_HOST \
    ACTION_COORDINATOR_MODEL \
    MODEL_NAME \
    GOOGLE_API_KEY \
    GEMINI_API_KEY \
    GOOGLE_CLOUD_PROJECT \
    GOOGLE_CLOUD_LOCATION \
    GOOGLE_CLOUD_REGION \
    GCP_PROJECT_ID \
    GCP_REGION \
    ACTION_DISABLE_ADK
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

export PORT="${ACTION_AGENT_PORT:-${PORT:-8080}}"

cd "$SCRIPT_DIR"

echo "Starting Oracle Inventory Action Go Agent on port $PORT"
exec go run .
