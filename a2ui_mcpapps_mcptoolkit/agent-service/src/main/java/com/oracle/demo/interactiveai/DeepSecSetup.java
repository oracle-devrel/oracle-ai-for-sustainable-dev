package com.oracle.demo.interactiveai;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Installs and verifies the local-user Deep Data Security demonstration. */
public final class DeepSecSetup {
    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,29}");
    private static final String DATA_ROLE = "INVENTORY_ENVIRONMENTAL_ROLE";
    private static final String LOGIN_ROLE = "INVENTORY_DEEPSEC_LOGIN_ROLE";

    private DeepSecSetup() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> environment = System.getenv();
        String url = required(environment, "DB_URL");
        String adminUser = identifier(
                environment.getOrDefault("DB_ADMIN_USERNAME", "ADMIN"),
                "DB_ADMIN_USERNAME");
        String adminPassword = required(environment, "DB_ADMIN_PASSWORD");
        String restrictedUser = identifier(
                required(environment, "DB_USERNAME2"), "DB_USERNAME2");
        String restrictedPassword = required(environment, "DB_PASSWORD2");

        try (Connection admin = connection(url, adminUser, adminPassword)) {
            admin.setAutoCommit(true);
            executeIgnore(admin, "CREATE ROLE " + LOGIN_ROLE, 1921);
            execute(admin, "GRANT CREATE SESSION TO " + LOGIN_ROLE);
            execute(admin, "CREATE DATA ROLE IF NOT EXISTS " + DATA_ROLE);
            execute(admin, "GRANT " + LOGIN_ROLE + " TO " + DATA_ROLE);
            execute(admin, "CREATE END USER IF NOT EXISTS " + restrictedUser
                    + " IDENTIFIED BY " + passwordLiteral(restrictedPassword));
            executeIgnore(admin, "ALTER END USER " + restrictedUser
                    + " IDENTIFIED BY " + passwordLiteral(restrictedPassword)
                    + " ACCOUNT UNLOCK", 28007);
            execute(admin, "ALTER END USER " + restrictedUser
                    + " ACCOUNT UNLOCK");
            execute(admin, "GRANT DATA ROLE " + DATA_ROLE
                    + " TO " + restrictedUser);
            execute(admin, "CREATE OR REPLACE DATA GRANT "
                    + "FINANCIAL.inventory_environmental_read AS SELECT "
                    + "ON FINANCIAL.stockout_transfer_recommendation_v "
                    + "WHERE category_name = 'Environmental Monitoring' "
                    + "TO " + DATA_ROLE);
            execute(admin, "SET USE DATA GRANTS ONLY ON "
                    + "FINANCIAL.stockout_transfer_recommendation_v ENABLED");
        }

        List<String> categories = new ArrayList<>();
        try (Connection restricted = connection(
                url, restrictedUser, restrictedPassword);
                Statement statement = restricted.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT DISTINCT category_name "
                                + "FROM FINANCIAL.stockout_transfer_recommendation_v "
                                + "ORDER BY category_name")) {
            while (rows.next()) categories.add(rows.getString(1));
        }
        if (!categories.equals(List.of("Environmental Monitoring"))) {
            throw new IllegalStateException(
                    "Deep Data Security verification returned unexpected categories: "
                            + categories);
        }
        System.out.println(
                "Deep Data Security verified: " + restrictedUser
                        + " can read only Environmental Monitoring recommendations.");
    }

    private static Connection connection(
            String url,
            String username,
            String password) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        return DriverManager.getConnection(url, properties);
    }

    private static void execute(Connection connection, String sql)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void executeIgnore(
            Connection connection,
            String sql,
            int ignoredErrorCode) throws SQLException {
        try {
            execute(connection, sql);
        } catch (SQLException exception) {
            if (exception.getErrorCode() != ignoredErrorCode) throw exception;
        }
    }

    private static String identifier(String value, String name) {
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be an unquoted Oracle identifier of 1 to 30 characters");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static String passwordLiteral(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
