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
required DB_SERVICE_NAME

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

for _ in $(seq 1 90); do
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
  echo "Java agent service did not become healthy within 90 seconds." >&2
  exit 1
fi

cd /opt/app/mcp-app
exec ./node_modules/.bin/tsx server.ts
