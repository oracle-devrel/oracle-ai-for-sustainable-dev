package oracleai.kafka;

import org.oracle.okafka.clients.consumer.KafkaConsumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class KafkaTopicsConfig {
    @Bean
    public List<String> kafkaTopics() {
        return List.of("INVENTORYRESP"); // or your topic(s)
    }
}