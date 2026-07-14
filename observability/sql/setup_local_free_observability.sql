-- Local Oracle Database Free + Podman observability setup.
--
-- Run from inside the Oracle Free container as SYSDBA, for example:
--   sqlplus / as sysdba @/path/to/setup_local_free_observability.sql
--
-- This script uses documented DBMS_OBSERVABILITY APIs and does not set hidden
-- instance parameters. It is intended for a local demo database.

set echo on
set feedback on
set verify off
set serveroutput on size unlimited
set pages 200
set lines 260
set long 200000
set longchunksize 200000

accept pdb_name char default 'FREEPDB1' prompt 'PDB name [FREEPDB1]: '
accept app_user char default 'FINANCIAL' prompt 'Application DB user [FINANCIAL]: '
accept app_password char hide prompt 'Application DB password: '
accept collector_host char default 'oracle-db-otel-jaeger' prompt 'Collector host [oracle-db-otel-jaeger]: '
accept collector_port char default '4318' prompt 'Collector OTLP HTTP port [4318]: '

define trace_endpoint = 'http://&&collector_host:&&collector_port/v1/traces'
define logs_endpoint = 'http://&&collector_host:&&collector_port/v1/logs'

prompt === Configure listener ===
alter system set local_listener='(ADDRESS=(PROTOCOL=tcp)(HOST=0.0.0.0)(PORT=1521))' scope=both;
alter system register;

prompt === Enable DBMS_OBSERVABILITY in CDB$ROOT ===
begin
  dbms_observability.enable_service;
end;
/

prompt === Switch to &&pdb_name ===
alter session set container=&&pdb_name;

prompt === Create or update application user ===
declare
  l_user varchar2(128) := dbms_assert.simple_sql_name(upper('&&app_user'));
  l_count number;
begin
  select count(*) into l_count from dba_users where username = l_user;
  if l_count = 0 then
    execute immediate 'create user ' || l_user || ' identified by "' || replace('&&app_password', '"', '""') || '"';
  else
    execute immediate 'alter user ' || l_user || ' identified by "' || replace('&&app_password', '"', '""') || '" account unlock';
  end if;
end;
/

grant create session to &&app_user;
grant create table to &&app_user;
grant create sequence to &&app_user;
grant alter session to &&app_user;
alter user &&app_user quota unlimited on users;
grant execute on sys.dbms_observability to &&app_user;
-- Demo diagnostics for /trace/agent-task. Use narrower reviewed privileges in
-- shared environments.
grant execute on sys.dbms_sql_monitor to &&app_user;
grant execute on sys.dbms_xplan to &&app_user;
grant execute on sys.utl_http to &&app_user;
grant select_catalog_role to &&app_user;

prompt === Enable DBMS_OBSERVABILITY in &&pdb_name ===
begin
  dbms_observability.enable_service;
  dbms_observability.enable_service_option(dbms_observability.show_extra_metadata);
end;
/

prompt === Add and enable OTLP trace endpoint ===
declare
begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_traces,
    endpoint => '&&trace_endpoint',
    credential_name => null
  );
exception
  when others then
    if sqlcode != -20000 then
      raise;
    end if;
end;
/

begin
  dbms_observability.enable_endpoint('&&trace_endpoint');
end;
/

prompt === Add and enable OTLP logs endpoint ===
declare
begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_logs,
    endpoint => '&&logs_endpoint',
    credential_name => null
  );
exception
  when others then
    if sqlcode != -20000 then
      raise;
    end if;
end;
/

begin
  dbms_observability.enable_endpoint('&&logs_endpoint');
end;
/

prompt === Configure network ACL for &&app_user ===
declare
begin
  dbms_network_acl_admin.append_host_ace(
    host => '&&collector_host',
    ace => xs$ace_type(
      privilege_list => xs$name_list('connect', 'resolve', 'http', 'http_proxy'),
      principal_name => upper('&&app_user'),
      principal_type => xs_acl.ptype_db
    )
  );
exception
  when others then
    if sqlcode not in (-24243, -46385) then
      raise;
    end if;
end;
/

prompt === Verification ===
select dbms_observability.show_endpoints from dual;
select dbms_observability.show_service_status from dual;

prompt Completed local Oracle Free observability setup.
