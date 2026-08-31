#!/usr/bin/env bash
set -euo pipefail

wallet_zip="${WALLET_ZIP_FILE:-/var/run/secrets/oracle-wallet/wallet.zip}"
wallet_target="/tmp/oracle-wallet"

if [ ! -f "$wallet_zip" ]; then
  echo "Oracle wallet ZIP is not mounted: $wallet_zip" >&2
  exit 1
fi

rm -rf "$wallet_target"
mkdir -p "$wallet_target"
unzip -q "$wallet_zip" -d "$wallet_target"

export TNS_ADMIN="$wallet_target"
export DB_URL="${DB_URL:-jdbc:oracle:thin:@${DB_SERVICE_NAME}?TNS_ADMIN=${TNS_ADMIN}}"

exec "$@"
