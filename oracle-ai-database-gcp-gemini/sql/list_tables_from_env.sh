#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${1:-$REPO_ROOT/.env}"

source "$REPO_ROOT/load_env_defaults.sh"
load_env_defaults "$ENV_FILE" \
  DB_USERNAME \
  DB_PASSWORD \
  DB_DSN \
  DB_WALLET_DIR \
  SQLCL_PATH \
  TNS_ADMIN

DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_DSN="${DB_DSN:-}"
SQLCL_PATH="${SQLCL_PATH:-/opt/sqlcl/bin/sql}"
TNS_ADMIN="${TNS_ADMIN:-${DB_WALLET_DIR:-}}"

if [[ -z "$DB_USERNAME" || -z "$DB_PASSWORD" || -z "$DB_DSN" ]]; then
  echo "DB_USERNAME, DB_PASSWORD, and DB_DSN must be set in $ENV_FILE" >&2
  exit 1
fi

if [[ -z "$TNS_ADMIN" ]]; then
  echo "TNS_ADMIN or DB_WALLET_DIR must be set in $ENV_FILE" >&2
  exit 1
fi

if [[ ! -d "$TNS_ADMIN" ]]; then
  echo "Wallet directory not found: $TNS_ADMIN" >&2
  exit 1
fi

if [[ ! -x "$SQLCL_PATH" ]]; then
  echo "SQLcl executable not found: $SQLCL_PATH" >&2
  exit 1
fi

export TNS_ADMIN

TMP_SQL="$(mktemp /tmp/list_tables_from_env.XXXX.sql)"
trap 'rm -f "$TMP_SQL"' EXIT

cat >"$TMP_SQL" <<'SQL'
set pagesize 50000
set linesize 220
set trimspool on
set feedback off
set heading on
column owner format a30
column table_name format a60

select owner, table_name
from all_tables
order by owner, table_name;

exit
SQL

"$SQLCL_PATH" -S "${DB_USERNAME}/${DB_PASSWORD}@${DB_DSN}" @"$TMP_SQL"
