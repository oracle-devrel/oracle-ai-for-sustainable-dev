#!/usr/bin/env bash
set -euo pipefail

PYTHON_APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MEMORY_DIR="$(cd -- "${PYTHON_APP_DIR}/.." && pwd)"
REPO_DIR="$(cd -- "${MEMORY_DIR}/.." && pwd)"
VENV_DIR="${MEMORY_DIR}/.venv"

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
export MEMORY_PYTHON_PORT="${MEMORY_PYTHON_PORT:-8092}"

if [[ -z "${DB_PASSWORD:-}" || -z "${TNS_ADMIN:-}" ]]; then
  echo "DB_PASSWORD and TNS_ADMIN or WALLET_DIR are required." >&2
  exit 1
fi

if [[ ! -x "${VENV_DIR}/bin/python" ]]; then
  PYTHON_BIN="${PYTHON_BIN:-/opt/homebrew/bin/python3.12}"
  "${PYTHON_BIN}" -m venv "${VENV_DIR}"
fi

env CARGO_HTTP_PROXY= CARGO_HTTPS_PROXY= \
  "${VENV_DIR}/bin/python" -m pip install -q -r "${PYTHON_APP_DIR}/requirements.txt"
exec "${VENV_DIR}/bin/python" "${PYTHON_APP_DIR}/app.py"
