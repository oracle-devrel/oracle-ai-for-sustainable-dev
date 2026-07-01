package com.oracle.demo.deepsecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class DeepDataSecurityService {

    private final DataSource dataSource;
    private final DeepDataSecurityProperties properties;
    private final ObjectMapper objectMapper;

    DeepDataSecurityService(
            DataSource dataSource,
            DeepDataSecurityProperties properties,
            ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<String> queryAsEndUser() throws SQLException {
        return runQuery(properties.getSql());
    }

    public String usernameAsEndUser() throws SQLException {
        List<String> rows = runQuery("select ORA_END_USER_CONTEXT.username from dual");
        return rows.isEmpty() ? null : normalizeJsonScalar(rows.get(0));
    }

    public DeepDataSecurityProperties.Browser browserConfig() {
        return properties.getBrowser();
    }

    private String normalizeJsonScalar(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                return objectMapper.readValue(trimmed, String.class);
            } catch (Exception ignored) {
                return value;
            }
        }
        return value;
    }

    private List<String> runQuery(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            runSessionInit(statement);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                java.util.ArrayList<String> rows = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    rows.add(resultSet.getString(1));
                }
                return rows;
            }
        }
    }

    private void runSessionInit(Statement statement) throws SQLException {
        String sessionInitSql = properties.getSessionInitSql();
        if (sessionInitSql != null && !sessionInitSql.isBlank()) {
            statement.execute(sessionInitSql);
        }
    }
}
