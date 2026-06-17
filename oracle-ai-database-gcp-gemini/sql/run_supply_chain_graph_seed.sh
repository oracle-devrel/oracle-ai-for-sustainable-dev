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
RUN_DIR="$LOG_ROOT/$RUN_ID-seed"
SQL_SEED_FILE="$SCRIPT_DIR/seed_supply_chain_graph_data.sql"
SQL_SESSION_FILE="$RUN_DIR/run_seed.sql"
SQL_SPOOL_FILE="$RUN_DIR/seed-spool.log"
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

prompt === Supply chain graph seed started ===
select systimestamp as started_at from dual;
select user as connected_user from dual;

prompt === Precheck: row counts before seed ===
select 'SC_SUPPLIERS' as table_name, count(*) as row_count from sc_suppliers
union all
select 'SC_PLANTS', count(*) from sc_plants
union all
select 'SC_PORTS', count(*) from sc_ports
union all
select 'SC_WAREHOUSES', count(*) from sc_warehouses
union all
select 'SC_PRODUCTS', count(*) from sc_products
union all
select 'SC_ALERTS', count(*) from sc_alerts
union all
select 'SC_SUPPLIER_PLANT', count(*) from sc_supplier_plant
union all
select 'SC_PLANT_PORT', count(*) from sc_plant_port
union all
select 'SC_PORT_WAREHOUSE', count(*) from sc_port_warehouse
union all
select 'SC_WAREHOUSE_PRODUCT', count(*) from sc_warehouse_product
union all
select 'SC_ALERT_PORT', count(*) from sc_alert_port
order by table_name;

@$SQL_SEED_FILE

prompt === Postcheck: row counts after seed ===
select 'SC_SUPPLIERS' as table_name, count(*) as row_count from sc_suppliers
union all
select 'SC_PLANTS', count(*) from sc_plants
union all
select 'SC_PORTS', count(*) from sc_ports
union all
select 'SC_WAREHOUSES', count(*) from sc_warehouses
union all
select 'SC_PRODUCTS', count(*) from sc_products
union all
select 'SC_ALERTS', count(*) from sc_alerts
union all
select 'SC_SUPPLIER_PLANT', count(*) from sc_supplier_plant
union all
select 'SC_PLANT_PORT', count(*) from sc_plant_port
union all
select 'SC_PORT_WAREHOUSE', count(*) from sc_port_warehouse
union all
select 'SC_WAREHOUSE_PRODUCT', count(*) from sc_warehouse_product
union all
select 'SC_ALERT_PORT', count(*) from sc_alert_port
order by table_name;

prompt === Verification query for SKU-500 ===
var product_id varchar2(40)
exec :product_id := 'SKU-500';

select
    supplier_name,
    plant_name,
    port_name,
    warehouse_name,
    product_id
from graph_table(
    supply_chain_graph
    match
        (s is supplier) -[e1 is supplies]-> (p is plant)
                        -[e2 is ships_via]-> (po is port)
                        -[e3 is routes_to]-> (w is warehouse)
                        -[e4 is stocks]-> (pr is product)
    where pr.product_id = :product_id
    columns (
        s.supplier_name as supplier_name,
        p.plant_name as plant_name,
        po.port_name as port_name,
        w.warehouse_name as warehouse_name,
        pr.product_id as product_id
    )
);

prompt === Verification query with alert data for SKU-500 ===
with dependency_path as (
    select
        port_id,
        supplier_name,
        plant_name,
        port_name,
        warehouse_name,
        product_id
    from graph_table(
        supply_chain_graph
        match
            (s is supplier) -[e1 is supplies]-> (p is plant)
                            -[e2 is ships_via]-> (po is port)
                            -[e3 is routes_to]-> (w is warehouse)
                            -[e4 is stocks]-> (pr is product)
        where pr.product_id = :product_id
        columns (
            po.port_id as port_id,
            s.supplier_name as supplier_name,
            p.plant_name as plant_name,
            po.port_name as port_name,
            w.warehouse_name as warehouse_name,
            pr.product_id as product_id
        )
    )
)
select
    dp.supplier_name,
    dp.plant_name,
    dp.port_name,
    dp.warehouse_name,
    dp.product_id,
    a.alert_name,
    a.lane_name,
    a.risk_score
from dependency_path dp
left join sc_alert_port ap
  on ap.port_id = dp.port_id
 and ap.is_current = 'Y'
left join sc_alerts a
  on a.alert_id = ap.alert_id
 and a.active_flag = 'Y';

prompt === Supply chain graph seed completed ===
select systimestamp as completed_at from dual;

spool off
exit success
SQL

echo "Run directory: $RUN_DIR"
echo "SQLcl output log: $SQL_STDOUT_FILE"
echo "SQL spool log: $SQL_SPOOL_FILE"
echo

"$SQLCL_PATH" -S "${DB_USERNAME}/${DB_PASSWORD}@${DB_DSN}" @"$SQL_SESSION_FILE" | tee "$SQL_STDOUT_FILE"
