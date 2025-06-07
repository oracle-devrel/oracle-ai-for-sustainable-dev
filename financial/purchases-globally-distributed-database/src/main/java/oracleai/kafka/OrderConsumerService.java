package oracleai.kafka;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Service
public class OrderConsumerService implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;

    // Spring will inject these beans at startup
    public OrderConsumerService(KafkaConsumer<String, String> consumer, List<String> topics) {
        this.consumer = consumer;
        this.topics = topics;
    }

    @PostConstruct
    public void startConsumerThread() {
        Thread consumerThread = new Thread(this::runConsumer);
        consumerThread.setName("OrderConsumerThread");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void runConsumer() {
        System.out.println("OrderConsumerService.runConsumer running...");
        this.consumer.subscribe(topics);
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            System.out.println("Consumed records: " + records.count());
            Connection conn = consumer.getDBConnection();
            for (ConsumerRecord<String, String> record : records) {
                System.out.println("OrderConsumerService.runConsumer conn:" + conn + " record.value():" + record.value());
            }
            consumer.commitSync();
        }
    }

    @Override
    public void close() {
        if (this.consumer != null) {
            this.consumer.close();
        }
    }
}

// --- Add this configuration class to provide the beans ---

@Configuration
class KafkaConsumerConfig {
    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        Properties properties = new Properties();
        properties.put("security.protocol", "SSL");
        //location containing Oracle Wallet, tnsname.ora and ojdbc.properties file...
        properties.put("oracle.net.tns_admin", "/oraclefinancial/creds"); //location of ojdbc.properties file
        properties.put("tns.alias", "financialdb_high");
        properties.put("enable.idempotence", "true");
        properties.put("oracle.transactional.producer", "true");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("group.id" , "MY_CONSUMER_GROUP");
        properties.put("enable.auto.commit","false");
        properties.put("max.poll.records", 2000);
        properties.put("auto.offset.reset", "earliest");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> okafkaConsumer = new KafkaConsumer<>(properties);
        return new KafkaConsumer<>(properties);
    }

    @Bean
    public List<String> kafkaTopics() {
        return List.of("NFTORDER"); // or your topic(s)
    }
}
