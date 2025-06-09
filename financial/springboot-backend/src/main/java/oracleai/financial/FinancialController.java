package oracleai.financial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/financial")
//@CrossOrigin(origins = "https://oracledatabase-financial.org")
@CrossOrigin(origins = "*")
//@CrossOrigin(origins = "http://158.180.20.119")
public class FinancialController {

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


    @GetMapping("/locations/coordinates")
    public List<Map<String, Object>> getLocationCoordinates() {
        String sql = "SELECT LON, LAT FROM locations";
        List<Map<String, Object>> coordinates = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> coord = new HashMap<>();
                coord.put("lat", resultSet.getDouble("LAT"));
                coord.put("lng", resultSet.getDouble("LON"));
                coordinates.add(coord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    @PostMapping("/locations/check-distance")
    public ResponseEntity<Boolean> checkDistance(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, String> firstLocation = (Map<String, String>) payload.get("firstLocation");
            Map<String, String> secondLocation = (Map<String, String>) payload.get("secondLocation");

            double lat1 = Double.parseDouble(firstLocation.get("latitude"));
            double lon1 = Double.parseDouble(firstLocation.get("longitude"));
            double lat2 = Double.parseDouble(secondLocation.get("latitude"));
            double lon2 = Double.parseDouble(secondLocation.get("longitude"));

            double distanceKm = haversine(lat1, lon1, lat2, lon2);

            return ResponseEntity.ok(distanceKm > 500.0);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
    }

    /**
     * Haversine formula to calculate the great-circle distance between two points.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in km
    }




    
    @PostMapping("/createtransfer")
    public ResponseEntity<Map<String, Object>> createTransfer(@RequestBody Map<String, Object> payload) {
        System.out.println("FinancialController.createTransfer");
        String insertSql = "INSERT INTO FINANCIAL.TRANSFERS (TXN_ID, SRC_ACCT_ID, DST_ACCT_ID, AMOUNT, DESCRIPTION) VALUES (TRANSFERS_SEQ.NEXTVAL, ?, ?, ?, ?)";
        Map<String, Object> result = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSql)) {

            Object srcAcctIdObj = payload.get("srcAcctId");
            Object dstAcctIdObj = payload.get("dstAcctId");
            Object amountObj = payload.get("amount");
            Object descriptionObj = payload.get("description");

            if (srcAcctIdObj == null || dstAcctIdObj == null || amountObj == null) {
                result.put("success", false);
                result.put("message", "Missing required fields");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            ps.setObject(1, srcAcctIdObj);
            ps.setObject(2, dstAcctIdObj);
            ps.setObject(3, amountObj);
            ps.setString(4, descriptionObj != null ? descriptionObj.toString() : null);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                result.put("success", true);
                result.put("message", "Transfer inserted successfully");
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "Insert failed");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/accounts")
    public List<Map<String, Object>> getAccounts() {
        System.out.println("FinancialController.getAccounts");
        String sql = "SELECT ACCOUNT_ID, ACCOUNT_BALANCE, ACCOUNT_NAME, ACCOUNT_OPENED_DATE, ACCOUNT_OTHER_DETAILS, ACCOUNT_TYPE, CUSTOMER_ID FROM FINANCIAL.ACCOUNTS";
        List<Map<String, Object>> accounts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> account = new HashMap<>();
                account.put("ACCOUNT_ID", resultSet.getObject("ACCOUNT_ID"));
                account.put("ACCOUNT_BALANCE", resultSet.getObject("ACCOUNT_BALANCE"));
                account.put("ACCOUNT_NAME", resultSet.getObject("ACCOUNT_NAME"));
                // Convert TIMESTAMP to String for JSON serialization
                Timestamp openedDate = resultSet.getTimestamp("ACCOUNT_OPENED_DATE");
                account.put("ACCOUNT_OPENED_DATE", openedDate != null ? openedDate.toString() : null);
                account.put("ACCOUNT_OTHER_DETAILS", resultSet.getObject("ACCOUNT_OTHER_DETAILS"));
                account.put("ACCOUNT_TYPE", resultSet.getObject("ACCOUNT_TYPE"));
                account.put("CUSTOMER_ID", resultSet.getObject("CUSTOMER_ID"));
                accounts.add(account);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    @GetMapping("/transfers")
    public List<Map<String, Object>> getTransfers() {
        String sql = "SELECT TXN_ID, SRC_ACCT_ID, DST_ACCT_ID, AMOUNT, DESCRIPTION FROM FINANCIAL.TRANSFERS";
        List<Map<String, Object>> transfers = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> transfer = new HashMap<>();
                transfer.put("TXN_ID", resultSet.getObject("TXN_ID"));
                transfer.put("SRC_ACCT_ID", resultSet.getObject("SRC_ACCT_ID"));
                transfer.put("DST_ACCT_ID", resultSet.getObject("DST_ACCT_ID"));
                transfer.put("AMOUNT", resultSet.getObject("AMOUNT"));
                transfer.put("DESCRIPTION", resultSet.getObject("DESCRIPTION"));
                transfers.add(transfer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transfers;
    }

    @PostMapping("/cleartransfers")
    public ResponseEntity<Map<String, Object>> clearTransfers() {
        Map<String, Object> result = new HashMap<>();
        String sql = "DELETE FROM FINANCIAL.TRANSFERS";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            result.put("success", true);
            result.put("deleted", rows);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
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






//AI AGENTS.....




    @PostMapping("/query")
    public ResponseEntity<String> proxyQuery(@RequestBody Map<String, Object> payload) {
        String backendUrl = "http://141.148.204.74:8000/query";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    backendUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Proxy failed: " + e.getMessage() + "\"}");
        }
    }







}
