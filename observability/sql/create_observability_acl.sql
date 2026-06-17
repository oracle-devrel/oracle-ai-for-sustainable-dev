-- Optional network ACL for a database user that needs to export telemetry to an
-- OTLP HTTP endpoint. Run as a user with DBMS_NETWORK_ACL_ADMIN privileges.

define otlp_host = 'collector.example.com'
define lower_port = 4318
define upper_port = 4318
define principal_name = 'FINANCIAL'

begin
  dbms_network_acl_admin.append_host_ace(
    host => '&otlp_host',
    lower_port => &lower_port,
    upper_port => &upper_port,
    ace => xs$ace_type(
      privilege_list => xs$name_list('http', 'http_proxy'),
      principal_name => '&principal_name',
      principal_type => xs_acl.ptype_db
    )
  );
end;
/
