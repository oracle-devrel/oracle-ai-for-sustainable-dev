package com.oracle.demo.interactiveai;

import java.util.List;

public interface SupplyChainRepository {
    default boolean writesAllowed() {
        return true;
    }

    List<TransferRecommendation> findTransferRecommendations(
            double minimumStockoutRisk,
            int maximumRows);

    List<SpatialHotspot> findSpatialHotspots(String sku, int maximumRows);

    TransferResult approveTransfer(
            TransferRecommendation recommendation,
            String approvalNotes,
            String requestedBy);

    int transferCount();
}
