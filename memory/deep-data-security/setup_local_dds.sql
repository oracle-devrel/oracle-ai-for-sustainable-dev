-- Local Oracle Deep Data Security setup for the memory demonstration.
-- Run as ADMIN after the MAGIC_PY_MEMORY table exists.

set verify off
set serveroutput on
whenever sqlerror exit sql.sqlcode rollback

accept app_schema char default 'FINANCIAL' prompt 'Memory schema [FINANCIAL]: '
accept ava_password char hide prompt 'Ava local end-user password: '
accept leo_password char hide prompt 'Leo local end-user password: '

CREATE END USER IF NOT EXISTS "AVA"
  IDENTIFIED BY "&&ava_password" SCHEMA &&app_schema;
CREATE END USER IF NOT EXISTS "LEO"
  IDENTIFIED BY "&&leo_password" SCHEMA &&app_schema;

CREATE DATA ROLE IF NOT EXISTS memory_trip_organizer;
CREATE DATA ROLE IF NOT EXISTS memory_trip_participant;
CREATE ROLE IF NOT EXISTS memory_dds_session_role;
GRANT CREATE SESSION TO memory_dds_session_role;
GRANT memory_dds_session_role TO memory_trip_organizer;
GRANT memory_dds_session_role TO memory_trip_participant;
GRANT DATA ROLE memory_trip_organizer TO "AVA";
GRANT DATA ROLE memory_trip_participant TO "LEO";

CREATE OR REPLACE DATA GRANT &&app_schema..MEMORY_ORGANIZER_ACCESS
  AS SELECT, UPDATE(METADATA)
  ON &&app_schema..MAGIC_PY_MEMORY
  WHERE UPPER(USER_ID) = UPPER(ORA_END_USER_CONTEXT.username)
     OR (USER_ID IS NULL AND JSON_VALUE(METADATA, '$.kind')
         IN ('experience_trace', 'procedural'))
  TO memory_trip_organizer;

CREATE OR REPLACE DATA GRANT &&app_schema..MEMORY_PARTICIPANT_ACCESS
  AS SELECT (RECORD_ID, ORDER_SEQ, THREAD_ID, USER_ID, AGENT_ID,
             SPACE_ID, MEMORY_TYPE, CONTENT, TIMESTAMP,
             EXPIRES_AT, CREATED_AT)
  ON &&app_schema..MAGIC_PY_MEMORY
  WHERE UPPER(USER_ID) = UPPER(ORA_END_USER_CONTEXT.username)
     OR (USER_ID IS NULL
         AND MEMORY_TYPE = 'guideline'
         AND JSON_VALUE(METADATA, '$.status') = 'approved')
  TO memory_trip_participant;

SELECT data_role, grantee, grantee_type
  FROM dba_data_role_grants
 WHERE data_role IN ('MEMORY_TRIP_ORGANIZER', 'MEMORY_TRIP_PARTICIPANT')
 ORDER BY data_role, grantee;
