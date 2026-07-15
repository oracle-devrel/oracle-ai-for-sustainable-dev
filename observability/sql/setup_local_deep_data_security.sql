-- Local Deep Data Security setup for the observability demo.
--
-- Run as SYS, ADMIN, or another user that can create Deep Data Security data
-- roles and data grants in the target PDB.
--
-- This deliberately does not require Entra ID or OCI IAM. It creates a local
-- DDS data role and a data grant over the demo table so the observability page
-- can show the difference between regular database roles and DDS data roles.

set echo on
set feedback on
set verify off
set serveroutput on size unlimited
set pages 200
set lines 260

accept pdb_name char default 'FREEPDB1' prompt 'PDB name [FREEPDB1]: '
accept app_schema char default 'FINANCIAL' prompt 'Application schema [FINANCIAL]: '
accept agent_id char default 'claims-investigator-agent' prompt 'Agent id [claims-investigator-agent]: '

alter session set container=&&pdb_name;

prompt === Create local DDS data role ===
CREATE OR REPLACE DATA ROLE agent_claims_investigator;

prompt === Create DDS data grant on the agent event log ===
CREATE OR REPLACE DATA GRANT &&app_schema..AGENT_EVENT_LOG_CLAIMS_ACCESS
  AS SELECT
  ON &&app_schema..agent_event_log
  WHERE agent_id = '&&agent_id'
  TO agent_claims_investigator;

prompt === Verify configured DDS data role ===
SELECT data_role, mapped_to, enabled_by_default
  FROM dba_data_roles
 WHERE data_role = 'AGENT_CLAIMS_INVESTIGATOR';

prompt === Verify configured DDS data grant ===
SELECT grant_name, object_owner, object_name, privilege, grantee, predicate
  FROM dba_data_grants
 WHERE grantee = 'AGENT_CLAIMS_INVESTIGATOR'
 ORDER BY grant_name, privilege;

prompt Completed local DDS setup for observability.
