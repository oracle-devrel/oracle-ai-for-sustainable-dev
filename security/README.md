# Security Demos

## Oracle Deep Data Security

The Deep Data Security demo is split into two Spring Boot versions:

- `deepdatasecurity-api-version`: explicit JDBC API implementation that creates an `EndUserSecurityContext` and calls `OracleConnection.setEndUserSecurityContext(...)`.
- `deepdatasecurity-provider-version`: provider/SPI implementation that uses `ojdbc-provider-spring` with Spring Security OAuth2 resource-server support.

Use the API version when you want to show every token and JDBC call in application code. Use the provider version when you want to show the configuration-driven approach for a Spring app whose protected endpoints require OAuth 2.0 bearer tokens.

## Provider Version Quick Start

The provider version now includes a small same-origin browser UI. The page signs in with Microsoft Entra ID, obtains an access token, and calls the protected Spring Boot endpoints with `Authorization: Bearer ...`. The backend remains a resource server, and `ojdbc-provider-spring` reads the authenticated end-user token from Spring Security when JDBC checks out a connection.

1. Create a local env file from the template:

   ```bash
   cd security/deepdatasecurity-provider-version
   cp .env_example .env
   ```

2. Fill in `.env` with your local wallet, database, and Entra values. Do not commit `.env`; it is for local secrets only.

3. In the Entra app registration used by `DEEPSEC_ENTRA_BROWSER_CLIENT_ID`, add this SPA redirect URI for local testing:

   ```text
   http://localhost:18080
   ```

4. Build and start the app:

   ```bash
   ./build_and_run.sh
   ```

5. Open the browser UI:

   ```text
   http://localhost:18080/
   ```

6. Click `Sign in`, then `Run query`. A successful run returns the signed-in username and the Deep Data Security-filtered row set.

The app also supports curl/Postman style testing. Use the optional bearer-token commands printed by `build_and_run.sh`; unauthenticated calls to `/deepsec/query` should return `401`.

## Configuration Notes

- `.env_example` contains placeholders only. Real database passwords, wallet paths, client secrets, and tokens belong in local `.env` files or another secret store.
- `DEEPSEC_ENTRA_APPLICATION_SCOPE` is the user-facing API scope used by the browser UI, for example `api://your-app-client-id/user_impersonation`.
- `DEEPSEC_ENTRA_DATABASE_SCOPE` is the database-access token scope used by the provider registration, typically `api://your-database-app-client-id/.default`.
- The provider version accepts both common Entra issuer forms for the configured tenant because some app registrations issue v1 access tokens while newer flows may issue v2 tokens.
- `DEEPSEC_SESSION_INIT_SQL` defaults to `alter session disable parallel query` to avoid a demo database context handler issue with parallel query. Set it to an empty value if your database policy package works under parallel execution.
