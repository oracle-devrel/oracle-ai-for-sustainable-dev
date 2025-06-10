// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oracle.finance.payments.model.EarnestPaymentEvent;
import org.springframework.stereotype.Component;

@Component
public class EarnestPaymentsService {
    private static final String getBalanceQuery = """
            select balance from ms_account
            where customer_id = ?
            """;

    private static final String withdrawalQuery = """
            update ms_account set balance = balance - ?
            where customer_id = ?
            """;

    private static final String depositQuery = """
            update ms_account set balance = balance + ?
            where customer_id = ?
            """;

    private static final String insertOverdraft = """
            insert into ms_overdraft(customer_id, amount)
            values (?, ?)
            """;

    public void processEarnestPayment(Connection conn, EarnestPaymentEvent event) throws SQLException, OverdraftException {
        // Check balance of source account is valid
        Double sourceBalance = getBalance(conn, event.getSourceCustomer());
        if (sourceBalance < event.getAmount()) {
            // If the source account balance is too low, throw an OverdraftException
            throw new OverdraftException(event.getSourceCustomer(), event.getAmount());
        }

        // Withdraw from the source account, deposit to the destination account
        try (PreparedStatement withdraw = conn.prepareStatement(withdrawalQuery);
             PreparedStatement deposit = conn.prepareStatement(depositQuery)) {
            // Withdraw from the source account
            withdraw.setDouble(1, event.getAmount());
            withdraw.setString(2, event.getSourceCustomer());
            withdraw.executeUpdate();

            // Deposit to the destination account
            deposit.setDouble(1, event.getAmount());
            deposit.setString(2, event.getDestinationCustomer());
            deposit.executeUpdate();
        }
    }

    public Double getBalance(Connection conn, String customerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(getBalanceQuery)) {
            ps.setString(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.00;
    }

    public void processOverdraft(Connection conn, OverdraftException oe) throws SQLException {
        // TODO: implement overdraft logging
    }
}
