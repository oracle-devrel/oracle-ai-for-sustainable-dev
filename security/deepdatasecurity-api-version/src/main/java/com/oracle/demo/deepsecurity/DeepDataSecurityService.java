package com.oracle.demo.deepsecurity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import oracle.jdbc.EndUserSecurityContext;
import oracle.jdbc.OracleConnection;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import org.springframework.stereotype.Service;

@Service
public class DeepDataSecurityService {

    private final DataSource dataSource;
    private final DeepDataSecurityProperties properties;
    private final DatabaseAccessTokenService databaseAccessTokenService;
    private final ObjectMapper objectMapper;
    private final OracleJsonFactory jsonFactory = new OracleJsonFactory();

    DeepDataSecurityService(
            DataSource dataSource,
            DeepDataSecurityProperties properties,
            DatabaseAccessTokenService databaseAccessTokenService,
            ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.databaseAccessTokenService = databaseAccessTokenService;
        this.objectMapper = objectMapper;
    }

    public List<String> queryAsEndUser(String endUserToken) throws SQLException {
        return queryAsEndUser(endUserToken, properties.getSql());
    }

    public String usernameAsEndUser(String endUserToken) throws SQLException {
        List<String> rows = queryAsEndUser(endUserToken, "select ORA_END_USER_CONTEXT.username from dual");
        return rows.isEmpty() ? null : normalizeJsonScalar(rows.get(0));
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

    private List<String> queryAsEndUser(String endUserToken, String sql) throws SQLException {
        String contextAuthenticationToken = getContextAuthenticationToken(endUserToken);
        String databaseAccessToken = requestDatabaseAccessToken(contextAuthenticationToken);

        try (Connection connection = dataSource.getConnection()) {
            DeepDataSecurityContextHandle contextHandle = DeepDataSecurityContextHandle.set(
                    connection,
                    databaseAccessToken,
                    contextAuthenticationToken, // aka endUserToken
                    attributes());
            try {
                return runQuery(connection, sql);
            } finally {
                contextHandle.clear();
            }
        }
    }

    private String requestDatabaseAccessToken(String endUserToken) {
        return databaseAccessTokenService.getDatabaseAccessToken(endUserToken);
    }

    private static String getContextAuthenticationToken(String endUserToken) {
        if (endUserToken == null || endUserToken.isBlank()) {
            throw new IllegalArgumentException("The signed-in user's Entra access token is required");
        }
        return endUserToken;
    }

    private List<String> runQuery(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
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

    private Map<String, OracleJsonObject> attributes() {
        String attributesJson = properties.getAttributesJson();
        if (attributesJson == null || attributesJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(attributesJson);
            if (!root.isObject()) {
                throw new IllegalArgumentException("deepsec.attributes-json must be a JSON object");
            }
            java.util.LinkedHashMap<String, OracleJsonObject> attributes = new java.util.LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> attributes.put(entry.getKey(), oracleJsonObject(entry.getValue())));
            return attributes;
        } catch (Exception e) {
            throw new IllegalArgumentException("deepsec.attributes-json must be a JSON object", e);
        }
    }

    private OracleJsonObject oracleJsonObject(JsonNode node) {
        try {
            OracleJsonValue value = jsonFactory
                    .createJsonTextValue(new StringReader(objectMapper.writeValueAsString(node)));
            if (value instanceof OracleJsonObject object) {
                return object;
            }
            throw new IllegalArgumentException("End-user context attribute values must be JSON objects");
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse end-user context attributes", e);
        }
    }

    private record DeepDataSecurityContextHandle(OracleConnection oracleConnection) {

        static DeepDataSecurityContextHandle set(
                Connection connection,
                String databaseAccessToken,
                String endUserToken,
                Map<String, OracleJsonObject> attributes) throws SQLException {
            OracleConnection oracleConnection = connection instanceof OracleConnection
                    ? (OracleConnection) connection
                    : connection.unwrap(OracleConnection.class);
            EndUserSecurityContext context = EndUserSecurityContext.createWithToken(databaseAccessToken, endUserToken);
            if (!attributes.isEmpty()) {
                context = context.withAttributes(attributes);
            }
            oracleConnection.setEndUserSecurityContext(context);
            return new DeepDataSecurityContextHandle(oracleConnection);
        }

        void clear() throws SQLException {
            oracleConnection.clearEndUserSecurityContext();
        }
    }
}
