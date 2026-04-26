package oracleai;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.springframework.core.env.Environment;

final class OracleJdbcSupport {

    private OracleJdbcSupport() {
    }

    static Connection openConnection(Environment environment) throws SQLException {
        String username = requiredProperty(environment, "DB_USERNAME");
        String password = requiredProperty(environment, "DB_PASSWORD");
        String dsn = requiredProperty(environment, "DB_DSN");
        String walletPassword = firstNonBlank(environment.getProperty("DB_WALLET_PASSWORD"), "");
        String tnsAdmin = firstNonBlank(
                environment.getProperty("TNS_ADMIN"),
                environment.getProperty("DB_WALLET_DIR")
        );

        if (tnsAdmin.isBlank()) {
            throw new IllegalArgumentException("TNS_ADMIN or DB_WALLET_DIR must be configured for database access.");
        }

        ensureOracleDriver();

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", username);
        connectionProperties.setProperty("password", password);
        connectionProperties.setProperty("oracle.net.tns_admin", tnsAdmin);
        connectionProperties.setProperty("TNS_ADMIN", tnsAdmin);
        connectionProperties.setProperty("oracle.net.ssl_server_dn_match", "false");

        System.setProperty("oracle.net.tns_admin", tnsAdmin);
        System.setProperty("TNS_ADMIN", tnsAdmin);
        System.setProperty("oracle.net.ssl_server_dn_match", "false");

        if (walletPassword.isBlank()) {
            String walletLocation =
                    "(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=" + tnsAdmin + ")))";
            connectionProperties.setProperty("oracle.net.wallet_location", walletLocation);
            System.setProperty("oracle.net.wallet_location", walletLocation);
        } else {
            String trustStore = tnsAdmin + "/truststore.jks";
            String keyStore = tnsAdmin + "/keystore.jks";

            connectionProperties.setProperty("javax.net.ssl.trustStore", trustStore);
            connectionProperties.setProperty("javax.net.ssl.trustStoreType", "JKS");
            connectionProperties.setProperty("javax.net.ssl.trustStorePassword", walletPassword);
            connectionProperties.setProperty("javax.net.ssl.keyStore", keyStore);
            connectionProperties.setProperty("javax.net.ssl.keyStoreType", "JKS");
            connectionProperties.setProperty("javax.net.ssl.keyStorePassword", walletPassword);

            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("javax.net.ssl.trustStorePassword", walletPassword);
            System.setProperty("javax.net.ssl.keyStore", keyStore);
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");
            System.setProperty("javax.net.ssl.keyStorePassword", walletPassword);
        }

        String jdbcUrl = "jdbc:oracle:thin:@" + dsn + "?TNS_ADMIN=" + tnsAdmin;
        return DriverManager.getConnection(jdbcUrl, connectionProperties);
    }

    static String requiredProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must be configured for database access.");
        }
        return value.trim();
    }

    private static void ensureOracleDriver() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Oracle JDBC driver is not available.", exception);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
