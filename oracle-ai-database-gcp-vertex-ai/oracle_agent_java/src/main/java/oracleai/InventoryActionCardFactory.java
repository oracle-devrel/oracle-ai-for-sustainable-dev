package oracleai;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;
import org.springframework.core.env.Environment;

final class InventoryActionCardFactory {

    private InventoryActionCardFactory() {
    }

    static AgentCard buildInventoryActionAgentCard(Environment environment) {
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
        String inventoryActionUrl = appendPath(baseUrl, "/inventory-action");

        return new AgentCard(
                "oracle_inventory_action_agent",
                "ADK-based coordinator for final-stage inventory action planning. It gathers graph, spatial, and external signals, checks policy, and drafts a recommended inventory move that still requires approval.",
                inventoryActionUrl,
                null,
                "0.0.2",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text/plain"),
                List.of("application/json", "text/plain"),
                List.of(
                        new AgentSkill(
                                "oracle_inventory_action_agent",
                                "inventory-action-coordinator",
                                "Coordinates the final inventory action stage by gathering evidence and recommending a transfer, expedite, substitute, or hold action.",
                                List.of("llm", "orchestration"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("application/json", "text/plain"),
                                null
                        ),
                        new AgentSkill(
                                "oracle_inventory_action_agent-recommendInventoryAction",
                                "recommendInventoryAction",
                                "Given a product or risk prompt, gather graph, spatial, and external evidence before proposing a draft inventory move and indicating whether approval is required.",
                                List.of("llm", "tools", "inventory"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("application/json", "text/plain"),
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
