-- Connect as admin and run this sql script to create the application users
-- Replace replace_with_strong_password with a strong password for each user

create user orderuser identified by replace_with_strong_password;
grant
   unlimited tablespace
to orderuser;
grant connect,resource to orderuser;
grant aq_user_role to orderuser;
grant execute on sys.dbms_aq to orderuser;
-- For inventory-plsql deployment
grant
   create job
to orderuser;
grant execute on sys.dbms_scheduler to orderuser;
WHENEVER SQLERROR CONTINUE
grant
   execute dynamic mle
to orderuser;
grant execute on javascript to orderuser;
WHENEVER SQLERROR EXIT 1


create user inventoryuser identified by replace_with_strong_password;
grant
   unlimited tablespace
to inventoryuser;
grant connect,resource to inventoryuser;
grant aq_user_role to inventoryuser;
grant execute on sys.dbms_aq to inventoryuser;
-- For inventory-plsql deployment
grant
   create job
to inventoryuser;
grant execute on sys.dbms_scheduler to inventoryuser;
WHENEVER SQLERROR CONTINUE
grant
   execute dynamic mle
to inventoryuser;
grant execute on javascript to inventoryuser;
WHENEVER SQLERROR EXIT 1