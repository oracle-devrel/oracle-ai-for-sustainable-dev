#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

PUBLIC_HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
APP_BASE_PATH="${APP_BASE_PATH:-/financial}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-https://${PUBLIC_HOSTNAME}${APP_BASE_PATH}}"

check() {
  local url="$1"
  echo "==> ${url}"
  curl --fail --show-error --location --max-time 20 "${url}" >/tmp/financial-smoke-response.txt
  head -c 500 /tmp/financial-smoke-response.txt
  printf '\n\n'
}

check "${PUBLIC_BASE_URL}/"
check "${PUBLIC_BASE_URL}/"
check "https://${PUBLIC_HOSTNAME}${APP_BASE_PATH}/api/test"
check "https://${PUBLIC_HOSTNAME}${APP_BASE_PATH}/api/accounts"
check "https://${PUBLIC_HOSTNAME}${APP_BASE_PATH}/accounts-api/accounts"

echo "Smoke checks completed."
