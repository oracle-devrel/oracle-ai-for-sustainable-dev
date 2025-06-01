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
public class GraphController {

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
}
