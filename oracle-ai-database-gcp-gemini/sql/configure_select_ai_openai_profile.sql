-- configure_select_ai_openai_profile.sql
--
-- Purpose
--   Configure an Oracle Select AI profile for the demo inventory tables using OpenAI.
--   This is written for Autonomous Database / Autonomous AI Database.
--
-- How to use
--   1. Replace the substitution variables below with your actual schema and API key.
--   2. Run this as the schema that owns the demo tables.
--   3. For JDBC or stateless app calls, prefer DBMS_CLOUD_AI.GENERATE(profile_name => ...).
--   4. For manual SQL worksheet sessions, call DBMS_CLOUD_AI.SET_PROFILE after connecting.
--
-- Official references:
--   - Manage AI profiles:
--     https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/select-ai-manage-profiles.html
--   - DBMS_CLOUD_AI package:
--     https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/dbms-cloud-ai-package.html
--   - Select AI examples:
--     https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/select-ai-examples.html

set verify off echo off serveroutput on feedback on

define demo_owner = 'ADMIN'
define credential_name = 'OPENAI_CRED'
define profile_name = 'OPENAI_INVENTORY_DEMO'
define openai_api_key = 'replace-with-openai-api-key'
define openai_model = 'gpt-4o-mini'

prompt host ACL for OpenAI
begin
    dbms_network_acl_admin.append_host_ace(
        host => 'api.openai.com',
        ace  => xs$ace_type(
            privilege_list => xs$name_list('http'),
            principal_name => upper('&demo_owner'),
            principal_type => xs_acl.ptype_db
        )
    );
exception
    when others then
        if sqlcode = -24243 then
            dbms_output.put_line('SKIP: host ACE already exists for api.openai.com');
        else
            raise;
        end if;
end;
/

prompt credential
begin
    begin
        dbms_cloud.drop_credential(credential_name => upper('&credential_name'));
    exception
        when others then
            if sqlcode != -20004 then
                raise;
            end if;
    end;

    dbms_cloud.create_credential(
        credential_name => upper('&credential_name'),
        username        => 'openai',
        password        => '&openai_api_key'
    );
end;
/

prompt profile
begin
    begin
        dbms_cloud_ai.drop_profile(profile_name => upper('&profile_name'));
    exception
        when others then
            null;
    end;

    dbms_cloud_ai.create_profile(
        profile_name => upper('&profile_name'),
        attributes   => json_object(
            'provider' value 'openai',
            'credential_name' value upper('&credential_name'),
            'model' value '&openai_model',
            'comments' value 'true',
            'object_list' value json_array(
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PRODUCTS'),
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSES'),
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_INVENTORY_RISK_SUMMARY'),
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSE_GEO'),
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSE_RISK_SNAPSHOT'),
                json_object('owner' value upper('&demo_owner'), 'name' value 'SC_INVENTORY_RISK_DEMO_V')
            )
        )
    );
end;
/

prompt set profile for current session
exec dbms_cloud_ai.set_profile(upper('&profile_name'));

prompt sample manual tests
select ai narrate which products are at risk of stockouts next quarter?;
select ai narrate which regions are driving the risk for SKU-500?;
select dbms_cloud_ai.generate(
           prompt       => 'Which warehouses are driving the risk for SKU-500?',
           profile_name => upper('&profile_name'),
           action       => 'narrate'
       ) as ai_response
  from dual;
