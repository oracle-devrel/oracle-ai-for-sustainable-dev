import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        Properties properties = new Properties();
        try (InputStream input = HibernateUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties in the classpath");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load application.properties", ex);
        }

        try {
            Configuration configuration = new Configuration();

            // Register annotated entity (example for transaction table)
            configuration.addAnnotatedClass(TransactionEntity.class);

            // Set Hibernate properties from the file
            configuration.setProperty("hibernate.dialect", properties.getProperty("hibernate.dialect"));
            configuration.setProperty("hibernate.hbm2ddl.auto", properties.getProperty("hibernate.hbm2ddl.auto"));
            configuration.setProperty("hibernate.show_sql", properties.getProperty("hibernate.show_sql"));
            configuration.setProperty("hibernate.format_sql", properties.getProperty("hibernate.format_sql"));

            // Use the UCP DataSource
            configuration.getStandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", DataSourceConfig.getDataSource());

            StandardServiceRegistryBuilder registryBuilder = configuration.getStandardServiceRegistryBuilder();
            registryBuilder.applySettings(configuration.getProperties());

            return configuration.buildSessionFactory(registryBuilder.build());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error building Hibernate SessionFactory", ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}