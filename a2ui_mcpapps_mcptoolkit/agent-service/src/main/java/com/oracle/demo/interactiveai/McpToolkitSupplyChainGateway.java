package com.oracle.demo.interactiveai;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Calls purpose-built tools on a standalone Oracle Database MCP Java Toolkit service. */
public final class McpToolkitSupplyChainGateway implements SupplyChainRepository, AutoCloseable {
    private static final Set<String> WRITE_TOOLS = Set.of(
            "find-stockout-transfer-recommendations",
            "get-stockout-transfer-details",
            "reserve-inventory-transfer-id",
            "approve-inventory-transfer",
            "count-inventory-transfers");
    private static final Set<String> READ_TOOLS = Set.of(
            "find-stockout-transfer-recommendations",
            "get-stockout-transfer-details");

    private final McpHttpClient client;
    private final boolean writesAllowed;

    private McpToolkitSupplyChainGateway(
            McpHttpClient client,
            boolean writesAllowed) {
        this.client = client;
        this.writesAllowed = writesAllowed;
        verifyToolkit(writesAllowed ? WRITE_TOOLS : READ_TOOLS);
    }

    public static McpToolkitSupplyChainGateway fromEnvironment(Map<String, String> environment) {
        return create(environment, "ORACLE_MCP_URL", "ORACLE_MCP_AUTH_TOKEN", true);
    }

    public static McpToolkitSupplyChainGateway secondaryFromEnvironment(
            Map<String, String> environment) {
        return create(environment, "ORACLE_MCP_READ_URL", "ORACLE_MCP_READ_AUTH_TOKEN", false);
    }

    private static McpToolkitSupplyChainGateway create(
            Map<String, String> environment,
            String urlName,
            String tokenName,
            boolean writesAllowed) {
        String endpoint = required(environment, urlName);
        String token = required(environment, tokenName);
        Path trustStore = optionalPath(environment, "ORACLE_MCP_TRUSTSTORE");
        char[] trustStorePassword = environment.getOrDefault(
                "ORACLE_MCP_TRUSTSTORE_PASSWORD", "").toCharArray();
        McpHttpClient client = new McpHttpClient(
                URI.create(endpoint), token, trustStore, trustStorePassword);
        client.initialize();
        return new McpToolkitSupplyChainGateway(client, writesAllowed);
    }

    @Override
    public boolean writesAllowed() {
        return writesAllowed;
    }

    @Override
    public List<TransferRecommendation> findTransferRecommendations(
            double minimumStockoutRisk,
            int maximumRows) {
        InputValidation.minimumStockoutRisk(minimumStockoutRisk);
        InputValidation.maximumRows(maximumRows);
        return rows(client.callTool("find-stockout-transfer-recommendations", Map.of(
                "minimumStockoutRisk", minimumStockoutRisk,
                "maximumRows", maximumRows))).stream()
                .map(row -> new TransferRecommendation(
                        stringValue(row, "RECOMMENDATION_ID"),
                        longValue(row, "PRODUCT_ID"),
                        stringValue(row, "SKU"),
                        stringValue(row, "PRODUCT_NAME"),
                        stringValue(row, "CATEGORY_NAME"),
                        longValue(row, "SOURCE_LOCATION_ID"),
                        stringValue(row, "SOURCE_LOCATION_CODE"),
                        stringValue(row, "SOURCE_LOCATION_NAME"),
                        longValue(row, "TARGET_LOCATION_ID"),
                        stringValue(row, "TARGET_LOCATION_CODE"),
                        stringValue(row, "TARGET_LOCATION_NAME"),
                        longValue(row, "SOURCE_AVAILABLE_QTY"),
                        longValue(row, "TARGET_AVAILABLE_QTY"),
                        longValue(row, "FORECAST_7D_QTY"),
                        longValue(row, "SAFETY_STOCK_QTY"),
                        longValue(row, "SHORTAGE_QTY"),
                        longValue(row, "RECOMMENDED_TRANSFER_QTY"),
                        Math.toIntExact(longValue(row, "TRANSIT_DAYS")),
                        doubleValue(row, "UNIT_TRANSFER_COST"),
                        doubleValue(row, "STOCKOUT_RISK_SCORE"),
                        stringValue(row, "RISK_LEVEL"),
                        stringValue(row, "RATIONALE")))
                .toList();
    }

    @Override
    public TransferResult approveTransfer(
            TransferRecommendation recommendation,
            String approvalNotes,
            String requestedBy) {
        if (!writesAllowed) {
            throw new IllegalStateException(
                    "This Deep Data Security identity has read-only inventory access");
        }
        InputValidation.approval(recommendation, approvalNotes, requestedBy);
        long transferId = longValue(
                firstRow(client.callTool("reserve-inventory-transfer-id", Map.of())),
                "TRANSFER_ID");
        client.callTool("approve-inventory-transfer", Map.of(
                "transferId", transferId,
                "productId", recommendation.productId(),
                "sourceLocationId", recommendation.sourceLocationId(),
                "targetLocationId", recommendation.targetLocationId(),
                "transferQuantity", recommendation.recommendedTransferQuantity(),
                "approvalNotes", approvalNotes,
                "requestedBy", requestedBy));
        return new TransferResult(
                transferId,
                recommendation.recommendationId(),
                recommendation.recommendedTransferQuantity(),
                "APPROVED");
    }

    @Override
    public int transferCount() {
        if (!writesAllowed) {
            throw new IllegalStateException(
                    "This Deep Data Security identity cannot inspect transfer audit rows");
        }
        return Math.toIntExact(longValue(
                firstRow(client.callTool("count-inventory-transfers", Map.of())),
                "TRANSFER_COUNT"));
    }

    @Override
    public void close() {
        client.close();
    }

    private void verifyToolkit(Set<String> requiredTools) {
        if (!"oracle-db-mcp-toolkit".equals(client.serverName())) {
            throw new IllegalStateException("Connected MCP server is not Oracle Database MCP Java Toolkit");
        }
        Set<String> available = client.listTools();
        if (!available.equals(requiredTools)) {
            throw new IllegalStateException("Oracle Database MCP Toolkit tool allowlist mismatch; expected "
                    + requiredTools.stream().sorted().toList() + " but received " + available.stream().sorted().toList());
        }
        System.out.printf("MCP initialized with %s %s; enabled tools=%s%n",
                client.serverName(), client.serverVersion(), available.stream().sorted().toList());
    }

    private static List<JsonNode> rows(McpHttpClient.ToolResult result) {
        JsonNode rows = result.structuredContent().path("rows");
        if (!rows.isArray()) throw new IllegalStateException("Oracle Database MCP tool did not return structured rows");
        return StreamSupport.stream(rows.spliterator(), false).toList();
    }

    private static JsonNode firstRow(McpHttpClient.ToolResult result) {
        List<JsonNode> rows = rows(result);
        if (rows.size() != 1) throw new IllegalStateException("Oracle Database MCP tool must return exactly one row");
        return rows.getFirst();
    }

    private static JsonNode value(JsonNode row, String name) {
        JsonNode value = row.get(name);
        if (value == null || value.isNull()) value = row.get(name.toLowerCase());
        if (value == null || value.isNull()) throw new IllegalStateException("Oracle Database MCP row is missing " + name);
        return value;
    }

    private static long longValue(JsonNode row, String name) {
        JsonNode value = value(row, name);
        return value.isNumber() ? value.decimalValue().longValueExact() : Long.parseLong(value.asText());
    }

    private static double doubleValue(JsonNode row, String name) {
        JsonNode value = value(row, name);
        return value.isNumber() ? value.doubleValue() : Double.parseDouble(value.asText());
    }

    private static String stringValue(JsonNode row, String name) {
        return value(row, name).asText();
    }

    private static Path optionalPath(Map<String, String> environment, String name) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

}
