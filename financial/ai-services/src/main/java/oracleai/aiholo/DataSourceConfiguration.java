package oracleai.aiholo;

import oracle.jdbc.pool.OracleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public DataSource dataSource() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
//        dataSource.setUser("moviestream");
        dataSource.setUser("financial");
        dataSource.setPassword("Welcome12345");
        dataSource.setURL("jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/Users/pparkins/Downloads/Wallet_financialdb");
//        try (Connection connection = dataSource.getConnection()) {
//            System.out.println("✅ Successfully connected to Oracle DB: " + connection.getMetaData().getDatabaseProductVersion());
//        }
        return dataSource;
    }
}
