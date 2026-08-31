#!/usr/bin/env bash

# shellcheck disable=SC1091
source ../agent-service/load-database-env.sh

runtime_dir="../.runtime/oracle-mcp-http"
mkdir -p "$runtime_dir"
chmod 700 "$runtime_dir"

token_file="$runtime_dir/full.token"
read_token_file="$runtime_dir/read.token"
certificate_password_file="$runtime_dir/certificate-password"

if [ -z "${ORACLE_MCP_AUTH_TOKEN:-}" ]; then
  if [ ! -f "$token_file" ]; then
    uuidgen | tr '[:upper:]' '[:lower:]' >"$token_file"
    chmod 600 "$token_file"
  fi
  export ORACLE_MCP_AUTH_TOKEN="$(<"$token_file")"
fi

if [ -z "${ORACLE_MCP_READ_AUTH_TOKEN:-}" ]; then
  if [ ! -f "$read_token_file" ]; then
    uuidgen | tr '[:upper:]' '[:lower:]' >"$read_token_file"
    chmod 600 "$read_token_file"
  fi
  export ORACLE_MCP_READ_AUTH_TOKEN="$(<"$read_token_file")"
fi

if [ -z "${ORACLE_MCP_CERTIFICATE_PASSWORD:-}" ]; then
  if [ ! -f "$certificate_password_file" ]; then
    uuidgen | tr '[:upper:]' '[:lower:]' >"$certificate_password_file"
    chmod 600 "$certificate_password_file"
  fi
  export ORACLE_MCP_CERTIFICATE_PASSWORD="$(<"$certificate_password_file")"
fi
export ORACLE_MCP_CERTIFICATE_PATH="${ORACLE_MCP_CERTIFICATE_PATH:-$PWD/../tls/oracle-db-mcp-toolkit.p12}"
export ORACLE_MCP_TRUSTSTORE="${ORACLE_MCP_TRUSTSTORE:-$ORACLE_MCP_CERTIFICATE_PATH}"
export ORACLE_MCP_TRUSTSTORE_PASSWORD="${ORACLE_MCP_TRUSTSTORE_PASSWORD:-$ORACLE_MCP_CERTIFICATE_PASSWORD}"
export ORACLE_MCP_URL="${ORACLE_MCP_URL:-https://localhost:${ORACLE_MCP_PORT:-45450}/mcp}"
if [ "${ORACLE_MCP_ENABLE_READ_PROFILE:-false}" = "true" ]; then
  export ORACLE_MCP_READ_URL="${ORACLE_MCP_READ_URL:-https://localhost:${ORACLE_MCP_READ_PORT:-45451}/mcp}"
fi
