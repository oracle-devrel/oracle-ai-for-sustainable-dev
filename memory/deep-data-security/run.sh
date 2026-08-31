#!/usr/bin/env bash
set -euo pipefail

DDS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MEMORY_DIR="$(cd -- "${DDS_DIR}/.." && pwd)"
REPO_DIR="$(cd -- "${MEMORY_DIR}/.." && pwd)"

if [[ -f "${MEMORY_DIR}/.env" ]]; then
  ENV_FILE="${MEMORY_DIR}/.env"
elif [[ -f "${REPO_DIR}/financial/setup/.env" ]]; then
  ENV_FILE="${REPO_DIR}/financial/setup/.env"
else
  echo "Create memory/.env from memory/.env.example." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

export DB_USERNAME="${DB_USERNAME:-${DB_USER:-FINANCIAL}}"
export DB_SERVICE="${DB_SERVICE:-financialdb_high}"
export TNS_ADMIN="${TNS_ADMIN:-${WALLET_DIR:-}}"
export DB_WALLET_PASSWORD="${DB_WALLET_PASSWORD:-${DB_PASSWORD:-}}"

: "${DDS_ADMIN_PASSWORD:?Export DDS_ADMIN_PASSWORD for the financial database ADMIN user.}"
: "${DB_PASSWORD:?DB_PASSWORD is required.}"
: "${TNS_ADMIN:?TNS_ADMIN or WALLET_DIR is required.}"

exec "${MEMORY_DIR}/.venv/bin/python" "${DDS_DIR}/bootstrap.py"
