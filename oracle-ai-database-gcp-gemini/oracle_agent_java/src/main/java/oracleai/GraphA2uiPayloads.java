package oracleai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class GraphA2uiPayloads {

    static final String VERSION = "v0.8";
    static final String STANDARD_CATALOG =
            "https://a2ui.org/specification/v0_8/standard_catalog_definition.json";
    static final String EXTENSION_URI =
            "https://a2ui.org/a2a-extension/a2ui/v0.8";
    static final String MIME_TYPE = "application/json+a2ui";

    private GraphA2uiPayloads() {
    }

    static List<Map<String, Object>> graphExplorerMessages(GraphTools.GraphResponse graphResponse) {
        String surfaceId = "supply-chain-graph-" + UUID.randomUUID();
        List<String> rootChildren = new ArrayList<>(List.of(
                "title",
                "source",
                "summary",
                "path-card",
                "nodes-title"
        ));
        List<Map<String, Object>> components = new ArrayList<>();
        components.add(component(
                "root",
                "Column",
                Map.of("children", explicitList(rootChildren))
        ));
        components.add(text(
                "title",
                "Supply-chain dependency graph for " + graphResponse.productId(),
                "h2"
        ));
        components.add(text(
                "source",
                "Live Oracle AI Database graph result rendered as native A2UI controls.",
                "caption"
        ));
        components.add(text("summary", GraphA2AConfiguration.formatResponse(graphResponse)));

        String edgeSummary = graphResponse.edges().stream()
                .map(edge -> edge.getOrDefault("from", "?")
                        + " -> "
                        + edge.getOrDefault("to", "?")
                        + " ("
                        + edge.getOrDefault("label", "RELATED")
                        + ")")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No dependency edges were returned.");
        components.add(component("path-card", "Card", Map.of("child", "path-content")));
        components.add(component(
                "path-content",
                "Column",
                Map.of("children", explicitList(List.of("path-title", "path-text")))
        ));
        components.add(text("path-title", "Dependency path", "h3"));
        components.add(text("path-text", edgeSummary));
        components.add(text("nodes-title", "Click a node to inspect the governed graph facts", "h3"));

        int index = 0;
        for (Map<String, String> node : graphResponse.nodes()) {
            index++;
            String cardId = "node-card-" + index;
            String contentId = "node-content-" + index;
            String buttonId = "inspect-node-" + index;
            String buttonTextId = "inspect-node-text-" + index;
            rootChildren.add(cardId);
            components.add(component(cardId, "Card", Map.of("child", contentId)));
            components.add(component(
                    contentId,
                    "Column",
                    Map.of("children", explicitList(List.of(
                            "node-label-" + index,
                            "node-type-" + index,
                            "node-detail-" + index,
                            "node-metric-" + index,
                            buttonId
                    )))
            ));
            components.add(text("node-label-" + index, node.getOrDefault("label", "Node"), "h3"));
            components.add(text("node-type-" + index, node.getOrDefault("type", "UNKNOWN"), "caption"));
            components.add(text("node-detail-" + index, node.getOrDefault("detail", "")));
            components.add(text("node-metric-" + index, node.getOrDefault("metric", "")));
            components.add(component(
                    buttonId,
                    "Button",
                    Map.of(
                            "child", buttonTextId,
                            "primary", false,
                            "action", Map.of(
                                    "name", "inspectGraphNode",
                                    "context", List.of(
                                            context("productId", graphResponse.productId()),
                                            context("nodeId", node.getOrDefault("id", "")),
                                            context("nodeType", node.getOrDefault("type", "")),
                                            context("nodeLabel", node.getOrDefault("label", ""))
                                    )
                            )
                    )
            ));
            components.add(text(buttonTextId, "Inspect this node"));
        }

        return List.of(
                Map.of(
                        "beginRendering",
                        Map.of("surfaceId", surfaceId, "root", "root")
                ),
                Map.of(
                        "surfaceUpdate",
                        Map.of("surfaceId", surfaceId, "components", components)
                )
        );
    }

    static Map<String, Object> extensionDeclaration() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("supportedCatalogIds", List.of(STANDARD_CATALOG));
        return Map.of(
                "uri", EXTENSION_URI,
                "description", "Provides agent driven UI using the A2UI JSON format.",
                "required", false,
                "params", params
        );
    }

    private static Map<String, Object> component(
            String componentId,
            String componentType,
            Map<String, Object> properties
    ) {
        return Map.of("id", componentId, "component", Map.of(componentType, properties));
    }

    private static Map<String, Object> text(String componentId, String value) {
        return text(componentId, value, "body");
    }

    private static Map<String, Object> text(String componentId, String value, String usageHint) {
        return component(
                componentId,
                "Text",
                Map.of(
                        "text", Map.of("literalString", value == null ? "" : value),
                        "usageHint", usageHint
                )
        );
    }

    private static Map<String, Object> explicitList(List<String> children) {
        return Map.of("explicitList", children);
    }

    private static Map<String, Object> context(String key, String value) {
        return Map.of(
                "key", key,
                "value", Map.of("literalString", value == null ? "" : value)
        );
    }
}
