# Oracle JDBC Thin SSL Configuration (Updated for 26ai)

This document updates the original whitepaper with modern Oracle Database 26ai and latest JDBC driver practices.

## Key Updates

- Oracle Database 26ai introduces enhanced TLS 1.3 support and improved default cipher suites.
- Latest JDBC drivers (`ojdbc11`/`ojdbc12`) support Java 11+ and TLS 1.3 by default.
- Deprecated older protocols (`SSLv3`, `TLS 1.0`/`TLS 1.1`) should not be used.

## Connection String Example

```text
jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=host)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=service)))
```

## Security Best Practices

- Use TLS 1.3 where possible.
- Enable server DN matching.
- Use wallets or Java keystores for certificate management.

## Diff Note

This version restructures sections for clarity and removes deprecated configurations.

## References

- Original Oracle JDBC Thin SSL whitepaper: https://www.oracle.com/fr/a/otn/docs/wp-oracle-jdbc-thin-ssl-130128.pdf
- Autonomous Database JDBC Thin TLS connections: https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/connect-jdbc-thin-tls.html
- Oracle Database 26ai JDBC client-side security: https://docs.oracle.com/en/database/oracle/oracle-database/26/jjdbc/client-side-security.html
- Oracle developer blog on JDBC SSL/TLS, JKS, and wallets: https://blogs.oracle.com/developers/ssl-connection-to-oracle-db-using-jdbc-tlsv12-jks-or-oracle-wallets-122-and-lower
