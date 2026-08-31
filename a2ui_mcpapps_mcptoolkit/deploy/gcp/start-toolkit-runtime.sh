#!/usr/bin/env bash

toolkit_pids=()

start_toolkit_instance() {
  local profile="$1"
  local port="$2"
  local toolset="$3"
  local username="$4"
  local password="$5"
  local token="$6"

  DB_USERNAME="$username" \
  DB_PASSWORD="$password" \
  ORACLE_DB_TOOLKIT_AUTH_TOKEN="$token" \
    java \
      -Dtransport=http \
      -Dhttps.port="$port" \
      -DcertificatePath="$ORACLE_MCP_CERTIFICATE_PATH" \
      -DcertificatePassword="$ORACLE_MCP_CERTIFICATE_PASSWORD" \
      -DenableAuthentication=true \
      -DconfigFile=/opt/app/oracle-db-mcp-toolkit/config/tools.yaml \
      -Dtools="$toolset" \
      -jar /opt/app/oracle-db-mcp-toolkit.jar &
  toolkit_pids+=("$!")

  for _ in $(seq 1 60); do
    if ! kill -0 "${toolkit_pids[-1]}" 2>/dev/null; then
      echo "Oracle MCP Toolkit $profile profile exited during startup." >&2
      return 1
    fi
    if curl --insecure --silent --output /dev/null "https://127.0.0.1:$port/mcp"; then
      return 0
    fi
    sleep 1
  done
  echo "Oracle MCP Toolkit $profile profile did not start within 60 seconds." >&2
  return 1
}

start_toolkit_runtime() {
  if [ -n "${ORACLE_MCP_URL:-}" ]; then
    : "${ORACLE_MCP_AUTH_TOKEN:?ORACLE_MCP_AUTH_TOKEN is required with ORACLE_MCP_URL}"
    return 0
  fi

  export ORACLE_MCP_CERTIFICATE_PATH="${ORACLE_MCP_CERTIFICATE_PATH:-/tmp/oracle-mcp-toolkit.p12}"
  export ORACLE_MCP_CERTIFICATE_PASSWORD="${ORACLE_MCP_CERTIFICATE_PASSWORD:-$(cat /proc/sys/kernel/random/uuid)}"
  export ORACLE_MCP_AUTH_TOKEN="${ORACLE_MCP_AUTH_TOKEN:-$(cat /proc/sys/kernel/random/uuid)}"

  keytool -genkeypair \
    -alias oracle-mcp-runtime \
    -keyalg RSA \
    -storetype PKCS12 \
    -keystore "$ORACLE_MCP_CERTIFICATE_PATH" \
    -storepass "$ORACLE_MCP_CERTIFICATE_PASSWORD" \
    -keypass "$ORACLE_MCP_CERTIFICATE_PASSWORD" \
    -dname "CN=localhost, O=Oracle MCP Runtime, C=US" \
    -ext "SAN=dns:localhost,ip:127.0.0.1" \
    -validity 30 \
    -noprompt >/dev/null

  export ORACLE_MCP_TRUSTSTORE="$ORACLE_MCP_CERTIFICATE_PATH"
  export ORACLE_MCP_TRUSTSTORE_PASSWORD="$ORACLE_MCP_CERTIFICATE_PASSWORD"
  export ORACLE_MCP_URL="https://localhost:45450/mcp"
  start_toolkit_instance \
    full 45450 supply-chain-exchange "$DB_USERNAME" "$DB_PASSWORD" "$ORACLE_MCP_AUTH_TOKEN"

  if [ -n "${DB_USERNAME2:-}" ] && [ -n "${DB_PASSWORD2:-}" ]; then
    export ORACLE_MCP_READ_AUTH_TOKEN="${ORACLE_MCP_READ_AUTH_TOKEN:-$(cat /proc/sys/kernel/random/uuid)}"
    export ORACLE_MCP_READ_URL="https://localhost:45451/mcp"
    start_toolkit_instance \
      read 45451 supply-chain-exchange-read "$DB_USERNAME2" "$DB_PASSWORD2" "$ORACLE_MCP_READ_AUTH_TOKEN"
  fi
}

stop_toolkit_runtime() {
  for pid in "${toolkit_pids[@]}"; do
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  done
}
