package com.oracle.demo.deepsecurity;

import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UcpDataSourceConfiguration {

    @Bean
    DataSource dataSource(DeepDataSecurityProperties properties) throws Exception {
        PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
        dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        dataSource.setURL(required(properties.getJdbcUrl(), "DEEPSEC_JDBC_URL"));
        dataSource.setUser(required(properties.getUsername(), "DEEPSEC_USERNAME"));
        dataSource.setPassword(required(properties.getPassword(), "DEEPSEC_PASSWORD"));
        dataSource.setConnectionPoolName("DeepDataSecurityUcpPool");
        dataSource.setInitialPoolSize(1);
        dataSource.setMinPoolSize(1);
        dataSource.setMaxPoolSize(10);
        return dataSource;
    }

    private static String required(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + envName);
        }
        return value;
    }
}
