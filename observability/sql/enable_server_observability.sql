-- Enable Oracle Database server-side OpenTelemetry trace export for a demo.
--
-- Edit the values below before running. The database must be able to reach the
-- OTLP endpoint. For Autonomous Database, that generally means a reachable HTTPS
-- collector endpoint rather than localhost.

define otlp_traces_endpoint = 'http://collector.example.com:4318/v1/traces'

set serveroutput on

-- Enable the DBMS_OBSERVABILITY service in the current container.
-- In a CDB, enable at CDB$ROOT first, then in the target PDB.
begin
  dbms_observability.enable_service;
end;
/

-- Add the traces endpoint. If your endpoint requires authentication, create a
-- DBMS_OBSERVABILITY credential and pass its name as credential_name.
begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_traces,
    endpoint => '&otlp_traces_endpoint',
    credential_name => null
  );
end;
/

-- Enable server-side tracing for the current session. The attached 23.9 notes
-- describe SQL_TRACE as the switch for server-side OpenTelemetry trace export.
alter session set sql_trace = true;

declare
  config clob;
begin
  select dbms_observability.show_service_status(dbms_observability.all_info)
    into config;
  dbms_output.put_line(config);
end;
/
