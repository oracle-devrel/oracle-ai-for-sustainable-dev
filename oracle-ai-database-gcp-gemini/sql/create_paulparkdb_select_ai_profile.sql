-- Create the narrow FINANCIAL-owned Select AI profile used by the Oracle AI
-- Database Agent. Connect as FINANCIAL after ADMIN has run
-- admin_prepare_paulparkdb_demo.sql.
--
-- The provider credential must already exist in FINANCIAL. The defaults below
-- use Google's Select AI provider; override the three DEFINE values when using
-- a different supported provider, credential, or model.

set verify off echo on feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

define profile_name = 'PAULPARK_SUPPLY_CHAIN_DEMO'
define provider = 'google'
define credential_name = 'GOOGLE_AI_CRED'
define model = 'gemini-2.5-flash'

declare
    l_count number;
    l_attributes clob;
begin
    if sys_context('USERENV', 'SESSION_USER') <> 'FINANCIAL' then
        raise_application_error(-20001, 'Connect as FINANCIAL before running this script.');
    end if;

    select count(*)
      into l_count
      from user_cloud_ai_profiles
     where profile_name = upper('&profile_name');

    if l_count > 0 then
        raise_application_error(
            -20002,
            'Profile ' || upper('&profile_name') ||
            ' already exists. Audit it before changing or replacing it.'
        );
    end if;

    select json_object(
               'provider' value '&provider',
               'credential_name' value upper('&credential_name'),
               'model' value '&model',
               'comments' value 'true',
               'object_list' value json_array(
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_PRODUCTS'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_WAREHOUSES'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_SUPPLIERS'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_PLANTS'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_PORTS'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_ALERTS'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_SUPPLIER_PLANT'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_PLANT_PORT'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_PORT_WAREHOUSE'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_WAREHOUSE_PRODUCT'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_ALERT_PORT'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_INVENTORY_RISK_SUMMARY'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_WAREHOUSE_GEO'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_WAREHOUSE_RISK_SNAPSHOT'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SC_INVENTORY_RISK_DEMO_V'),
                   json_object('owner' value 'FINANCIAL', 'name' value 'SUPPLY_CHAIN_GRAPH')
               )
               returning clob
           )
      into l_attributes
      from dual;

    dbms_cloud_ai.create_profile(
        profile_name => upper('&profile_name'),
        attributes   => l_attributes,
        description  => 'Narrow supply-chain inventory demo profile for Gemini Enterprise'
    );

    dbms_output.put_line('Created ' || upper('&profile_name'));
end;
/

select profile_name, status, description
  from user_cloud_ai_profiles
 where profile_name = upper('&profile_name');

select dbms_cloud_ai.generate(
           prompt       => 'Which products are at risk of stockouts next quarter, and which regions are driving that risk?',
           profile_name => upper('&profile_name'),
           action       => 'showsql'
       ) as generated_sql
  from dual;

exit success
