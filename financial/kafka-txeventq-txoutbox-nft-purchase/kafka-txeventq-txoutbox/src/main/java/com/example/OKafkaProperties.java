package com.example;

import java.util.Objects;
import java.util.Properties;

public class OKafkaProperties {
    public static Properties getLocalConnectionProps(String ojdbcPropertiesFile,
                                                     Integer port)  {
        Properties producerProps = new Properties();
        // We use the default PDB for Oracle Database 23ai.
//        props.put("oracle.service.name", "freepdb1");
//        // The localhost connection uses PLAINTEXT.
//        props.put("security.protocol", "PLAINTEXT");
//        props.put("bootstrap.servers", String.format("localhost:%d",
//                Objects.requireNonNullElse(port, 1521)));
//        props.put("oracle.net.tns_admin", ojdbcPropertiesFile);



        producerProps.put("security.protocol", "SSL");
        //location containing Oracle Wallet, tnsname.ora and ojdbc.properties file...
//        producerProps.put("oracle.net.tns_admin", "/oraclefinancial/creds"); //location of ojdbc.properties file
        producerProps.put("oracle.net.tns_admin", "/Users/pparkins/Downloads/Wallet_financialdb"); //location of ojdbc.properties file
        producerProps.put("tns.alias", "financialdb_high");
        producerProps.put("enable.idempotence", "true");
        producerProps.put("oracle.transactional.producer", "true");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return producerProps;
    }
}
