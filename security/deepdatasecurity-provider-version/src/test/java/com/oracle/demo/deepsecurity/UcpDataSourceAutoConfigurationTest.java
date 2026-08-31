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

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                    .withPropertyValues(
                            "spring.datasource.type=oracle.ucp.jdbc.PoolDataSource",
                            "spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/freepdb1",
                            "spring.datasource.username=hr_app_user",
                            "spring.datasource.password=test-only",
                            "spring.datasource.driver-class-name=oracle.jdbc.OracleDriver");

    @Test
    void springBootCreatesUcpFromTheEntraProfile() throws IOException {
        assertProfile("application-entraid.yaml", "entra");
    }

    @Test
    void springBootCreatesUcpFromTheOciIamProfile() throws IOException {
        assertProfile("application-oci-iam.yaml", "oci-iam");
    }

    private void assertProfile(String resourceName, String registrationId) throws IOException {
        var propertySources = new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName));

        contextRunner
                .withInitializer(context -> propertySources.forEach(
                        propertySource -> context.getEnvironment()
                                .getPropertySources()
                                .addLast(propertySource)))
                .run(
                context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context.getBean(DataSource.class)).isInstanceOf(PoolDataSource.class);

                    PoolDataSource dataSource = (PoolDataSource) context.getBean(DataSource.class);
                    assertThat(dataSource.getConnectionFactoryClassName())
                            .isEqualTo("oracle.jdbc.datasource.impl.OracleDataSource");
                    assertThat(dataSource.getConnectionPoolName())
                            .isEqualTo("DeepDataSecurityProviderUcpPool");
                    assertThat(dataSource.getInitialPoolSize()).isEqualTo(1);
                    assertThat(dataSource.getMinPoolSize()).isEqualTo(1);
                    assertThat(dataSource.getMaxPoolSize()).isEqualTo(4);
                    assertThat(dataSource.getConnectionProperty(
                                    "oracle.jdbc.provider.endUserSecurityContext"))
                            .isEqualTo("ojdbc-provider-spring-end-user-security-context");
                    assertThat(dataSource.getConnectionProperty(
                                    "oracle.jdbc.provider.endUserSecurityContext.registrationId"))
                            .isEqualTo(registrationId);
                });
    }
}
