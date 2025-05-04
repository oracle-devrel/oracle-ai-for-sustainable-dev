package oracleai;

import oracle.jdbc.driver.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class DataSourceConfig {
    private static PoolDataSource dataSource;
    static {
        Properties properties = new Properties();
        try (InputStream input = DataSourceConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties in the classpath");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load application.properties", ex);
        }
        try {
            dataSource = PoolDataSourceFactory.getPoolDataSource();
            dataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            dataSource.setURL(properties.getProperty("oracle.url"));
            dataSource.setUser(properties.getProperty("oracle.username"));
            dataSource.setPassword(properties.getProperty("oracle.password"));
            // Pool settings
            dataSource.setInitialPoolSize(Integer.parseInt(properties.getProperty("oracle.initialPoolSize")));
            dataSource.setMinPoolSize(Integer.parseInt(properties.getProperty("oracle.minPoolSize")));
            dataSource.setMaxPoolSize(Integer.parseInt(properties.getProperty("oracle.maxPoolSize")));
            dataSource.setFastConnectionFailoverEnabled(true);
            dataSource.setValidateConnectionOnBorrow(true);
            dataSource.setConnectionProperty("oracle.jdbc.implicitStatementCacheSize", "10"); //Oracle JDBC thin driver provides an implicit statement cache that will take care of optimizing how statements and cursors are allocated and freed
            dataSource.setConnectionProperty("oracle.jdbc.useShardingDriverConnection", "true"); // sharding driver property
            dataSource.setConnectionProperty("oracle.jdbc.defaultConnectionValidation", "SOCKET"); // Oracle recommends setting a lightweight connection validation property oracle.jdbc.defaultConnectionValidation to "LOCAL" or "SOCKET" (SOCKET has a small performance penalty while LOCAL might limit the driver's ability to detect the outage) to mitigate the impact:
            dataSource.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_AUTOCOMMIT, "false"); // set auto-commit property
            dataSource.setConnectionProperty("oracle.jdbc.allowSingleShardTransactionSupport", "true");//This property is used to allow single shard transaction support when using sharding datasource. When the property value is set to "true", transactions can be started on direct shard,

            System.out.println("UCP DataSource initialized successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing UCP DataSource", e);
        }
    }
    public static DataSource getDataSource() {
        return dataSource;
    }
}









