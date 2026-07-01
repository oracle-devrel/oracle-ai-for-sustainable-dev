# Oracle AI Database Deep Data Security API-Version Demo

The complete end-to-end walkthrough for this demo is in [MEDIUM_BLOG.html](MEDIUM_BLOG.html).

This version demonstrates the explicit JDBC API approach: the Spring Boot code obtains the end-user token and database-access token, creates an `EndUserSecurityContext`, calls `OracleConnection.setEndUserSecurityContext(...)`, runs SQL, then clears the context before returning the pooled connection.

Its public query endpoint intentionally matches the provider-version demo:
`GET /deepsec/query`. The difference is inside the controller/service plumbing:
this API version receives `OAuth2AuthorizedClient` in the controller so app code
can explicitly pass the signed-in user's token into Oracle JDBC.

The app uses Oracle UCP for pooling. HikariCP is excluded from the JDBC starter
so the API and provider demos use the same Oracle-aware pool while comparing the
explicit API and provider/SPI Deep Data Security approaches.

It covers the Microsoft Entra ID setup, Oracle AI Database/Oracle Database Free external authentication SQL, HR sample and Deep Data Security SQL scripts, Spring Boot configuration, and running the app.

The application source code, SQL setup scripts, and runnable build scripts are in this directory.
