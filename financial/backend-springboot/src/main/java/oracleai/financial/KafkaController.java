package oracleai.financial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kafka")
@CrossOrigin(origins = "https://oracledatabase-financial.org")
public class KafkaController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/transfer")
    public String transfer() {
        return "Rollback successful";
    }

    // CREATE: Add a new account
    @PostMapping("/accounts")
    public String createAccount(@RequestBody Map<String, Object> accountData) {
        System.out.println("FinancialController.accounts createAccount");
        String sql = """
                INSERT INTO accounts (account_id, name, official_name, type, subtype, mask, available_balance, current_balance, limit_balance, verification_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, (String) accountData.get("account_id"));
            preparedStatement.setString(2, (String) accountData.get("name"));
            preparedStatement.setString(3, (String) accountData.get("official_name"));
            preparedStatement.setString(4, (String) accountData.get("type"));
            preparedStatement.setString(5, (String) accountData.get("subtype"));
            preparedStatement.setString(6, (String) accountData.get("mask"));
            preparedStatement.setObject(7, accountData.get("available_balance"));
            preparedStatement.setObject(8, accountData.get("current_balance"));
            preparedStatement.setObject(9, accountData.get("limit_balance"));
            preparedStatement.setString(10, (String) accountData.get("verification_status"));

            int rowsInserted = preparedStatement.executeUpdate();
            return rowsInserted > 0 ? "Account created successfully!" : "Failed to create account.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // READ: Get all accounts
    @GetMapping("/accounts")
    public List<Map<String, Object>> getAllAccounts() {
        System.out.println("FinancialController.accounts getAllAccounts");
        String sql = "SELECT * FROM accounts";
        List<Map<String, Object>> accounts = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> account = new HashMap<>();
                account.put("account_id", resultSet.getString("account_id"));
                account.put("name", resultSet.getString("name"));
                account.put("official_name", resultSet.getString("official_name"));
                account.put("type", resultSet.getString("type"));
                account.put("subtype", resultSet.getString("subtype"));
                account.put("mask", resultSet.getString("mask"));
                account.put("available_balance", resultSet.getBigDecimal("available_balance"));
                account.put("current_balance", resultSet.getBigDecimal("current_balance"));
                account.put("limit_balance", resultSet.getBigDecimal("limit_balance"));
                account.put("verification_status", resultSet.getString("verification_status"));
                accounts.add(account);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    // READ: Get a single account by ID
    @GetMapping("/accounts/{id}")
    public Map<String, Object> getAccountById(@PathVariable("id") String accountId) {
        System.out.println("FinancialController.accounts getAccountById accountId:" + accountId);
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        Map<String, Object> account = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, accountId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    account.put("account_id", resultSet.getString("account_id"));
                    account.put("name", resultSet.getString("name"));
                    account.put("official_name", resultSet.getString("official_name"));
                    account.put("type", resultSet.getString("type"));
                    account.put("subtype", resultSet.getString("subtype"));
                    account.put("mask", resultSet.getString("mask"));
                    account.put("available_balance", resultSet.getBigDecimal("available_balance"));
                    account.put("current_balance", resultSet.getBigDecimal("current_balance"));
                    account.put("limit_balance", resultSet.getBigDecimal("limit_balance"));
                    account.put("verification_status", resultSet.getString("verification_status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return account;
    }

    // UPDATE: Update an account by ID
    @PutMapping("/accounts/{id}")
    public String updateAccount(@PathVariable("id") String accountId, @RequestBody Map<String, Object> accountData) {
        System.out.println("FinancialController.accounts updateAccount accountId:" + accountId);
        String sql = """
                UPDATE accounts
                SET name = ?, official_name = ?, type = ?, subtype = ?, mask = ?, available_balance = ?, current_balance = ?, limit_balance = ?, verification_status = ?
                WHERE account_id = ?
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, (String) accountData.get("name"));
            preparedStatement.setString(2, (String) accountData.get("official_name"));
            preparedStatement.setString(3, (String) accountData.get("type"));
            preparedStatement.setString(4, (String) accountData.get("subtype"));
            preparedStatement.setString(5, (String) accountData.get("mask"));
            preparedStatement.setObject(6, accountData.get("available_balance"));
            preparedStatement.setObject(7, accountData.get("current_balance"));
            preparedStatement.setObject(8, accountData.get("limit_balance"));
            preparedStatement.setString(9, (String) accountData.get("verification_status"));
            preparedStatement.setString(10, accountId);

            int rowsUpdated = preparedStatement.executeUpdate();
            return rowsUpdated > 0 ? "Account updated successfully!" : "Failed to update account.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // DELETE: Delete an account by ID
    @DeleteMapping("/accounts/{id}")
    public String deleteAccount(@PathVariable("id") String accountId) {
        System.out.println("FinancialController.accounts deleteAccount accountId:" + accountId);
        String sql = "DELETE FROM accounts WHERE account_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, accountId);
            int rowsDeleted = preparedStatement.executeUpdate();
            return rowsDeleted > 0 ? "Account deleted successfully!" : "Failed to delete account.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
