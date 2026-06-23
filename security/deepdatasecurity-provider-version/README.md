# Oracle AI Database Deep Data Security Provider-Version Demo

This version demonstrates the `ojdbc-provider-spring` SPI approach for Oracle AI Database Deep Data Security.

The app is a Spring Boot OAuth2 resource server. Protected endpoints require an incoming `Authorization: Bearer <access-token>` request. Spring Security validates that token and stores it in the request security context. Oracle JDBC then uses `ojdbc-provider-spring` to construct the `EndUserSecurityContext` from:

- the incoming end-user access token from Spring Security
- a database-access token obtained through the configured Spring OAuth2 client registration

The application code does not call `OracleConnection.setEndUserSecurityContext(...)` directly.

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

The provider is enabled in `src/main/resources/application.yaml` through Hikari data-source properties:

```yaml
oracle.jdbc.provider.endUserSecurityContext: ojdbc-provider-spring-end-user-security-context
oracle.jdbc.provider.endUserSecurityContext.registrationId: entra
```

The `registrationId` must match the OAuth2 client registration that can obtain a database-access token for Oracle Database.

The Oracle documentation currently lists `com.oracle.database.jdbc:ojdbc-provider-spring:1.1.0`, but Maven Central may not have that artifact available yet. The local `build.sh` and `run.sh` scripts call `install-ojdbc-provider-spring.sh`, which installs the provider from Oracle's `ojdbc-extensions` source into your local Maven cache when the artifact is not already resolvable.

For the explicit API-call version, see `../deepdatasecurity-api-version`.
