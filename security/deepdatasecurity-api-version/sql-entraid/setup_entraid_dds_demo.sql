-- Oracle AI Database Deep Data Security + Microsoft Entra ID demo setup.
--
-- Run this as ADMIN or another database user that can create end-user
-- contexts, packages in HR, database roles, data roles, and data grants.
--
-- Prerequisite HR sample data:
--   Run 00_create_hr_sample_objects.sql first unless you already have a
--   compatible HR.EMPLOYEES table. A fresh Oracle AI Database, Autonomous
--   Database, or Oracle Database Free instance is not guaranteed to include
--   this exact demo table by default.
--
--   At minimum, this Java demo expects:
--
--     hr.employees(
--       employee_id, first_name, last_name, user_name, manager_id,
--       ssn, salary, department_id, phone_number
--     )
--
--   The app's default query reads employee_id, first_name, and last_name.
--   The employee DDS grant uses user_name and phone_number. The manager
--   DDS grant uses manager_id and protects ssn.
--   For rows to be visible, HR.EMPLOYEES.USER_NAME must match the username
--   Oracle receives in the end-user token. The app's diagnostic
--   /deepsec/whoami endpoint shows that exact value.
--
-- Microsoft Entra prerequisites:
--   The database/API app registration has app roles named EMPLOYEES and
--   MANAGERS. Oracle maps those role values from the token roles claim to
--   the DDS data roles below.
--
-- Direct-logon note:
--   Some DDS examples grant CREATE SESSION to a database role and then grant
--   that role to the data role. That is for Entra/IAM users who log directly
--   into the database. This Spring Boot app does not do direct end-user
--   database logon; it connects with the pooled application database user and
--   attaches the end-user security context with JDBC. The application database
--   user separately needs CREATE SESSION and CREATE END USER SECURITY CONTEXT.

SET DEFINE ON
SET SERVEROUTPUT ON

PROMPT Checking expected HR sample objects...
SELECT owner, table_name
  FROM all_tables
 WHERE owner = 'HR'
   AND table_name = 'EMPLOYEES';

PROMPT Creating manager end-user context...
--
-- The employee policy can compare directly to ORA_END_USER_CONTEXT.username,
-- which comes from the Entra token. The manager policy needs the signed-in
-- user's employee_id so it can find direct reports by manager_id.
--
-- HR.EMP_CTX.ID is that computed value. Its onFirstRead handler runs
-- HR.CTX_PKG.INIT_USER_CONTEXT, which looks up hr.employees.user_name and
-- writes the matching employee_id into the end-user context.
--
-- HR needs UPDATE ANY END USER CONTEXT because the package updates the
-- current end-user context value at runtime.
GRANT UPDATE ANY END USER CONTEXT TO HR;

CREATE OR REPLACE END USER CONTEXT HR.EMP_CTX USING JSON SCHEMA '{
  "type": "object",
  "properties": {
    "ID": {
      "type": "integer",
      "o:onFirstRead": "HR.ctx_pkg.init_user_context"
    }
  }
}';

CREATE OR REPLACE PACKAGE hr.ctx_pkg AS
  PROCEDURE init_user_context;
END;
/

CREATE OR REPLACE PACKAGE BODY hr.ctx_pkg AS
  PROCEDURE init_user_context IS
    sql_stmt VARCHAR2(4000);
  BEGIN
    sql_stmt := '
      UPDATE END_USER_CONTEXT t
      SET t.CONTEXT.ID = (
         SELECT e.employee_id
         FROM hr.employees e
         WHERE upper(e.user_name) = upper(ORA_END_USER_CONTEXT.username)
       )
      WHERE owner = ''HR''
      AND name = ''EMP_CTX'';
    ';
    EXECUTE IMMEDIATE sql_stmt;
  END;
END;
/

PROMPT Creating Entra-mapped DDS data roles...
CREATE OR REPLACE DATA ROLE hrapp_employees MAPPED TO 'AZURE_ROLE=EMPLOYEES';
CREATE OR REPLACE DATA ROLE hrapp_managers  MAPPED TO 'AZURE_ROLE=MANAGERS';

PROMPT Granting context helper access to DDS data roles...
--
-- The data roles need EXECUTE on HR.CTX_PKG because HR.EMP_CTX.ID is filled
-- by an onFirstRead package call while the end-user context is active.
CREATE ROLE IF NOT EXISTS employee_context_admin;
GRANT EXECUTE ON hr.ctx_pkg TO employee_context_admin;
GRANT employee_context_admin TO hrapp_employees;
GRANT employee_context_admin TO hrapp_managers;

--
-- The data roles also need SELECT on the HR.EMP_CTX row in SYS.END_USER_CONTEXT
-- because the manager predicate reads ORA_END_USER_CONTEXT.HR.EMP_CTX.ID.
CREATE OR REPLACE DATA GRANT hr.EMPLOYEE_CONTEXT_GRANT
  AS SELECT
  ON sys.end_user_context
  WHERE owner = 'HR' AND name = 'EMP_CTX'
  TO hrapp_employees, hrapp_managers;

PROMPT Creating DDS data grants on HR.EMPLOYEES...
--
-- Employee role: see/update only the row whose user_name matches the
-- username claim Oracle received in the end-user token.
CREATE OR REPLACE DATA GRANT hr.HRAPP_EMPLOYEES_ACCESS
  AS SELECT, UPDATE(phone_number, first_name)
  ON hr.employees
  WHERE upper(user_name) = upper(ORA_END_USER_CONTEXT.username)
  TO hrapp_employees;

--
-- Manager role: see direct reports by manager_id, excluding SSN, and allow
-- updates only to the listed columns.
CREATE OR REPLACE DATA GRANT hr.HRAPP_MANAGER_ACCESS
  AS SELECT (ALL COLUMNS EXCEPT ssn), UPDATE (salary, department_id, first_name)
  ON hr.employees
  WHERE manager_id = ORA_END_USER_CONTEXT.HR.EMP_CTX.ID
  TO hrapp_managers;

PROMPT Verifying Entra data role mappings...
SELECT data_role, mapped_to, enabled_by_default
  FROM dba_data_roles
 WHERE data_role IN ('HRAPP_EMPLOYEES', 'HRAPP_MANAGERS')
 ORDER BY data_role;

PROMPT Verifying HR.EMPLOYEES data grants...
SELECT grant_name, privilege, grantee, predicate
  FROM dba_data_grants
 WHERE object_owner = 'HR'
   AND object_name = 'EMPLOYEES'
 ORDER BY grant_name, privilege, grantee;
