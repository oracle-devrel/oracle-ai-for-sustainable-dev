// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
package com.oracle.finance.payments.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverdraftController {
    private final DataSource dataSource;

    public OverdraftController(@Qualifier("overdraftDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/overdraft")
    public List<Overdraft> getOverdraft() throws SQLException {
        List<Overdraft> overdrafts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT customer_id, amount FROM ms_overdraft");
            while (rs.next()) {
                overdrafts.add(new Overdraft(rs.getString("customer_id"), rs.getDouble("amount")));
            }
        }
        return overdrafts;
    }

    public record Overdraft(String customerId, Double amount) {}
}
