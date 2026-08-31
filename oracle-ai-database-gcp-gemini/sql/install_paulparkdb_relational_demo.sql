-- Continue an installation when the schema owner has not yet been granted
-- CREATE PROPERTY GRAPH. The base SC_* tables must already exist.

set define off echo on feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

prompt === Seed supply-chain relational data ===
@seed_supply_chain_graph_data.sql

prompt === Create inventory-risk and spatial objects ===
@setup_inventory_risk_demo_schema.sql

prompt === Seed inventory-risk and spatial data ===
@seed_inventory_risk_demo_data.sql

prompt === paulparkdb relational demo installation complete ===
commit;
exit success
