/*
 
 **
 ** Copyright (c) 2021 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */
package io.helidon.data.examples;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderDAO {
    private final String tableName = "orders_json";

    public Order get(Connection conn, String id) throws SQLException {
        String sql = "SELECT order_data FROM " + tableName + " WHERE orderid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String jsonData = rs.getString("order_data");
                    return JsonUtils.read(jsonData, Order.class);
                }
            }
        }
        return null;
    }

    public Order create(Connection conn, Order order) throws SQLException {
        // Create table if it doesn't exist
        createTableIfNotExists(conn);

        String sql = "INSERT INTO " + tableName + " (orderid, order_data) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.getOrderid());
            stmt.setString(2, JsonUtils.writeValueAsString(order));
            stmt.executeUpdate();
        }
        System.out.println("Created order:" + order);
        return order;
    }

    public void update(Connection conn, Order order) throws SQLException {
        String sql = "UPDATE " + tableName + " SET order_data = ? WHERE orderid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, JsonUtils.writeValueAsString(order));
            stmt.setString(2, order.getOrderid());
            stmt.executeUpdate();
        }
        System.out.println("Updated order:" + order);
    }

    public String drop(Connection conn) throws SQLException {
        String sql = "DROP TABLE " + tableName;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        return tableName + " dropped";
    }

    public int delete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE orderid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate();
        }
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName
                + " (orderid VARCHAR2(100) PRIMARY KEY, order_data JSON)";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}