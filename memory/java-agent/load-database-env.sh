#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MEMORY_DIR="$(cd -- "${APP_DIR}/.." && pwd)"
REPO_DIR="$(cd -- "${MEMORY_DIR}/.." && pwd)"

ENV_FILE=""
if [[ -f "${MEMORY_DIR}/.env" ]]; then
  ENV_FILE="${MEMORY_DIR}/.env"
elif [[ -f "${REPO_DIR}/financial/setup/.env" ]]; then
  ENV_FILE="${REPO_DIR}/financial/setup/.env"
fi

if [[ -n "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

export DB_USERNAME="${DB_USERNAME:-${DB_USER:-FINANCIAL}}"
export TNS_ADMIN="${TNS_ADMIN:-${WALLET_DIR:-}}"
export DB_SERVICE="${DB_SERVICE:-financialdb_high}"

if [[ -n "${TNS_ADMIN}" ]]; then
  export DB_URL="jdbc:oracle:thin:@${DB_SERVICE}?TNS_ADMIN=${TNS_ADMIN}"
fi

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "DB_PASSWORD is not set. Create memory/.env or financial/setup/.env." >&2
  exit 1
fi

if [[ -z "${DB_URL:-}" ]]; then
  echo "DB_URL is not set. Configure DB_URL or TNS_ADMIN." >&2
  exit 1
fi
