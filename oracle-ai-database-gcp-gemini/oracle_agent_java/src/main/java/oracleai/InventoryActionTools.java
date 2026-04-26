package oracleai;

import com.google.adk.tools.Annotations.Schema;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class InventoryActionTools {

    private final Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies;
    private final SpatialTools spatialTools;

    public InventoryActionTools(
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            SpatialTools spatialTools
    ) {
        this.getSupplyChainDependencies = getSupplyChainDependencies;
        this.spatialTools = spatialTools;
    }

    @Schema(
            name = "getGraphEvidence",
            description = "Look up Oracle supply-chain dependency evidence for a product and summarize the supplier path, alert, and warehouse involved."
    )
    public Map<String, Object> getGraphEvidence(
            @Schema(name = "productId", description = "The product identifier, such as SKU-500.", optional = false)
            String productId
    ) {
        String normalizedProductId = normalizeProductId(productId);
        try {
            GraphTools.GraphResponse response = getSupplyChainDependencies.apply(
                    new GraphTools.GraphRequest(normalizedProductId, null, false, null)
            );

            Map<String, String> alertNode = firstNodeOfType(response.nodes(), "ALERT");
            Map<String, String> warehouseNode = firstNodeOfType(response.nodes(), "WAREHOUSE");
            Map<String, Object> result = orderedMap();
            result.put("status", "ok");
            result.put("productId", response.productId());
            result.put("dependencyPath", dependencyPath(response.nodes()));
            result.put("warehouse", warehouseNode.getOrDefault("label", "Warehouse unavailable"));
            result.put("warehouseMetric", warehouseNode.getOrDefault("metric", ""));
            result.put("activeAlert", alertNode.getOrDefault("label", "Alert unavailable"));
            result.put("alertDetail", alertNode.getOrDefault("detail", ""));
            result.put("alertMetric", alertNode.getOrDefault("metric", ""));
            result.put("sourceMode", response.sourceMode());
            result.put("sourceDetail", response.sourceDetail());
            return result;
        } catch (Exception exception) {
            Map<String, Object> result = orderedMap();
            result.put("status", "unavailable");
            result.put("productId", normalizedProductId);
            result.put("message", exception.getMessage());
            return result;
        }
    }

    @Schema(
            name = "getSpatialEvidence",
            description = "Return a seeded hotspot summary for a product, including suggested source and destination warehouses for a balancing move."
    )
    public Map<String, Object> getSpatialEvidence(
            @Schema(name = "productId", description = "The product identifier, such as SKU-500.", optional = false)
            String productId
    ) {
        String normalizedProductId = normalizeProductId(productId);
        return spatialTools.spatialEvidenceFor(normalizedProductId);
    }

    @Schema(
            name = "getExternalSignals",
            description = "Return a seeded external-signals summary, such as weather or geopolitical risk, for a product's supply lane."
    )
    public Map<String, Object> getExternalSignals(
            @Schema(name = "productId", description = "The product identifier, such as SKU-500.", optional = false)
            String productId
    ) {
        String normalizedProductId = normalizeProductId(productId);
        ExternalSignal signal = externalSignalFor(normalizedProductId);
        Map<String, Object> result = orderedMap();
        result.put("status", "ok");
        result.put("productId", normalizedProductId);
        result.put("signalName", signal.signalName());
        result.put("signalSummary", signal.signalSummary());
        result.put("riskLevel", signal.riskLevel());
        result.put("recommendedLeadTimeAction", signal.recommendedLeadTimeAction());
        result.put("observedDate", LocalDate.now().toString());
        result.put("sourceDetail", "Seeded external signal summary");
        return result;
    }

    @Schema(
            name = "checkTransferPolicy",
            description = "Check whether a proposed inventory move is allowed immediately or requires approval based on unit volume and route choice."
    )
    public Map<String, Object> checkTransferPolicy(
            @Schema(name = "productId", description = "The product identifier, such as SKU-500.", optional = false)
            String productId,
            @Schema(name = "sourceWarehouse", description = "Warehouse shipping inventory out.", optional = false)
            String sourceWarehouse,
            @Schema(name = "destinationWarehouse", description = "Warehouse receiving inventory.", optional = false)
            String destinationWarehouse,
            @Schema(name = "units", description = "Number of units to transfer.", optional = false)
            Integer units,
            @Schema(name = "reason", description = "Why the move is being proposed.", optional = false)
            String reason
    ) {
        String normalizedProductId = normalizeProductId(productId);
        int normalizedUnits = units == null ? 0 : units;
        boolean sameWarehouse = normalizeLabel(sourceWarehouse).equalsIgnoreCase(normalizeLabel(destinationWarehouse));
        boolean requiresApproval = normalizedUnits >= 400 || sameWarehouse;
        boolean allowed = !sameWarehouse && normalizedUnits > 0;

        Map<String, Object> result = orderedMap();
        result.put("status", allowed ? "ok" : "blocked");
        result.put("productId", normalizedProductId);
        result.put("allowed", allowed);
        result.put("requiresApproval", requiresApproval);
        result.put("policySummary", allowed
                ? (requiresApproval
                ? "Transfer can be drafted but requires approval before execution."
                : "Transfer can be drafted immediately with standard review.")
                : "Transfer is blocked because source and destination warehouses are the same or units are missing.");
        result.put("unitsThresholdForApproval", 400);
        result.put("reason", valueOrDefault(reason, "No reason supplied"));
        return result;
    }

    @Schema(
            name = "draftInventoryTransferAction",
            description = "Create a draft inventory-transfer action recommendation. This does not execute the move."
    )
    public Map<String, Object> draftInventoryTransferAction(
            @Schema(name = "productId", description = "The product identifier, such as SKU-500.", optional = false)
            String productId,
            @Schema(name = "sourceWarehouse", description = "Warehouse shipping inventory out.", optional = false)
            String sourceWarehouse,
            @Schema(name = "destinationWarehouse", description = "Warehouse receiving inventory.", optional = false)
            String destinationWarehouse,
            @Schema(name = "units", description = "Number of units to transfer.", optional = false)
            Integer units,
            @Schema(name = "reason", description = "Why the move is being proposed.", optional = false)
            String reason
    ) {
        String normalizedProductId = normalizeProductId(productId);
        int normalizedUnits = units == null ? 0 : units;
        Map<String, Object> result = orderedMap();
        result.put("status", "drafted");
        result.put("actionType", "INVENTORY_TRANSFER");
        result.put("draftActionId", "draft-transfer-" + normalizedProductId.toLowerCase());
        result.put("productId", normalizedProductId);
        result.put("sourceWarehouse", valueOrDefault(sourceWarehouse, "Unknown source"));
        result.put("destinationWarehouse", valueOrDefault(destinationWarehouse, "Unknown destination"));
        result.put("units", normalizedUnits);
        result.put("reason", valueOrDefault(reason, "No reason supplied"));
        result.put("executionState", "NOT_EXECUTED");
        result.put("approvalState", normalizedUnits >= 400 ? "PENDING_APPROVAL" : "STANDARD_REVIEW");
        return result;
    }

    private static String dependencyPath(List<Map<String, String>> nodes) {
        return nodes.stream()
                .map(node -> node.getOrDefault("label", "Unknown"))
                .filter(label -> !label.isBlank())
                .reduce((left, right) -> left + " -> " + right)
                .orElse("No dependency path available");
    }

    private static Map<String, String> firstNodeOfType(List<Map<String, String>> nodes, String type) {
        return nodes.stream()
                .filter(node -> type.equalsIgnoreCase(node.getOrDefault("type", "")))
                .findFirst()
                .orElse(Map.of());
    }

    private static String normalizeProductId(String productId) {
        String normalized = valueOrDefault(productId, "SKU-500").trim().toUpperCase();
        return normalized.isBlank() ? "SKU-500" : normalized;
    }

    private static String normalizeLabel(String value) {
        return value == null ? "" : value.trim();
    }

    private static <K, V> Map<K, V> orderedMap() {
        return new LinkedHashMap<>();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static ExternalSignal externalSignalFor(String productId) {
        return switch (productId) {
            case "SKU-600" -> new ExternalSignal(
                    "Rail congestion watch",
                    "Midwest rail congestion is extending inbound replenishment by roughly 18 hours.",
                    "medium",
                    "Shift transfer timing forward by one day."
            );
            case "SKU-700" -> new ExternalSignal(
                    "Port labor caution",
                    "Northeast port labor uncertainty could delay replenishment windows next week.",
                    "medium",
                    "Pre-stage transfer inventory before the next inbound delay window."
            );
            default -> new ExternalSignal(
                    "Weather delay",
                    "Pacific lane weather is raising delay risk on the inbound route that feeds Newark.",
                    "high",
                    "Prioritize an internal transfer before the next external replenishment cycle."
            );
        };
    }

    private record ExternalSignal(
            String signalName,
            String signalSummary,
            String riskLevel,
            String recommendedLeadTimeAction
    ) {}
}
