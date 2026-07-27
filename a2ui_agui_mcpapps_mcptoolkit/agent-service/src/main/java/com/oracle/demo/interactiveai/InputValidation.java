package com.oracle.demo.interactiveai;

public final class InputValidation {
    private InputValidation() {
    }

    public static void minimumStockoutRisk(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException(
                    "minimumStockoutRisk must be between 0 and 100");
        }
    }

    public static void maximumRows(int value) {
        if (value < 1 || value > 50) {
            throw new IllegalArgumentException("maximumRows must be between 1 and 50");
        }
    }

    public static void approval(
            TransferRecommendation recommendation,
            String notes,
            String requestedBy) {
        if (recommendation == null) {
            throw new IllegalArgumentException("A governed transfer recommendation is required");
        }
        if (recommendation.productId() < 1
                || recommendation.sourceLocationId() < 1
                || recommendation.targetLocationId() < 1
                || recommendation.sourceLocationId() == recommendation.targetLocationId()) {
            throw new IllegalArgumentException(
                    "Recommendation has invalid product or location identifiers");
        }
        if (recommendation.recommendedTransferQuantity() < 1) {
            throw new IllegalArgumentException(
                    "Recommended transfer quantity must be positive");
        }
        if (notes == null || notes.trim().length() < 3 || notes.length() > 2000) {
            throw new IllegalArgumentException(
                    "approvalNotes must contain 3 to 2000 characters");
        }
        if (requestedBy == null
                || requestedBy.trim().length() < 3
                || requestedBy.length() > 200) {
            throw new IllegalArgumentException(
                    "requestedBy must contain 3 to 200 characters");
        }
    }
}
