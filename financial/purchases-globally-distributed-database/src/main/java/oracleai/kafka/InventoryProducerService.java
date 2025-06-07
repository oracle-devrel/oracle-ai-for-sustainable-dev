package oracleai.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.oracle.okafka.clients.producer.KafkaProducer;

import java.sql.Connection;


public class InventoryProducerService implements AutoCloseable {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public InventoryProducerService(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
        this.producer.initTransactions();
    }

    public void produce(String message) {
        producer.beginTransaction();
        Connection conn = producer.getDBConnection();
        System.out.println("OrderProducerService.produce message conn:" + conn);
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "defaultKey", message);
        producer.send(producerRecord);
        System.out.println("OrderProducerService.produce message sent:" + message);
        producer.commitTransaction();
        System.out.println("OrderProducerService.produce message committed:" + message);
    }


    @Override
    public void close() throws Exception {
        if (this.producer != null) {
            producer.close();
        }
    }
}
