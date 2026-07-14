package com.oracle.finance.payments;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.finance.payments.consumer.EarnestPaymentsConsumerFactory;
import com.oracle.finance.payments.model.EarnestPaymentEvent;
import javax.sql.DataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.oracle.okafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.MountableFile;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
public class EarnestPaymentsAppTest {
    private static final String oracleImage = "gvenzl/oracle-free:23.7-slim-faststart";
    private static final String testUser = "testuser";
    private static final String testPassword = "Welcome123#";

    // Configure an Oracle Database Free container for the test
    @Container
    @ServiceConnection
    private static final OracleContainer oracleContainer = new OracleContainer(oracleImage)
            .withStartupTimeout(Duration.ofMinutes(3)) // allow possible slow startup
            .withInitScripts("init.sql", "insert-test-data.sql")
            .withUsername(testUser)
            .withPassword(testPassword);

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        // Configure Spring Properties for OKafka
        registry.add("okafka.bootstrapServers", EarnestPaymentsAppTest::bootstrapServers);
        registry.add("okafka.ojdbcPath", EarnestPaymentsAppTest::ojdbcPath);
        registry.add("okafka.securityProtocol", () -> "PLAINTEXT");
    }

    @BeforeAll
    static void setUp() throws Exception {
        // Configure the Oracle Database container with the TxEventQ test user
        oracleContainer.start();
        oracleContainer.copyFileToContainer(MountableFile.forClasspathResource("user-grants.sql"), "/tmp/init.sql");
        oracleContainer.execInContainer("sqlplus", "sys / as sysdba", "@/tmp/init.sql");
    }

    @Autowired
    EarnestPaymentsConsumerFactory consumerFactory;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void processPayments() throws Exception {
        Producer<String, byte[]> producer = getProducer();

        // Send an earnest payment event with a valid payment amount
        sendEvent(producer, new EarnestPaymentEvent(
                "CUST0001",
                "CUST0003",
                500.00
        ));

        // Send an earnest payment event with an invalid payment amount
        sendEvent(producer, new EarnestPaymentEvent(
                "CUST0002",
                "CUST0004",
                10000.00
        ));

        Thread.sleep(5000);
        DataSource ds = getDataSource();
        // Verify earnest payment was processed
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            try (Connection connection = ds.getConnection();
                 Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("select balance from ms_account where customer_id = 'CUST0001'");
                if (!rs.next()) {
                    return false;
                }
                return rs.getDouble(1) == 500.00;
            }
        });

        // Verify overdraft was logged
        // Uncomment if overdraft logging is enabled
//        await().atMost(Duration.ofSeconds(10)).until(() -> {
//            try (Connection connection = ds.getConnection();
//                 Statement stmt = connection.createStatement()) {
//                ResultSet rs = stmt.executeQuery("select amount from ms_overdraft where customer_id = 'CUST0002'");
//                if (!rs.next()) {
//                    return false;
//                }
//                return rs.getDouble(1) == 10000.00;
//            }
//        });

        consumerFactory.destroy();
    }

    private void sendEvent(Producer<String, byte[]> producer, EarnestPaymentEvent event) {
        try {
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    "earnest_payment",
                    objectMapper.writeValueAsBytes(event)
            );
            producer.send(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private static Producer<String, byte[]> getProducer() {
        Properties props = new Properties();
        props.put("oracle.service.name", "freepdb1");
        props.put("security.protocol", "PLAINTEXT");
        props.put("bootstrap.servers", bootstrapServers());
        // If using Oracle Database wallet, pass wallet directory
        props.put("oracle.net.tns_admin", ojdbcPath());
        Serializer<String> keySerializer = new StringSerializer();
        Serializer<byte[]> valueSerializer = new ByteArraySerializer();
        return new KafkaProducer<>(props, keySerializer, valueSerializer);
    }

    private static String ojdbcPath() {
        return new File("src/test/resources").getAbsolutePath();
    }

    private static String bootstrapServers() {
        return "localhost:" + oracleContainer.getOraclePort();
    }

    private static DataSource getDataSource() throws SQLException {
        PoolDataSource ds = PoolDataSourceFactory.getPoolDataSource();
        ds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        ds.setConnectionPoolName(UUID.randomUUID().toString());
        ds.setURL(oracleContainer.getJdbcUrl());
        ds.setUser(oracleContainer.getUsername());
        ds.setPassword(oracleContainer.getPassword());
        ds.setConnectionPoolName(UUID.randomUUID().toString());
        ds.setMaxPoolSize(30);
        ds.setInitialPoolSize(10);
        ds.setMinPoolSize(1);

        return ds;
    }
}
