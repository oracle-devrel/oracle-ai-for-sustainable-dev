package oracleai.financial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/graph")
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




    
    @PostMapping("/transfers")
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
}
