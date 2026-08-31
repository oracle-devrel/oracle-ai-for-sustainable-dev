-- Run after setup_inventory_risk_deepsec_regions.sql. SQLcl arguments:
-- owner, NA user/password, APAC user/password, service name.

set define on verify off echo off feedback on pagesize 100 linesize 220
whenever sqlerror exit sql.sqlcode rollback

define owner_name = '&&1'
define na_username = '&&2'
define na_password = '&&3'
define apac_username = '&&4'
define apac_password = '&&5'
define service_name = '&&6'

prompt NA Deep Sec end user: expected only NA
connect &&na_username/"&&na_password"@&&service_name
select distinct market_region
  from &&owner_name..sc_inventory_risk_demo_v
 order by market_region;
select product_id, product_name, stockout_probability,
       projected_revenue_impact_usd, primary_region,
       warehouse_name, region_name
  from &&owner_name..sc_inventory_risk_demo_v
 where hotspot_rank = 1
 order by stockout_probability desc;

prompt APAC Deep Sec end user: expected only APAC
connect &&apac_username/"&&apac_password"@&&service_name
select distinct market_region
  from &&owner_name..sc_inventory_risk_demo_v
 order by market_region;
select product_id, product_name, stockout_probability,
       projected_revenue_impact_usd, primary_region,
       warehouse_name, region_name
  from &&owner_name..sc_inventory_risk_demo_v
 where hotspot_rank = 1
 order by stockout_probability desc;

exit success
