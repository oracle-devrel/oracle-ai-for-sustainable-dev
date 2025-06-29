// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.finance.payments.model;

public class EarnestPaymentEvent {
    private String sourceCustomer;
    private String destinationCustomer;
    private double amount;

    public EarnestPaymentEvent() {}

    public EarnestPaymentEvent(String sourceCustomer, String destinationAccountId, double amount) {
        this.sourceCustomer = sourceCustomer;
        this.destinationCustomer = destinationAccountId;
        this.amount = amount;
    }

    public String getSourceCustomer() {
        return sourceCustomer;
    }

    public void setSourceCustomer(String sourceCustomer) {
        this.sourceCustomer = sourceCustomer;
    }

    public String getDestinationCustomer() {
        return destinationCustomer;
    }

    public void setDestinationCustomer(String destinationAccountId) {
        this.destinationCustomer = destinationAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "EarnestPaymentEvent{" +
                "sourceAccountId=" + sourceCustomer +
                ", destinationAccountId=" + destinationCustomer +
                ", amount=" + amount +
                '}';
    }
}
