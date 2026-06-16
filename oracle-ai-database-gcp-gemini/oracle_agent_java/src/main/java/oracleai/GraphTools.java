package oracleai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.env.Environment;

@Configuration
public class GraphTools {

    private static final String DEFAULT_PRODUCT_ID = "SKU-500";
    private static final String PAYLOAD_SCHEMA_VERSION = "1.0";
    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "SUPPLIER",
            "PLANT",
            "PORT",
            "WAREHOUSE",
            "PRODUCT",
            "ALERT"
    );
    private static final Set<String> ALLOWED_EDGE_LABELS = Set.of(
            "SUPPLIES",
            "SHIPS_VIA",
            "ROUTES_TO",
            "STOCKS",
            "AFFECTS"
    );
    private static final String DATABASE_GRAPH_QUERY = """
            WITH dependency_path AS (
                SELECT
                    supplier_name,
                    tier_level,
                    supplier_region,
                    on_time_pct,
                    plant_name,
                    cycle_days,
                    utilization_pct,
                    port_id,
                    port_name,
                    eta_hours,
                    delay_risk_score,
                    warehouse_name,
                    inventory_units,
                    fill_rate_pct,
                    product_id,
                    demand_change_pct,
                    margin_pct
                FROM GRAPH_TABLE (
                    supply_chain_graph
                    MATCH
                        (s IS supplier) -[e1 IS supplies]-> (p IS plant)
                                        -[e2 IS ships_via]-> (po IS port)
                                        -[e3 IS routes_to]-> (w IS warehouse)
                                        -[e4 IS stocks]-> (pr IS product)
                    WHERE pr.product_id = ?
                      AND s.active_flag = 'Y'
                      AND p.active_flag = 'Y'
                      AND po.active_flag = 'Y'
                      AND w.active_flag = 'Y'
                      AND pr.active_flag = 'Y'
                      AND e1.is_current = 'Y'
                      AND e2.is_current = 'Y'
                      AND e3.is_current = 'Y'
                      AND e4.is_current = 'Y'
                    COLUMNS (
                        s.supplier_name AS supplier_name,
                        s.tier_level AS tier_level,
                        s.region_code AS supplier_region,
                        s.on_time_pct AS on_time_pct,
                        p.plant_name AS plant_name,
                        p.cycle_days AS cycle_days,
                        p.utilization_pct AS utilization_pct,
                        po.port_id AS port_id,
                        po.port_name AS port_name,
                        po.eta_hours AS eta_hours,
                        po.delay_risk_score AS delay_risk_score,
                        w.warehouse_name AS warehouse_name,
                        w.inventory_units AS inventory_units,
                        w.fill_rate_pct AS fill_rate_pct,
                        pr.product_id AS product_id,
                        pr.demand_change_pct AS demand_change_pct,
                        pr.margin_pct AS margin_pct
                    )
                )
            )
            SELECT
                dp.supplier_name,
                dp.tier_level,
                dp.supplier_region,
                dp.on_time_pct,
                dp.plant_name,
                dp.cycle_days,
                dp.utilization_pct,
                dp.port_name,
                dp.eta_hours,
                dp.delay_risk_score,
                dp.warehouse_name,
                dp.inventory_units,
                dp.fill_rate_pct,
                dp.product_id,
                dp.demand_change_pct,
                dp.margin_pct,
                a.alert_name,
                a.lane_name,
                a.risk_score AS alert_risk
            FROM dependency_path dp
            LEFT JOIN sc_alert_port ap
              ON ap.port_id = dp.port_id
             AND ap.is_current = 'Y'
            LEFT JOIN sc_alerts a
              ON a.alert_id = ap.alert_id
             AND a.active_flag = 'Y'
            FETCH FIRST 1 ROW ONLY
            """;

    public enum GraphDataMode {
        AUTO,
        DATABASE,
        PAYLOAD;

        static GraphDataMode from(String value) {
            if (value == null || value.isBlank()) {
                return AUTO;
            }

            return switch (value.trim().toUpperCase()) {
                case "DATABASE" -> DATABASE;
                case "PAYLOAD" -> PAYLOAD;
                default -> AUTO;
            };
        }
    }

    public record GraphNode(
            String id,
            String type,
            String label,
            String detail,
            String metric
    ) {}

    public record GraphEdge(
            String from,
            String to,
            String label
    ) {}

    public record GraphPayload(
            String schemaVersion,
            String productId,
            List<GraphNode> nodes,
            List<GraphEdge> edges
    ) {}

    public record GraphRequest(
            String textProductId,
            GraphPayload payload,
            boolean payloadDetected,
            String payloadError
    ) {}

    public record GraphResponse(
            String productId,
            List<Map<String, String>> nodes,
            List<Map<String, String>> edges,
            String sourceMode,
            String sourceDetail
    ) {}

    @Bean
    @Description("Fetches supply chain dependencies from Oracle Property Graph for a specific product ID")
    public Function<GraphRequest, GraphResponse> getSupplyChainDependencies(Environment environment) {
        return request -> resolveGraphResponse(environment, request);
    }

    private static GraphResponse resolveGraphResponse(Environment environment, GraphRequest request) {
        GraphDataMode dataMode = GraphDataMode.from(environment.getProperty("GRAPH_DATA_MODE"));

        return switch (dataMode) {
            case DATABASE -> resolveDatabaseGraph(environment, request);
            case PAYLOAD -> resolvePayloadGraph(request);
            case AUTO -> {
                if (request.payloadError() != null && !request.payloadError().isBlank()) {
                    throw new IllegalArgumentException(request.payloadError());
                }

                if (request.payload() != null) {
                    yield resolvePayloadGraph(request);
                }

                yield resolveDatabaseGraph(environment, request);
            }
        };
    }

    private static GraphResponse resolvePayloadGraph(GraphRequest request) {
        if (request.payloadError() != null && !request.payloadError().isBlank()) {
            throw new IllegalArgumentException(request.payloadError());
        }
        if (request.payload() == null) {
            throw new IllegalArgumentException(
                    "GRAPH_DATA_MODE=payload requires a valid structured graph payload."
            );
        }

        GraphPayload payload = request.payload();
        String effectiveProductId = resolveRequestedProductId(request);
        validateSchemaVersion(payload.schemaVersion());

        List<Map<String, String>> normalizedNodes = normalizePayloadNodes(payload.nodes(), effectiveProductId);
        List<Map<String, String>> normalizedEdges = normalizePayloadEdges(payload.edges(), normalizedNodes);

        return new GraphResponse(
                effectiveProductId,
                normalizedNodes,
                normalizedEdges,
                "payload",
                "Validated upstream graph payload"
        );
    }

    private static GraphResponse resolveDatabaseGraph(Environment environment, GraphRequest request) {
        String productId = resolveRequestedProductId(request);
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(DATABASE_GRAPH_QUERY)
        ) {
            statement.setString(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException(
                            "No Oracle graph path was found for productId " + productId + "."
                    );
                }

                return graphResponseFromDatabaseRow(productId, resultSet);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Oracle Database graph lookup failed for productId " + productId + ": " + e.getMessage(),
                    e
            );
        }
    }

    private static GraphResponse graphResponseFromDatabaseRow(String productId, ResultSet resultSet)
            throws SQLException {
        String supplierName = resultSet.getString("supplier_name");
        String tierLevel = resultSet.getString("tier_level");
        String supplierRegion = resultSet.getString("supplier_region");
        String onTimePct = resultSet.getString("on_time_pct");
        String plantName = resultSet.getString("plant_name");
        String cycleDays = resultSet.getString("cycle_days");
        String utilizationPct = resultSet.getString("utilization_pct");
        String portName = resultSet.getString("port_name");
        String etaHours = resultSet.getString("eta_hours");
        String delayRiskScore = resultSet.getString("delay_risk_score");
        String warehouseName = resultSet.getString("warehouse_name");
        String inventoryUnits = resultSet.getString("inventory_units");
        String fillRatePct = resultSet.getString("fill_rate_pct");
        String demandChangePct = resultSet.getString("demand_change_pct");
        String marginPct = resultSet.getString("margin_pct");
        String alertName = resultSet.getString("alert_name");
        String laneName = resultSet.getString("lane_name");
        String alertRisk = resultSet.getString("alert_risk");

        List<Map<String, String>> nodes = List.of(
                node(
                        "supplier",
                        "Supplier: " + supplierName,
                        "SUPPLIER",
                        "Tier " + valueOrFallback(tierLevel, "?") + " | " + valueOrFallback(supplierRegion, "Unknown"),
                        "On-time " + formatPercent(onTimePct)
                ),
                node(
                        "plant",
                        "Plant: " + plantName,
                        "PLANT",
                        "Cycle " + valueOrFallback(cycleDays, "n/a") + " days",
                        "Utilization " + formatPercent(utilizationPct)
                ),
                node(
                        "port",
                        "Port: " + portName,
                        "PORT",
                        "ETA " + valueOrFallback(etaHours, "n/a") + " hrs",
                        "Delay risk " + valueOrFallback(delayRiskScore, "n/a")
                ),
                node(
                        "warehouse",
                        "Warehouse: " + warehouseName,
                        "WAREHOUSE",
                        "Inventory " + valueOrFallback(inventoryUnits, "n/a") + " units",
                        "Fill rate " + formatPercent(fillRatePct)
                ),
                node(
                        "product",
                        "Product: " + productId,
                        "PRODUCT",
                        "Demand " + formatSignedPercent(demandChangePct),
                        "Margin " + formatPercent(marginPct)
                ),
                node(
                        "alert",
                        "Alert: " + valueOrFallback(alertName, "No Active Alert"),
                        "ALERT",
                        valueOrFallback(laneName, "No active lane"),
                        "Risk " + valueOrFallback(alertRisk, "n/a")
                )
        );

        List<Map<String, String>> edges = List.of(
                edge("supplier", "plant", "SUPPLIES"),
                edge("plant", "port", "SHIPS_VIA"),
                edge("port", "warehouse", "ROUTES_TO"),
                edge("warehouse", "product", "STOCKS"),
                edge("alert", "port", "AFFECTS")
        );

        return new GraphResponse(
                productId,
                nodes,
                edges,
                "database",
                "Oracle Database property graph"
        );
    }

    private static void validateSchemaVersion(String schemaVersion) {
        if (schemaVersion != null && !schemaVersion.isBlank() && !PAYLOAD_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported graph payload schemaVersion " + schemaVersion
                            + ". Supported version: " + PAYLOAD_SCHEMA_VERSION + "."
            );
        }
    }

    private static List<Map<String, String>> normalizePayloadNodes(List<GraphNode> nodes, String productId) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Graph payload must include a non-empty nodes array.");
        }

        List<Map<String, String>> normalizedNodes = new ArrayList<>();
        Set<String> nodeIds = new LinkedHashSet<>();
        boolean hasProductNode = false;

        for (GraphNode node : nodes) {
            String id = requiredValue(node == null ? null : node.id(), "node.id");
            String type = requiredValue(node == null ? null : node.type(), "node.type").toUpperCase();
            String label = requiredValue(node == null ? null : node.label(), "node.label");
            String detail = valueOrFallback(node == null ? null : node.detail(), "");
            String metric = valueOrFallback(node == null ? null : node.metric(), "");

            if (!ALLOWED_NODE_TYPES.contains(type)) {
                throw new IllegalArgumentException(
                        "Unsupported node.type " + type + ". Allowed types: " + ALLOWED_NODE_TYPES + "."
                );
            }
            if (!nodeIds.add(id)) {
                throw new IllegalArgumentException("Graph payload contains duplicate node.id " + id + ".");
            }
            if ("PRODUCT".equals(type)) {
                hasProductNode = true;
            }

            normalizedNodes.add(node(id, label, type, detail, metric));
        }

        if (!hasProductNode) {
            throw new IllegalArgumentException(
                    "Graph payload must include one PRODUCT node for productId " + productId + "."
            );
        }

        return normalizedNodes;
    }

    private static List<Map<String, String>> normalizePayloadEdges(
            List<GraphEdge> edges,
            List<Map<String, String>> normalizedNodes
    ) {
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("Graph payload must include a non-empty edges array.");
        }

        Set<String> knownNodeIds = new LinkedHashSet<>();
        for (Map<String, String> node : normalizedNodes) {
            knownNodeIds.add(node.get("id"));
        }

        List<Map<String, String>> normalizedEdges = new ArrayList<>();
        for (GraphEdge edge : edges) {
            String from = requiredValue(edge == null ? null : edge.from(), "edge.from");
            String to = requiredValue(edge == null ? null : edge.to(), "edge.to");
            String label = requiredValue(edge == null ? null : edge.label(), "edge.label").toUpperCase();

            if (!ALLOWED_EDGE_LABELS.contains(label)) {
                throw new IllegalArgumentException(
                        "Unsupported edge.label " + label + ". Allowed labels: " + ALLOWED_EDGE_LABELS + "."
                );
            }
            if (!knownNodeIds.contains(from) || !knownNodeIds.contains(to)) {
                throw new IllegalArgumentException(
                        "Graph payload edge " + label + " references unknown node ids: " + from + " -> " + to + "."
                );
            }

            normalizedEdges.add(edge(from, to, label));
        }

        return normalizedEdges;
    }

    private static String resolveRequestedProductId(GraphRequest request) {
        String payloadProductId = request.payload() == null ? "" : valueOrFallback(request.payload().productId(), "");
        String textProductId = valueOrFallback(request.textProductId(), "");

        if (!payloadProductId.isBlank() && !textProductId.isBlank() && !payloadProductId.equals(textProductId)) {
            throw new IllegalArgumentException(
                    "Structured graph payload productId " + payloadProductId
                            + " does not match text productId " + textProductId + "."
            );
        }

        return firstNonBlank(payloadProductId, textProductId, DEFAULT_PRODUCT_ID);
    }

    private static String requiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Graph payload field " + fieldName + " is required.");
        }
        return value.trim();
    }

    private static Map<String, String> node(
            String id,
            String label,
            String type,
            String detail,
            String metric
    ) {
        return Map.of(
                "id", id,
                "label", label,
                "type", type,
                "detail", detail,
                "metric", metric
        );
    }

    private static Map<String, String> edge(
            String from,
            String to,
            String label
    ) {
        return Map.of(
                "from", from,
                "to", to,
                "label", label
        );
    }

    private static String formatPercent(String value) {
        return valueOrFallback(value, "n/a") + "%";
    }

    private static String formatSignedPercent(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }

        return (value.startsWith("-") ? "" : "+") + value + "%";
    }

    private static String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
