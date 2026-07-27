package com.oracle.demo.interactiveai;

import java.util.List;

public interface SupplyChainRepository {
    List<TransferRecommendation> findTransferRecommendations(
            double minimumStockoutRisk,
            int maximumRows);

    TransferResult approveTransfer(
            TransferRecommendation recommendation,
            String approvalNotes,
            String requestedBy);

    int transferCount();
}
