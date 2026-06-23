# Oracle AI Database Deep Data Security API-Version Demo

The complete end-to-end walkthrough for this demo is in [MEDIUM_BLOG.html](MEDIUM_BLOG.html).

This version demonstrates the explicit JDBC API approach: the Spring Boot code obtains the end-user token and database-access token, creates an `EndUserSecurityContext`, calls `OracleConnection.setEndUserSecurityContext(...)`, runs SQL, then clears the context before returning the pooled connection.

It covers the Microsoft Entra ID setup, Oracle AI Database/Oracle Database Free external authentication SQL, HR sample and Deep Data Security SQL scripts, Spring Boot configuration, and running the app.

The application source code, SQL setup scripts, and runnable build scripts are in this directory.
