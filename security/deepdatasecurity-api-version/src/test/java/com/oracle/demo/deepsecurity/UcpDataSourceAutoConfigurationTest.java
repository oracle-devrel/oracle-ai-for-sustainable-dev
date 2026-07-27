package com.oracle.demo.deepsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

class UcpDataSourceAutoConfigurationTest {

    @Test
    void springBootCreatesUcpFromApplicationYaml() throws IOException {
        var propertySources = new YamlPropertySourceLoader()
                .load("application.yaml", new ClassPathResource("application.yaml"));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/freepdb1",
                        "spring.datasource.username=hr_app_user",
                        "spring.datasource.password=test-only")
                .withInitializer(context -> propertySources.forEach(
                        propertySource -> context.getEnvironment()
                                .getPropertySources()
                                .addLast(propertySource)))
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(PoolDataSource.class);

                    PoolDataSource dataSource = (PoolDataSource) context.getBean(DataSource.class);
                    assertThat(dataSource.getConnectionFactoryClassName())
                            .isEqualTo("oracle.jdbc.datasource.impl.OracleDataSource");
                    assertThat(dataSource.getConnectionPoolName())
                            .isEqualTo("DeepDataSecurityApiUcpPool");
                    assertThat(dataSource.getInitialPoolSize()).isEqualTo(1);
                    assertThat(dataSource.getMinPoolSize()).isEqualTo(1);
                    assertThat(dataSource.getMaxPoolSize()).isEqualTo(10);
                });
    }
}
