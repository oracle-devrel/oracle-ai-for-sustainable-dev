package com.oracle.demo.memory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/** Removes only this demo's incompatible two-table prototype schema. */
final class LegacyMemorySchemaMigration {
    private static final String MESSAGE_TABLE = "OAMJ_CONCIERGE_MESSAGE";

    private LegacyMemorySchemaMigration() {
    }

    static void migrateIfNecessary(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "OAMJ_CONCIERGE_RECORDS")
                    && (!tableExists(connection, MESSAGE_TABLE)
                    || columnExists(connection, MESSAGE_TABLE, "EXPIRES_AT"))) {
                return;
            }
            for (String table : List.of(
                    "OAMJ_CONCIERGE_RECORD_CHUNKS",
                    "OAMJ_CONCIERGE_MESSAGE",
                    "OAMJ_CONCIERGE_MEMORY",
                    "OAMJ_CONCIERGE_RECORDS",
                    "OAMJ_CONCIERGE_ACTOR_PROFILE",
                    "OAMJ_CONCIERGE_THREAD",
                    "OAMJ_CONCIERGE_ORACLEAGENTMEMORY_SCHEMA_META")) {
                dropIfPresent(connection, table);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to migrate the legacy OAMJ_CONCIERGE schema",
                    exception);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = ?")) {
            statement.setString(1, table);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(
            Connection connection,
            String table,
            String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = ? AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1) > 0;
            }
        }
    }

    private static void dropIfPresent(Connection connection, String table) throws SQLException {
        if (!tableExists(connection, table)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE " + table + " CASCADE CONSTRAINTS PURGE");
        }
    }
}
