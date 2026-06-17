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
    DB_USERNAME \
    DB_PASSWORD \
    DB_DSN \
    DB_WALLET_DIR \
    SQLCL_PATH \
    TNS_ADMIN
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

SQLCL_PATH="${SQLCL_PATH:-/opt/sqlcl/bin/sql}"
DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_DSN="${DB_DSN:-}"
TNS_ADMIN="${TNS_ADMIN:-${DB_WALLET_DIR:-}}"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
LOG_ROOT="${LOG_ROOT:-$SCRIPT_DIR/logs}"
RUN_DIR="$LOG_ROOT/$RUN_ID"
SQL_SETUP_FILE="$SCRIPT_DIR/setup_supply_chain_graph_schema.sql"
SQL_SESSION_FILE="$RUN_DIR/run_setup.sql"
SQL_SPOOL_FILE="$RUN_DIR/setup-spool.log"
SQL_STDOUT_FILE="$RUN_DIR/sqlcl-output.log"

mkdir -p "$RUN_DIR"

if [[ -z "$DB_USERNAME" || -z "$DB_PASSWORD" || -z "$DB_DSN" ]]; then
  echo "DB_USERNAME, DB_PASSWORD, and DB_DSN must be set."
  exit 1
fi

if [[ -z "$TNS_ADMIN" ]]; then
  echo "TNS_ADMIN or DB_WALLET_DIR must be set."
  exit 1
fi

if [[ ! -d "$TNS_ADMIN" ]]; then
  echo "Wallet directory not found: $TNS_ADMIN"
  exit 1
fi

if [[ ! -x "$SQLCL_PATH" ]]; then
  echo "SQLcl executable not found or not executable: $SQLCL_PATH"
  exit 1
fi

export TNS_ADMIN

cat > "$SQL_SESSION_FILE" <<SQL
set echo on
set feedback on
set timing on
set verify off
set define off
set sqlblanklines on
set serveroutput on size unlimited
whenever oserror exit failure rollback
whenever sqlerror exit sql.sqlcode rollback

spool $SQL_SPOOL_FILE

prompt === Supply chain graph setup started ===
select systimestamp as started_at from dual;
select user as connected_user from dual;

prompt === Precheck: existing objects before setup ===
column object_name format a40
column object_type format a20
select object_name, object_type, status
from user_objects
where object_name in (
  'SC_SUPPLIERS',
  'SC_PLANTS',
  'SC_PORTS',
  'SC_WAREHOUSES',
  'SC_PRODUCTS',
  'SC_ALERTS',
  'SC_SUPPLIER_PLANT',
  'SC_PLANT_PORT',
  'SC_PORT_WAREHOUSE',
  'SC_WAREHOUSE_PRODUCT',
  'SC_ALERT_PORT',
  'SUPPLY_CHAIN_GRAPH'
)
order by object_type, object_name;

@$SQL_SETUP_FILE

prompt === Postcheck: objects after setup ===
select object_name, object_type, status
from user_objects
where object_name in (
  'SC_SUPPLIERS',
  'SC_PLANTS',
  'SC_PORTS',
  'SC_WAREHOUSES',
  'SC_PRODUCTS',
  'SC_ALERTS',
  'SC_SUPPLIER_PLANT',
  'SC_PLANT_PORT',
  'SC_PORT_WAREHOUSE',
  'SC_WAREHOUSE_PRODUCT',
  'SC_ALERT_PORT',
  'SUPPLY_CHAIN_GRAPH'
)
order by object_type, object_name;

prompt === Postcheck: graph metadata ===
declare
  graph_count number;
begin
  begin
    execute immediate q'[select count(*) from user_property_graphs where graph_name = 'SUPPLY_CHAIN_GRAPH']'
      into graph_count;
    dbms_output.put_line('user_property_graphs count for SUPPLY_CHAIN_GRAPH = ' || graph_count);
  exception
    when others then
      dbms_output.put_line('INFO: user_property_graphs check unavailable -> ' || sqlerrm);
  end;
end;
/

prompt === Supply chain graph setup completed ===
select systimestamp as completed_at from dual;

spool off
exit success
SQL

echo "Run directory: $RUN_DIR"
echo "SQLcl output log: $SQL_STDOUT_FILE"
echo "SQL spool log: $SQL_SPOOL_FILE"
echo

"$SQLCL_PATH" -S "${DB_USERNAME}/${DB_PASSWORD}@${DB_DSN}" @"$SQL_SESSION_FILE" | tee "$SQL_STDOUT_FILE"
