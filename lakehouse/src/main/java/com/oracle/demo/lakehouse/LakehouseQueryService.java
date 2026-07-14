package com.oracle.demo.lakehouse;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Clob;
import java.sql.Blob;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class LakehouseQueryService {

    private static final Pattern SQL_IDENTIFIER =
            Pattern.compile("[A-Za-z][A-Za-z0-9_$#]*(\\.[A-Za-z][A-Za-z0-9_$#]*)?");

    private final DataSource dataSource;
    private final LakehouseProperties properties;

    LakehouseQueryService(DataSource dataSource, LakehouseProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    public Map<String, Object> databaseInfo() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("databaseProductName", metadata.getDatabaseProductName());
            info.put("databaseProductVersion", metadata.getDatabaseProductVersion());
            info.put("driverName", metadata.getDriverName());
            info.put("driverVersion", metadata.getDriverVersion());
            info.put("jdbcUrl", sanitizeUrl(metadata.getURL()));
            info.put("configuredIcebergTable", emptyToNull(properties.tableName()));
            return info;
        }
    }

    public Map<String, Object> configuredTableSummary() throws SQLException {
        String tableName = configuredTableName();
        int limit = properties.sampleLimit();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tableName", tableName);
        summary.put("rowCount", scalarNumber("select count(*) from " + tableName));
        summary.put("columns", columns(tableName));
        summary.put("sampleRows", sampleRows(tableName, limit));
        return summary;
    }

    public List<Map<String, Object>> sampleRows(String requestedTableName, int requestedLimit)
            throws SQLException {
        String tableName = validateTableName(requestedTableName);
        int limit = Math.max(1, Math.min(requestedLimit, 500));
        String sql = "select * from " + tableName + " fetch first " + limit + " rows only";
        return queryRows(sql);
    }

    public List<Map<String, Object>> columns(String requestedTableName) throws SQLException {
        String tableName = validateTableName(requestedTableName);
        String sql = """
                select owner, table_name, column_name, data_type, data_length, nullable
                  from all_tab_columns
                 where (owner, table_name) = (
                       select coalesce(:owner, sys_context('USERENV', 'CURRENT_SCHEMA')),
                              :table_name
                         from dual)
                 order by column_id
                """;

        String[] parts = tableName.toUpperCase(Locale.ROOT).split("\\.");
        String owner = parts.length == 2 ? parts[0] : null;
        String simpleName = parts.length == 2 ? parts[1] : parts[0];
        String executableSql = sql
                .replace(":owner", owner == null ? "null" : "'" + owner + "'")
                .replace(":table_name", "'" + simpleName + "'");
        return queryRows(executableSql);
    }

    public List<Map<String, Object>> visibleObjects(String nameLike) throws SQLException {
        String pattern = nameLike == null || nameLike.isBlank()
                ? "%"
                : nameLike.trim().toUpperCase(Locale.ROOT);
        if (!pattern.contains("%") && !pattern.contains("_")) {
            pattern = "%" + pattern + "%";
        }
        String sql = """
                select owner, object_name, object_type
                  from all_objects
                 where object_type in ('TABLE', 'VIEW', 'MATERIALIZED VIEW', 'SYNONYM')
                   and object_name like '%s'
                 order by owner, object_type, object_name
                 fetch first 100 rows only
                """.formatted(escapeSqlLiteral(pattern));
        return queryRows(sql);
    }

    private Number scalarNumber(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(properties.queryTimeoutSeconds());
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    return (Number) resultSet.getObject(1);
                }
            }
        }
        return 0;
    }

    private List<Map<String, Object>> queryRows(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.setFetchSize(100);
            statement.setQueryTimeout(properties.queryTimeoutSeconds());
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                return readRows(resultSet);
            }
        }
    }

    private static List<Map<String, Object>> readRows(ResultSet resultSet) throws SQLException {
        int columnCount = resultSet.getMetaData().getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(resultSet.getMetaData().getColumnLabel(i), jsonSafeValue(resultSet, i));
            }
            rows.add(row);
        }
        return rows;
    }

    private static Object jsonSafeValue(ResultSet resultSet, int columnIndex) throws SQLException {
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Date || value instanceof Time) {
            return value.toString();
        }
        if (value instanceof Clob clob) {
            return clob.getSubString(1, Math.toIntExact(Math.min(clob.length(), 10_000)));
        }
        if (value instanceof Blob blob) {
            return "<BLOB " + blob.length() + " bytes>";
        }
        if (value instanceof SQLXML sqlxml) {
            return sqlxml.getString();
        }
        Package valuePackage = value.getClass().getPackage();
        if (valuePackage != null && valuePackage.getName().startsWith("oracle.sql")) {
            return resultSet.getString(columnIndex);
        }
        return value;
    }

    private String configuredTableName() {
        return validateTableName(properties.tableName());
    }

    private static String validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Set ICEBERG_TABLE_NAME to an Oracle table or view name.");
        }
        String trimmed = tableName.trim();
        if (!SQL_IDENTIFIER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        return trimmed;
    }

    private static String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(?i)(password=)[^;)]+", "$1***");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
