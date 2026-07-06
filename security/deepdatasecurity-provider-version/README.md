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

## Important Configuration

The app uses Oracle UCP for pooling. HikariCP is excluded from the JDBC starter
so the two Deep Data Security demos use the same Oracle-aware pool.

The provider is enabled in `src/main/resources/application.yaml` and applied as
UCP connection properties in `UcpDataSourceConfiguration`:

```yaml
deepsec:
  ucp:
    max-pool-size: ${DEEPSEC_POOL_SIZE:4}
  jdbc-provider:
    end-user-security-context: ojdbc-provider-spring-end-user-security-context
    registration-id: entra
```

The `registrationId` must match the OAuth2 client registration that can obtain a database-access token for Oracle Database.

`com.oracle.database.jdbc:ojdbc-provider-spring:1.1.0` is available from Maven Central, so the local build and run scripts resolve it directly through Maven. No manual `ojdbc-extensions` clone or local Maven install step is required.

For the explicit API-call version, see `../deepdatasecurity-api-version`.
