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

There is deliberately no partial `application.yaml`. Each profile is complete so
the configuration for one deployment can be read in one place:

- `src/main/resources/application-entraid.yaml`: complete datasource, UCP,
  JDBC provider, Entra OAuth2 client, trusted issuers, SQL, and browser settings.
- `src/main/resources/application-oci-iam.yaml`: complete datasource, UCP,
  JDBC provider, OCI IAM OAuth2 client, trusted issuer, SQL, and browser settings.

Always activate exactly one of these profiles when starting the application.

Run with Entra:

```bash
SPRING_PROFILES_ACTIVE=entraid ./run.sh
```

Run with OCI IAM:

```bash
DEEPSEC_ENV_FILE=.env_oci_iam ./build_and_run.sh
```

Where `.env_oci_iam` is a private copy of `.env_oci_iam_example`.

## Required and Optional Configuration

The settings fall into three groups.

### Required for the JDBC Deep Data Security provider

- A working Oracle JDBC URL and the database pool user's username and password.
- `oracle.jdbc.provider.endUserSecurityContext` set to
  `ojdbc-provider-spring-end-user-security-context`. This loads the provider.
- `oracle.jdbc.provider.endUserSecurityContext.registrationId`. Its value must
  exactly match a Spring OAuth2 client registration ID, `entra` or `oci-iam` in
  this example.
- The OAuth2 client's `client-id`, `client-secret`, `token-uri`, database scope,
  and one selected database-token grant type. These allow the provider to obtain
  the database-access token.
- An authenticated incoming request whose bearer token is present in Spring
  Security's `SecurityContextHolder`. This is the end-user token propagated to
  Oracle AI Database.
- Matching database-side IAM trust, application identity or mapped data roles,
  pool-user privileges, and data grants.

This application sets the JDBC provider properties on Oracle UCP in
`UcpDataSourceConfiguration`:

```yaml
deepsec:
  ucp:
    max-pool-size: ${DEEPSEC_POOL_SIZE:4}
  jdbc-provider:
    end-user-security-context: ojdbc-provider-spring-end-user-security-context
    registration-id: entra # or oci-iam through SPRING_PROFILES_ACTIVE=oci-iam
```

### Required by this HTTP demo, but not by the JDBC provider itself

- `deepsec.jwt.trusted-issuers` is used by the Spring resource server to validate
  incoming bearer tokens. A different application could configure Spring
  Security with a fixed issuer/JWK set instead.
- `authorization-uri`, a browser client ID, browser scope, and redirect URI are
  needed only for an interactive browser authorization-code/PKCE login. The
  JDBC provider's client-credentials or OBO token request itself uses the token
  endpoint and does not redirect a browser.

### Optional

- `client-name` is only a descriptive Spring registration label.
- UCP pool name and size settings are operational tuning.
- `deepsec.sql`, `session-init-sql`, browser UI settings, and logging levels are
  demo or operational settings.
- Fixed `data-roles` and `end-user-context-attributes` are optional
  application-managed Deep Sec inputs. IAM-mapped data roles do not need to be
  repeated here.

### Database-access token grant type

Select one grant type; they are alternatives, not two stages of one flow:

- `client_credentials` is the default in both profile files. The database-access
  token represents the confidential application. The separate incoming bearer
  token still supplies the end-user identity and role claims.
- `urn:ietf:params:oauth:grant-type:jwt-bearer` selects OBO. The application
  exchanges the incoming end-user assertion for a database-scoped token, and the
  IAM registration must be configured to permit that delegation.

Set `DEEPSEC_ENTRA_DATABASE_GRANT_TYPE` or
`DEEPSEC_OCI_IAM_DATABASE_GRANT_TYPE` to choose. Do not add an
`authorization_code` grant to the JDBC provider registration merely to support
the browser; the browser and database-token concerns are separate.

JWT issuer validation for this resource-server demo is profile-driven through:

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
