-- Read-only preflight for the paulparkdb supply-chain demo.
-- Run through run_paulparkdb_demo_audit.sh so DEMO_OWNER is supplied safely.

set verify off echo off feedback on pagesize 500 linesize 220 trimspool on
set serveroutput on size unlimited

define demo_owner = '&1'

prompt === Connection ===
column database_name format a20
column service_name format a70
column session_user format a24
column current_schema format a24
select sys_context('USERENV', 'DB_NAME') as database_name,
       sys_context('USERENV', 'SERVICE_NAME') as service_name,
       sys_context('USERENV', 'SESSION_USER') as session_user,
       sys_context('USERENV', 'CURRENT_SCHEMA') as current_schema
  from dual;

prompt === Relevant database users ===
column username format a30
select username
  from all_users
 where username in ('ADMIN', 'FINANCIAL', 'SALES_USER', upper('&demo_owner'))
 order by username;

prompt === Existing supply-chain objects visible to this user ===
column owner format a30
column object_type format a24
select owner, object_type, count(*) as object_count
  from all_objects
 where (object_name like 'SC\_%' escape '\' or object_name = 'SUPPLY_CHAIN_GRAPH')
   and owner in ('ADMIN', 'FINANCIAL', 'SALES_USER', upper('&demo_owner'))
 group by owner, object_type
 order by owner, object_type;

prompt === Expected object-by-object state ===
column object_name format a44
column status format a12
select owner, object_name, object_type, status
  from all_objects
 where owner = upper('&demo_owner')
   and object_name in (
       'SC_SUPPLIERS', 'SC_PLANTS', 'SC_PORTS', 'SC_WAREHOUSES',
       'SC_PRODUCTS', 'SC_ALERTS', 'SC_SUPPLIER_PLANT', 'SC_PLANT_PORT',
       'SC_PORT_WAREHOUSE', 'SC_WAREHOUSE_PRODUCT', 'SC_ALERT_PORT',
       'SC_INVENTORY_RISK_SUMMARY', 'SC_WAREHOUSE_GEO',
       'SC_WAREHOUSE_RISK_SNAPSHOT', 'SC_INVENTORY_RISK_DEMO_V',
       'SUPPLY_CHAIN_GRAPH'
   )
 order by object_name;

prompt === Existing Select AI profiles owned by the connected user ===
declare
    l_count number;
begin
    begin
        execute immediate q'[
            select count(distinct profile_name)
              from user_cloud_ai_profile_attributes
        ]' into l_count;
        dbms_output.put_line('Select AI profile count: ' || l_count);
        for item in (
            select distinct profile_name
              from user_cloud_ai_profile_attributes
             order by profile_name
        ) loop
            dbms_output.put_line('Profile: ' || item.profile_name);
        end loop;
    exception
        when others then
            dbms_output.put_line('Select AI profile metadata unavailable: ' || sqlerrm);
    end;
end;
/

prompt === Existing Select AI agent teams owned by the connected user ===
declare
    l_count number;
begin
    begin
        execute immediate 'select count(*) from user_ai_agent_teams' into l_count;
        dbms_output.put_line('Select AI agent team count: ' || l_count);
    exception
        when others then
            dbms_output.put_line('Select AI agent team metadata unavailable: ' || sqlerrm);
    end;
end;
/

prompt === Select AI agent-team view columns for this database release ===
column column_name format a42
column data_type format a28
select owner, column_id, column_name, data_type
  from all_tab_columns
 where table_name = 'USER_AI_AGENT_TEAMS'
 order by owner, column_id;

prompt === Package visibility ===
column object_name format a36
select owner, object_name, object_type, status
  from all_objects
 where object_name in ('DBMS_CLOUD_AI', 'DBMS_CLOUD_AI_AGENT')
   and object_type in ('PACKAGE', 'PACKAGE BODY')
 order by object_name, object_type;

prompt === Package synonyms and direct grants ===
column synonym_name format a36
column table_owner format a24
column table_name format a36
select owner, synonym_name, table_owner, table_name
  from all_synonyms
 where synonym_name in ('DBMS_CLOUD_AI', 'DBMS_CLOUD_AI_AGENT')
 order by synonym_name, owner;

column grantee format a24
column privilege format a18
select grantee, table_schema as owner, table_name, privilege
  from all_tab_privs
 where table_name in ('DBMS_CLOUD_AI', 'DBMS_CLOUD_AI_AGENT')
   and grantee in (user, 'PUBLIC')
 order by table_name, grantee, privilege;

prompt === Audit complete; no data was changed ===

exit success
