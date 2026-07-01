package com.oracle.demo.deepsecurity;

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
            DeepDataSecurityProperties properties,
            @Value("${deepsec.ucp.connection-pool-name:DeepDataSecurityApiUcpPool}") String poolName,
            @Value("${deepsec.ucp.initial-pool-size:1}") int initialPoolSize,
            @Value("${deepsec.ucp.min-pool-size:1}") int minPoolSize,
            @Value("${deepsec.ucp.max-pool-size:10}") int maxPoolSize) throws Exception {
        PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
        dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        dataSource.setURL(required(properties.getJdbcUrl(), "DEEPSEC_JDBC_URL"));
        dataSource.setUser(required(properties.getUsername(), "DEEPSEC_USERNAME"));
        dataSource.setPassword(required(properties.getPassword(), "DEEPSEC_PASSWORD"));
        dataSource.setConnectionPoolName(poolName);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setMaxPoolSize(maxPoolSize);
        return dataSource;
    }

    private static String required(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + envName);
        }
        return value;
    }
}
