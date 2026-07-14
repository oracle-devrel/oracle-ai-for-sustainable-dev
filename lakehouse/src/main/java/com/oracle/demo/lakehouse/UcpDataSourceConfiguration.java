package com.oracle.demo.lakehouse;

import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UcpDataSourceConfiguration {

    @Bean
    DataSource dataSource(
            @Value("${DB_URL:${spring.datasource.url:}}") String url,
            @Value("${DB_USERNAME:${spring.datasource.username:}}") String username,
            @Value("${DB_PASSWORD:${spring.datasource.password:}}") String password,
            @Value("${lakehouse.ucp.connection-pool-name:LakehouseIcebergUcpPool}") String poolName,
            @Value("${lakehouse.ucp.initial-pool-size:1}") int initialPoolSize,
            @Value("${lakehouse.ucp.min-pool-size:1}") int minPoolSize,
            @Value("${lakehouse.ucp.max-pool-size:4}") int maxPoolSize,
            @Value("${lakehouse.ucp.row-prefetch:100}") int rowPrefetch)
            throws Exception {
        PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
        dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        dataSource.setURL(required(url, "DB_URL"));
        dataSource.setUser(required(username, "DB_USERNAME"));
        dataSource.setPassword(required(password, "DB_PASSWORD"));
        dataSource.setConnectionPoolName(poolName);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setConnectionProperty("oracle.jdbc.defaultRowPrefetch", Integer.toString(rowPrefetch));
        return dataSource;
    }

    private static String required(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + envName);
        }
        return value;
    }
}
