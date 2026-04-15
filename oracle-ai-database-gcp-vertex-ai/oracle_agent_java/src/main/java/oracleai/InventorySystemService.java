package oracleai;

import io.a2a.spec.Artifact;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
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
    private final Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies;
    private final SpatialTools spatialTools;

    public InventorySystemService(
            Environment environment,
            SelectAiService selectAiService,
            OracleAiDatabaseAgentClient oracleAiDatabaseAgentClient,
            Function<GraphTools.GraphRequest, GraphTools.GraphResponse> getSupplyChainDependencies,
            SpatialTools spatialTools
    ) {
        this.environment = environment;
        this.selectAiService = selectAiService;
        this.oracleAiDatabaseAgentClient = oracleAiDatabaseAgentClient;
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
            case GRAPH -> routeGraph(normalized);
            case SPATIAL -> routeSpatial(normalized);
            case DATABASE -> routeDatabase(normalized, authorizationHeader);
        };
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

    enum Route {
        GRAPH,
        SPATIAL,
        DATABASE;

        static Route classify(String userInput) {
            String normalized = userInput == null ? "" : userInput.toLowerCase();
            if (containsAny(normalized, "map", "spatial", "hotspot", "county", "latitude", "longitude")) {
                return SPATIAL;
            }
            if (containsAny(normalized, "graph", "dependency", "supplier", "upstream", "downstream", "property graph")) {
                return GRAPH;
            }
            return DATABASE;
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
