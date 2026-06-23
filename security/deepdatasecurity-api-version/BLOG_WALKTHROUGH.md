# Blog Walkthrough: Deep Data Security with Spring Boot and Oracle AI Database 26ai

Use this as source material for a blog, live demo, or recorded walkthrough. It follows the README TL;DR and calls out where Deep Data Security enforces fine-grained declarative SQL policies.

## Storyline

Modern AI and agentic applications often connect to the database through a shared application user. That is convenient for connection pooling, but risky if authorization is enforced only in application code. Oracle Deep Data Security keeps authorization close to the data: the Spring Boot app propagates the end-user identity and context, and Oracle AI Database enforces declarative SQL policies at runtime.

The key message: the app does not hand-code row filtering, column masking, or cell-level rules. It attaches an end-user security context to a JDBC connection, and the database enforces `CREATE DATA GRANT` policies.

## Demo Architecture

- Browser opens `GET /deepsec/query`.
- Spring Security redirects the browser to Microsoft Entra ID.
- The user signs in.
- Entra ID redirects back to Spring Boot with a one-time authorization code.
- Spring Security exchanges that authorization code for the end-user access token.
- The app obtains a separate database-access token with the OAuth 2.0 on-behalf-of flow.
- The app creates an `EndUserSecurityContext` with:
  - database-access token
  - end-user token
- Entra app roles from the token activate the mapped database data roles automatically.
- Optional local DDS data roles or runtime attributes can be attached when explicitly configured.
- The app unwraps the pooled JDBC connection to `OracleConnection`.
- The app calls `setEndUserSecurityContext(...)`.
- Oracle AI Database applies data grants during SQL execution.
- The app calls `clearEndUserSecurityContext()` before the connection returns to the pool.

## What to Capture

1. Repository layout:
   ```bash
   tree -L 4 security/deepdatasecurity-api-version
   ```

2. Configuration:
   ```bash
   sed -n '1,100p' .env_example
   sed -n '1,120p' src/main/resources/application.yaml
   ```

3. UCP and JDBC dependencies:
   ```bash
   sed -n '1,100p' pom.xml
   ```

4. The security context lifecycle:
   ```bash
   sed -n '1,220p' src/main/java/com/oracle/demo/deepsecurity/DeepDataSecurityService.java
   ```

5. The HTTP endpoints:
   ```bash
   sed -n '1,180p' src/main/java/com/oracle/demo/deepsecurity/DeepDataSecurityController.java
   ```

6. Build:
   ```bash
   ./build.sh
   ```

7. Run:
   ```bash
   ./run.sh
   ```

8. Smoke check:
   ```bash
   curl http://localhost:8080/deepsec/health
   ```

9. Policy explainer endpoint:
   ```bash
   curl http://localhost:8080/deepsec/policies
   ```

10. Protected query:
    ```text
    http://localhost:8080/deepsec/query
    ```

## Database Setup Narrative

Before the app can enforce Deep Data Security, configure the database and identity provider.

1. Enable TLS/TCPS between the app and database.
2. Create or choose the app connection-pool user.
3. Grant the pool user:
   ```sql
   GRANT CREATE SESSION TO hr_app_user;
   GRANT CREATE END USER SECURITY CONTEXT TO hr_app_user;
   ```
4. Configure Microsoft Entra ID access:
   - create the database app registration/resource in Entra ID
   - set the database app Application ID URI to `api://<db-app-client-id>`
   - set the database app manifest `accessTokenAcceptedVersion` to `2`
   - create app roles such as `EMPLOYEES` and `MANAGERS`
   - create the Spring Boot app registration/client in Entra ID
   - assign users or groups to the database app roles
   - configure Oracle AI Database to trust the Entra tenant and database app
5. Create data roles and data grants in the database.

## Microsoft Entra ID Token Flow

The app uses two different Entra ID tokens.

- **End-user access token:** issued after the user signs in through authorization-code flow. Spring Security handles the redirect, callback, code exchange, and token storage. The app reads this token from `OAuth2AuthorizedClient`.
- **Database-access token:** issued by exchanging the signed-in user's token through the on-behalf-of grant. `EntraDatabaseAccessTokenService` requests it server-side with this app's client credentials, the user's token as the assertion, and the database/API delegated scope.

For OBO, the browser login token must be addressed to the Spring Boot app. The database/API scope is requested only in the backend OBO exchange. The database/API app should issue v2 access tokens; with v2 tokens Oracle expects the database-access token audience to use the app ID value.

End-user authorization-code flow:

```text
Browser -> /deepsec/query
Spring Security -> redirects to https://login.microsoftonline.com/${DEEPSEC_ENTRA_TENANT_ID}/oauth2/v2.0/authorize
Entra ID -> user signs in
Entra ID -> redirects to /login/oauth2/code/entra?code=...
Spring Security -> POST /oauth2/v2.0/token with grant_type=authorization_code
Entra ID -> returns the user's access_token
```

Database-access token on-behalf-of flow:

```text
EntraDatabaseAccessTokenService
  -> POST https://login.microsoftonline.com/${DEEPSEC_ENTRA_TENANT_ID}/oauth2/v2.0/token
  -> client_id=${DEEPSEC_ENTRA_CLIENT_ID}
  -> client_secret=${DEEPSEC_ENTRA_CLIENT_SECRET}
  -> grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
  -> requested_token_use=on_behalf_of
  -> assertion=<signed-in user's access token>
  -> scope=${DEEPSEC_ENTRA_DATABASE_SCOPE}
Entra ID
  -> returns access_token for the database resource on behalf of the user
```

Both tokens go into the DDS context:

```java
EndUserSecurityContext context =
    EndUserSecurityContext.createWithToken(databaseAccessToken, endUserToken);
```

For Entra-mapped data roles, Oracle AI Database reads the Entra `roles` claim and activates matching mappings such as `HRAPP_EMPLOYEES -> AZURE_ROLE=EMPLOYEES` and `HRAPP_MANAGERS -> AZURE_ROLE=MANAGERS`.

## Fine-Grained Policy Examples

Use these examples in the blog to show row, column, and cell-level declarative policy behavior.

### Row-Level

Employees see only their own record:

```sql
CREATE OR REPLACE DATA GRANT hr.HRAPP_EMPLOYEES_ACCESS
  AS SELECT, UPDATE(phone_number, first_name)
  ON hr.employees
  WHERE upper(user_name) = upper(ORA_END_USER_CONTEXT.username)
  TO HRAPP_EMPLOYEES;
```

### Column-Level

Managers see direct reports, but not SSN values:

```sql
CREATE OR REPLACE DATA GRANT hr.HRAPP_MANAGER_ACCESS
  AS SELECT (ALL COLUMNS EXCEPT ssn), UPDATE (salary, department_id, first_name)
  ON hr.employees
  WHERE manager_id = ORA_END_USER_CONTEXT.HR.EMP_CTX.ID
  TO HRAPP_MANAGERS;
```

### Cell-Level

Employees can update only the `phone_number` column on their own row:

```sql
CREATE OR REPLACE DATA GRANT hr.HRAPP_EMPLOYEES_ACCESS
  AS SELECT, UPDATE (phone_number)
  ON hr.employees
  WHERE upper(user_name) = upper(ORA_END_USER_CONTEXT.username)
  TO HRAPP_EMPLOYEES;
```

## Suggested Blog Outline

1. The problem: AI apps often use powerful shared DB users.
2. The goal: keep authorization in the database, not scattered through app code.
3. The architecture: OAuth token, database-access token, Spring Boot, UCP, Oracle AI Database.
4. The key code: `EndUserSecurityContext`, `OracleConnection`, set, query, clear.
5. The policy model: `CREATE DATA GRANT` for row, column, and cell-level controls.
6. The Entra ID setup: database app registration, app roles, Spring Boot app registration, users/groups.
7. The demo: build, run, `/health`, `/policies`, `/query`.
8. What to productionize: secret storage, token cache tuning, real TLS wallet, CI/CD-managed data grants.

## Production Hardening Notes

- Store the Entra client secret in a secrets manager.
- Add explicit token/claim validation rules for the user token, including issuer, audience, scopes, and expected group claims.
- Manage data grants as versioned database migration scripts.
- Add tests that call the same SQL as different users/roles and assert row, column, and cell-level outcomes.
- Audit data grants and active end-user contexts with Oracle Deep Data Security dictionary views.

## References to Use in the Blog

- Oracle Deep Data Security concepts: https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/understand-oracle-deep-data-security.html
- Fine-grained data authorization: https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/fine-grained-data-authorization.html
- Configure data grants: https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/configure-data-grants.html
- `CREATE DATA GRANT` SQL reference: https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-data-grant.html
- JDBC Deep Data Security support: https://docs.oracle.com/en/database/oracle/oracle-database/26/jjdbc/JDBC-getting-started.html#GUID-10D8EFA5-7AF0-4462-986C-2DF461555D27
- Microsoft Entra ID with Oracle AI Database: https://docs.oracle.com/en/database/oracle/oracle-database/26/dbseg/authenticating-and-authorizing-microsoft-entra-id-ms-ei-users-oracle-databases-oracle-exadata-datab.html
- LiveLabs FastLab, Oracle Deep Data Security and Microsoft Entra ID: https://livelabs.oracle.com/ords/r/dbpm/livelabs/run-workshop?p210_wid=4396
