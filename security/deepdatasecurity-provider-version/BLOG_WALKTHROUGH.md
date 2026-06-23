# Blog Walkthrough: Deep Data Security with ojdbc-provider-spring

## Storyline

Modern AI and agentic applications often connect to the database through a shared application user. That is convenient for connection pooling, but risky if authorization is enforced only in application code. Oracle Deep Data Security keeps authorization close to the data: the Spring Boot app propagates the end-user identity and context, and Oracle AI Database enforces declarative SQL policies at runtime.

The key message for this version: the app does not call the Deep Data Security JDBC API directly. It exposes protected HTTP endpoints, Spring Security validates an OAuth 2.0 bearer token, and `ojdbc-provider-spring` propagates the end-user security context through the JDBC provider SPI.

## Demo Architecture

- Client calls `GET /deepsec/query` with `Authorization: Bearer <end-user-access-token>`.
- Spring Security resource-server support validates the token.
- The Oracle JDBC Spring provider reads the authenticated token from Spring Security.
- The provider obtains a separate database-access token through the configured OAuth2 client registration.
- The provider supplies an `EndUserSecurityContext` to Oracle JDBC.
- Oracle AI Database applies data grants during SQL execution.
- The application service code just borrows a JDBC connection and runs SQL.

## What to Capture

1. Repository layout:
   ```bash
   tree -L 4 security/deepdatasecurity-provider-version
   ```

2. Configuration:
   ```bash
   sed -n '1,100p' .env_example
   sed -n '1,120p' src/main/resources/application.yaml
   ```

3. JDBC provider dependencies:
   ```bash
   sed -n '1,100p' pom.xml
   ```

4. The provider-driven query code:
   ```bash
   sed -n '1,180p' src/main/java/com/oracle/demo/deepsecurity/DeepDataSecurityService.java
   ```

5. The HTTP endpoints:
   ```bash
   sed -n '1,180p' src/main/java/com/oracle/demo/deepsecurity/DeepDataSecurityController.java
   ```

6. Build:
   ```bash
   ./build.sh
   ```

   `build.sh` installs `ojdbc-provider-spring` from Oracle's `ojdbc-extensions` source into the local Maven cache if Maven Central does not yet have the documented artifact.

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

10. Protected query with a bearer token:
    ```bash
    curl -H "Authorization: Bearer ${ACCESS_TOKEN}" http://localhost:8080/deepsec/query
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

- **End-user access token:** received in the HTTP `Authorization` header and validated by Spring Security resource-server support.
- **Database-access token:** requested by `ojdbc-provider-spring` through the Spring OAuth2 client registration configured as `entra`.

The protected endpoint therefore satisfies the provider requirement: it requires an OAuth 2.0 access token, and that token is available from Spring Security's request context when JDBC executes SQL.

Provider configuration:

```yaml
spring.datasource.hikari.data-source-properties:
  "[oracle.jdbc.provider.endUserSecurityContext]": ojdbc-provider-spring-end-user-security-context
  "[oracle.jdbc.provider.endUserSecurityContext.registrationId]": entra
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
3. The architecture: bearer token, database-access token, Spring Boot resource server, HikariCP, Oracle AI Database.
4. The key configuration: `ojdbc-provider-spring` and the matching OAuth2 client registration.
5. The policy model: `CREATE DATA GRANT` for row, column, and cell-level controls.
6. The Entra ID setup: database app registration, app roles, Spring Boot app registration, users/groups.
7. The demo: build, run, `/health`, `/policies`, `/query` with a bearer token.
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
