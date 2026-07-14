# Oracle AI Database Deep Data Security Provider-Version Demo

This version demonstrates the `ojdbc-provider-spring` SPI approach for Oracle AI Database Deep Data Security.

The app is a Spring Boot OAuth2 resource server. Protected endpoints require an incoming `Authorization: Bearer <access-token>` request. Spring Security validates that token and stores it in the request security context. Oracle JDBC then uses `ojdbc-provider-spring` to construct the `EndUserSecurityContext` from:

- the incoming end-user access token from Spring Security
- a database-access token obtained through the configured Spring OAuth2 client registration

The application code does not call `OracleConnection.setEndUserSecurityContext(...)` directly.
Its public query endpoint intentionally matches the API-version demo:
`GET /deepsec/query`. The difference is inside the controller/service plumbing:
the provider version does not receive `OAuth2AuthorizedClient`; the JDBC provider
reads the validated Spring Security context through SPI.

## Run

1. Copy `.env_example` to `.env` and fill in the database and Microsoft Entra ID values.
   For OCI IAM, copy `.env_oci_iam_example` to a private env file and set
   `DEEPSEC_ENV_FILE` when running `build_and_run.sh`.
2. Build:
   ```bash
   ./build.sh
   ```
3. Run:
   ```bash
   ./run.sh
   ```
4. Check the public endpoints:
   ```bash
   curl http://localhost:8080/deepsec/health
   curl http://localhost:8080/deepsec/policies
   ```
5. Call protected endpoints with an Entra access token:
   ```bash
   curl -H "Authorization: Bearer ${ACCESS_TOKEN}" http://localhost:8080/deepsec/query
   curl -H "Authorization: Bearer ${ACCESS_TOKEN}" http://localhost:8080/deepsec/whoami
   ```

## Identity Provider Profiles

The provider demo keeps one Spring Boot application and separates identity
provider settings with Spring profiles:

- `entraid`: Microsoft Entra ID. This is the default in `run.sh` and
  `build_and_run.sh`, and it supports the checked-in browser UI because the page
  uses MSAL.
- `oci-iam`: OCI IAM / OCI Identity Domains. This shares the same controller,
  UCP datasource, SQL query path, and `ojdbc-provider-spring` provider logic, but
  uses an `oci-iam` OAuth2 client registration and OCI IAM trusted JWT issuer
  settings. The current browser page is Entra-specific, so test OCI IAM with a
  bearer token and curl unless you add an OCI IAM browser login page.

The profile files are:

- `src/main/resources/application.yaml`: shared datasource, UCP, SQL, and JDBC
  provider settings.
- `src/main/resources/application-entraid.yaml`: Entra OAuth2 client,
  trusted issuers, and browser sign-in settings.
- `src/main/resources/application-oci-iam.yaml`: OCI IAM OAuth2 client,
  trusted issuer, and backend-only browser config.

Run with Entra:

```bash
SPRING_PROFILES_ACTIVE=entraid ./run.sh
```

Run with OCI IAM:

```bash
DEEPSEC_ENV_FILE=.env_oci_iam ./build_and_run.sh
```

Where `.env_oci_iam` is a private copy of `.env_oci_iam_example`.

## Important Configuration

The app uses Oracle UCP for pooling so the two Deep Data Security demos use the
same Oracle-aware pool.

The provider is enabled in `src/main/resources/application.yaml` and applied as
UCP connection properties in `UcpDataSourceConfiguration`:

```yaml
deepsec:
  ucp:
    max-pool-size: ${DEEPSEC_POOL_SIZE:4}
  jdbc-provider:
    end-user-security-context: ojdbc-provider-spring-end-user-security-context
    registration-id: entra # or oci-iam through SPRING_PROFILES_ACTIVE=oci-iam
```

The `registrationId` must match the OAuth2 client registration that can obtain a
database-access token for Oracle Database. The profile overlays set this to
`entra` or `oci-iam`.

JWT issuer validation is also profile-driven through:

```yaml
deepsec:
  jwt:
    trusted-issuers: ...
```

This is deliberately not hardcoded to Entra so the same provider app can validate
OCI IAM bearer tokens.

`com.oracle.database.jdbc:ojdbc-provider-spring:1.1.0` is available from Maven Central, so the local build and run scripts resolve it directly through Maven. No manual `ojdbc-extensions` clone or local Maven install step is required.

## Database Setup

For Entra ID, use:

- `sql-entraid/00_create_hr_sample_objects.sql`
- `sql-entraid/01_enable_adb_entra_external_authentication.sql` or
  `sql-entraid/01_enable_database_free_entra_external_authentication.sql`
- `sql-entraid/setup_entraid_dds_demo.sql`

For OCI IAM, follow the OCI IAM LiveLabs workshop and the notes in:

- `sql-oci-iam/README.md`

The Deep Data Security grants themselves are mostly identity-provider agnostic.
The part most likely to change is the `CREATE DATA ROLE ... MAPPED TO ...`
clause if OCI IAM emits different role claim names than the Entra demo.

For the explicit API-call version, see `../deepdatasecurity-api-version`.
