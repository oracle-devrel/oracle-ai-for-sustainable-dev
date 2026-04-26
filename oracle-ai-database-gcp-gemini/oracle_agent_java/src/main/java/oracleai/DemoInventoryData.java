package oracleai;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DemoInventoryData {

    private static final Map<String, InventoryRiskSummary> SUMMARIES = Map.of(
            "SKU-500",
            new InventoryRiskSummary(
                    "SKU-500",
                    "Sustainable Widget 500",
                    "2026-Q3",
                    "HIGH",
                    0.72,
                    1240,
                    185000,
                    "Northeast corridor",
                    "Rebalance inventory into Newark before the next delayed inbound cycle."
            ),
            "SKU-700",
            new InventoryRiskSummary(
                    "SKU-700",
                    "Low Carbon Kit 700",
                    "2026-Q3",
                    "MEDIUM",
                    0.49,
                    780,
                    96000,
                    "Upper Midwest",
                    "Pre-stage buffer inventory in Chicago before demand spikes."
            ),
            "SKU-900",
            new InventoryRiskSummary(
                    "SKU-900",
                    "Circular Sensor 900",
                    "2026-Q3",
                    "MEDIUM",
                    0.41,
                    620,
                    87000,
                    "Southeast",
                    "Maintain current buffers but watch Gulf-port capacity."
            )
    );

    private static final Map<String, List<WarehouseHotspot>> HOTSPOTS = Map.of(
            "SKU-500",
            List.of(
                    new WarehouseHotspot(
                            "SKU-500",
                            4001,
                            "WH-101",
                            "Newark Inventory Hub",
                            "Essex",
                            "NJ",
                            "Northeast corridor",
                            40.7357,
                            -74.1724,
                            1,
                            0.86,
                            4.2,
                            410,
                            91.0,
                            620,
                            92000,
                            "CRITICAL",
                            "DESTINATION_HOTSPOT"
                    ),
                    new WarehouseHotspot(
                            "SKU-500",
                            4002,
                            "WH-202",
                            "DFW Hub",
                            "Tarrant",
                            "TX",
                            "Southern buffer",
                            32.8998,
                            -97.0403,
                            2,
                            0.31,
                            11.8,
                            120,
                            97.0,
                            180,
                            41000,
                            "BUFFER",
                            "SOURCE_BUFFER"
                    ),
                    new WarehouseHotspot(
                            "SKU-500",
                            4003,
                            "WH-303",
                            "Chicago Crossdock",
                            "Cook",
                            "IL",
                            "Midwest relay",
                            41.9742,
                            -87.9073,
                            3,
                            0.48,
                            8.1,
                            190,
                            94.0,
                            210,
                            52000,
                            "WATCH",
                            "RELAY_NODE"
                    )
            ),
            "SKU-700",
            List.of(
                    new WarehouseHotspot(
                            "SKU-700",
                            4003,
                            "WH-303",
                            "Chicago Crossdock",
                            "Cook",
                            "IL",
                            "Upper Midwest",
                            41.9742,
                            -87.9073,
                            1,
                            0.74,
                            5.0,
                            360,
                            92.0,
                            430,
                            61000,
                            "HIGH",
                            "DESTINATION_HOTSPOT"
                    ),
                    new WarehouseHotspot(
                            "SKU-700",
                            4002,
                            "WH-202",
                            "DFW Hub",
                            "Tarrant",
                            "TX",
                            "Southern buffer",
                            32.8998,
                            -97.0403,
                            2,
                            0.36,
                            10.4,
                            110,
                            98.0,
                            170,
                            22000,
                            "BUFFER",
                            "SOURCE_BUFFER"
                    ),
                    new WarehouseHotspot(
                            "SKU-700",
                            4001,
                            "WH-101",
                            "Newark Inventory Hub",
                            "Essex",
                            "NJ",
                            "Northeast",
                            40.7357,
                            -74.1724,
                            3,
                            0.29,
                            12.6,
                            90,
                            96.0,
                            140,
                            13000,
                            "WATCH",
                            "SATELLITE_NODE"
                    )
            ),
            "SKU-900",
            List.of(
                    new WarehouseHotspot(
                            "SKU-900",
                            4002,
                            "WH-202",
                            "DFW Hub",
                            "Tarrant",
                            "TX",
                            "Southeast feeder",
                            32.8998,
                            -97.0403,
                            1,
                            0.63,
                            6.5,
                            280,
                            93.0,
                            300,
                            47000,
                            "HIGH",
                            "DESTINATION_HOTSPOT"
                    ),
                    new WarehouseHotspot(
                            "SKU-900",
                            4003,
                            "WH-303",
                            "Chicago Crossdock",
                            "Cook",
                            "IL",
                            "Midwest relay",
                            41.9742,
                            -87.9073,
                            2,
                            0.34,
                            11.0,
                            115,
                            97.0,
                            150,
                            19000,
                            "BUFFER",
                            "SOURCE_BUFFER"
                    ),
                    new WarehouseHotspot(
                            "SKU-900",
                            4001,
                            "WH-101",
                            "Newark Inventory Hub",
                            "Essex",
                            "NJ",
                            "Northeast",
                            40.7357,
                            -74.1724,
                            3,
                            0.28,
                            13.2,
                            80,
                            97.0,
                            110,
                            12000,
                            "WATCH",
                            "SATELLITE_NODE"
                    )
            )
    );

    private DemoInventoryData() {
    }

    static InventoryRiskSummary summaryFor(String productId) {
        return SUMMARIES.getOrDefault(productId, SUMMARIES.get("SKU-500"));
    }

    static List<InventoryRiskSummary> topSummaries() {
        return SUMMARIES.values().stream()
                .sorted(Comparator.comparingDouble(InventoryRiskSummary::stockoutProbability).reversed())
                .toList();
    }

    static List<WarehouseHotspot> hotspotsFor(String productId) {
        return HOTSPOTS.getOrDefault(productId, HOTSPOTS.get("SKU-500"));
    }

    static Map<String, Object> spatialEvidenceFor(String productId) {
        InventoryRiskSummary summary = summaryFor(productId);
        List<WarehouseHotspot> hotspots = hotspotsFor(productId);
        WarehouseHotspot destination = hotspots.stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("DESTINATION"))
                .findFirst()
                .orElse(hotspots.get(0));
        WarehouseHotspot source = hotspots.stream()
                .filter(hotspot -> hotspot.recommendedRole().contains("SOURCE"))
                .findFirst()
                .orElse(hotspots.get(hotspots.size() - 1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("productId", summary.productId());
        result.put("hotspotRegion", summary.primaryRegion());
        result.put(
                "hotspotSummary",
                destination.warehouseName() + " in " + destination.countyName() + ", " + destination.stateCode()
                        + " is the primary hotspot (" + formatNumber(destination.hotspotScore()) + "), while "
                        + source.warehouseName() + " remains the best relief source."
        );
        result.put("recommendedSourceWarehouse", "Warehouse: " + source.warehouseName());
        result.put("recommendedDestinationWarehouse", "Warehouse: " + destination.warehouseName());
        result.put("suggestedTransferUnits", 500);
        result.put("coverageRiskDays", destination.coverageDays());
        result.put("sourceDetail", "Seeded spatial hotspot summary");
        return result;
    }

    static String fallbackShowSql(String productId, boolean regionsOnly) {
        if (regionsOnly) {
            return """
                    SELECT product_id,
                           warehouse_name,
                           county_name,
                           state_code,
                           hotspot_rank,
                           hotspot_score,
                           coverage_days,
                           revenue_impact_usd
                      FROM sc_inventory_risk_demo_v
                     WHERE product_id = '%s'
                     ORDER BY hotspot_rank
                    """.formatted(productId);
        }

        return """
                SELECT product_id,
                       product_name,
                       quarter_label,
                       risk_level,
                       stockout_probability,
                       at_risk_units,
                       projected_revenue_impact_usd,
                       primary_region
                  FROM sc_inventory_risk_summary
                 ORDER BY stockout_probability DESC
                """;
    }

    private static String formatNumber(double value) {
        return String.format("%.2f", value);
    }

    record InventoryRiskSummary(
            String productId,
            String productName,
            String quarterLabel,
            String riskLevel,
            double stockoutProbability,
            int atRiskUnits,
            int projectedRevenueImpactUsd,
            String primaryRegion,
            String recommendationSummary
    ) {}

    record WarehouseHotspot(
            String productId,
            int warehouseId,
            String warehouseCode,
            String warehouseName,
            String countyName,
            String stateCode,
            String regionName,
            double latitude,
            double longitude,
            int hotspotRank,
            double hotspotScore,
            double coverageDays,
            int backlogUnits,
            double serviceLevelPct,
            int atRiskUnits,
            int revenueImpactUsd,
            String riskLevel,
            String recommendedRole
    ) {}
}
