package oracleai;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;
import org.springframework.core.env.Environment;

final class SpatialAgentCardFactory {

    private SpatialAgentCardFactory() {
    }

    static AgentCard buildSpatialAgentCard(Environment environment) {
        String publicProtocol = valueOrDefault(environment, "PUBLIC_PROTOCOL", "http");
        String publicHost = valueOrDefault(environment, "PUBLIC_HOST", "localhost");
        String graphPort = valueOrDefault(
                environment,
                "GRAPH_AGENT_PORT",
                valueOrDefault(environment, "PORT", "8081")
        );
        String baseUrl = firstNonBlank(
                environment.getProperty("GRAPH_AGENT_URL"),
                environment.getProperty("A2A_URL"),
                String.format("%s://%s:%s", publicProtocol, publicHost, graphPort)
        );
        String spatialUrl = appendPath(baseUrl, "/spatial");

        return new AgentCard(
                "oracle_spatial_agent",
                "Spatial hotspot specialist for Oracle inventory risk workflows. It renders a PNG map from warehouse hotspot data, highlighting the primary pressure point and suggested relief route.",
                spatialUrl,
                null,
                "0.0.1",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text/plain"),
                List.of("image/png", "text/plain"),
                List.of(
                        new AgentSkill(
                                "oracle_spatial_agent",
                                "spatial-hotspot-renderer",
                                "Specialist in Oracle spatial hotspot visualizations for warehouse and inventory pressure.",
                                List.of("llm", "spatial"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("image/png", "text/plain"),
                                null
                        ),
                        new AgentSkill(
                                "oracle_spatial_agent-renderHotspotMap",
                                "renderHotspotMap",
                                "Render a PNG hotspot map for a product such as SKU-500, using Oracle-backed warehouse risk data when available.",
                                List.of("llm", "tools", "map"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("image/png"),
                                null
                        )
                ),
                false,
                null,
                null,
                null,
                List.of(),
                "JSONRPC",
                "0.3.0",
                null
        );
    }

    private static String appendPath(String baseUrl, String path) {
        String trimmedBase = baseUrl == null ? "" : baseUrl.trim();
        if (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }
        return trimmedBase + path;
    }

    private static String valueOrDefault(Environment environment, String key, String defaultValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }
}
