# OCI IAM Setup Notes

Use this folder for the database-side OCI IAM setup that parallels `sql-entraid/`.

The Spring Boot provider app now supports an `oci-iam` profile, but the exact
database identity-provider setup should follow the OCI IAM LiveLabs workshop:

https://livelabs.oracle.com/ords/r/dbpm/livelabs/run-workshop?p210_wid=4453

## How the App Uses the OCI IAM Setup

After the database and OCI IAM identity-domain application are configured by the
LiveLabs steps, copy `.env_oci_iam_example` to a private env file and fill in:

```bash
SPRING_PROFILES_ACTIVE="oci-iam"
DEEPSEC_OCI_IAM_DOMAIN_URL="https://your-identity-domain.identity.oraclecloud.com"
DEEPSEC_OCI_IAM_CLIENT_ID="your_oci_iam_confidential_app_client_id"
DEEPSEC_OCI_IAM_CLIENT_SECRET="your_oci_iam_confidential_app_client_secret"
DEEPSEC_OCI_IAM_DATABASE_SCOPE="your_database_scope_or_resource/.default"
```

The profile maps those values to:

- Spring Security resource-server JWT validation through `deepsec.jwt.trusted-issuers`
- Spring OAuth2 client registration `oci-iam`
- `ojdbc-provider-spring` registration id `oci-iam`

## Data Grants

The Deep Data Security demo data roles and data grants are identity-provider
agnostic once the incoming token supplies the mapped roles/claims expected by the
database. Start with:

```bash
../sql-entraid/00_create_hr_sample_objects.sql
../sql-entraid/setup_entraid_dds_demo.sql
```

If the OCI IAM workshop uses different role claim names or role mappings, create
an OCI-specific copy of `setup_entraid_dds_demo.sql` here and change only the
`CREATE DATA ROLE ... MAPPED TO ...` clauses.

The rest of the grants, such as `HRAPP_EMPLOYEES_ACCESS` and
`HRAPP_MANAGER_ACCESS`, can remain the same when the data role names are kept as
`HRAPP_EMPLOYEES` and `HRAPP_MANAGERS`.

## Browser UI

The checked-in browser page uses Microsoft MSAL and is therefore Entra-specific.
For OCI IAM, call the protected endpoints with an OCI IAM bearer token:

```bash
curl -H "Authorization: Bearer ${OCI_IAM_ACCESS_TOKEN}" \
  http://localhost:18080/deepsec/whoami

curl -H "Authorization: Bearer ${OCI_IAM_ACCESS_TOKEN}" \
  http://localhost:18080/deepsec/query
```

Add an OCI IAM browser-login page later only if you want an interactive browser
demo equivalent to the current Entra/MSAL page.
