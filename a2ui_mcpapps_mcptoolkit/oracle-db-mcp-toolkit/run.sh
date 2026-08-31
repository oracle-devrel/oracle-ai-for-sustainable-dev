#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# shellcheck disable=SC1091
source ./local-runtime-env.sh

profile="${1:-full}"
case "$profile" in
  full)
    port="${ORACLE_MCP_PORT:-45450}"
    toolset="supply-chain-exchange"
    token="$ORACLE_MCP_AUTH_TOKEN"
    ;;
  read)
    port="${ORACLE_MCP_READ_PORT:-45451}"
    toolset="supply-chain-exchange-read"
    token="$ORACLE_MCP_READ_AUTH_TOKEN"
    export DB_USERNAME="${DB_USERNAME2:?DB_USERNAME2 is required for the read profile}"
    export DB_PASSWORD="${DB_PASSWORD2:?DB_PASSWORD2 is required for the read profile}"
    ;;
  *)
    echo "Usage: ./run.sh [full|read]" >&2
    exit 2
    ;;
esac

toolkit_jar="${ORACLE_MCP_TOOLKIT_JAR:-$(../agent-service/prepare-mcp-toolkit.sh)}"
config_file="${ORACLE_MCP_CONFIG_FILE:-$PWD/config/tools.yaml}"
certificate_path="$(./prepare-local-tls.sh)"

export ORACLE_DB_TOOLKIT_AUTH_TOKEN="$token"

echo "Starting standalone Oracle Database MCP Java Toolkit ($profile) at https://localhost:$port/mcp" >&2
exec "${JAVA_COMMAND:-java}" \
  -Dtransport=http \
  -Dhttps.port="$port" \
  -DcertificatePath="$certificate_path" \
  -DcertificatePassword="$ORACLE_MCP_CERTIFICATE_PASSWORD" \
  -DenableAuthentication=true \
  -DconfigFile="$config_file" \
  -Dtools="$toolset" \
  -jar "$toolkit_jar"
