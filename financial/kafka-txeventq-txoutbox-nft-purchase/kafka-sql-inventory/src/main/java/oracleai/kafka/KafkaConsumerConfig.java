package oracleai.kafka;

import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;



// --- Add this configuration class to provide the beans ---

@Configuration
public class KafkaConsumerConfig {
//    public KafkaConsumerConfig() {
//    }

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
        properties.put("group.id", "MY_CONSUMER_GROUP");
        properties.put("enable.auto.commit", "false");
        properties.put("max.poll.records", 2000);
        properties.put("auto.offset.reset", "earliest");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> okafkaConsumer = new KafkaConsumer<String, String>(properties);
        return new KafkaConsumer<String, String>(properties);
    }

}