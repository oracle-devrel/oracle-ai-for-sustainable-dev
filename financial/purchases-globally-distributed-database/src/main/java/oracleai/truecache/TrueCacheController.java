package oracleai.truecache;

import oracleai.kafka.OrderProducerService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.oracle.okafka.clients.admin.AdminClient;
import org.oracle.okafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/financial/truecache")
//@CrossOrigin(origins = "https://oracledatabase-financial.org")
@CrossOrigin(origins = "*")
//@CrossOrigin(origins = "http://158.180.20.119")
public class TrueCacheController {

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

//TRUE CACHE.....




    

    @GetMapping("/stockticker")
    public List<Map<String, Object>> getStockTicker() {
        String sql = "SELECT TICKER, COMPANY_NAME, CURRENT_PRICE FROM FINANCIAL.STOCK_PRICES";
        List<Map<String, Object>> stocks = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> stock = new HashMap<>();
                stock.put("TICKER", rs.getString("TICKER"));
                stock.put("COMPANY_NAME", rs.getString("COMPANY_NAME"));
                stock.put("CURRENT_PRICE", rs.getBigDecimal("CURRENT_PRICE"));
                stocks.add(stock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stocks;
    }

    @PostMapping("/stockpurchase")
    public ResponseEntity<Map<String, Object>> createStockPurchase(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String insertSql = "INSERT INTO FINANCIAL.STOCK_PURCHASES (CUSTOMER_ID, TICKER, QUANTITY, PURCHASE_PRICE) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE FINANCIAL.STOCK_PRICES SET CURRENT_PRICE = CURRENT_PRICE - 1 WHERE TICKER = ?";
        Connection connection = null;
        PreparedStatement insertPs = null;
        PreparedStatement updatePs = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            Object customerId = payload.get("customerId");
            Object ticker = payload.get("ticker");
            Object quantity = payload.get("quantity");
            Object purchasePrice = payload.get("purchasePrice");

            if (customerId == null || ticker == null || quantity == null || purchasePrice == null) {
                result.put("success", false);
                result.put("message", "Missing required fields");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            // Insert purchase
            insertPs = connection.prepareStatement(insertSql);
            insertPs.setObject(1, customerId);
            insertPs.setObject(2, ticker);
            insertPs.setObject(3, quantity);
            insertPs.setObject(4, purchasePrice);
            int rows = insertPs.executeUpdate();

            // Update stock price
            updatePs = connection.prepareStatement(updateSql);
            updatePs.setObject(1, ticker);
            updatePs.executeUpdate();

            connection.commit();
            result.put("success", true);
            result.put("message", "Stock purchase recorded and price updated");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            if (connection != null) try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } finally {
            try { if (insertPs != null) insertPs.close(); } catch (Exception e) { }
            try { if (updatePs != null) updatePs.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }

    @PostMapping("/stockbuyorsell")
    public ResponseEntity<Map<String, Object>> stockBuyOrSell(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        String insertSql = "INSERT INTO FINANCIAL.STOCK_PURCHASES (CUSTOMER_ID, TICKER, QUANTITY, PURCHASE_PRICE) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE FINANCIAL.STOCK_PRICES SET CURRENT_PRICE = CURRENT_PRICE + ? WHERE TICKER = ?";
        Connection connection = null;
        PreparedStatement insertPs = null;
        PreparedStatement updatePs = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            Object customerId = payload.get("customerId");
            Object ticker = payload.get("ticker");
            Object quantityObj = payload.get("quantity");
            Object purchasePrice = payload.get("purchasePrice");
            Object actionObj = payload.get("action");

            if (customerId == null || ticker == null || quantityObj == null || purchasePrice == null || actionObj == null) {
                result.put("success", false);
                result.put("message", "Missing required fields");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            int quantity = Integer.parseInt(quantityObj.toString());
            String action = actionObj.toString();
            // If it's a sell, make the shares negative for the price update
            int sharesDelta = action.equalsIgnoreCase("sell") ? -quantity : quantity;

            // Insert purchase (quantity is always positive in the purchase record)
            insertPs = connection.prepareStatement(insertSql);
            insertPs.setObject(1, customerId);
            insertPs.setObject(2, ticker);
            insertPs.setObject(3, quantity);
            insertPs.setObject(4, purchasePrice);
            insertPs.executeUpdate();

            // Update stock price (add or subtract based on action)
            updatePs = connection.prepareStatement(updateSql);
            updatePs.setInt(1, sharesDelta);
            updatePs.setObject(2, ticker);
            updatePs.executeUpdate();

            connection.commit();
            result.put("success", true);
            result.put("message", "Stock buy/sell recorded and price updated");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            if (connection != null) try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } finally {
            try { if (insertPs != null) insertPs.close(); } catch (Exception e) { }
            try { if (updatePs != null) updatePs.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }




    
    @PostMapping("/stockinfoforcustid")
    public ResponseEntity<List<Map<String, Object>>> getStockInfoForCustomer(@RequestBody Map<String, Object> payload) {
        Object customerIdObj = payload.get("customerId");
        if (customerIdObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }
        String customerId = customerIdObj.toString();

        String sql = "SELECT sp.TICKER, sp.COMPANY_NAME, sp.CURRENT_PRICE, sp.LAST_UPDATED, " +
                     "p.PURCHASE_ID, p.QUANTITY, p.PURCHASE_PRICE, p.PURCHASE_DATE " +
                     "FROM FINANCIAL.STOCK_PURCHASES p " +
                     "JOIN FINANCIAL.STOCK_PRICES sp ON p.TICKER = sp.TICKER " +
                     "WHERE p.CUSTOMER_ID = ? " +
                     "ORDER BY p.PURCHASE_DATE DESC";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("TICKER", rs.getString("TICKER"));
                    row.put("COMPANY_NAME", rs.getString("COMPANY_NAME"));
                    row.put("CURRENT_PRICE", rs.getBigDecimal("CURRENT_PRICE"));
                    row.put("LAST_UPDATED", rs.getTimestamp("LAST_UPDATED") != null ? rs.getTimestamp("LAST_UPDATED").toString() : null);
                    row.put("PURCHASE_ID", rs.getObject("PURCHASE_ID"));
                    row.put("QUANTITY", rs.getObject("QUANTITY"));
                    row.put("PURCHASE_PRICE", rs.getBigDecimal("PURCHASE_PRICE"));
                    row.put("PURCHASE_DATE", rs.getTimestamp("PURCHASE_DATE") != null ? rs.getTimestamp("PURCHASE_DATE").toString() : null);
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
        return ResponseEntity.ok(results);
    }







}
