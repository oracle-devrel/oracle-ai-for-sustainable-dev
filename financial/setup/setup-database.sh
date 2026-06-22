#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FINANCIAL_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

if [[ "${SETUP_DATABASE:-false}" != "true" ]]; then
  echo "SETUP_DATABASE is not true; skipping database setup."
  exit 0
fi

SQL_BIN="${SQL_BIN:-}"
if [[ -z "${SQL_BIN}" ]]; then
  if command -v sql >/dev/null 2>&1; then
    SQL_BIN="sql"
  elif command -v sqlplus >/dev/null 2>&1; then
    SQL_BIN="sqlplus"
  else
    echo "Could not find SQLcl (sql) or SQL*Plus (sqlplus). Set SQL_BIN explicitly." >&2
    exit 1
  fi
fi

DB_ADMIN_CONNECT="${DB_ADMIN_CONNECT:?Set DB_ADMIN_CONNECT, for example admin/password@financialdb_high}"
APP_DB_CONNECT="${APP_DB_CONNECT:?Set APP_DB_CONNECT, for example financial/password@financialdb_high}"
DB_USER="${DB_USER:-financial}"
DB_PASSWORD="${DB_PASSWORD:?Set DB_PASSWORD for the FINANCIAL application user}"

run_sql() {
  local connect="$1"
  local script="$2"
  shift 2
  echo "==> Running ${script}"
  "${SQL_BIN}" -S "${connect}" @"${script}" "$@"
}

run_sql "${DB_ADMIN_CONNECT}" "${FINANCIAL_DIR}/sql/setup-admin.sql" "${DB_USER}" "${DB_PASSWORD}"
run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/financial.sql"
run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/accounts_inserts.sql"

if [[ "${RUN_GRAPH_SETUP:-false}" == "true" ]]; then
  run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/graph.sql"
fi

if [[ "${RUN_KAFKA_SETUP:-false}" == "true" ]]; then
  run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/kafka-messaging.sql"
fi

if [[ "${RUN_MONGO_JSON_DUALITY_SETUP:-false}" == "true" ]]; then
  run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/mongodb-jsonduality.sql"
fi

if [[ "${RUN_ORDS_SETUP:-false}" == "true" ]]; then
  run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/ords-APIs.sql"
fi

if [[ "${RUN_TRUECACHE_SETUP:-false}" == "true" ]]; then
  run_sql "${APP_DB_CONNECT}" "${FINANCIAL_DIR}/sql/truecache-stock.sql"
fi

echo "Database setup completed."
