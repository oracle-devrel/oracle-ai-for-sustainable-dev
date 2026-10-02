package com.oracle.demo.interactiveai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        application.setDefaultProperties(Map.of(
                "server.address", "127.0.0.1",
                "server.port", System.getenv().getOrDefault("AGENT_PORT", "8080"),
                "spring.application.name", "interactive-ai-agent-service"));
        application.run(args);
    }

    static String recommendationsJson(List<TransferRecommendation> recommendations) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "recommendations",
                recommendations.stream().map(Json::recommendationMap).toList()));
    }

    static String reviewJson(
            List<TransferRecommendation> recommendations,
            String approvalId,
            boolean writesAllowed) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "approvalId", approvalId,
                "writesAllowed", writesAllowed,
                "recommendations",
                recommendations.stream().map(Json::recommendationMap).toList()));
    }

    static String spatialJson(String sku, List<SpatialHotspot> hotspots) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "sku", sku,
                "hotspots", hotspots.stream().map(hotspot -> Map.ofEntries(
                        Map.entry("productId", hotspot.productId()),
                        Map.entry("sku", hotspot.sku()),
                        Map.entry("locationId", hotspot.locationId()),
                        Map.entry("locationCode", hotspot.locationCode()),
                        Map.entry("locationName", hotspot.locationName()),
                        Map.entry("latitude", hotspot.latitude()),
                        Map.entry("longitude", hotspot.longitude()),
                        Map.entry("stockoutRiskScore", hotspot.stockoutRiskScore()),
                        Map.entry("riskLevel", hotspot.riskLevel()),
                        Map.entry("recommendedTransferQuantity", hotspot.recommendedTransferQuantity()),
                        Map.entry("recommendedRole", hotspot.recommendedRole()))).toList()));
    }
}
