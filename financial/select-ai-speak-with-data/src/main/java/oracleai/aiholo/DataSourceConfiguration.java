package oracleai.aiholo;

import oracle.jdbc.pool.OracleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public DataSource dataSource() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setUser(getEnv("DB_USER", "moviestream"));
        dataSource.setPassword(requireEnv("DB_PASSWORD"));
        dataSource.setURL(getEnv("DB_URL", "jdbc:oracle:thin:@selectaidb_high?TNS_ADMIN=/path/to/Wallet_SelectAIDB"));
//        try (Connection connection = dataSource.getConnection()) {
//            System.out.println("✅ Successfully connected to Oracle DB: " + connection.getMetaData().getDatabaseProductVersion());
//        }
        return dataSource;
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }

    private String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
