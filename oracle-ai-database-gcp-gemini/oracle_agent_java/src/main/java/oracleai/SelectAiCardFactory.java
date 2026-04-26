package oracleai;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;
import org.springframework.core.env.Environment;

final class SelectAiCardFactory {

    private SelectAiCardFactory() {
    }

    static AgentCard buildSelectAiAgentCard(Environment environment) {
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
        String selectAiUrl = appendPath(baseUrl, "/select-ai");

        return new AgentCard(
                "oracle_select_ai_agent",
                "Select AI-style inventory analysis agent. It can answer natural-language questions over the demo risk tables using DBMS_CLOUD_AI when a profile is configured, and otherwise falls back to direct Oracle SQL summaries.",
                selectAiUrl,
                null,
                "0.0.1",
                null,
                new AgentCapabilities(false, false, false, List.of()),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(
                        new AgentSkill(
                                "oracle_select_ai_agent",
                                "select-ai-analyst",
                                "Natural-language analyst for Oracle inventory and warehouse risk data.",
                                List.of("llm", "sql", "analytics"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("text/plain"),
                                null
                        ),
                        new AgentSkill(
                                "oracle_select_ai_agent-answerInventoryQuestion",
                                "answerInventoryQuestion",
                                "Answer an inventory-risk question, optionally using DBMS_CLOUD_AI.GENERATE with a configured Select AI profile.",
                                List.of("llm", "tools", "database"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("text/plain"),
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
