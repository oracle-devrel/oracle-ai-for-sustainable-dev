package com.oracle.demo.interactiveai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory test fixture; it is not packaged with the running application. */
final class DemoSupplyChainRepository implements SupplyChainRepository {
    private static final List<TransferRecommendation> RECOMMENDATIONS = List.of(
            recommendation(
                    "1001:102:101",
                    1001,
                    "BAT-48V",
                    "48V Solar Battery Pack",
                    102,
                    "PHX-DC",
                    101,
                    "ATL-DC",
                    20,
                    96,
                    "CRITICAL"),
            recommendation(
                    "1002:101:102",
                    1002,
                    "THERM-PRO",
                    "Smart Thermostat Pro",
                    101,
                    "ATL-DC",
                    102,
                    "PHX-DC",
                    33,
                    88,
                    "HIGH"),
            recommendation(
                    "1004:101:104",
                    1004,
                    "WATER-SENSE",
                    "Connected Water Sensor",
                    101,
                    "ATL-DC",
                    104,
                    "SEA-FC",
                    51,
                    81,
                    "HIGH"));

    private final AtomicLong transferIds = new AtomicLong(1000);
    private final List<TransferResult> transfers = new ArrayList<>();

    @Override
    public List<TransferRecommendation> findTransferRecommendations(
            double minimumStockoutRisk,
            int maximumRows) {
        InputValidation.minimumStockoutRisk(minimumStockoutRisk);
        InputValidation.maximumRows(maximumRows);
        return RECOMMENDATIONS.stream()
                .filter(recommendation ->
                        recommendation.stockoutRiskScore()
                                >= minimumStockoutRisk)
                .sorted(Comparator.comparingDouble(
                                TransferRecommendation::stockoutRiskScore)
                        .reversed())
                .limit(maximumRows)
                .toList();
    }

    @Override
    public synchronized TransferResult approveTransfer(
            TransferRecommendation recommendation,
            String approvalNotes,
            String requestedBy) {
        InputValidation.approval(
                recommendation,
                approvalNotes,
                requestedBy);
        if (RECOMMENDATIONS.stream().noneMatch(candidate ->
                candidate.recommendationId().equals(
                        recommendation.recommendationId()))) {
            throw new IllegalArgumentException(
                    "Transfer recommendation not found");
        }
        var result = new TransferResult(
                transferIds.incrementAndGet(),
                recommendation.recommendationId(),
                recommendation.recommendedTransferQuantity(),
                "APPROVED");
        transfers.add(result);
        return result;
    }

    @Override
    public synchronized int transferCount() {
        return transfers.size();
    }

    private static TransferRecommendation recommendation(
            String recommendationId,
            long productId,
            String sku,
            String productName,
            long sourceLocationId,
            String sourceLocationCode,
            long targetLocationId,
            String targetLocationCode,
            long transferQuantity,
            double riskScore,
            String riskLevel) {
        return new TransferRecommendation(
                recommendationId,
                productId,
                sku,
                productName,
                "Sustainable Infrastructure",
                sourceLocationId,
                sourceLocationCode,
                sourceLocationCode + " Distribution Center",
                targetLocationId,
                targetLocationCode,
                targetLocationCode + " Fulfillment Center",
                85,
                14,
                26,
                8,
                transferQuantity,
                transferQuantity,
                3,
                18.50,
                riskScore,
                riskLevel,
                "Transfer balances governed stockout exposure and source surplus.");
    }
}
