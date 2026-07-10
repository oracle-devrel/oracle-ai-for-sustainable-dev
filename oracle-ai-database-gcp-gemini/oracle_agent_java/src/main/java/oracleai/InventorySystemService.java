package oracleai;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class InventorySystemService {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");
    private final Environment environment;
    private final SelectAiService selectAiService;
    private final OracleAiDatabaseAgentClient oracleAiDatabaseAgentClient;
    private final InventoryActionAdkService inventoryActionAdkService;
    private final Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies;
    private final SpatialTools spatialTools;

    public InventorySystemService(
            Environment environment,
            SelectAiService selectAiService,
            OracleAiDatabaseAgentClient oracleAiDatabaseAgentClient,
            InventoryActionAdkService inventoryActionAdkService,
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            SpatialTools spatialTools
    ) {
        this.environment = environment;
        this.selectAiService = selectAiService;
        this.oracleAiDatabaseAgentClient = oracleAiDatabaseAgentClient;
        this.inventoryActionAdkService = inventoryActionAdkService;
        this.getSupplyChainDependencies = getSupplyChainDependencies;
        this.spatialTools = spatialTools;
    }

    public InventorySystemResult answer(String userInput) {
        return answer(userInput, null);
    }

    public InventorySystemResult answer(String userInput, String authorizationHeader) {
        String normalized = userInput == null ? "" : userInput.trim();
        Route route = Route.classify(normalized);

        return switch (route) {
            case ACTION -> routeAction(normalized);
            case GRAPH -> routeGraph(normalized);
            case SPATIAL -> routeSpatial(normalized);
            case DATABASE -> routeDatabase(normalized, authorizationHeader);
        };
    }

    private InventorySystemResult routeAction(String userInput) {
        InventoryActionAdkService.InventoryActionResult result = inventoryActionAdkService.run(
                actionPrompt(userInput),
                UUID.randomUUID().toString()
        );

        return new InventorySystemResult(
                result.responseText(),
                "inventory-action",
                result.orchestrationMode(),
                "Delegated to the inventory action coordinator; traceCount=" + result.trace().size(),
                actionArtifacts(result),
                "delegate-inventory-action"
        );
    }

    private InventorySystemResult routeDatabase(String userInput, String authorizationHeader) {
        String databasePrompt = databasePrompt(userInput);
        try {
            OracleAiDatabaseAgentClient.RemoteDatabaseResult result = oracleAiDatabaseAgentClient.answer(
                    databasePrompt,
                    authorizationHeader
            );
            return new InventorySystemResult(
                    result.responseText(),
                    "oracle-ai-database-agent",
                    result.executionMode(),
                    result.sourceDetail(),
                    result.artifacts(),
                    result.action()
            );
        } catch (Exception exception) {
            if (!allowsLocalFallback()) {
                return new InventorySystemResult(
                        databaseDelegateErrorText(userInput, exception),
                        "oracle-ai-database-agent-error",
                        "delegate-error",
                        "Oracle AI Database agent delegation failed: " + exception.getMessage(),
                        List.of(),
                        "delegate-oracle-ai-database-agent-error"
                );
            }
            SelectAiService.SelectAiResult fallbackResult = selectAiService.answer(userInput);
            String responseText = "The Oracle AI Database agent handoff was unavailable, so this answer came from the local Select AI fallback.\n\n"
                    + fallbackResult.responseText();
            return new InventorySystemResult(
                    responseText,
                    "select-ai-fallback",
                    fallbackResult.executionMode(),
                    "Oracle AI Database agent delegation failed: "
                            + exception.getMessage()
                            + ". Fallback source: "
                            + fallbackResult.sourceDetail(),
                    List.of(),
                    "delegate-oracle-ai-database-agent-fallback-local"
            );
        }
    }

    private InventorySystemResult routeGraph(String userInput) {
        String productId = extractProductId(userInput);
        GraphTools.GraphResponse response = getSupplyChainDependencies.apply(
                new GraphTools.GraphRequest(productId, null, false, null)
        );
        String responseText = GraphA2AConfiguration.formatResponse(response);
        String imageBytes;
        try {
            imageBytes = GraphA2AConfiguration.renderGraphPng(response.productId(), response);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render graph output: " + exception.getMessage(), exception);
        }

        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .name("supply_chain_graph_png")
                .description("Oracle property graph visualization")
                .parts(new FilePart(new FileWithBytes("image/png", "supply-chain-graph.png", imageBytes)))
                .metadata(Map.of(
                        "productId", response.productId(),
                        "sourceMode", response.sourceMode(),
                        "contentType", "image/png"
                ))
                .extensions(List.of())
                .build();

        return new InventorySystemResult(
                responseText,
                "graph",
                response.sourceMode(),
                response.sourceDetail(),
                List.of(artifact),
                "delegate-graph"
        );
    }

    private InventorySystemResult routeSpatial(String userInput) {
        SpatialTools.SpatialResponse response = spatialTools.resolveSpatialResponse(userInput);
        String imageBytes;
        try {
            imageBytes = spatialTools.renderHotspotPng(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render spatial output: " + exception.getMessage(), exception);
        }

        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .name("warehouse_hotspot_map_png")
                .description("Oracle spatial hotspot visualization")
                .parts(new FilePart(new FileWithBytes("image/png", "warehouse-hotspot-map.png", imageBytes)))
                .metadata(Map.of(
                        "productId", response.productId(),
                        "sourceMode", response.sourceMode(),
                        "contentType", "image/png"
                ))
                .extensions(List.of())
                .build();

        return new InventorySystemResult(
                response.summaryText(),
                "spatial",
                response.sourceMode(),
                response.sourceDetail(),
                List.of(artifact),
                "delegate-spatial"
        );
    }

    private static String extractProductId(String userInput) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return DemoInventoryData.DEFAULT_PRODUCT_ID;
    }

    private static String actionPrompt(String userInput) {
        String prompt = userInput == null || userInput.isBlank()
                ? "Suggest the best inventory action to take."
                : userInput.trim();
        String guidance = "Gather graph, spatial, and external evidence, then draft an inventory transfer action "
                + "when a transfer is the best next move.";
        if (containsProductId(prompt)) {
            return prompt + "\n" + guidance;
        }
        return prompt + "\nUse " + DemoInventoryData.DEFAULT_PRODUCT_ID
                + " as the default product id. " + guidance;
    }

    private static boolean containsProductId(String userInput) {
        return PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase()).find();
    }

    private static String databasePrompt(String userInput) {
        String prompt = userInput == null || userInput.isBlank()
                ? "Which products are at risk of stockouts next quarter, and which regions are driving that risk?"
                : userInput.trim();
        if (!isInventoryRiskDatabasePrompt(prompt)) {
            return prompt;
        }

        String productId = extractProductId(prompt);
        return """
                You are answering for the supply-chain inventory-risk demo. Use only these Oracle objects:
                - SALES_USER.SC_INVENTORY_RISK_SUMMARY(product_id, quarter_label, risk_level, stockout_probability, at_risk_units, projected_revenue_impact_usd, primary_region, recommendation_summary, active_flag)
                - SALES_USER.SC_INVENTORY_RISK_DEMO_V(product_id, product_name, quarter_label, overall_risk_level, stockout_probability, product_at_risk_units, projected_revenue_impact_usd, primary_region, recommendation_summary, warehouse_name, county_name, state_code, region_name, hotspot_rank, hotspot_score, coverage_days, backlog_units, service_level_pct, at_risk_units, revenue_impact_usd, recommended_role, active_flag)

                Run this SQL exactly. It intentionally reads the small active demo view without a string-literal
                WHERE predicate:

                SELECT PRODUCT_ID, PRODUCT_NAME, QUARTER_LABEL, OVERALL_RISK_LEVEL, STOCKOUT_PROBABILITY,
                       PRODUCT_AT_RISK_UNITS, PROJECTED_REVENUE_IMPACT_USD, PRIMARY_REGION,
                       RECOMMENDATION_SUMMARY, REGION_NAME, SUM(AT_RISK_UNITS) AS REGION_AT_RISK_UNITS,
                       SUM(REVENUE_IMPACT_USD) AS REGION_REVENUE_IMPACT_USD,
                       MIN(COVERAGE_DAYS) AS MIN_COVERAGE_DAYS
                FROM SALES_USER.SC_INVENTORY_RISK_DEMO_V
                GROUP BY PRODUCT_ID, PRODUCT_NAME, QUARTER_LABEL, OVERALL_RISK_LEVEL, STOCKOUT_PROBABILITY,
                         PRODUCT_AT_RISK_UNITS, PROJECTED_REVENUE_IMPACT_USD, PRIMARY_REGION,
                         RECOMMENDATION_SUMMARY, REGION_NAME
                ORDER BY PRODUCT_ID, REGION_AT_RISK_UNITS DESC

                Use product_id %s, also known as Sustainable Widget 500, as the primary demo product for broad
                stockout-risk prompts that do not specify a product. After the SQL returns, summarize only values
                from the result set: product id, product name, quarter, stockout probability, risk level, primary
                region, product at-risk units, projected revenue impact, recommendation, and the region names
                driving the risk. Do not mention product ids, regions, probabilities, units, or revenue amounts that
                are not in the SQL result. If the Oracle AI Database agent cannot access the inventory-risk view or
                %s is not present in the SQL result, say that clearly and do not answer from generic sales/product
                tables, numeric PROD_ID sample data, SALES, CUSTOMERS, or CHANNELS. Do not synthesize or infer
                missing values.

                User question: %s
                """.formatted(productId, productId, prompt);
    }

    private static String databaseDelegateErrorText(String userInput, Exception exception) {
        if (isInventoryRiskDatabasePrompt(userInput)) {
            String productId = extractProductId(userInput);
            return "Oracle AI Database agent could not query the inventory-risk demo objects for "
                    + productId
                    + ". I asked it to use SALES_USER.SC_INVENTORY_RISK_SUMMARY and "
                    + "SALES_USER.SC_INVENTORY_RISK_DEMO_V, but the delegate failed.\n\n"
                    + "Delegate error: " + exception.getMessage()
                    + "\n\nNo local Select AI fallback was used. This usually means the Oracle AI Database "
                    + "agent is not currently registered against, profiled for, or permitted to access the "
                    + "SKU inventory-risk tables.";
        }
        return "Oracle AI Database agent handoff failed and local fallback is disabled for this gateway.\n\n"
                + "Delegate error: " + exception.getMessage();
    }

    private static boolean isInventoryRiskDatabasePrompt(String userInput) {
        String text = userInput == null ? "" : userInput.toLowerCase();
        return containsAny(
                text,
                "stockout",
                "stock out",
                "inventory risk",
                "inventory-risk",
                "warehouse risk",
                "coverage days",
                "backlog",
                "at-risk units",
                "at risk units",
                "revenue impact"
        ) || (text.contains("products") && text.contains("at risk") && text.contains("regions"));
    }

    private static List<Artifact> actionArtifacts(InventoryActionAdkService.InventoryActionResult result) {
        if (result.draftAction() == null || result.draftAction().isEmpty()) {
            return List.of();
        }

        Map<String, Object> actionData = new LinkedHashMap<>();
        actionData.put("action", result.draftAction());
        if (result.policyResult() != null && !result.policyResult().isEmpty()) {
            actionData.put("policy", result.policyResult());
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actionType", stringValue(result.draftAction().get("actionType")));
        metadata.put("draftActionId", stringValue(result.draftAction().get("draftActionId")));
        metadata.put("contentType", "application/json");

        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .name("inventory_transfer_action")
                .description("Draft inventory-transfer action recommendation")
                .parts(new DataPart(actionData))
                .metadata(metadata)
                .extensions(List.of())
                .build();
        return List.of(artifact);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    enum Route {
        ACTION,
        GRAPH,
        SPATIAL,
        DATABASE;

        static Route classify(String userInput) {
            String normalized = userInput == null ? "" : userInput.toLowerCase();
            if (isActionIntent(normalized)) {
                return ACTION;
            }
            if (containsAny(normalized, "map", "spatial", "hotspot", "county", "latitude", "longitude")) {
                return SPATIAL;
            }
            if (containsAny(normalized, "graph", "dependency", "supplier", "upstream", "downstream", "property graph")) {
                return GRAPH;
            }
            return DATABASE;
        }

        private static boolean isActionIntent(String text) {
            return containsAny(
                    text,
                    "suggest action",
                    "suggest actions",
                    "actions to take",
                    "action to take",
                    "what action",
                    "which action",
                    "recommend action",
                    "recommended action",
                    "inventory action",
                    "next step",
                    "next move",
                    "what should we do",
                    "transfer inventory",
                    "inventory transfer",
                    "move inventory",
                    "rebalance inventory",
                    "draft transfer",
                    "draft an inventory",
                    "take action"
            ) || (text.contains("transfer") && text.contains("warehouse"));
        }

        private static boolean containsAny(String text, String... needles) {
            return InventorySystemService.containsAny(text, needles);
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean allowsLocalFallback() {
        String configuredValue = environment.getProperty("INVENTORY_SYSTEM_ALLOW_LOCAL_SELECT_AI_FALLBACK");
        return configuredValue != null && configuredValue.equalsIgnoreCase("true");
    }

    public record InventorySystemResult(
            String responseText,
            String delegatedTo,
            String executionMode,
            String sourceDetail,
            List<Artifact> artifacts,
            String action
    ) {}
}
