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
    PUBLIC_HOST \
    PUBLIC_PROTOCOL \
    GRAPH_AGENT_PORT \
    PORT \
    BIND_HOST \
    CERTBOT_CERT_NAME \
    CERT_COPY_DIR \
    SSL_CERTIFICATE \
    SSL_CERTIFICATE_PRIVATE_KEY \
    SSL_RELOAD_ON_UPDATE
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

CERTBOT_CERT_NAME="${CERTBOT_CERT_NAME:-oracle-graph-agent-ip}"
CERT_COPY_DIR="${CERT_COPY_DIR:-$HOME/.config/oracle-graph-agent-certs/$CERTBOT_CERT_NAME}"
export PUBLIC_PROTOCOL="${PUBLIC_PROTOCOL:-https}"
export BIND_HOST="${BIND_HOST:-0.0.0.0}"
export GRAPH_AGENT_PORT="${GRAPH_AGENT_PORT:-${PORT:-8080}}"

if [[ -f "$CERT_COPY_DIR/fullchain.pem" && -f "$CERT_COPY_DIR/privkey.pem" ]]; then
  export SSL_CERTIFICATE="${SSL_CERTIFICATE:-$CERT_COPY_DIR/fullchain.pem}"
  export SSL_CERTIFICATE_PRIVATE_KEY="${SSL_CERTIFICATE_PRIVATE_KEY:-$CERT_COPY_DIR/privkey.pem}"
else
  export SSL_CERTIFICATE="${SSL_CERTIFICATE:-/etc/letsencrypt/live/$CERTBOT_CERT_NAME/fullchain.pem}"
  export SSL_CERTIFICATE_PRIVATE_KEY="${SSL_CERTIFICATE_PRIVATE_KEY:-/etc/letsencrypt/live/$CERTBOT_CERT_NAME/privkey.pem}"
fi

export SSL_RELOAD_ON_UPDATE="${SSL_RELOAD_ON_UPDATE:-true}"

if [[ -z "${PUBLIC_HOST:-}" ]]; then
  echo "PUBLIC_HOST must be set to the VM's public hostname or IP address."
  exit 1
fi

exec "$SCRIPT_DIR/run.sh"
