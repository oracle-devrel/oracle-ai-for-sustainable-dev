-- Local Deep Data Security setup for the observability demo.
--
-- Run as SYS, ADMIN, or another user that can create Deep Data Security data
-- roles and data grants in the target PDB.
--
-- This deliberately does not require Entra ID, OCI IAM, or an application-side
-- EndUserSecurityContext. It creates a password-authenticated local END USER,
-- grants a local DATA ROLE to that end user, and protects the demo table with a
-- DATA GRANT. The Spring Boot app connects directly as this end user.

set echo on
set feedback on
set verify off
set serveroutput on size unlimited
set pages 200
set lines 260
whenever sqlerror exit sql.sqlcode rollback

accept pdb_name char default 'FREEPDB1' prompt 'PDB name [FREEPDB1]: '
accept app_schema char default 'FINANCIAL' prompt 'Application schema [FINANCIAL]: '
accept agent_id char default 'claims-investigator-agent' prompt 'Agent id [claims-investigator-agent]: '
accept agent_password char hide prompt 'Local Deep Sec end-user password: '
accept collector_host char default 'otel-tls-proxy' prompt 'Collector HTTPS proxy host [otel-tls-proxy]: '

alter session set container=&&pdb_name;

prompt === Verify the schema-owned demo table exists ===
declare
  l_count number;
begin
  select count(*)
    into l_count
    from dba_tables
   where owner = upper('&&app_schema')
     and table_name = 'AGENT_EVENT_LOG';
  if l_count = 0 then
    raise_application_error(
      -20001,
      'Run setup_local_free_observability.sql first; &&app_schema..AGENT_EVENT_LOG does not exist.');
  end if;
end;
/

prompt === Create the password-authenticated local Deep Sec end user ===
CREATE END USER IF NOT EXISTS "&&agent_id"
  IDENTIFIED BY "&&agent_password"
  SCHEMA &&app_schema;

prompt === Create local DDS data role ===
CREATE DATA ROLE IF NOT EXISTS agent_claims_investigator;

prompt === Create the standard database role carried by the data role ===
declare
  l_count number;
begin
  select count(*) into l_count from dba_roles
   where role = 'AGENT_OBSERVABILITY_SESSION_ROLE';
  if l_count = 0 then
    execute immediate 'create role agent_observability_session_role';
  end if;
end;
/

grant create session to agent_observability_session_role;
grant execute on sys.dbms_observability to agent_observability_session_role;
grant execute on sys.dbms_sql_monitor to agent_observability_session_role;
grant execute on sys.dbms_xplan to agent_observability_session_role;
grant select_catalog_role to agent_observability_session_role;

prompt === Permit server-side OTLP export for the active role ===
begin
  dbms_network_acl_admin.append_host_ace(
    host => '&&collector_host',
    ace => xs$ace_type(
      privilege_list => xs$name_list('connect', 'resolve', 'http', 'http_proxy'),
      principal_name => 'AGENT_OBSERVABILITY_SESSION_ROLE',
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

grant agent_observability_session_role to agent_claims_investigator;
GRANT DATA ROLE agent_claims_investigator TO "&&agent_id";

prompt === Create DDS data grant on the agent event log ===
CREATE OR REPLACE DATA GRANT &&app_schema..AGENT_EVENT_LOG_CLAIMS_ACCESS
  AS SELECT
  ON &&app_schema..agent_event_log
  WHERE agent_id = '&&agent_id'
  TO agent_claims_investigator;

prompt === Verify configured DDS data role ===
SELECT data_role, mapped_to, enabled_by_default
  FROM dba_data_roles
 WHERE data_role = 'AGENT_CLAIMS_INVESTIGATOR';

prompt === Verify local end user and assigned data role ===
SELECT username, schema, authentication_type, account_status
  FROM dba_end_users
 WHERE username = '&&agent_id';

SELECT data_role, grantee, grantee_type
  FROM dba_data_role_grants
 WHERE data_role = 'AGENT_CLAIMS_INVESTIGATOR'
 ORDER BY grantee;

prompt === Verify configured DDS data grant ===
SELECT grant_name, object_owner, object_name, privilege, grantee, predicate
  FROM dba_data_grants
 WHERE grantee = 'AGENT_CLAIMS_INVESTIGATOR'
 ORDER BY grant_name, privilege;

prompt Completed local DDS direct-login setup for observability.
