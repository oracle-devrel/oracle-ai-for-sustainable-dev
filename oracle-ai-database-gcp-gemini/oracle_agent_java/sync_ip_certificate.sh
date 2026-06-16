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
    CERTBOT_CERT_NAME \
    CERT_COPY_DIR
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

CERTBOT_CERT_NAME="${CERTBOT_CERT_NAME:-oracle-graph-agent-ip}"
CERT_COPY_DIR="${CERT_COPY_DIR:-$HOME/.config/oracle-graph-agent-certs/$CERTBOT_CERT_NAME}"
SOURCE_DIR="/etc/letsencrypt/live/$CERTBOT_CERT_NAME"

if ! sudo test -r "$SOURCE_DIR/fullchain.pem" || ! sudo test -r "$SOURCE_DIR/privkey.pem"; then
  echo "Certificate files are not readable at $SOURCE_DIR."
  echo "Run this script with sudo-capable access after issuing the certificate."
  exit 1
fi

sudo install -d -m 700 -o "$(id -u)" -g "$(id -g)" "$CERT_COPY_DIR"
sudo install -m 600 -o "$(id -u)" -g "$(id -g)" "$SOURCE_DIR/fullchain.pem" "$CERT_COPY_DIR/fullchain.pem"
sudo install -m 600 -o "$(id -u)" -g "$(id -g)" "$SOURCE_DIR/privkey.pem" "$CERT_COPY_DIR/privkey.pem"

echo "Copied certificate files to:"
echo "  $CERT_COPY_DIR/fullchain.pem"
echo "  $CERT_COPY_DIR/privkey.pem"
