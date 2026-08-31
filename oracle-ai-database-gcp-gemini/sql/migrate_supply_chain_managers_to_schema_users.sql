-- Replace the two local Deep Sec end users with ordinary schema users so the
-- managed Autonomous AI Database A2A OAuth endpoint can authenticate them.
--
-- The regional Deep Sec roles and data grants are intentionally retained for
-- possible future testing, but they are no longer assigned to these users.
-- Both users receive the same conventional access to the FINANCIAL supply-chain
-- objects used by the Select AI profile.

set define on verify off echo off feedback on serveroutput on size unlimited
whenever sqlerror exit sql.sqlcode rollback

define owner_name = '&&1'
define na_username = '&&2'
define na_password = '&&3'
define apac_username = '&&4'
define apac_password = '&&5'

prompt [1/5] Validate the administrator session and source schema
declare
    l_count number;
begin
    if sys_context('USERENV', 'SESSION_USER') <> 'ADMIN' then
        raise_application_error(-20001, 'Connect as ADMIN before running this script.');
    end if;

    select count(*)
      into l_count
      from dba_users
     where username = upper('&&owner_name');

    if l_count = 0 then
        raise_application_error(-20002, 'Source schema &&owner_name does not exist.');
    end if;
end;
/

prompt [2/5] Revoke regional data roles and remove local Deep Sec identities
declare
    procedure remove_end_user(
        p_username  varchar2,
        p_data_role varchar2
    ) is
        l_count number;
    begin
        select count(*)
          into l_count
          from dba_data_role_grants
         where grantee = upper(p_username)
           and data_role = upper(p_data_role);

        if l_count > 0 then
            execute immediate
                'revoke data role ' || dbms_assert.simple_sql_name(p_data_role) ||
                ' from ' || dbms_assert.simple_sql_name(p_username);
        end if;

        select count(*)
          into l_count
          from dba_end_users
         where username = upper(p_username);

        if l_count > 0 then
            execute immediate
                'drop end user ' || dbms_assert.simple_sql_name(p_username);
        end if;
    end remove_end_user;
begin
    remove_end_user('&&na_username', 'INVENTORY_NA_MANAGER_ROLE');
    remove_end_user('&&apac_username', 'INVENTORY_APAC_MANAGER_ROLE');
end;
/

prompt [3/5] Create or refresh ordinary schema users
declare
    procedure ensure_schema_user(
        p_username varchar2,
        p_password varchar2
    ) is
        l_count number;
    begin
        select count(*)
          into l_count
          from dba_users
         where username = upper(p_username);

        if l_count = 0 then
            execute immediate
                'create user ' || dbms_assert.simple_sql_name(p_username) ||
                ' identified by "' || replace(p_password, '"', '""') ||
                '" default tablespace data temporary tablespace temp quota 100M on data account unlock';
        else
            begin
                execute immediate
                    'alter user ' || dbms_assert.simple_sql_name(p_username) ||
                    ' identified by "' || replace(p_password, '"', '""') ||
                    '" account unlock';
            exception
                when others then
                    if sqlcode <> -28007 then
                        raise;
                    end if;
            end;
            execute immediate
                'alter user ' || dbms_assert.simple_sql_name(p_username) ||
                ' account unlock';
            execute immediate
                'alter user ' || dbms_assert.simple_sql_name(p_username) ||
                ' quota 100M on data';
        end if;
    end ensure_schema_user;
begin
    ensure_schema_user('&&na_username', '&&na_password');
    ensure_schema_user('&&apac_username', '&&apac_password');
end;
/

grant create session, create table, create procedure to &&na_username;
grant create synonym to &&na_username;
grant execute on dbms_cloud to &&na_username;
grant execute on dbms_cloud_ai to &&na_username;
grant execute on dbms_cloud_ai_agent to &&na_username;

grant create session, create table, create procedure to &&apac_username;
grant create synonym to &&apac_username;
grant execute on dbms_cloud to &&apac_username;
grant execute on dbms_cloud_ai to &&apac_username;
grant execute on dbms_cloud_ai_agent to &&apac_username;

grant execute on &&owner_name..openai_cred to &&na_username;
grant execute on &&owner_name..openai_cred to &&apac_username;

create or replace synonym &&na_username..openai_cred
  for &&owner_name..openai_cred;
create or replace synonym &&apac_username..openai_cred
  for &&owner_name..openai_cred;

prompt [4/5] Grant both users identical access to FINANCIAL supply-chain objects
declare
    procedure grant_supply_chain_objects(p_username varchar2) is
    begin
        for object_record in (
            select object_name
              from dba_objects
             where owner = upper('&&owner_name')
               and object_name like 'SC\_%' escape '\'
               and object_type in ('TABLE', 'VIEW', 'MATERIALIZED VIEW')
             order by object_name
        ) loop
            execute immediate
                'grant select on ' ||
                dbms_assert.schema_name(upper('&&owner_name')) || '.' ||
                dbms_assert.simple_sql_name(object_record.object_name) ||
                ' to ' || dbms_assert.simple_sql_name(p_username);
        end loop;
    end grant_supply_chain_objects;
begin
    grant_supply_chain_objects('&&na_username');
    grant_supply_chain_objects('&&apac_username');
end;
/

-- Conventional schema users rely on normal object grants. Deep Sec's
-- data-grants-only mode would otherwise prevent those grants from being used.
begin
    execute immediate
        'set use data grants only on &&owner_name..sc_inventory_risk_demo_v disabled';
end;
/

begin
    for principal in (
        select upper('&&na_username') username from dual
        union all
        select upper('&&apac_username') from dual
    ) loop
        begin
            dbms_network_acl_admin.append_host_ace(
                host => 'api.openai.com',
                ace  => xs$ace_type(
                    privilege_list => xs$name_list('http'),
                    principal_name => principal.username,
                    principal_type => xs_acl.ptype_db
                )
            );
        exception
            when others then
                if sqlcode <> -24243 then
                    raise;
                end if;
        end;
    end loop;
end;
/

prompt [5/5] Verify ordinary users and commit
select username, account_status, authentication_type, default_tablespace
  from dba_users
 where username in (upper('&&na_username'), upper('&&apac_username'))
 order by username;

commit;
prompt Supply-chain manager schema-user migration complete.
exit success
