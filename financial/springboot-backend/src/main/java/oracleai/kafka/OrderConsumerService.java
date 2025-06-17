package oracleai.kafka;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class OrderConsumerService implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;

    public OrderConsumerService(
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
                System.out.println("OrderConsumerService.runConsumer conn:" + conn + " record.value():" + record.value()+ " type:" + type);
            }
            consumer.commitSync();
        }
    }

    public void consumeMessages() {
        consumer.subscribe(topics);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        //process records
        Connection connection = consumer.getDBConnection();
        if(somethingWentWrong) connection.rollback();
        else consumer.commitSync();
    }

    @Override
    public void close() {
        if (this.consumer != null) {
            this.consumer.close();
        }
    }
}

