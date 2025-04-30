// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
package com.oracle.finance.payments.service;

public class OverdraftException extends Exception {
    private final String customerId;
    private final double amount;

    public OverdraftException(String customerId, double amount) {
        this.customerId = customerId;
        this.amount = amount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "OverdraftException{" +
                "accountId=" + customerId +
                ", amount=" + amount +
                '}';
    }
}
