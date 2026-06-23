-- Idempotent-ish admin setup for the financial demo application user.
--
-- Usage:
--   @setup-admin.sql FINANCIAL "StrongPassword123"
--
-- Run as ADMIN or another privileged database user.

set define on
set serveroutput on
set verify off
whenever sqlerror exit sql.sqlcode rollback

define app_user = '&1'
define app_password = '&2'

declare
  l_user_name varchar2(128) := dbms_assert.simple_sql_name(upper('&&app_user'));
  l_count number;
begin
  select count(*) into l_count from dba_users where username = l_user_name;

  if l_count = 0 then
    execute immediate 'create user ' || l_user_name || ' identified by "' ||
      replace('&&app_password', '"', '""') || '"';
    dbms_output.put_line('Created user ' || l_user_name);
  else
    execute immediate 'alter user ' || l_user_name || ' identified by "' ||
      replace('&&app_password', '"', '""') || '" account unlock';
    dbms_output.put_line('Updated password and unlocked user ' || l_user_name);
  end if;
end;
/

grant connect to &&app_user;
grant resource to &&app_user;
alter user &&app_user quota unlimited on data;

begin
  execute immediate 'grant console_developer to &&app_user';
exception
  when others then dbms_output.put_line('Skipping CONSOLE_DEVELOPER grant: ' || sqlerrm);
end;
/

begin
  execute immediate 'grant dwrole to &&app_user';
exception
  when others then dbms_output.put_line('Skipping DWROLE grant: ' || sqlerrm);
end;
/

begin
  execute immediate 'grant graph_developer to &&app_user';
exception
  when others then dbms_output.put_line('Skipping GRAPH_DEVELOPER grant: ' || sqlerrm);
end;
/

begin
  execute immediate 'grant oml_developer to &&app_user';
exception
  when others then dbms_output.put_line('Skipping OML_DEVELOPER grant: ' || sqlerrm);
end;
/

begin
  execute immediate 'grant aq_user_role to &&app_user';
  execute immediate 'grant execute on dbms_aq to &&app_user';
  execute immediate 'grant execute on dbms_aqadm to &&app_user';
  execute immediate 'begin dbms_aqadm.grant_priv_for_rm_plan(:user_name); end;'
    using upper('&&app_user');
exception
  when others then dbms_output.put_line('Skipping TxEventQ grants: ' || sqlerrm);
end;
/

begin
  execute immediate q'[
    begin
      ords_admin.enable_schema(
        p_enabled => true,
        p_schema => :schema_name,
        p_url_mapping_type => 'BASE_PATH',
        p_url_mapping_pattern => :url_mapping,
        p_auto_rest_auth => false
      );
    end;]' using upper('&&app_user'), lower('&&app_user');
exception
  when others then dbms_output.put_line('Skipping ORDS schema enablement: ' || sqlerrm);
end;
/

begin
  execute immediate 'alter user &&app_user grant connect through graph$proxy_user';
exception
  when others then dbms_output.put_line('Skipping Graph proxy grant: ' || sqlerrm);
end;
/

begin
  execute immediate 'alter user &&app_user grant connect through oml$proxy';
exception
  when others then dbms_output.put_line('Skipping OML proxy grant: ' || sqlerrm);
end;
/

commit;
