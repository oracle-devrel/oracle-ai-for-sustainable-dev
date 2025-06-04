package oracleai.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.oracle.okafka.clients.admin.AdminClient;

import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class AdminUtil {
    public static boolean createTopicIfNotExists(  NewTopic newTopic) {
        Properties props = new Properties();
        props.put("security.protocol", "SSL");
        //location containing Oracle Wallet, tnsname.ora and ojdbc.properties file...
        props.put("oracle.net.tns_admin", "/oraclefinancial/creds");
        props.put("tns.alias", "financialdb_high");
//PLAINTEXT, container db version...
//        props.put("oracle.service.name", "freepdb1");
//        props.put("security.protocol", "PLAINTEXT");
//        props.put("bootstrap.servers", String.format("localhost:%d",
//                Objects.requireNonNullElse(1521, 1521)));
        props.put("oracle.net.tns_admin", "ojdbcPropertiesFile");
        try (Admin admin = AdminClient.create(props)) {
            admin.createTopics(Collections.singletonList(newTopic))
                    .all()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof TopicExistsException) {
                System.out.println("Topic already exists, skipping creation");
            } else {
//                throw new RuntimeException(e);
                return false;
            }
        }
        return true;
    }

}
