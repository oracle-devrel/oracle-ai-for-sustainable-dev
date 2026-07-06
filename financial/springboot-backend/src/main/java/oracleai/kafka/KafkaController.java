package oracleai.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.oracle.okafka.clients.admin.AdminClient;
import org.oracle.okafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/financial/kafka")
@CrossOrigin(origins = "*")
public class KafkaController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/testconn")
    public String testconn() throws Exception{
        System.out.println("FinancialController.testconn dataSource (about to get connection):" + dataSource);
        Connection connection = dataSource.getConnection();
        System.out.println("FinancialController.testconn connection:" + connection);
        return "connection = " + connection;
    }




    @PostMapping("/admin/create-topic")
    public ResponseEntity<Map<String, Object>> createTopicIfNotExists(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            String topicName = (String) payload.get("topicName");
            NewTopic topic = new NewTopic(topicName, 1, (short) 0);
            boolean created = createTopicIfNotExists(topic);
            result.put("message", created ? "Topic created." : "Topic already exists.");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }


    public  boolean createTopicIfNotExists(  NewTopic newTopic) throws Exception {
        try (Admin admin = AdminClient.create(getOKafkaConnectionProperties())) {
            admin.createTopics(Collections.singletonList(newTopic))
                    .all()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof TopicExistsException) {
                System.out.println(newTopic.name() + " topic already exists, skipping creation");
                return false;
            } else {
                throw new Exception(e);
            }
        }
        System.out.println(newTopic.name() + " topic created");
        return true;
    }

    private Properties getOKafkaConnectionProperties() {

        Properties props = new Properties();
        props.put("security.protocol", "SSL");
        //location containing Oracle Wallet, tnsname.ora and ojdbc.properties file...
        props.put("oracle.net.tns_admin", "/oraclefinancial/creds"); //location of ojdbc.properties file
        props.put("tns.alias", "financialdb_high");
//PLAINTEXT, container db version...
//        props.put("oracle.service.name", "freepdb1");
//        props.put("security.protocol", "PLAINTEXT");
//        props.put("bootstrap.servers", String.format("localhost:%d",
//                Objects.requireNonNullElse(1521, 1521)));
//        props.put("oracle.net.tns_admin", "ojdbc.properties");
        return props;




//        props.put("oracle.service.name", "freepdb1");
//        props.put("security.protocol", "PLAINTEXT");
//        props.put("bootstrap.servers", "localhost:1521");
//        props.put("oracle.net.tns_admin", ojbdcFilePath);
//        return props;
    }






    // --- Messaging.js endpoints ---

    @PostMapping("/orders/deleteAll")
    public ResponseEntity<String> deleteAllOrders(@RequestBody(required = false) Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("Received /orders/deleteAll POST\n");
        if (payload != null) {
            sb.append("Fields:\n");
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        sb.append("All orders deleted.");
        return ResponseEntity.ok(sb.toString());
    }

    private static boolean isOrderCalledAlready = false;

    @PostMapping("/orders/place")
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, Object> payload) {
        System.out.println("FinancialController.placeOrder orderid:" + payload.get("orderId").toString());
        System.out.println("FinancialController.placeOrder nftDrop:" + payload.get("nftDrop").toString());
        String txnCrashOption = (String) payload.get("txnCrashOption");
        String messagingOption = (String) payload.get("messagingOption");
        String orderId = payload.get("orderId") != null ? payload.get("orderId").toString() : null;
        String nft = payload.get("nftDrop") != null ? payload.get("nftDrop").toString() : null;

        // Check for the crashOrderAfterInventoryMsg scenario
        if ("crashOrderAfterInventoryMsg".equals(txnCrashOption)) {
            isOrderCalledAlready = true;
            if ("Kafka with MongoDB and Postgres".equals(messagingOption)) {
                Map<String, Object> result = new HashMap<>();
                result.put("orderId", orderId);
                result.put("nft", nft);
                result.put("status", "pending");
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "pending");
                return ResponseEntity.ok(result);
            }
        }

        // Default: Print all form fields, including messagingOption
        StringBuilder sb = new StringBuilder();
        sb.append("Received /orders/place POST\n");
        sb.append("Fields:\n");
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("Selected messagingOption: ").append(messagingOption).append("\n");
        sb.append("Order placed, sending message....");
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
        new OrderProducerService(kafkaProducer, "INVENTORYREQ").produce("test message from orderservice INVENTORYREQ");
        return ResponseEntity.ok(sb.toString());
    }

    @PostMapping("/orders/show")
    public ResponseEntity<String> showOrder(@RequestBody Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("Received /orders/show POST\n");
        sb.append("Fields:\n");
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("Order(s) shown.");
        return ResponseEntity.ok(sb.toString());
    }



//    kafka inventory

    @PostMapping("/inventory/add")
    public ResponseEntity<Map<String, Object>> addInventory(@RequestBody Map<String, Object> payload) {
        String inventoryId = (String) payload.get("nftDrop");
        Integer amount = null;
        Map<String, Object> result = new HashMap<>();
        try {
            amount = Integer.parseInt(payload.get("amount").toString());
        } catch (Exception e) {
            result.put("error", "Invalid or missing amount");
            return ResponseEntity.badRequest().body(result);
        }

        String updateSql = "UPDATE INVENTORY SET inventorycount = inventorycount + ? WHERE inventoryid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setInt(1, amount);
            ps.setString(2, inventoryId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                result.put("message", "Inventory updated.");
                result.put("rowsAffected", rows);
                return ResponseEntity.ok(result);
            } else {
                result.put("error", "Inventory ID not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/inventory/remove")
    public ResponseEntity<Map<String, Object>> removeInventory(@RequestBody Map<String, Object> payload) {
        String inventoryId = (String) payload.get("nftDrop");
        Integer amount = null;
        Map<String, Object> result = new HashMap<>();
        try {
            amount = Integer.parseInt(payload.get("amount").toString());
        } catch (Exception e) {
            result.put("error", "Invalid or missing amount");
            return ResponseEntity.badRequest().body(result);
        }

        String updateSql = "UPDATE INVENTORY SET inventorycount = inventorycount - ? WHERE inventoryid = ? AND inventorycount >= ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setInt(1, amount);
            ps.setString(2, inventoryId);
            ps.setInt(3, amount);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                result.put("message", "Inventory removed.");
                result.put("rowsAffected", rows);
                return ResponseEntity.ok(result);
            } else {
                result.put("error", "Not enough inventory or inventory not found.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/inventory/get")
    public ResponseEntity<Map<String, Object>> getInventory(@RequestBody Map<String, Object> payload) {
        String inventoryId = (String) payload.get("nftDrop");
        String selectSql = "SELECT inventoryid, inventorylocation, inventorycount FROM INVENTORY WHERE inventoryid = ?";
        Map<String, Object> result = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, inventoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("inventoryid", rs.getString("inventoryid"));
                    result.put("inventorylocation", rs.getString("inventorylocation"));
                    result.put("inventorycount", rs.getInt("inventorycount"));
                    return ResponseEntity.ok(result);
                } else {
                    result.put("error", "Inventory not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }



}
