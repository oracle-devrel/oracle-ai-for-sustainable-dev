package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GraphRequestParser {

    private static final Pattern PRODUCT_ID_PATTERN =
            Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");
    private static final Pattern JSON_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public GraphRequestParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GraphTools.GraphRequest parse(String userInput) {
        String safeInput = userInput == null ? "" : userInput;
        String normalizedInput = normalizeMarkdownEscapes(safeInput);
        String textProductId = extractProductId(safeInput);
        String jsonCandidate = extractJsonCandidate(safeInput);
        if (jsonCandidate == null && !normalizedInput.equals(safeInput)) {
            jsonCandidate = extractJsonCandidate(normalizedInput);
        }

        if (jsonCandidate == null) {
            return new GraphTools.GraphRequest(textProductId, null, false, null);
        }

        try {
            JsonNode root = parsePayloadJson(jsonCandidate);
            JsonNode payloadNode = unwrapPayloadNode(root);

            if (!payloadNode.isObject()) {
                return new GraphTools.GraphRequest(
                        textProductId,
                        null,
                        true,
                        "Structured graph payload must be a JSON object."
                );
            }

            if (!payloadNode.has("nodes") || !payloadNode.has("edges")) {
                return new GraphTools.GraphRequest(
                        textProductId,
                        null,
                        true,
                        "Structured graph payload must include nodes[] and edges[]."
                );
            }

            String payloadProductId = textValue(payloadNode.get("productId"));
            String schemaVersion = textValue(payloadNode.get("schemaVersion"));

            List<GraphTools.GraphNode> nodes = new ArrayList<>();
            for (JsonNode node : payloadNode.path("nodes")) {
                nodes.add(new GraphTools.GraphNode(
                        textValue(node.get("id")),
                        textValue(node.get("type")),
                        textValue(node.get("label")),
                        textValue(node.get("detail")),
                        textValue(node.get("metric"))
                ));
            }

            List<GraphTools.GraphEdge> edges = new ArrayList<>();
            for (JsonNode edge : payloadNode.path("edges")) {
                edges.add(new GraphTools.GraphEdge(
                        textValue(edge.get("from")),
                        textValue(edge.get("to")),
                        textValue(edge.get("label"))
                ));
            }

            return new GraphTools.GraphRequest(
                    textProductId,
                    new GraphTools.GraphPayload(schemaVersion, payloadProductId, nodes, edges),
                    true,
                    null
            );
        } catch (Exception e) {
            return new GraphTools.GraphRequest(
                    textProductId,
                    null,
                    true,
                    "Structured graph payload could not be parsed: " + e.getMessage()
            );
        }
    }

    private static String extractProductId(String userInput) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(userInput);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String extractJsonCandidate(String userInput) {
        String trimmed = normalizeMarkdownEscapes(userInput).trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        Matcher fenceMatcher = JSON_FENCE_PATTERN.matcher(trimmed);
        if (fenceMatcher.find()) {
            return fenceMatcher.group(1);
        }

        return extractEmbeddedJsonObject(trimmed);
    }

    private static String extractEmbeddedJsonObject(String userInput) {
        int length = userInput.length();
        for (int start = 0; start < length; start++) {
            if (userInput.charAt(start) != '{') {
                continue;
            }

            String candidate = balancedJsonObject(userInput, start);
            if (candidate == null) {
                continue;
            }

            try {
                JsonNode root = new ObjectMapper().readTree(candidate);
                JsonNode payloadNode = unwrapPayloadNode(root);
                if (payloadNode.isObject()
                        && (payloadNode.has("nodes")
                        || payloadNode.has("edges")
                        || payloadNode.has("graphPayload")
                        || payloadNode.has("graph"))) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // Keep scanning until we find a valid embedded JSON payload.
            }
        }

        return null;
    }

    private JsonNode parsePayloadJson(String jsonCandidate) throws Exception {
        try {
            return objectMapper.readTree(jsonCandidate);
        } catch (Exception firstFailure) {
            String normalized = normalizeMarkdownEscapes(jsonCandidate);
            if (normalized.equals(jsonCandidate)) {
                throw firstFailure;
            }
            return objectMapper.readTree(normalized);
        }
    }

    private static String normalizeMarkdownEscapes(String input) {
        return input
                .replace("\\[", "[")
                .replace("\\]", "]")
                .replace("\\{", "{")
                .replace("\\}", "}");
    }

    private static String balancedJsonObject(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        return null;
    }

    private static JsonNode unwrapPayloadNode(JsonNode root) {
        if (root.has("graphPayload") && root.get("graphPayload").isObject()) {
            return root.get("graphPayload");
        }
        if (root.has("graph") && root.get("graph").isObject()) {
            return root.get("graph");
        }
        return root;
    }

    private static String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }
}
