package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class GeminiVisualRenderer {

    public enum RenderMode {
        DETERMINISTIC,
        GEMINI,
        BOTH;

        static RenderMode from(String value) {
            if (value == null || value.isBlank()) {
                return DETERMINISTIC;
            }
            return switch (value.trim().toUpperCase()) {
                case "GEMINI" -> GEMINI;
                case "BOTH" -> BOTH;
                default -> DETERMINISTIC;
            };
        }
    }

    private static final String DEFAULT_MODEL = "gemini-3.1-flash-image";
    private static final URI GEMINI_INTERACTIONS_URI =
            URI.create("https://generativelanguage.googleapis.com/v1beta/interactions");

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiVisualRenderer(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public RenderMode renderMode() {
        return RenderMode.from(firstNonBlank(
                environment.getProperty("VISUAL_RENDERER"),
                environment.getProperty("IMAGE_RENDERER")
        ));
    }

    public boolean includeDeterministic() {
        RenderMode mode = renderMode();
        return mode == RenderMode.DETERMINISTIC || mode == RenderMode.BOTH;
    }

    public boolean includeGemini() {
        RenderMode mode = renderMode();
        return mode == RenderMode.GEMINI || mode == RenderMode.BOTH;
    }

    public Optional<String> renderGraph(GraphTools.GraphResponse response) {
        return renderImage(graphPrompt(response));
    }

    public Optional<String> renderSpatial(SpatialTools.SpatialResponse response) {
        return renderImage(spatialPrompt(response));
    }

    private Optional<String> renderImage(String prompt) {
        String apiKey = firstNonBlank(
                environment.getProperty("GEMINI_API_KEY"),
                environment.getProperty("GOOGLE_API_KEY")
        );
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("model", firstNonBlank(
                    environment.getProperty("GEMINI_IMAGE_MODEL"),
                    environment.getProperty("VISUAL_IMAGE_MODEL"),
                    DEFAULT_MODEL
            ));

            ArrayNode input = requestJson.putArray("input");
            ObjectNode textInput = input.addObject();
            textInput.put("type", "text");
            textInput.put("text", prompt);

            HttpRequest request = HttpRequest.newBuilder(GEMINI_INTERACTIONS_URI)
                    .timeout(Duration.ofSeconds(longProperty("GEMINI_IMAGE_TIMEOUT_SECONDS", 45)))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestJson)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode outputImage = responseJson.path("output_image");
            String imageData = outputImage.path("data").asText("");
            if (!imageData.isBlank()) {
                return Optional.of(imageData);
            }

            return findImageData(responseJson);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> findImageData(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        if (node.isObject()) {
            String mimeType = node.path("mime_type").asText(node.path("mimeType").asText(""));
            String data = node.path("data").asText("");
            if (!data.isBlank() && mimeType.startsWith("image/")) {
                return Optional.of(data);
            }
            for (JsonNode child : node) {
                Optional<String> result = findImageData(child);
                if (result.isPresent()) {
                    return result;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<String> result = findImageData(child);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    private static String graphPrompt(GraphTools.GraphResponse response) {
        return """
                Create a polished executive supply-chain dependency diagram as a PNG.
                Use the structured data exactly as the source of truth. Do not invent nodes, edge labels, product IDs,
                supplier names, locations, or metrics. Render the product flow left to right, highlight ALERT nodes in red,
                and include a small caption that says "Illustrative Gemini rendering; deterministic Oracle graph image is the source of truth."

                Product ID: %s
                Source mode: %s
                Source detail: %s
                Nodes: %s
                Edges: %s
                """.formatted(
                response.productId(),
                response.sourceMode(),
                response.sourceDetail(),
                response.nodes(),
                response.edges()
        );
    }

    private static String spatialPrompt(SpatialTools.SpatialResponse response) {
        return """
                Create a polished executive warehouse hotspot map as a PNG.
                Use the structured data exactly as the source of truth. Do not invent warehouses, product IDs, risk levels,
                regions, coordinates, or metrics. Show the hotspot region, ranked warehouse markers, and transfer pressure.
                Include a small caption that says "Illustrative Gemini rendering; deterministic Oracle Spatial image is the source of truth."

                Product ID: %s
                Summary: %s
                Source mode: %s
                Source detail: %s
                Hotspots: %s
                """.formatted(
                response.productId(),
                response.summaryText(),
                response.sourceMode(),
                response.sourceDetail(),
                response.hotspots()
        );
    }

    private long longProperty(String name, long defaultValue) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
