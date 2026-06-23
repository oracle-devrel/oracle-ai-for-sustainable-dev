# Security Demos

## Oracle Deep Data Security

The Deep Data Security demo is split into two Spring Boot versions:

- `deepdatasecurity-api-version`: explicit JDBC API implementation that creates an `EndUserSecurityContext` and calls `OracleConnection.setEndUserSecurityContext(...)`.
- `deepdatasecurity-provider-version`: provider/SPI implementation that uses `ojdbc-provider-spring` with Spring Security OAuth2 resource-server support.

Use the API version when you want to show every token and JDBC call in application code. Use the provider version when you want to show the configuration-driven approach for a Spring app whose protected endpoints require OAuth 2.0 bearer tokens.
