-- Run as ADMIN or another user with privilege to grant SYS package access.
-- The Spring Boot demo connects as this principal.

define principal_name = 'FINANCIAL'

grant create session to &principal_name;
grant create table to &principal_name;
grant create sequence to &principal_name;
grant alter session to &principal_name;
grant execute on sys.dbms_observability to &principal_name;
-- Demo diagnostics for /trace/agent-task. Use narrower reviewed privileges in
-- shared environments.
grant execute on sys.dbms_sql_monitor to &principal_name;
grant execute on sys.dbms_xplan to &principal_name;
grant execute on sys.utl_http to &principal_name;
grant select_catalog_role to &principal_name;
