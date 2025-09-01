// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.configuration;

import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OKafkaProperties.class)
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class ApplicationConfiguration {
    private final OKafkaProperties okafkaProperties;

    public ApplicationConfiguration(OKafkaProperties okafkaProperties) {
        this.okafkaProperties = okafkaProperties;
    }


    @Qualifier("overdraftDataSource")
    @Bean
    public DataSource overdraftDataSource() throws SQLException {
        PoolDataSource ds = PoolDataSourceFactory.getPoolDataSource();
        ds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        ds.setConnectionPoolName(UUID.randomUUID().toString());
        ds.setURL("jdbc:oracle:thin:@%s?TNS_ADMIN=%s".formatted(System.getenv("SERVICE_NAME"), System.getenv("OJDBC_PATH")));
        ds.setConnectionPoolName(UUID.randomUUID().toString());
        ds.setMaxPoolSize(30);
        ds.setInitialPoolSize(10);
        ds.setMinPoolSize(1);
        return ds;
    }
}
