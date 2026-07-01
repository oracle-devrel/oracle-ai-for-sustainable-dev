package com.oracle.demo.deepsecurity;

import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UcpDataSourceConfiguration {

    @Bean
    DataSource dataSource(
            DataSourceProperties properties,
            @Value("${deepsec.ucp.connection-pool-name:DeepDataSecurityProviderUcpPool}") String poolName,
            @Value("${deepsec.ucp.initial-pool-size:1}") int initialPoolSize,
            @Value("${deepsec.ucp.min-pool-size:1}") int minPoolSize,
            @Value("${deepsec.ucp.max-pool-size:4}") int maxPoolSize,
            @Value("${deepsec.jdbc-provider.end-user-security-context:ojdbc-provider-spring-end-user-security-context}")
                    String endUserSecurityContextProvider,
            @Value("${deepsec.jdbc-provider.registration-id:entra}") String registrationId,
            @Value("${deepsec.jdbc-provider.data-roles:}") String dataRoles,
            @Value("${deepsec.jdbc-provider.end-user-context-attributes:}") String endUserContextAttributes)
            throws Exception {
        PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
        dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        dataSource.setURL(required(properties.getUrl(), "DEEPSEC_JDBC_URL"));
        dataSource.setUser(required(properties.getUsername(), "DEEPSEC_USERNAME"));
        dataSource.setPassword(required(properties.getPassword(), "DEEPSEC_PASSWORD"));
        dataSource.setConnectionPoolName(poolName);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setConnectionProperty(
                "oracle.jdbc.provider.endUserSecurityContext", endUserSecurityContextProvider);
        dataSource.setConnectionProperty(
                "oracle.jdbc.provider.endUserSecurityContext.registrationId", registrationId);
        setOptionalConnectionProperty(
                dataSource, "oracle.jdbc.provider.endUserSecurityContext.dataRoles", dataRoles);
        setOptionalConnectionProperty(
                dataSource,
                "oracle.jdbc.provider.endUserSecurityContext.endUserContextAttributes",
                endUserContextAttributes);
        return dataSource;
    }

    private static void setOptionalConnectionProperty(
            PoolDataSource dataSource, String name, String value) throws Exception {
        if (value != null && !value.isBlank()) {
            dataSource.setConnectionProperty(name, value);
        }
    }

    private static String required(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + envName);
        }
        return value;
    }
}
