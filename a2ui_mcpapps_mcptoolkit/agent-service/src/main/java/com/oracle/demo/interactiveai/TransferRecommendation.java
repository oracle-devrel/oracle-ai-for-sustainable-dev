package com.oracle.demo.interactiveai;

public record TransferRecommendation(
        String recommendationId,
        long productId,
        String sku,
        String productName,
        String categoryName,
        long sourceLocationId,
        String sourceLocationCode,
        String sourceLocationName,
        long targetLocationId,
        String targetLocationCode,
        String targetLocationName,
        long sourceAvailableQuantity,
        long targetAvailableQuantity,
        long forecast7dQuantity,
        long safetyStockQuantity,
        long shortageQuantity,
        long recommendedTransferQuantity,
        int transitDays,
        double unitTransferCost,
        double stockoutRiskScore,
        String riskLevel,
        String rationale) {
}
