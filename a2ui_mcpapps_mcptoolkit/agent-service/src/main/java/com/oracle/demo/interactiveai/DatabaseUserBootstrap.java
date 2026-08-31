package com.oracle.demo.interactiveai;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Creates the dedicated application principal from a one-time admin job. */
public final class DatabaseUserBootstrap {
    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Z][A-Z0-9_]{0,29}");

    private DatabaseUserBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> environment = System.getenv();
        String applicationUsername = validatedIdentifier(
                environment.getOrDefault(
                        "DB_APPLICATION_USERNAME",
                        "FINANCIAL"));
        String applicationPassword = validatedPassword(
                environment.get("DB_APPLICATION_PASSWORD"));
        PoolDataSource dataSource =
                UcpDataSourceConfiguration.fromEnvironment(environment);
        try (Connection connection = dataSource.getConnection()) {
            if (userExists(connection, applicationUsername)) {
                throw new IllegalStateException(
                        "Application database user already exists; "
                                + "refusing to reset its password: "
                                + applicationUsername);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "CREATE USER " + applicationUsername
                                + " IDENTIFIED BY \""
                                + applicationPassword
                                + "\" DEFAULT TABLESPACE DATA "
                                + "TEMPORARY TABLESPACE TEMP "
                                + "QUOTA UNLIMITED ON DATA");
                statement.execute(
                        "GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, "
                                + "CREATE PROCEDURE, CREATE SEQUENCE TO "
                                + applicationUsername);
            }
            System.out.println(
                    "Created dedicated application database user: "
                            + applicationUsername);
        } finally {
            destroyPool(dataSource);
        }
    }

    static String validatedIdentifier(String value) {
        String normalized = value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "DB_APPLICATION_USERNAME must be a simple Oracle "
                            + "identifier of at most 30 characters");
        }
        return normalized;
    }

    private static String validatedPassword(String value) {
        if (value == null
                || value.length() < 20
                || value.length() > 128
                || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "DB_APPLICATION_PASSWORD must contain 20 through 128 "
                            + "characters and cannot contain quotes or newlines");
        }
        return value;
    }

    private static boolean userExists(
            Connection connection,
            String username) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM dba_users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    private static void destroyPool(PoolDataSource dataSource) {
        try {
            UniversalConnectionPoolManagerImpl
                    .getUniversalConnectionPoolManager()
                    .destroyConnectionPool(
                            dataSource.getConnectionPoolName());
        } catch (UniversalConnectionPoolException exception) {
            throw new IllegalStateException(
                    "Unable to close Oracle UCP after user bootstrap",
                    exception);
        }
    }
}
