CREATE USER aiuser identified BY [Yourpassword];
grant CREATE session TO aiuser;
grant RESOURCE TO aiuser;
grant unlimited tablespace TO aiuser;
grant execute on DBMS_CLOUD to aiuser;

