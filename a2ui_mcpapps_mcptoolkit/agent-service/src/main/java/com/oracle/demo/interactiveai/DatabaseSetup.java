package com.oracle.demo.interactiveai;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Guarded local setup runner for environments where SQLcl is unavailable. */
public final class DatabaseSetup {
    private static final Set<String> SCHEMA_TABLES = Set.of(
            "SUPPLY_LOCATIONS",
            "SUPPLY_PRODUCTS",
            "INVENTORY_POSITIONS",
            "SUPPLY_LANES",
            "INVENTORY_TRANSFERS");
    private static final Set<String> CORE_OBJECTS = Set.of(
            "SUPPLY_LOCATIONS",
            "SUPPLY_PRODUCTS",
            "INVENTORY_POSITIONS",
            "SUPPLY_LANES",
            "INVENTORY_TRANSFERS",
            "APPROVE_INVENTORY_TRANSFER_MCP",
            "INVENTORY_TRANSFER_MCP_SEQ",
            "STOCKOUT_TRANSFER_RECOMMENDATION_V");
    private static final Map<String, Integer> EXPECTED_SEED_COUNTS = Map.of(
            "SUPPLY_LOCATIONS", 4,
            "SUPPLY_PRODUCTS", 5,
            "INVENTORY_POSITIONS", 20,
            "SUPPLY_LANES", 12,
            "INVENTORY_TRANSFERS", 0);

    private DatabaseSetup() {
    }

    public static void main(String[] args) throws Exception {
        Path databaseRoot = Path.of(
                        System.getProperty("database.root", "../database"))
                .toAbsolutePath()
                .normalize();
        PoolDataSource dataSource =
                UcpDataSourceConfiguration.fromEnvironment(System.getenv());
        try (Connection connection = dataSource.getConnection()) {
            Set<String> existing = existingCoreObjects(connection);
            if (existing.equals(CORE_OBJECTS)) {
                System.out.println(
                        "Supply-chain exchange database objects already exist; "
                                + "no setup changes were made.");
                return;
            }

            if (existing.isEmpty()) {
                System.out.println(
                        "No supply-chain exchange objects exist; "
                                + "installing the schema and deterministic seed data.");
                executeSqlScript(
                        connection,
                        databaseRoot.resolve("01-schema.sql"));
                executeSqlScript(
                        connection,
                        databaseRoot.resolve("02-seed-data.sql"));
            } else {
                if (!existing.containsAll(SCHEMA_TABLES)
                        || !verifiedSeedState(connection)) {
                    throw new IllegalStateException(
                            "Refusing setup because an unknown partial "
                                    + "supply-chain schema exists: "
                                    + existing);
                }
                System.out.println(
                        "Resuming setup from the verified deterministic "
                                + "supply-chain seed state; no tables will be dropped.");
            }

            executeSqlScript(
                    connection,
                    databaseRoot.resolve("04-views.sql"));
            if (!existingCoreObjects(connection)
                    .contains("INVENTORY_TRANSFER_MCP_SEQ")) {
                executeSqlScript(
                        connection,
                        databaseRoot.resolve("05-mcp-sequence.sql"));
            }
            executePlsqlScript(
                    connection,
                    databaseRoot.resolve("06-mcp-procedure.sql"));

            Set<String> installed = existingCoreObjects(connection);
            if (!installed.equals(CORE_OBJECTS)) {
                throw new IllegalStateException(
                        "Database setup did not create every expected object: "
                                + installed);
            }
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT COUNT(*) "
                                    + "FROM stockout_transfer_recommendation_v")) {
                result.next();
                System.out.println(
                        "Supply-chain exchange database setup complete; "
                                + "current recommendations="
                                + result.getInt(1));
            }
        } finally {
            destroyPool(dataSource);
        }
    }

    private static Set<String> existingCoreObjects(Connection connection)
            throws SQLException {
        String sql = """
                SELECT object_name
                  FROM user_objects
                 WHERE object_name IN (
                       'SUPPLY_LOCATIONS', 'SUPPLY_PRODUCTS',
                       'INVENTORY_POSITIONS', 'SUPPLY_LANES',
                       'INVENTORY_TRANSFERS',
                       'APPROVE_INVENTORY_TRANSFER_MCP',
                       'INVENTORY_TRANSFER_MCP_SEQ',
                       'STOCKOUT_TRANSFER_RECOMMENDATION_V')
                   AND object_type IN ('TABLE', 'PROCEDURE', 'VIEW', 'SEQUENCE')
                """;
        Set<String> objects = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                objects.add(rows.getString(1).toUpperCase(Locale.ROOT));
            }
        }
        return Set.copyOf(objects);
    }

    private static boolean verifiedSeedState(Connection connection)
            throws SQLException {
        for (Map.Entry<String, Integer> expected
                : EXPECTED_SEED_COUNTS.entrySet()) {
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT COUNT(*) FROM " + expected.getKey())) {
                result.next();
                if (result.getInt(1) != expected.getValue()) return false;
            }
        }
        return true;
    }

    private static void executeSqlScript(
            Connection connection,
            Path script) throws IOException, SQLException {
        for (String sql : splitStatements(readScript(script))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private static void executePlsqlScript(
            Connection connection,
            Path script) throws IOException, SQLException {
        String sql = readScript(script)
                .replaceFirst("(?m)^/\\s*$", "")
                .trim();
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String readScript(Path script) throws IOException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(script)) {
            String trimmed =
                    line.stripLeading().toUpperCase(Locale.ROOT);
            if (trimmed.startsWith("WHENEVER SQLERROR")
                    || trimmed.startsWith("PROMPT ")) {
                continue;
            }
            sql.append(line).append('\n');
        }
        return sql.toString().trim();
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            if (character == '\''
                    && quoted
                    && index + 1 < script.length()
                    && script.charAt(index + 1) == '\'') {
                current.append(character).append(script.charAt(++index));
                continue;
            }
            if (character == '\'') quoted = !quoted;
            if (character == ';' && !quoted) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) statements.add(statement);
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        String statement = current.toString().trim();
        if (!statement.isEmpty()) statements.add(statement);
        return List.copyOf(statements);
    }

    private static void destroyPool(PoolDataSource dataSource) {
        try {
            UniversalConnectionPoolManagerImpl
                    .getUniversalConnectionPoolManager()
                    .destroyConnectionPool(
                            dataSource.getConnectionPoolName());
        } catch (UniversalConnectionPoolException exception) {
            throw new IllegalStateException(
                    "Unable to close Oracle UCP after database setup",
                    exception);
        }
    }
}
