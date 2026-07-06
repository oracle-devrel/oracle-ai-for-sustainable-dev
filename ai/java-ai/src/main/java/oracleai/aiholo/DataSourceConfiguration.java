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
        dataSource.setUser("moviestream");
        dataSource.setPassword("Welcome12345");
        dataSource.setURL("jdbc:oracle:thin:@selectaidb_high?TNS_ADMIN=C:/Users/opc/Downloads/Wallet_SelectAIDB");
//        try (Connection connection = dataSource.getConnection()) {
//            System.out.println("✅ Successfully connected to Oracle DB: " + connection.getMetaData().getDatabaseProductVersion());
//        }
        return dataSource;
    }
}
