package oracleai;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;
import org.springframework.core.env.Environment;

final class InventorySystemCardFactory {

    private InventorySystemCardFactory() {
    }

    static AgentCard buildInventorySystemAgentCard(Environment environment) {
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
        String inventorySystemUrl = appendPath(baseUrl, "/inventory-system");
        String description = "Inventory-system gateway agent. Routes general inventory questions to the Oracle AI "
                + "Database Agent, while reserving dependency-graph requests for the Oracle property graph service "
                + "and map or hotspot requests for the Oracle spatial service.";

        return new AgentCard.Builder()
                .name("oracle_inventory_system_agent")
                .description(description)
                .url(inventorySystemUrl)
                .version("0.0.3")
                .capabilities(new AgentCapabilities(false, false, false, List.of()))
                .defaultInputModes(List.of("text/plain"))
                .defaultOutputModes(List.of("image/png", "text/html", "text/plain"))
                .skills(List.of(
                        new AgentSkill(
                                "oracle_inventory_system_agent",
                                "inventory-system-router",
                                "Routes inventory questions to the best specialist: Oracle AI Database Agent for "
                                        + "general data analysis, Oracle property graph for dependency views, or "
                                        + "Oracle spatial for hotspot rendering.",
                                List.of(
                                        "llm",
                                        "router",
                                        "inventory",
                                        "graph",
                                        "spatial",
                                        "database",
                                        "a2a"
                                ),
                                List.of(
                                        "Which products are at risk of stockouts next quarter, and which regions are driving that risk?",
                                        "Create a chart of projected revenue impact by region for the current quarter.",
                                        "Show the supply chain dependency graph for SKU-500 and explain the active alert.",
                                        "Show that on a map for SKU-500 and highlight the warehouse hotspots."
                                ),
                                List.of("text/plain"),
                                List.of("image/png", "text/html", "text/plain"),
                                null
                        )
                ))
                .supportsAuthenticatedExtendedCard(false)
                .securitySchemes(OracleAiDatabaseAgentCardFactory.buildOAuthSecuritySchemes(environment))
                .security(OracleAiDatabaseAgentCardFactory.buildOAuthSecurityRequirements())
                .preferredTransport("JSONRPC")
                .protocolVersion("0.3.0")
                .build();
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
