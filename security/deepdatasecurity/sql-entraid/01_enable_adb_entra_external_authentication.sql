-- Microsoft Entra ID external authentication for Autonomous Database.
--
-- This is optional only in the sense that not every demo runs on ADB. If you
-- are using Autonomous Database, this is a required database-admin prerequisite
-- before Oracle can validate the Entra database-access token used by DDS.
--
-- Run once as ADMIN after editing the DEFINE values below.

SET DEFINE ON
SET SERVEROUTPUT ON

DEFINE entra_tenant_id = your-tenant-id
DEFINE db_app_client_id = your-database-api-app-client-id
DEFINE db_app_id_uri = api://your-database-api-app-client-id

BEGIN
  DBMS_CLOUD_ADMIN.ENABLE_EXTERNAL_AUTHENTICATION(
    type   => 'AZURE_AD',
    params => JSON_OBJECT(
      'tenant_id'          VALUE '&&entra_tenant_id',
      'application_id'     VALUE '&&db_app_client_id',
      'application_id_uri' VALUE '&&db_app_id_uri'
    ),
    force  => TRUE
  );
END;
/

SELECT name, value
  FROM v$parameter
 WHERE name IN ('identity_provider_type', 'identity_provider_config');
