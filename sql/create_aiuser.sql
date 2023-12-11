--run as admin

CREATE USER aiuser identified BY [Yourpassword];
grant CREATE session TO aiuser;
grant RESOURCE, db_developer_role TO aiuser;
grant unlimited tablespace TO aiuser;
grant EXECUTE ON javascript TO aiuser;
grant EXECUTE dynamic mle TO aiuser;
grant execute on DBMS_CLOUD to aiuser;
