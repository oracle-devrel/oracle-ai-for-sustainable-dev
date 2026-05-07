package com.oracle.demo.deepsecurity;

import javax.sql.DataSource;
import oracle.jdbc.pool.OracleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OracleDataSourceConfiguration {

    @Bean
    DataSource dataSource(DeepDataSecurityProperties properties) throws Exception {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(required(properties.getJdbcUrl(), "DEEPSEC_JDBC_URL"));
        dataSource.setUser(required(properties.getUsername(), "DEEPSEC_USERNAME"));
        dataSource.setPassword(required(properties.getPassword(), "DEEPSEC_PASSWORD"));
        return dataSource;
    }

    private static String required(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + envName);
        }
        return value;
    }
}
