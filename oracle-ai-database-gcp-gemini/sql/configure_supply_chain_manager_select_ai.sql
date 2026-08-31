-- Create an equivalent Select AI profile inside one supply-chain manager
-- schema. Run while connected as that schema user. The OPENAI_CRED private
-- synonym securely reuses the encrypted FINANCIAL credential without exposing
-- or duplicating the provider secret.

set define on verify off echo off feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

define object_owner = '&&1'
define credential_name = '&&2'
define profile_name = '&&3'

begin
    begin
        dbms_cloud_ai.drop_profile(
            profile_name => upper('&&profile_name')
        );
    exception
        when others then
            null;
    end;

    dbms_cloud_ai.create_profile(
        profile_name => upper('&&profile_name'),
        attributes   => json_object(
            'provider' value 'openai',
            'credential_name' value upper('&&credential_name'),
            'model' value 'gpt-4o-mini',
            'comments' value 'true',
            'object_list' value json_array(
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_PRODUCTS'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_WAREHOUSES'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_SUPPLIERS'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_PLANTS'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_PORTS'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_ALERTS'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_SUPPLIER_PLANT'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_PLANT_PORT'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_PORT_WAREHOUSE'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_WAREHOUSE_PRODUCT'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_ALERT_PORT'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_INVENTORY_RISK_SUMMARY'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_WAREHOUSE_GEO'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_WAREHOUSE_RISK_SNAPSHOT'),
                json_object('owner' value upper('&&object_owner'), 'name' value 'SC_INVENTORY_RISK_DEMO_V')
            )
        )
    );

    dbms_output.put_line(
        'Created ' || sys_context('USERENV', 'SESSION_USER') || '.' ||
        upper('&&profile_name')
    );
end;
/
exit success
