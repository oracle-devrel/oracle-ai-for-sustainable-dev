-- Region-scoped Oracle Deep Data Security demonstration for the inventory
-- analysis data used by the Gemini Enterprise application.
--
-- FINANCIAL remains the shared application schema and Select AI agent-team
-- owner. The two password-authenticated local Deep Sec end users receive
-- different region-specific data roles and data grants over the same protected
-- view. No OCI IAM or Microsoft Entra ID propagation is used in this demo.
-- Run through run_inventory_risk_deepsec_regions.sh; do not hard-code secrets.

set define on verify off echo off feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

define owner_name = '&&1'
define na_username = '&&2'
define na_password = '&&3'
define apac_username = '&&4'
define apac_password = '&&5'

prompt [1/8] validate administrator session
begin
    if sys_context('USERENV', 'SESSION_USER') <> 'ADMIN' then
        raise_application_error(-20001, 'Connect as ADMIN before running this script.');
    end if;
end;
/

prompt [2/8] add explicit market-region columns
declare
    l_count number;
begin
    select count(*) into l_count
      from dba_tab_columns
     where owner = upper('&&owner_name')
       and table_name = 'SC_INVENTORY_RISK_SUMMARY'
       and column_name = 'MARKET_REGION';
    if l_count = 0 then
        execute immediate 'alter table &&owner_name..sc_inventory_risk_summary add (market_region varchar2(20))';
    end if;

    select count(*) into l_count
      from dba_tab_columns
     where owner = upper('&&owner_name')
       and table_name = 'SC_WAREHOUSE_GEO'
       and column_name = 'MARKET_REGION';
    if l_count = 0 then
        execute immediate 'alter table &&owner_name..sc_warehouse_geo add (market_region varchar2(20))';
    end if;
end;
/

update &&owner_name..sc_inventory_risk_summary
   set market_region = 'NA'
 where market_region is null;

update &&owner_name..sc_warehouse_geo
   set market_region = 'NA'
 where market_region is null;

prompt [3/8] seed synthetic APAC products and warehouses
merge into &&owner_name..sc_products target
using (
    select 'SKU-APAC-210' product_id, 'Smart Cooling Controller' product_name,
           18 demand_change_pct, 28 margin_pct, 'Y' active_flag from dual
    union all
    select 'SKU-APAC-420', 'Grid Storage Sensor', 14, 25, 'Y' from dual
) source
on (target.product_id = source.product_id)
when matched then update set
    target.product_name = source.product_name,
    target.demand_change_pct = source.demand_change_pct,
    target.margin_pct = source.margin_pct,
    target.active_flag = source.active_flag
when not matched then insert
    (product_id, product_name, demand_change_pct, margin_pct, active_flag)
values
    (source.product_id, source.product_name, source.demand_change_pct,
     source.margin_pct, source.active_flag);

merge into &&owner_name..sc_warehouses target
using (
    select 4101 warehouse_id, 'Singapore Distribution Hub' warehouse_name,
           1260 inventory_units, 89 fill_rate_pct, 'Y' active_flag from dual
    union all
    select 4102, 'Sydney Fulfillment Centre', 1740, 92, 'Y' from dual
) source
on (target.warehouse_id = source.warehouse_id)
when matched then update set
    target.warehouse_name = source.warehouse_name,
    target.inventory_units = source.inventory_units,
    target.fill_rate_pct = source.fill_rate_pct,
    target.active_flag = source.active_flag
when not matched then insert
    (warehouse_id, warehouse_name, inventory_units, fill_rate_pct, active_flag)
values
    (source.warehouse_id, source.warehouse_name, source.inventory_units,
     source.fill_rate_pct, source.active_flag);

merge into &&owner_name..sc_warehouse_geo target
using (
    select 4101 warehouse_id, 'WH-SG-01' warehouse_code,
           'Central Region' county_name, 'SG' state_code,
           'Singapore' region_name, 'APAC' market_region,
           1.352100 latitude, 103.819800 longitude from dual
    union all
    select 4102, 'WH-AU-02', 'New South Wales', 'NSW',
           'Australia East', 'APAC', -33.868800, 151.209300 from dual
) source
on (target.warehouse_id = source.warehouse_id)
when matched then update set
    target.warehouse_code = source.warehouse_code,
    target.county_name = source.county_name,
    target.state_code = source.state_code,
    target.region_name = source.region_name,
    target.market_region = source.market_region,
    target.latitude = source.latitude,
    target.longitude = source.longitude
when not matched then insert
    (warehouse_id, warehouse_code, county_name, state_code, region_name,
     market_region, latitude, longitude)
values
    (source.warehouse_id, source.warehouse_code, source.county_name,
     source.state_code, source.region_name, source.market_region,
     source.latitude, source.longitude);

prompt [4/8] seed region-specific stockout risks
merge into &&owner_name..sc_inventory_risk_summary target
using (
    select 'SKU-APAC-210' product_id,
           to_char(add_months(trunc(sysdate, 'Q'), 3), 'YYYY-"Q"Q') quarter_label,
           'CRITICAL' risk_level, 0.81 stockout_probability,
           980 at_risk_units, 214000 projected_revenue_impact_usd,
           'APAC' primary_region,
           'Rebalance cooling-controller inventory into Singapore.' recommendation_summary,
           'APAC' market_region, 'Y' active_flag from dual
    union all
    select 'SKU-APAC-420',
           to_char(add_months(trunc(sysdate, 'Q'), 3), 'YYYY-"Q"Q'),
           'HIGH', 0.67, 720, 148000, 'APAC',
           'Increase the Sydney safety-stock buffer before the demand peak.',
           'APAC', 'Y' from dual
) source
on (target.product_id = source.product_id)
when matched then update set
    target.quarter_label = source.quarter_label,
    target.risk_level = source.risk_level,
    target.stockout_probability = source.stockout_probability,
    target.at_risk_units = source.at_risk_units,
    target.projected_revenue_impact_usd = source.projected_revenue_impact_usd,
    target.primary_region = source.primary_region,
    target.recommendation_summary = source.recommendation_summary,
    target.market_region = source.market_region,
    target.active_flag = source.active_flag
when not matched then insert
    (product_id, quarter_label, risk_level, stockout_probability,
     at_risk_units, projected_revenue_impact_usd, primary_region,
     recommendation_summary, market_region, active_flag)
values
    (source.product_id, source.quarter_label, source.risk_level,
     source.stockout_probability, source.at_risk_units,
     source.projected_revenue_impact_usd, source.primary_region,
     source.recommendation_summary, source.market_region, source.active_flag);

merge into &&owner_name..sc_warehouse_risk_snapshot target
using (
    select 'SKU-APAC-210' product_id, 4101 warehouse_id, 1 hotspot_rank,
           0.91 hotspot_score, 3.4 coverage_days, 470 backlog_units,
           88 service_level_pct, 610 at_risk_units, 139000 revenue_impact_usd,
           'CRITICAL' risk_level, 'DESTINATION_HOTSPOT' recommended_role,
           'Y' active_flag from dual
    union all
    select 'SKU-APAC-210', 4102, 2, 0.44, 9.6, 140, 96, 270, 75000,
           'WATCH', 'SOURCE_BUFFER', 'Y' from dual
    union all
    select 'SKU-APAC-420', 4102, 1, 0.78, 4.8, 330, 91, 460, 101000,
           'HIGH', 'DESTINATION_HOTSPOT', 'Y' from dual
    union all
    select 'SKU-APAC-420', 4101, 2, 0.39, 10.2, 110, 97, 190, 47000,
           'BUFFER', 'SOURCE_BUFFER', 'Y' from dual
) source
on (target.product_id = source.product_id
    and target.warehouse_id = source.warehouse_id)
when matched then update set
    target.hotspot_rank = source.hotspot_rank,
    target.hotspot_score = source.hotspot_score,
    target.coverage_days = source.coverage_days,
    target.backlog_units = source.backlog_units,
    target.service_level_pct = source.service_level_pct,
    target.at_risk_units = source.at_risk_units,
    target.revenue_impact_usd = source.revenue_impact_usd,
    target.risk_level = source.risk_level,
    target.recommended_role = source.recommended_role,
    target.active_flag = source.active_flag
when not matched then insert
    (product_id, warehouse_id, hotspot_rank, hotspot_score, coverage_days,
     backlog_units, service_level_pct, at_risk_units, revenue_impact_usd,
     risk_level, recommended_role, active_flag)
values
    (source.product_id, source.warehouse_id, source.hotspot_rank,
     source.hotspot_score, source.coverage_days, source.backlog_units,
     source.service_level_pct, source.at_risk_units,
     source.revenue_impact_usd, source.risk_level,
     source.recommended_role, source.active_flag);

prompt [5/8] recreate the regional inventory view
create or replace view &&owner_name..sc_inventory_risk_demo_v as
select
    summary.product_id,
    product.product_name,
    summary.quarter_label,
    summary.risk_level as overall_risk_level,
    summary.stockout_probability,
    summary.at_risk_units as product_at_risk_units,
    summary.projected_revenue_impact_usd,
    summary.primary_region,
    summary.market_region,
    summary.recommendation_summary,
    warehouse.warehouse_id,
    warehouse.warehouse_name,
    geo.warehouse_code,
    geo.county_name,
    geo.state_code,
    geo.region_name,
    geo.latitude,
    geo.longitude,
    snapshot.hotspot_rank,
    snapshot.hotspot_score,
    snapshot.coverage_days,
    snapshot.backlog_units,
    snapshot.service_level_pct,
    snapshot.at_risk_units,
    snapshot.revenue_impact_usd,
    snapshot.risk_level,
    snapshot.recommended_role,
    summary.active_flag
from &&owner_name..sc_inventory_risk_summary summary
join &&owner_name..sc_products product
  on product.product_id = summary.product_id
join &&owner_name..sc_warehouse_risk_snapshot snapshot
  on snapshot.product_id = summary.product_id
 and snapshot.active_flag = 'Y'
join &&owner_name..sc_warehouses warehouse
  on warehouse.warehouse_id = snapshot.warehouse_id
join &&owner_name..sc_warehouse_geo geo
  on geo.warehouse_id = snapshot.warehouse_id
 and geo.market_region = summary.market_region
where summary.active_flag = 'Y';

comment on column &&owner_name..sc_inventory_risk_summary.market_region is
    'Authorization region used by the Oracle Deep Data Security demonstration.';
comment on column &&owner_name..sc_warehouse_geo.market_region is
    'Authorization region used by the Oracle Deep Data Security demonstration.';

prompt [6/8] create regional Deep Sec users and roles
declare
    role_exists exception;
    pragma exception_init(role_exists, -1921);
begin
    begin
        execute immediate 'create role inventory_deepsec_login_role';
    exception
        when role_exists then null;
    end;
end;
/

grant create session to inventory_deepsec_login_role;

create data role if not exists inventory_na_manager_role;
create data role if not exists inventory_apac_manager_role;
grant inventory_deepsec_login_role to inventory_na_manager_role;
grant inventory_deepsec_login_role to inventory_apac_manager_role;

create end user if not exists &&na_username identified by "&&na_password";
begin
    execute immediate 'alter end user &&na_username identified by "&&na_password" account unlock';
exception
    when others then
        if sqlcode <> -28007 then
            raise;
        end if;
end;
/
alter end user &&na_username account unlock;
grant data role inventory_na_manager_role to &&na_username;

create end user if not exists &&apac_username identified by "&&apac_password";
begin
    execute immediate 'alter end user &&apac_username identified by "&&apac_password" account unlock';
exception
    when others then
        if sqlcode <> -28007 then
            raise;
        end if;
end;
/
alter end user &&apac_username account unlock;
grant data role inventory_apac_manager_role to &&apac_username;

prompt [7/8] create region-scoped data grants
create or replace data grant &&owner_name..inventory_na_risk_view_read
  as select on &&owner_name..sc_inventory_risk_demo_v
  where market_region = 'NA'
  to inventory_na_manager_role;

create or replace data grant &&owner_name..inventory_apac_risk_view_read
  as select on &&owner_name..sc_inventory_risk_demo_v
  where market_region = 'APAC'
  to inventory_apac_manager_role;

begin
    execute immediate
        'set use data grants only on &&owner_name..sc_inventory_risk_demo_v enabled';
end;
/

prompt [8/8] commit setup
commit;

prompt Deep Data Security regional setup complete.
exit success
