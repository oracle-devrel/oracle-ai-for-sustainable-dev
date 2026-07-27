package com.oracle.demo.interactiveai;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

public final class UcpDataSourceConfiguration {
    private static final String DEFAULT_ALIAS = "financialdb_high";
    private static final String DEFAULT_USER = "FINANCIAL";

    private UcpDataSourceConfiguration() {
    }

    public static PoolDataSource fromEnvironment(Map<String, String> environment) {
        String tnsAdmin = firstNonBlank(environment.get("TNS_ADMIN"), defaultWalletPath());
        String defaultUrl = tnsAdmin == null
                ? null
                : "jdbc:oracle:thin:@" + DEFAULT_ALIAS + "?TNS_ADMIN=" + tnsAdmin;
        String url = required(firstNonBlank(environment.get("DB_URL"), defaultUrl), "DB_URL or TNS_ADMIN");
        String username = firstNonBlank(environment.get("DB_USERNAME"), DEFAULT_USER);
        String password = required(environment.get("DB_PASSWORD"), "DB_PASSWORD");

        try {
            PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
            dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            dataSource.setURL(url);
            dataSource.setUser(username);
            dataSource.setPassword(password);
            dataSource.setConnectionPoolName(environment.getOrDefault("DB_POOL_NAME", "InteractiveAiFinancialUcpPool"));
            dataSource.setInitialPoolSize(integer(environment, "DB_POOL_INITIAL_SIZE", 1, 0, 20));
            dataSource.setMinPoolSize(integer(environment, "DB_POOL_MIN_SIZE", 1, 0, 20));
            dataSource.setMaxPoolSize(integer(environment, "DB_POOL_MAX_SIZE", 4, 1, 50));
            dataSource.setConnectionWaitDuration(Duration.ofSeconds(
                    integer(environment, "DB_CONNECTION_WAIT_SECONDS", 10, 1, 120)));
            dataSource.setInactiveConnectionTimeout(integer(environment, "DB_INACTIVE_CONNECTION_SECONDS", 60, 0, 3600));
            dataSource.setValidateConnectionOnBorrow(true);
            dataSource.setConnectionProperty("oracle.jdbc.defaultRowPrefetch", environment.getOrDefault("DB_ROW_PREFETCH", "50"));
            return dataSource;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to configure Oracle UCP", exception);
        }
    }

    private static String defaultWalletPath() {
        Path candidate = Path.of(System.getProperty("user.home"), "Downloads", "Wallet_financialdb");
        return Files.isDirectory(candidate) ? candidate.toString() : null;
    }

    private static int integer(Map<String, String> environment, String name, int fallback, int minimum, int maximum) {
        String configured = environment.get(name);
        int value = configured == null || configured.isBlank() ? fallback : Integer.parseInt(configured);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing required environment variable: " + name);
        return value.trim();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first.trim() : second;
    }
}
