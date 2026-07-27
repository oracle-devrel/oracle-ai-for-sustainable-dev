package com.oracle.demo.interactiveai;

public record TransferResult(
        long transferId,
        String recommendationId,
        long transferQuantity,
        String status) {
}
