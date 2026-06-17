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
    GRAPH_AGENT_URL \
    A2A_URL \
    PUBLIC_PROTOCOL \
    PUBLIC_HOST \
    GRAPH_AGENT_PORT \
    PORT \
    BIND_HOST \
    SSL_CERTIFICATE \
    SSL_CERTIFICATE_PRIVATE_KEY \
    SSL_RELOAD_ON_UPDATE
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required but was not found on PATH."
  exit 1
fi

cd "$SCRIPT_DIR"

JAR_PATH="$(find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "No built jar found. Running build first..."
  "$SCRIPT_DIR/build.sh"
  JAR_PATH="$(find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
fi

if [[ -z "$JAR_PATH" ]]; then
  echo "Unable to find a runnable jar under $SCRIPT_DIR/target after build."
  exit 1
fi

GRAPH_PORT="${GRAPH_AGENT_PORT:-${PORT:-8080}}"

normalize_resource_location() {
  local value="$1"
  if [[ "$value" == *:* ]]; then
    printf '%s\n' "$value"
  else
    printf 'file:%s\n' "$value"
  fi
}

JAVA_ARGS=(
  -Djava.awt.headless=true
  -jar "$JAR_PATH"
  "--server.port=$GRAPH_PORT"
  "--server.address=${BIND_HOST:-0.0.0.0}"
)

if [[ -n "${SSL_CERTIFICATE:-}" || -n "${SSL_CERTIFICATE_PRIVATE_KEY:-}" ]]; then
  if [[ -z "${SSL_CERTIFICATE:-}" || -z "${SSL_CERTIFICATE_PRIVATE_KEY:-}" ]]; then
    echo "Both SSL_CERTIFICATE and SSL_CERTIFICATE_PRIVATE_KEY must be set to enable HTTPS."
    exit 1
  fi

  SSL_CERTIFICATE_LOCATION="$(normalize_resource_location "$SSL_CERTIFICATE")"
  SSL_PRIVATE_KEY_LOCATION="$(normalize_resource_location "$SSL_CERTIFICATE_PRIVATE_KEY")"
  SSL_RELOAD_ON_UPDATE="${SSL_RELOAD_ON_UPDATE:-true}"

  echo "Starting Oracle Graph Agent with HTTPS on port $GRAPH_PORT"
  JAVA_ARGS+=(
    "--server.ssl.enabled=true"
    "--server.ssl.bundle=graph"
    "--spring.ssl.bundle.pem.graph.reload-on-update=$SSL_RELOAD_ON_UPDATE"
    "--spring.ssl.bundle.pem.graph.keystore.certificate=$SSL_CERTIFICATE_LOCATION"
    "--spring.ssl.bundle.pem.graph.keystore.private-key=$SSL_PRIVATE_KEY_LOCATION"
  )
else
  echo "Starting Oracle Graph Agent on port $GRAPH_PORT"
fi

exec java "${JAVA_ARGS[@]}"
