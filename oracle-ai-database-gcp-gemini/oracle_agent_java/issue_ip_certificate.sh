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
    CERTBOT_EMAIL \
    CERTBOT_CERT_NAME
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

PUBLIC_IP="${PUBLIC_HOST:-}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
CERTBOT_CERT_NAME="${CERTBOT_CERT_NAME:-oracle-graph-agent-ip}"

if [[ -z "$PUBLIC_IP" ]]; then
  echo "PUBLIC_HOST must be set to the VM's public IPv4 address before requesting an IP certificate."
  exit 1
fi

if [[ ! "$PUBLIC_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  echo "PUBLIC_HOST must be an IPv4 address for IP certificate issuance. Current value: $PUBLIC_IP"
  exit 1
fi

if ! command -v certbot >/dev/null 2>&1; then
  echo "certbot is required. Install Certbot 5.3.0 or newer before running this script."
  exit 1
fi

CERTBOT_VERSION="$(certbot --version 2>/dev/null | awk '{print $2}')"
if [[ -z "$CERTBOT_VERSION" ]]; then
  echo "Unable to determine the installed Certbot version."
  exit 1
fi

python3 - "$CERTBOT_VERSION" <<'PY'
import sys

parts = sys.argv[1].split(".")
version = tuple(int(p) for p in parts[:3])
if version < (5, 3, 0):
    raise SystemExit("Certbot 5.3.0 or newer is required for --ip-address support.")
PY

cat <<EOF
Requesting a short-lived Let's Encrypt certificate for IP address $PUBLIC_IP.

Requirements:
- inbound TCP 80 must be reachable from the public internet during issuance and renewal
- nothing else can be listening on port 80 while Certbot runs

Certificate name: $CERTBOT_CERT_NAME
EOF

CERTBOT_REGISTRATION_ARGS=()
if [[ -n "$CERTBOT_EMAIL" ]]; then
  CERTBOT_REGISTRATION_ARGS+=(--email "$CERTBOT_EMAIL")
else
  cat <<'EOF'
Warning:
- CERTBOT_EMAIL is not set
- proceeding with --register-unsafely-without-email
- add CERTBOT_EMAIL later if you want expiration and security notices
EOF
  CERTBOT_REGISTRATION_ARGS+=(--register-unsafely-without-email)
fi

sudo certbot certonly \
  --standalone \
  --non-interactive \
  --agree-tos \
  --preferred-profile shortlived \
  --keep-until-expiring \
  --cert-name "$CERTBOT_CERT_NAME" \
  "${CERTBOT_REGISTRATION_ARGS[@]}" \
  --ip-address "$PUBLIC_IP"

echo
echo "Certificate files:"
echo "  /etc/letsencrypt/live/$CERTBOT_CERT_NAME/fullchain.pem"
echo "  /etc/letsencrypt/live/$CERTBOT_CERT_NAME/privkey.pem"
