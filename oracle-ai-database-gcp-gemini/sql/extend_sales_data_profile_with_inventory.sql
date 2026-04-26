-- extend_sales_data_profile_with_inventory.sql
--
-- Purpose
--   Carefully extend the working SALES_DATA_PROFILE with the supply-chain /
--   inventory-risk demo objects while preserving the existing sales objects and
--   provider settings. This is intended for cases where an upstream Oracle AI
--   Database agent is bound to SALES_DATA_PROFILE and must keep sales access.
--
-- Safety
--   - Creates SALES_DATA_PROFILE_BEFORE_SC as a rollback snapshot first, and
--     preserves it on re-runs.
--   - Rebuilds SALES_DATA_PROFILE from its existing attributes plus SC_* objects.
--   - Does not drop or modify the underlying sales or inventory tables.

set verify off echo off serveroutput on feedback on

define demo_owner = 'SALES_USER'
define profile_name = 'SALES_DATA_PROFILE'
define backup_profile_name = 'SALES_DATA_PROFILE_BEFORE_SC'

declare
    l_provider           varchar2(4000);
    l_credential_name    varchar2(4000);
    l_model              varchar2(4000);
    l_max_tokens         varchar2(4000);
    l_comments           varchar2(4000);
    l_oci_compartment_id varchar2(4000);
    l_source_object_list clob;
    l_demo_object_list   clob;
    l_object_list        clob;
    l_original_attrs     clob;
    l_extended_attrs     clob;
    l_before_count       number;
    l_after_count        number;
    l_sc_count           number;
    l_backup_exists      number;

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

    function profile_attrs_json(
        p_provider varchar2,
        p_credential_name varchar2,
        p_model varchar2,
        p_comments varchar2,
        p_max_tokens varchar2,
        p_oci_compartment_id varchar2,
        p_object_list clob
    ) return clob is
        l_json clob;
    begin
        select json_object(
                   'provider' value p_provider,
                   'credential_name' value p_credential_name,
                   'model' value p_model,
                   'comments' value p_comments,
                   'max_tokens' value case
                       when p_max_tokens is not null and regexp_like(p_max_tokens, '^\d+$')
                       then to_number(p_max_tokens)
                   end,
                   'oci_compartment_id' value p_oci_compartment_id,
                   'object_list' value p_object_list format json
                   absent on null
                   returning clob
               )
          into l_json
          from dual;
        return l_json;
    end profile_attrs_json;
begin
    l_provider := profile_attr('&profile_name', 'provider');
    l_credential_name := profile_attr('&profile_name', 'credential_name');
    l_model := profile_attr('&profile_name', 'model');
    l_max_tokens := profile_attr('&profile_name', 'max_tokens');
    l_comments := profile_attr('&profile_name', 'comments');
    l_oci_compartment_id := profile_attr('&profile_name', 'oci_compartment_id');
    l_source_object_list := coalesce(profile_attr('&profile_name', 'object_list'), '[]');

    if l_provider is null or l_credential_name is null or l_model is null then
        raise_application_error(
            -20001,
            'Profile ' || upper('&profile_name') || ' is missing required provider, credential_name, or model attributes.'
        );
    end if;

    select count(*)
      into l_before_count
      from json_table(
               l_source_object_list,
               '$[*]' columns (object_name varchar2(128) path '$.name')
           );

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

    select count(*),
           sum(case when substr(object_name, 1, 3) = 'SC_' then 1 else 0 end)
      into l_after_count, l_sc_count
      from json_table(
               l_object_list,
               '$[*]' columns (object_name varchar2(128) path '$.name')
           );

    if l_after_count < l_before_count then
        raise_application_error(-20002, 'Merged object list is smaller than the original list; refusing to continue.');
    end if;

    l_original_attrs := profile_attrs_json(
        l_provider,
        l_credential_name,
        l_model,
        l_comments,
        l_max_tokens,
        l_oci_compartment_id,
        l_source_object_list
    );

    l_extended_attrs := profile_attrs_json(
        l_provider,
        l_credential_name,
        l_model,
        l_comments,
        l_max_tokens,
        l_oci_compartment_id,
        l_object_list
    );

    select count(*)
      into l_backup_exists
      from user_cloud_ai_profile_attributes
     where profile_name = upper('&backup_profile_name');

    if l_backup_exists = 0 then
        dbms_cloud_ai.create_profile(
            profile_name => upper('&backup_profile_name'),
            attributes   => l_original_attrs
        );
    end if;

    begin
        dbms_cloud_ai.drop_profile(profile_name => upper('&profile_name'));
    exception
        when others then
            raise;
    end;

    dbms_cloud_ai.create_profile(
        profile_name => upper('&profile_name'),
        attributes   => l_extended_attrs
    );

    if l_backup_exists = 0 then
        dbms_output.put_line('OK: created rollback snapshot ' || upper('&backup_profile_name'));
    else
        dbms_output.put_line('OK: preserved existing rollback snapshot ' || upper('&backup_profile_name'));
    end if;
    dbms_output.put_line('OK: extended ' || upper('&profile_name'));
    dbms_output.put_line('Original object count: ' || l_before_count);
    dbms_output.put_line('Extended object count: ' || l_after_count);
    dbms_output.put_line('SC object count: ' || l_sc_count);
end;
/

prompt profile object counts
column profile_name format a32
column object_count format 999
column sc_object_count format 999
column other_object_count format 999

with object_attrs as (
    select profile_name, attribute_value as object_list
    from user_cloud_ai_profile_attributes
    where attribute_name = 'object_list'
      and profile_name in (upper('&profile_name'), upper('&backup_profile_name'))
), profile_objects as (
    select profile_name, jt.object_name
    from object_attrs,
         json_table(object_list, '$[*]' columns (
             object_name varchar2(128) path '$.name'
         )) jt
)
select profile_name,
       count(*) as object_count,
       sum(case when substr(object_name, 1, 3) = 'SC_' then 1 else 0 end) as sc_object_count,
       sum(case when substr(object_name, 1, 3) = 'SC_' then 0 else 1 end) as other_object_count
from profile_objects
group by profile_name
order by profile_name;
