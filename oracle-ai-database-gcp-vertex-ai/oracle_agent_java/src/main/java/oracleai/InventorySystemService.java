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
        try {
            OracleAiDatabaseAgentClient.RemoteDatabaseResult result = oracleAiDatabaseAgentClient.answer(
                    userInput,
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
                        "Oracle AI Database agent handoff failed and local fallback is disabled for this gateway.\n\n"
                                + "Delegate error: " + exception.getMessage(),
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
        return "SKU-500";
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
        return prompt + "\nUse SKU-500 as the default product id. " + guidance;
    }

    private static boolean containsProductId(String userInput) {
        return PRODUCT_ID_PATTERN.matcher(userInput == null ? "" : userInput.toUpperCase()).find();
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
            for (String needle : needles) {
                if (text.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
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
