package com.oracle.demo.interactiveai;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder(value.length() + 16).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    public static String value(Object value) {
        if (value == null) return "null";
        if (value instanceof String string) return quote(string);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> quote(String.valueOf(entry.getKey()))
                            + ":" + value(entry.getValue()))
                    .reduce(
                            "{",
                            (left, item) -> left.equals("{")
                                    ? left + item
                                    : left + "," + item)
                    + "}";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Json::value)
                    .reduce(
                            "[",
                            (left, item) -> left.equals("[")
                                    ? left + item
                                    : left + "," + item)
                    + "]";
        }
        return quote(value.toString());
    }

    public static String recommendation(TransferRecommendation recommendation) {
        return value(recommendationMap(recommendation));
    }

    public static Map<String, Object> recommendationMap(
            TransferRecommendation recommendation) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("recommendationId", recommendation.recommendationId());
        row.put("productId", recommendation.productId());
        row.put("sku", recommendation.sku());
        row.put("productName", recommendation.productName());
        row.put("categoryName", recommendation.categoryName());
        row.put("sourceLocationId", recommendation.sourceLocationId());
        row.put("sourceLocationCode", recommendation.sourceLocationCode());
        row.put("sourceLocationName", recommendation.sourceLocationName());
        row.put("targetLocationId", recommendation.targetLocationId());
        row.put("targetLocationCode", recommendation.targetLocationCode());
        row.put("targetLocationName", recommendation.targetLocationName());
        row.put("sourceAvailableQuantity", recommendation.sourceAvailableQuantity());
        row.put("targetAvailableQuantity", recommendation.targetAvailableQuantity());
        row.put("forecast7dQuantity", recommendation.forecast7dQuantity());
        row.put("safetyStockQuantity", recommendation.safetyStockQuantity());
        row.put("shortageQuantity", recommendation.shortageQuantity());
        row.put(
                "recommendedTransferQuantity",
                recommendation.recommendedTransferQuantity());
        row.put("transitDays", recommendation.transitDays());
        row.put("unitTransferCost", recommendation.unitTransferCost());
        row.put("stockoutRiskScore", recommendation.stockoutRiskScore());
        row.put("riskLevel", recommendation.riskLevel());
        row.put("rationale", recommendation.rationale());
        return Map.copyOf(row);
    }
}
