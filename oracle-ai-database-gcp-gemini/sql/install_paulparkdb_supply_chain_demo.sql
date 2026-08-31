-- Idempotent installation driver for the paulparkdb supply-chain demo schema.
-- Connect as the narrow demo schema owner (FINANCIAL in adb-pm-prod).

set define off echo on feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

prompt === Create relational and property-graph objects ===
@setup_supply_chain_graph_schema.sql

prompt === Seed relational and graph data ===
@seed_supply_chain_graph_data.sql

prompt === Create inventory-risk and spatial objects ===
@setup_inventory_risk_demo_schema.sql

prompt === Seed inventory-risk and spatial data ===
@seed_inventory_risk_demo_data.sql

prompt === paulparkdb supply-chain demo installation complete ===
commit;
exit success
