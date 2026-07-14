set serveroutput on

declare
  config clob;
  endpoints clob;
begin
  select dbms_observability.show_service_status
    into config;
  dbms_output.put_line('Service status:');
  dbms_output.put_line(config);

  select dbms_observability.show_endpoints
    into endpoints;
  dbms_output.put_line('Endpoints:');
  dbms_output.put_line(endpoints);
end;
/
