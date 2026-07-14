-- Enable Oracle Database server-side OpenTelemetry trace export for a demo.
--
-- Edit the values below before running. The database must be able to reach the
-- OTLP endpoint. For Autonomous Database, that generally means a reachable HTTPS
-- collector endpoint rather than localhost.

define otlp_traces_endpoint = 'https://oracledev.ai/financial/otel/v1/traces'

set serveroutput on

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

-- Registering is not enough; enable the endpoint and service explicitly.
begin
  dbms_observability.enable_endpoint('&otlp_traces_endpoint');
  dbms_observability.enable_service_option(dbms_observability.show_extra_metadata);
  dbms_observability.enable_service(dbms_observability.all_services);
end;
/

-- Enable server-side tracing for the current session. The attached 23.9 notes
-- describe SQL_TRACE as the session switch for server-side trace export.
alter session set sql_trace = true;

declare
  config clob;
begin
  select dbms_observability.show_service_status
    into config;
  dbms_output.put_line(config);
end;
/
