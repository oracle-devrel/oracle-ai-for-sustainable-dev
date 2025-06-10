package oracleai.kafka;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.oracle.okafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

@Service
public class InventoryConsumerService implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;

    public InventoryConsumerService(
        KafkaConsumer<String, String> consumer,
        List<String> topics // No @Qualifier needed
    ) {
        this.consumer = consumer;
        this.topics = topics;
    }

    @PostConstruct
    public void startConsumerThread() {
        Thread consumerThread = new Thread(this::runConsumer);
        consumerThread.setName("InventoryConsumerThread");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void runConsumer() {
        System.out.println("InventoryConsumerService.runConsumer running...");
        this.consumer.subscribe(topics);
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            if (records.count()>0) System.out.println("Consumed records (Inventory Service receiving order messages): " + records.count());
            Connection conn = consumer.getDBConnection();
            for (ConsumerRecord<String, String> record : records) {
                String type = null;
                if (record.headers() != null) {
                    Header typeHeader = record.headers().lastHeader("type");
                    if (typeHeader != null) {
                        type = new String(typeHeader.value(), StandardCharsets.UTF_8);
                    }
                }
//                if ("inventory".equals(type)) {
                    // process message
                System.out.println("OrderConsumerService.runConsumer conn:" + conn + " record.value():" + record.value());
//                }
                Properties properties  = new Properties();
                properties.put("security.protocol", "SSL");
                //location containing Oracle Wallet, tnsname.ora and ojdbc.properties file...
                properties.put("oracle.net.tns_admin", "/oraclefinancial/creds"); //location of ojdbc.properties file
                properties.put("tns.alias", "financialdb_high");
                properties.put("enable.idempotence", "true");
                properties.put("oracle.transactional.producer", "true");
                properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
                new InventoryProducerService(kafkaProducer, "INVENTORYRESP").produce("test message from inventoryservice INVENTORYRESP");
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

