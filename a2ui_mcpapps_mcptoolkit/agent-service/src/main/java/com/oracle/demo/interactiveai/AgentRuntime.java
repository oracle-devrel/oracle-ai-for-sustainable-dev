package com.oracle.demo.interactiveai;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
final class AgentRuntime {
    private final Map<String, SupplyChainRepository> repositories;
    private final ApprovalService approvals = new ApprovalService();
    private final String actor = System.getenv().getOrDefault(
            "REQUESTED_BY", "supply.planner@example.com");

    AgentRuntime() {
        Map<String, SupplyChainRepository> configured = new LinkedHashMap<>();
        configured.put("full", McpToolkitSupplyChainGateway.fromEnvironment(System.getenv()));
        if (System.getenv().containsKey("ORACLE_MCP_READ_URL")) {
            configured.put(
                    "environmental",
                    McpToolkitSupplyChainGateway.secondaryFromEnvironment(System.getenv()));
        }
        repositories = Map.copyOf(configured);
    }

    ApprovalService approvals() {
        return approvals;
    }

    String accessProfile(Map<String, String> values) {
        String profile = values.getOrDefault("accessProfile", "full");
        if (!repositories.containsKey(profile)) {
            throw new IllegalArgumentException(
                    "Unknown or unavailable access profile: " + profile);
        }
        return profile;
    }

    SupplyChainRepository repository(String accessProfile) {
        return repositories.get(accessProfile);
    }

    String scopedActor(String accessProfile) {
        return actor + ":" + accessProfile;
    }

    @PreDestroy
    void close() {
        repositories.values().forEach(AgentRuntime::closeQuietly);
    }

    private static void closeQuietly(Object candidate) {
        if (!(candidate instanceof AutoCloseable closeable)) return;
        try {
            closeable.close();
        } catch (Exception exception) {
            System.err.println("Unable to close MCP repository: " + exception.getMessage());
        }
    }
}
