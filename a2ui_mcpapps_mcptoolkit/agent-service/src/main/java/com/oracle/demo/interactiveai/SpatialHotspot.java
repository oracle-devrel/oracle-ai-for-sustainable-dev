package com.oracle.demo.interactiveai;

public record SpatialHotspot(
        long productId,
        String sku,
        long locationId,
        String locationCode,
        String locationName,
        double latitude,
        double longitude,
        double stockoutRiskScore,
        String riskLevel,
        long recommendedTransferQuantity,
        String recommendedRole) {
}
