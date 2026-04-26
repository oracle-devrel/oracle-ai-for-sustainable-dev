-- configure_select_ai_demo_profile.sql
--
-- Purpose
--   Create a dedicated Select AI profile that preserves the objects from an
--   already-working source profile such as SALES_DATA_PROFILE and adds the
--   supply-chain / inventory-risk demo objects.

set verify off echo off serveroutput on feedback on

define demo_owner = 'SALES_USER'
define source_profile = 'SALES_DATA_PROFILE'
define profile_name = 'SALES_SUPPLY_CHAIN_DEMO'

declare
    l_provider           varchar2(4000);
    l_credential_name    varchar2(4000);
    l_model              varchar2(4000);
    l_max_tokens         varchar2(4000);
    l_oci_compartment_id varchar2(4000);
    l_source_object_list clob;
    l_demo_object_list   clob;
    l_object_list        clob;
    l_attributes         clob;

    function profile_attr(p_profile_name varchar2, p_attribute_name varchar2) return varchar2 is
        l_value user_cloud_ai_profile_attributes.attribute_value%type;
    begin
        select attribute_value
          into l_value
          from user_cloud_ai_profile_attributes
         where profile_name = upper(p_profile_name)
           and attribute_name = lower(p_attribute_name);
        return l_value;
    exception
        when no_data_found then
            return null;
    end profile_attr;
begin
    l_provider := profile_attr('&source_profile', 'provider');
    l_credential_name := profile_attr('&source_profile', 'credential_name');
    l_model := profile_attr('&source_profile', 'model');
    l_max_tokens := profile_attr('&source_profile', 'max_tokens');
    l_oci_compartment_id := profile_attr('&source_profile', 'oci_compartment_id');
    l_source_object_list := coalesce(profile_attr('&source_profile', 'object_list'), '[]');

    if l_provider is null or l_credential_name is null or l_model is null then
        raise_application_error(
            -20001,
            'Source profile ' || upper('&source_profile') || ' is missing required provider, credential_name, or model attributes.'
        );
    end if;

    select json_array(
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PRODUCTS'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSES'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_SUPPLIERS'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PLANTS'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PORTS'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_ALERTS'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_SUPPLIER_PLANT'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PLANT_PORT'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_PORT_WAREHOUSE'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSE_PRODUCT'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_ALERT_PORT'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_INVENTORY_RISK_SUMMARY'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSE_GEO'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_WAREHOUSE_RISK_SNAPSHOT'),
               json_object('owner' value upper('&demo_owner'), 'name' value 'SC_INVENTORY_RISK_DEMO_V')
               returning clob
           )
      into l_demo_object_list
      from dual;

    select json_arrayagg(
               json_object('owner' value owner_name, 'name' value object_name)
               order by first_seen
               returning clob
           )
      into l_object_list
      from (
          select owner_name, object_name, min(sort_order) as first_seen
          from (
              select upper(owner_name) as owner_name, upper(object_name) as object_name, rownum as sort_order
              from json_table(
                       l_source_object_list,
                       '$[*]' columns (
                           owner_name  varchar2(128) path '$.owner',
                           object_name varchar2(128) path '$.name'
                       )
                   )
              where owner_name is not null
                and object_name is not null
              union all
              select upper(owner_name) as owner_name, upper(object_name) as object_name, 100000 + rownum as sort_order
              from json_table(
                       l_demo_object_list,
                       '$[*]' columns (
                           owner_name  varchar2(128) path '$.owner',
                           object_name varchar2(128) path '$.name'
                       )
                   )
          )
          group by owner_name, object_name
      );

    begin
        dbms_cloud_ai.drop_profile(profile_name => upper('&profile_name'));
    exception
        when others then
            null;
    end;

    select json_object(
               'provider' value l_provider,
               'credential_name' value l_credential_name,
               'model' value l_model,
               'comments' value 'true',
               'max_tokens' value case
                   when l_max_tokens is not null and regexp_like(l_max_tokens, '^\d+$')
                   then to_number(l_max_tokens)
               end,
               'oci_compartment_id' value l_oci_compartment_id,
               'object_list' value l_object_list format json
               absent on null
               returning clob
           )
      into l_attributes
      from dual;

    dbms_cloud_ai.create_profile(
        profile_name => upper('&profile_name'),
        attributes   => l_attributes
    );

    dbms_output.put_line('OK: created profile ' || upper('&profile_name'));
    dbms_output.put_line('Source profile: ' || upper('&source_profile'));
    dbms_output.put_line('Provider: ' || l_provider);
    dbms_output.put_line('Credential: ' || l_credential_name);
    dbms_output.put_line('Model: ' || l_model);
    dbms_output.put_line('Object list: source profile objects plus supply-chain demo objects');
end;
/

prompt profile attributes
column attribute_name format a30
column attribute_value format a120
select attribute_name, attribute_value
  from user_cloud_ai_profile_attributes
 where profile_name = upper('&profile_name')
 order by attribute_name;

prompt sample narrate test
select dbms_cloud_ai.generate(
           prompt       => 'Which products are at risk of stockouts next quarter, and which regions are driving that risk?',
           profile_name => upper('&profile_name'),
           action       => 'narrate'
       ) as ai_response
  from dual;
