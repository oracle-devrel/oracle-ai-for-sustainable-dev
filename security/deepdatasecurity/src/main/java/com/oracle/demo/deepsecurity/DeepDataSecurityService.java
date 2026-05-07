package com.oracle.demo.deepsecurity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class DeepDataSecurityService {

    private static final TypeReference<Map<String, Object>> ATTRIBUTES_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final DeepDataSecurityProperties properties;
    private final DatabaseAccessTokenService databaseAccessTokenService;
    private final ObjectMapper objectMapper;

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
        String databaseAccessToken = databaseAccessTokenService.getDatabaseAccessToken(endUserToken);

        try (Connection connection = dataSource.getConnection()) {
            DeepDataSecurityContextHandle contextHandle = DeepDataSecurityContextHandle.set(
                    connection,
                    databaseAccessToken,
                    endUserToken,
                    dataRoles(),
                    attributes());
            try {
                return runBusinessQuery(connection);
            } finally {
                contextHandle.clear();
            }
        }
    }

    private List<String> runBusinessQuery(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(properties.getSql())) {
            java.util.ArrayList<String> rows = new java.util.ArrayList<>();
            while (resultSet.next()) {
                rows.add(resultSet.getString(1));
            }
            return rows;
        }
    }

    private Set<String> dataRoles() {
        if (properties.getDataRoles() == null || properties.getDataRoles().isBlank()) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        Arrays.stream(properties.getDataRoles().split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .forEach(roles::add);
        return roles;
    }

    private Map<String, Object> attributes() {
        String attributesJson = properties.getAttributesJson();
        if (attributesJson == null || attributesJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(attributesJson, ATTRIBUTES_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("deepsec.attributes-json must be a JSON object", e);
        }
    }

    private record DeepDataSecurityContextHandle(Object oracleConnection) {

        static DeepDataSecurityContextHandle set(
                Connection connection,
                String databaseAccessToken,
                String endUserToken,
                Set<String> dataRoles,
                Map<String, Object> attributes) throws SQLException {
            try {
                Class<?> oracleConnectionClass = Class.forName("oracle.jdbc.OracleConnection");
                Class<?> securityContextClass = Class.forName("oracle.jdbc.EndUserSecurityContext");

                Object oracleConnection = connection.unwrap(oracleConnectionClass);
                Object securityContext = securityContextClass
                        .getMethod("createWithToken", String.class, String.class)
                        .invoke(null, databaseAccessToken, endUserToken);

                if (!dataRoles.isEmpty()) {
                    securityContext = securityContextClass
                            .getMethod("withDataRoles", Set.class)
                            .invoke(securityContext, dataRoles);
                }
                if (!attributes.isEmpty()) {
                    securityContext = securityContextClass
                            .getMethod("withAttributes", Map.class)
                            .invoke(securityContext, attributes);
                }

                oracleConnectionClass
                        .getMethod("setEndUserSecurityContext", securityContextClass)
                        .invoke(oracleConnection, securityContext);
                return new DeepDataSecurityContextHandle(oracleConnection);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new IllegalStateException(
                        "The Oracle JDBC driver on the classpath does not expose the Deep Data Security "
                                + "EndUserSecurityContext APIs. Use an Oracle JDBC 26ai driver artifact "
                                + "that includes oracle.jdbc.EndUserSecurityContext and "
                                + "OracleConnection.setEndUserSecurityContext(...).",
                        e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to attach the Oracle Deep Data Security context", e);
            }
        }

        void clear() {
            try {
                oracleConnection.getClass().getMethod("clearEndUserSecurityContext").invoke(oracleConnection);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "The Oracle JDBC driver on the classpath does not expose "
                                + "OracleConnection.clearEndUserSecurityContext().",
                        e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to clear the Oracle Deep Data Security context", e);
            }
        }
    }
}
