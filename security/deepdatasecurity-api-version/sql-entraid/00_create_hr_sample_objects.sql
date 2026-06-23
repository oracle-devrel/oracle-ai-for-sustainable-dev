-- Compact HR sample objects for the Oracle AI Database Deep Data Security
-- Entra ID demo.
--
-- Run this as ADMIN, or another user that can create the HR schema and
-- create objects in it, before setup_entraid_dds_demo.sql.
--
-- This file replaces the workshop/FastLab sample-data dependency for the
-- Java/Spring Boot demo. It creates only the small HR.EMPLOYEES shape used by
-- the app default query and DDS policies:
--
--   employee_id, first_name, last_name, user_name, manager_id,
--   ssn, salary, department_id, phone_number
--
-- If HR.EMPLOYEES already exists, this script leaves it in place. That lets
-- you use a richer existing HR sample schema without losing data.

SET SERVEROUTPUT ON

PROMPT Creating HR schema if needed...
DECLARE
  l_user_count NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO l_user_count
    FROM all_users
   WHERE username = 'HR';

  IF l_user_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE USER hr IDENTIFIED BY "Welcome_12345#"';
    DBMS_OUTPUT.PUT_LINE('Created demo HR user. Change or lock this password if it is not needed.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('HR user already exists; password was not changed.');
  END IF;
END;
/

PROMPT Granting HR privileges needed for the demo objects...
GRANT CREATE SESSION, CREATE TABLE, CREATE PROCEDURE TO HR;

DECLARE
  l_default_tablespace VARCHAR2(128);
BEGIN
  SELECT default_tablespace
    INTO l_default_tablespace
    FROM dba_users
   WHERE username = 'HR';

  EXECUTE IMMEDIATE
    'ALTER USER hr QUOTA UNLIMITED ON ' ||
    DBMS_ASSERT.SIMPLE_SQL_NAME(l_default_tablespace);
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Could not grant HR tablespace quota automatically: ' || SQLERRM);
    DBMS_OUTPUT.PUT_LINE('If table creation fails, grant HR quota on its default tablespace and rerun this script.');
END;
/

PROMPT Creating compact HR.EMPLOYEES table and sample rows if needed...
DECLARE
  l_table_count NUMBER;
BEGIN
  SELECT COUNT(*)
    INTO l_table_count
    FROM all_tables
   WHERE owner = 'HR'
     AND table_name = 'EMPLOYEES';

  IF l_table_count = 0 THEN
    EXECUTE IMMEDIATE q'[
      CREATE TABLE hr.employees (
        employee_id   NUMBER(10)    CONSTRAINT employees_pk PRIMARY KEY,
        first_name    VARCHAR2(50)  NOT NULL,
        last_name     VARCHAR2(50)  NOT NULL,
        user_name     VARCHAR2(128) NOT NULL,
        manager_id    NUMBER(10),
        ssn           VARCHAR2(11),
        salary        NUMBER(10,2),
        department_id NUMBER(10),
        phone_number  VARCHAR2(30)
      )
    ]';

    EXECUTE IMMEDIATE q'[
      ALTER TABLE hr.employees ADD CONSTRAINT employees_manager_fk
      FOREIGN KEY (manager_id) REFERENCES hr.employees(employee_id)
    ]';

    EXECUTE IMMEDIATE q'[
      CREATE UNIQUE INDEX hr.employees_user_name_uk ON hr.employees(user_name)
    ]';

    EXECUTE IMMEDIATE q'[
      CREATE INDEX hr.employees_manager_ix ON hr.employees(manager_id)
    ]';

    EXECUTE IMMEDIATE q'[
      INSERT INTO hr.employees
        (employee_id, first_name, last_name, user_name, manager_id, ssn, salary, department_id, phone_number)
      VALUES
        (100, 'Maya', 'Manager', 'maya.manager@example.com', NULL, '000-00-0100', 175000, 90, '555.0100')
    ]';

    EXECUTE IMMEDIATE q'[
      INSERT INTO hr.employees
        (employee_id, first_name, last_name, user_name, manager_id, ssn, salary, department_id, phone_number)
      VALUES
        (101, 'Avery', 'Stone', 'avery.stone@example.com', 100, '000-00-0101', 125000, 60, '555.0101')
    ]';

    EXECUTE IMMEDIATE q'[
      INSERT INTO hr.employees
        (employee_id, first_name, last_name, user_name, manager_id, ssn, salary, department_id, phone_number)
      VALUES
        (102, 'Riley', 'Chen', 'riley.chen@example.com', 100, '000-00-0102', 118000, 60, '555.0102')
    ]';

    EXECUTE IMMEDIATE q'[
      INSERT INTO hr.employees
        (employee_id, first_name, last_name, user_name, manager_id, ssn, salary, department_id, phone_number)
      VALUES
        (103, 'Jordan', 'Patel', 'jordan.patel@example.com', 100, '000-00-0103', 116000, 60, '555.0103')
    ]';

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Created HR.EMPLOYEES with compact demo rows.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('HR.EMPLOYEES already exists; leaving existing table and rows unchanged.');
  END IF;
END;
/

PROMPT Verifying HR.EMPLOYEES shape...
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'HR'
   AND table_name = 'EMPLOYEES'
   AND column_name IN (
     'EMPLOYEE_ID', 'FIRST_NAME', 'LAST_NAME', 'USER_NAME', 'MANAGER_ID',
     'SSN', 'SALARY', 'DEPARTMENT_ID', 'PHONE_NUMBER'
   )
 ORDER BY column_id;

PROMPT Sample rows...
SELECT employee_id, first_name, last_name, user_name, manager_id
  FROM hr.employees
 ORDER BY employee_id;
