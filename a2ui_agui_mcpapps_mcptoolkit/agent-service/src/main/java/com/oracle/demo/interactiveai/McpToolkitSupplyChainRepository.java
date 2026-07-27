package com.oracle.demo.interactiveai;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Calls purpose-built Oracle Database MCP Java Toolkit tools over MCP stdio. */
public final class McpToolkitSupplyChainRepository implements SupplyChainRepository, AutoCloseable {
    private static final Set<String> REQUIRED_TOOLS = Set.of(
            "find-stockout-transfer-recommendations",
            "get-stockout-transfer-details",
            "reserve-inventory-transfer-id",
            "approve-inventory-transfer",
            "count-inventory-transfers");

    private final McpStdioClient client;

    private McpToolkitSupplyChainRepository(McpStdioClient client) {
        this.client = client;
        verifyToolkit();
    }

    public static McpToolkitSupplyChainRepository fromEnvironment(Map<String, String> environment) {
        Path toolkitJar = requiredPath(environment, "ORACLE_MCP_TOOLKIT_JAR");
        Path configFile = optionalPath(environment, "ORACLE_MCP_CONFIG_FILE",
                Path.of("../oracle-db-mcp-toolkit/config/tools.yaml"));
        String databaseUrl = required(environment, "DB_URL");
        String databaseUser = firstNonBlank(environment.get("DB_USERNAME"), environment.get("DB_USER"));
        String databasePassword = required(environment, "DB_PASSWORD");
        if (databaseUser == null) throw new IllegalArgumentException("DB_USERNAME or DB_USER is required");

        String javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        McpStdioClient client = new McpStdioClient(
                List.of(javaCommand,
                        "-DconfigFile=" + configFile,
                        "-Dtools=supply-chain-exchange",
                        "-jar", toolkitJar.toString()),
                Map.of(
                        "DB_URL", databaseUrl,
                        "DB_USERNAME", databaseUser,
                        "DB_PASSWORD", databasePassword));
        client.initialize();
        return new McpToolkitSupplyChainRepository(client);
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
        return Math.toIntExact(longValue(
                firstRow(client.callTool("count-inventory-transfers", Map.of())),
                "TRANSFER_COUNT"));
    }

    @Override
    public void close() {
        client.close();
    }

    private void verifyToolkit() {
        if (!"oracle-db-mcp-toolkit".equals(client.serverName())) {
            throw new IllegalStateException("Connected MCP server is not Oracle Database MCP Java Toolkit");
        }
        Set<String> available = client.listTools();
        if (!available.equals(REQUIRED_TOOLS)) {
            throw new IllegalStateException("Oracle Database MCP Toolkit tool allowlist mismatch; expected "
                    + REQUIRED_TOOLS.stream().sorted().toList() + " but received " + available.stream().sorted().toList());
        }
        System.out.printf("MCP initialized with %s %s; enabled tools=%s%n",
                client.serverName(), client.serverVersion(), available.stream().sorted().toList());
    }

    private static List<JsonNode> rows(McpStdioClient.ToolResult result) {
        JsonNode rows = result.structuredContent().path("rows");
        if (!rows.isArray()) throw new IllegalStateException("Oracle Database MCP tool did not return structured rows");
        return StreamSupport.stream(rows.spliterator(), false).toList();
    }

    private static JsonNode firstRow(McpStdioClient.ToolResult result) {
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

    private static Path requiredPath(Map<String, String> environment, String name) {
        return checkedPath(required(environment, name), name);
    }

    private static Path optionalPath(Map<String, String> environment, String name, Path fallback) {
        String value = environment.get(name);
        return checkedPath(value == null || value.isBlank() ? fallback.toString() : value, name);
    }

    private static Path checkedPath(String value, String name) {
        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException(name + " does not identify a file: " + path);
        return path;
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }
}
