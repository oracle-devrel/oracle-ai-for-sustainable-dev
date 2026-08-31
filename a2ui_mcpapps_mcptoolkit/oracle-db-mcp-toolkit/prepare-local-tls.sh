#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

certificate_path="${ORACLE_MCP_CERTIFICATE_PATH:-$PWD/tls/oracle-db-mcp-toolkit.p12}"
certificate_password="${ORACLE_MCP_CERTIFICATE_PASSWORD:?ORACLE_MCP_CERTIFICATE_PASSWORD is required}"

if [ ! -f "$certificate_path" ]; then
  command -v keytool >/dev/null 2>&1 || {
    echo "keytool is required to create the local Toolkit certificate." >&2
    exit 1
  }
  mkdir -p "$(dirname "$certificate_path")"
  keytool -genkeypair \
    -alias oracle-mcp-local \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore "$certificate_path" \
    -storepass "$certificate_password" \
    -keypass "$certificate_password" \
    -dname "CN=localhost, OU=Development, O=Oracle MCP Demo, C=US" \
    -ext "SAN=dns:localhost,ip:127.0.0.1" \
    -validity 3650 \
    -noprompt >/dev/null
  chmod 600 "$certificate_path"
fi

printf '%s\n' "$certificate_path"
