-- Microsoft Entra ID external authentication for Oracle AI Database /
-- Oracle Database Free running in a local container or other non-ADB install.
--
-- This is optional only in the sense that not every demo runs on Database Free.
-- If you are using Database Free, this is a required database-admin
-- prerequisite before Oracle can validate the Entra database-access token used
-- by DDS.
--
-- Run once as SYSDBA or another user that can alter system parameters. If you
-- connect directly to the target PDB, comment out ALTER SESSION SET CONTAINER.
--
-- Oracle docs:
-- - IDENTITY_PROVIDER_TYPE enables AZURE_AD as the external identity provider.
-- - IDENTITY_PROVIDER_CONFIG supplies the database/API app registration values.
-- - For non-ADB databases, application_id_uri is documented as a
--   domain-qualified https:// URI. Use the exact Application ID URI from the
--   database/API app registration, and keep DEEPSEC_ENTRA_DATABASE_SCOPE and
--   tnsnames.ora azure_db_app_id_uri aligned to that same URI.

SET DEFINE ON

DEFINE pdb_name = FREEPDB1
DEFINE entra_tenant_id = your-tenant-id
DEFINE db_app_client_id = your-database-api-app-client-id
DEFINE db_app_id_uri = https://your-verified-domain.example.com/mydb

ALTER SESSION SET CONTAINER = &&pdb_name;

ALTER SYSTEM SET IDENTITY_PROVIDER_TYPE = AZURE_AD SCOPE=BOTH;

ALTER SYSTEM SET IDENTITY_PROVIDER_CONFIG = '{
  "application_id_uri": "&&db_app_id_uri",
  "tenant_id": "&&entra_tenant_id",
  "app_id": "&&db_app_client_id"
}' SCOPE=BOTH;

SELECT name, value
  FROM v$parameter
 WHERE name IN ('identity_provider_type', 'identity_provider_config');
