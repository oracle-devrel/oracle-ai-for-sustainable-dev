#!/usr/bin/env bash
set -euo pipefail

required() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required DB_USERNAME
required DB_PASSWORD
required DB_USERNAME2
required DB_PASSWORD2
required DB_SERVICE_NAME
required PUBLIC_A2A_URL

if [ -n "${WALLET_ZIP_FILE:-}" ] && [ -f "$WALLET_ZIP_FILE" ]; then
  wallet_target="/tmp/oracle-wallet"
  rm -rf "$wallet_target"
  mkdir -p "$wallet_target"
  unzip -q "$WALLET_ZIP_FILE" -d "$wallet_target"
  export TNS_ADMIN="$wallet_target"
fi

if [ ! -f "$TNS_ADMIN/tnsnames.ora" ]; then
  echo "Oracle wallet is missing tnsnames.ora in TNS_ADMIN=$TNS_ADMIN" >&2
  exit 1
fi

export DB_URL="${DB_URL:-jdbc:oracle:thin:@${DB_SERVICE_NAME}?TNS_ADMIN=${TNS_ADMIN}}"
export GEMINI_ENTERPRISE_A2A_PORT="${GEMINI_ENTERPRISE_A2A_PORT:-${PORT:-8080}}"

# The agent is an HTTP MCP client. This packaged deployment starts the Toolkit
# as an independently managed process; production may instead set ORACLE_MCP_URL
# to a separately deployed Toolkit service.
# shellcheck disable=SC1091
source /usr/local/bin/start-toolkit-runtime
start_toolkit_runtime

java -Dweb.root=/opt/app/web-client \
  -jar /opt/app/interactive-ai-agent-service.jar &
java_pid=$!

cleanup() {
  kill "$java_pid" 2>/dev/null || true
  wait "$java_pid" 2>/dev/null || true
  stop_toolkit_runtime
}
trap cleanup EXIT INT TERM

for _ in $(seq 1 60); do
  if ! kill -0 "$java_pid" 2>/dev/null; then
    echo "Java agent service exited during startup." >&2
    wait "$java_pid"
  fi
  if curl --fail --silent --show-error \
      "http://127.0.0.1:${AGENT_PORT}/api/health" >/dev/null; then
    break
  fi
  sleep 1
done

if ! curl --fail --silent --show-error \
    "http://127.0.0.1:${AGENT_PORT}/api/health" >/dev/null; then
  echo "Java agent service did not become healthy within 60 seconds." >&2
  exit 1
fi

cd /opt/app/gemini-enterprise-a2a
exec /opt/venv/bin/python main.py
