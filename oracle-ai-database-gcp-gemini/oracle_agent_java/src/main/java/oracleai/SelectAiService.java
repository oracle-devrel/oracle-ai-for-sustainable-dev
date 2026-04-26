package oracleai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SelectAiService {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");
    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String GENERATE_SQL =
            "SELECT DBMS_CLOUD_AI.GENERATE(prompt => ?, profile_name => ?, action => ?) AS ai_response FROM dual";
    private static final String SUMMARY_SQL = """
            SELECT
                product_id,
                product_name,
                quarter_label,
                risk_level,
                stockout_probability,
                at_risk_units,
                projected_revenue_impact_usd,
                primary_region,
                recommendation_summary
            FROM sc_inventory_risk_summary
            ORDER BY stockout_probability DESC
            FETCH FIRST 3 ROWS ONLY
            """;
    private static final String DRIVER_SQL = """
            SELECT
                product_id,
                warehouse_name,
                county_name,
                state_code,
                hotspot_rank,
                hotspot_score,
                coverage_days,
                revenue_impact_usd
            FROM sc_inventory_risk_demo_v
            WHERE product_id = ?
            ORDER BY hotspot_rank
            """;

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SelectAiService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public SelectAiResult answer(String userInput) {
        String normalizedPrompt = normalizePrompt(userInput);
        String action = determineAction(normalizedPrompt);
        String productId = extractProductId(normalizedPrompt);
        PromptScope promptScope = resolvePromptScope();
        String profileName = firstNonBlank(
                environment.getProperty("SELECT_AI_PROFILE"),
                environment.getProperty("DBMS_CLOUD_AI_PROFILE")
        );
        String openAiApiKey = resolveOpenAiApiKey();

        if (!profileName.isBlank()) {
            try {
                return runSelectAi(normalizedPrompt, productId, action, profileName, promptScope);
            } catch (Exception exception) {
                if (promptScope == PromptScope.INVENTORY_DEMO && !openAiApiKey.isBlank()) {
                    try {
                        return runDirectOpenAi(normalizedPrompt, productId, action, profileName, openAiApiKey, exception);
                    } catch (Exception openAiException) {
                        return fallbackResponse(
                                normalizedPrompt,
                                productId,
                                action,
                                exception.getMessage() + "; direct OpenAI fallback also failed: " + openAiException.getMessage(),
                                promptScope,
                                profileName
                        );
                    }
                }
                return fallbackResponse(
                        normalizedPrompt,
                        productId,
                        action,
                        exception.getMessage(),
                        promptScope,
                        profileName
                );
            }
        }

        if (promptScope == PromptScope.INVENTORY_DEMO && !openAiApiKey.isBlank()) {
            try {
                return runDirectOpenAi(normalizedPrompt, productId, action, "", openAiApiKey, null);
            } catch (Exception exception) {
                return fallbackResponse(
                        normalizedPrompt,
                        productId,
                        action,
                        exception.getMessage(),
                        promptScope,
                        profileName
                );
            }
        }

        return fallbackResponse(
                normalizedPrompt,
                productId,
                action,
                "SELECT_AI_PROFILE is not configured",
                promptScope,
                profileName
        );
    }

    private SelectAiResult runSelectAi(
            String prompt,
            String productId,
            String action,
            String profileName,
            PromptScope promptScope
    ) throws Exception {
        return new SelectAiResult(
                runSelectAiPrompt(buildProfileAwarePrompt(prompt, productId, action, profileName, promptScope), action, profileName),
                "select-ai",
                action,
                "DBMS_CLOUD_AI.GENERATE via profile " + profileName
        );
    }

    private SelectAiResult runDirectOpenAi(
            String prompt,
            String productId,
            String action,
            String profileName,
            String openAiApiKey,
            Exception dbmsCloudAiFailure
    ) throws Exception {
        String sourceDetail = profileName.isBlank()
                ? "OpenAI chat.completions direct over live Oracle table context"
                : "OpenAI chat.completions direct after DBMS_CLOUD_AI.GENERATE failed for profile " + profileName;

        return new SelectAiResult(
                callOpenAiChat(buildDirectOpenAiPrompt(prompt, productId, action), openAiApiKey),
                "openai-direct-fallback",
                action,
                sourceDetail
        );
    }

    private String runSelectAiPrompt(String prompt, String action, String profileName) throws Exception {
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(GENERATE_SQL)
        ) {
            statement.setString(1, prompt);
            statement.setString(2, profileName);
            statement.setString(3, action);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("DBMS_CLOUD_AI.GENERATE returned no rows.");
                }

                String content = resultSet.getString("ai_response");
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("DBMS_CLOUD_AI.GENERATE returned an empty response.");
                }
                return content.trim();
            }
        }
    }

    private String callOpenAiChat(String prompt, String apiKey) throws Exception {
        String modelName = firstNonBlank(
                environment.getProperty("SELECT_AI_OPENAI_MODEL"),
                environment.getProperty("OPENAI_MODEL_NAME"),
                "gpt-4o-mini"
        );

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelName);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 700);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "user")
                .put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(OPENAI_CHAT_COMPLETIONS_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI chat.completions returned HTTP " + response.statusCode()
                    + ": " + trimForError(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
        if (content.isBlank()) {
            throw new IllegalStateException("OpenAI chat.completions returned an empty message.");
        }
        return content;
    }

    private String buildProfileAwarePrompt(
            String userPrompt,
            String productId,
            String action,
            String profileName,
            PromptScope promptScope
    ) {
        if (promptScope == PromptScope.GENERIC_PROFILE) {
            return buildGenericProfilePrompt(userPrompt, productId, action, profileName);
        }

        String objectOwner = firstNonBlank(
                environment.getProperty("SELECT_AI_OBJECT_OWNER"),
                environment.getProperty("DB_SCHEMA_OWNER"),
                environment.getProperty("DB_USERNAME"),
                "ADMIN"
        ).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        builder.append("You are answering questions over an Oracle demo inventory-risk dataset. ");

        if ("showsql".equals(action)) {
            builder.append("Return only the SQL needed to answer the user's question. ");
        } else if ("explainsql".equals(action)) {
            builder.append("Explain the SQL or query approach needed to answer the user's question. ");
        } else {
            builder.append("Answer in concise business language using only the data available in the listed Oracle objects. ");
            builder.append("If the question cannot be answered from these objects, say so clearly. ");
        }

        if (productId != null && !productId.isBlank()) {
            builder.append("If the request references product ").append(productId).append(", keep the answer focused on that product when appropriate. ");
        }

        builder.append("Available Oracle objects and their purpose:\n");
        builder.append("- ").append(objectOwner).append(".SC_PRODUCTS(product_id, product_name): product master for the demo SKUs.\n");
        builder.append("- ").append(objectOwner).append(".SC_WAREHOUSES(warehouse_id, warehouse_name): warehouse master referenced by the hotspot rows.\n");
        builder.append("- ").append(objectOwner).append(".SC_INVENTORY_RISK_SUMMARY(product_id, quarter_label, risk_level, stockout_probability, at_risk_units, projected_revenue_impact_usd, primary_region, recommendation_summary, active_flag): product-level quarterly stockout risk summary.\n");
        builder.append("- ").append(objectOwner).append(".SC_WAREHOUSE_GEO(warehouse_id, warehouse_code, county_name, state_code, region_name, latitude, longitude): warehouse geography and region metadata.\n");
        builder.append("- ").append(objectOwner).append(".SC_WAREHOUSE_RISK_SNAPSHOT(product_id, warehouse_id, hotspot_rank, hotspot_score, coverage_days, backlog_units, service_level_pct, at_risk_units, revenue_impact_usd, risk_level, recommended_role, active_flag): warehouse-level hotspot metrics by product.\n");
        builder.append("- ").append(objectOwner).append(".SC_INVENTORY_RISK_DEMO_V(product_id, product_name, quarter_label, overall_risk_level, stockout_probability, product_at_risk_units, projected_revenue_impact_usd, primary_region, recommendation_summary, warehouse_name, county_name, state_code, region_name, hotspot_rank, hotspot_score, coverage_days, backlog_units, service_level_pct, at_risk_units, revenue_impact_usd, recommended_role, active_flag): flattened view that combines product risk, warehouse hotspots, and geography.\n");
        builder.append("Prefer ").append(objectOwner).append(".SC_INVENTORY_RISK_DEMO_V for combined product, warehouse, county, state, region, hotspot, and coverage questions. ");
        builder.append("Use ").append(objectOwner).append(".SC_INVENTORY_RISK_SUMMARY for top-risk product and revenue-impact questions. ");
        builder.append("Use ").append(objectOwner).append(".SC_WAREHOUSE_RISK_SNAPSHOT with ").append(objectOwner).append(".SC_WAREHOUSE_GEO for warehouse hotspot detail when needed.\n\n");
        builder.append("User question:\n").append(userPrompt);
        return builder.toString();
    }

    private String buildGenericProfilePrompt(String userPrompt, String productId, String action, String profileName) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are answering questions over Oracle Database objects exposed through the DBMS_CLOUD_AI profile ");
        builder.append(profileName == null || profileName.isBlank() ? "configured for this session" : profileName);
        builder.append(". ");

        if ("showsql".equals(action)) {
            builder.append("Return only the SQL needed to answer the user's question. ");
        } else if ("explainsql".equals(action)) {
            builder.append("Explain the SQL or query approach needed to answer the user's question. ");
        } else {
            builder.append("Use any table or view available through that profile. ");
            builder.append("Do not assume the dataset is limited to the demo inventory-risk tables. ");
            builder.append("If the user asks about sales, channels, customers, products, finance, operations, or any other domain, answer from whatever objects the profile exposes. ");
            builder.append("If the question cannot be answered from the profile-accessible objects, say so clearly and briefly explain what data appears to be missing. ");
            builder.append("Keep the answer concise and grounded in the Oracle objects available through the profile. ");
        }

        if (productId != null && !productId.isBlank()) {
            builder.append("If the request references product ").append(productId).append(", keep the answer focused on that product when relevant. ");
        }

        builder.append("\n\nUser question:\n").append(userPrompt);
        return builder.toString();
    }

    private String buildDirectOpenAiPrompt(String userPrompt, String productId, String action) {
        if ("showsql".equals(action) || "explainsql".equals(action)) {
            return buildProfileAwarePrompt(userPrompt, productId, action, "", PromptScope.INVENTORY_DEMO);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("You are answering questions about an Oracle inventory-risk demo dataset. ");
        builder.append("Only use the Oracle-derived context below. ");
        builder.append("If the answer is not supported by the context, say so plainly. ");
        builder.append("Keep the answer concise and factual.\n\n");
        builder.append("Oracle context:\n");
        builder.append(buildRiskNarrative("OpenAI direct context", false)).append("\n\n");
        builder.append(buildRegionDriverNarrative(productId, "OpenAI direct context", false)).append("\n\n");
        builder.append("User question:\n").append(userPrompt);
        return builder.toString();
    }

    private SelectAiResult fallbackResponse(
            String prompt,
            String productId,
            String action,
            String reason,
            PromptScope promptScope,
            String profileName
    ) {
        if (promptScope == PromptScope.GENERIC_PROFILE) {
            return genericProfileFallbackResponse(action, reason, profileName);
        }

        String lower = prompt.toLowerCase(Locale.ROOT);
        boolean wantsRiskSummary = lower.contains("product")
                || lower.contains("stockout")
                || lower.contains("next quarter")
                || lower.contains("risk");
        boolean wantsRegionDrivers = asksForRegions(lower);

        if ("showsql".equals(action)) {
            String sql = DemoInventoryData.fallbackShowSql(productId, asksForRegions(lower));
            return new SelectAiResult(
                    "Fallback SQL for the current demo tables:\n\n" + sql
                            + "\nThe live Select AI profile was unavailable, so this is a deterministic SQL fallback ("
                            + reason + ").",
                    "direct-sql-fallback",
                    action,
                    "Deterministic SQL fallback"
            );
        }

        if (wantsRiskSummary && wantsRegionDrivers) {
            return new SelectAiResult(
                    buildRiskNarrative(reason, false) + "\n\n" + buildRegionDriverNarrative(productId, reason, false)
                            + "\n\n"
                            + buildFallbackFooter(reason),
                    "direct-sql-fallback",
                    action,
                    "Direct Oracle SQL summary over demo tables"
            );
        }

        if (wantsRegionDrivers) {
            return new SelectAiResult(
                    buildRegionDriverNarrative(productId, reason, true),
                    "direct-sql-fallback",
                    action,
                    "Direct Oracle SQL summary over demo tables"
            );
        }

        return new SelectAiResult(
                buildRiskNarrative(reason, true) + "\n" + buildFallbackFooter(reason),
                "direct-sql-fallback",
                action,
                "Direct Oracle SQL summary over demo tables"
        );
    }

    private SelectAiResult genericProfileFallbackResponse(String action, String reason, String profileName) {
        String resolvedProfileName = firstNonBlank(profileName, "the configured DBMS_CLOUD_AI profile");
        StringBuilder builder = new StringBuilder();
        builder.append("The local Select AI fallback could not answer this request through ")
                .append(resolvedProfileName)
                .append(". ");
        builder.append("This service is running in generic profile mode, so it did not fall back to the demo inventory tables. ");
        if ("showsql".equals(action)) {
            builder.append("Try again with a more specific prompt that names the schema object, column, or metric you want. ");
        } else if ("explainsql".equals(action)) {
            builder.append("Try again with a more specific prompt that names the schema object or business metric you want explained. ");
        } else {
            builder.append("Try again with a more specific prompt naming the table, view, or metric you want, or restore the Oracle AI Database agent handoff if you expected this request to run remotely. ");
        }
        builder.append("Underlying issue: ").append(reason).append(".");

        return new SelectAiResult(
                builder.toString(),
                "generic-profile-fallback",
                action,
                "Generic DBMS_CLOUD_AI fallback without demo-table assumptions"
        );
    }

    private String buildRiskNarrative(String reason, boolean includeFooter) {
        List<DemoInventoryData.InventoryRiskSummary> summaries = new ArrayList<>();
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                summaries.add(new DemoInventoryData.InventoryRiskSummary(
                        resultSet.getString("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("quarter_label"),
                        resultSet.getString("risk_level"),
                        resultSet.getDouble("stockout_probability"),
                        resultSet.getInt("at_risk_units"),
                        resultSet.getInt("projected_revenue_impact_usd"),
                        resultSet.getString("primary_region"),
                        resultSet.getString("recommendation_summary")
                ));
            }
        } catch (Exception ignored) {
            summaries = DemoInventoryData.topSummaries();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Inventory-risk summary for the next quarter:\n");
        for (DemoInventoryData.InventoryRiskSummary summary : summaries) {
            builder.append("- ")
                    .append(summary.productId())
                    .append(" (")
                    .append(summary.productName())
                    .append(") is ")
                    .append(summary.riskLevel())
                    .append(" risk with stockout probability ")
                    .append(String.format("%.0f%%", summary.stockoutProbability() * 100.0))
                    .append(", at-risk units ")
                    .append(summary.atRiskUnits())
                    .append(", projected revenue impact $")
                    .append(String.format("%,d", summary.projectedRevenueImpactUsd()))
                    .append(", driven mainly by ")
                    .append(summary.primaryRegion())
                    .append(".\n");
        }
        if (includeFooter) {
            builder.append(buildFallbackFooter(reason));
        }
        return builder.toString().trim();
    }

    private String buildRegionDriverNarrative(String productId, String reason, boolean includeFooter) {
        List<RegionDriverRow> rows = new ArrayList<>();
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(DRIVER_SQL)
        ) {
            statement.setString(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new RegionDriverRow(
                            resultSet.getString("product_id"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("county_name"),
                            resultSet.getString("state_code"),
                            resultSet.getInt("hotspot_rank"),
                            resultSet.getDouble("hotspot_score"),
                            resultSet.getDouble("coverage_days"),
                            resultSet.getDouble("revenue_impact_usd")
                    ));
                }
            }
        } catch (Exception ignored) {
            for (DemoInventoryData.WarehouseHotspot hotspot : DemoInventoryData.hotspotsFor(productId)) {
                rows.add(new RegionDriverRow(
                        hotspot.productId(),
                        hotspot.warehouseName(),
                        hotspot.countyName(),
                        hotspot.stateCode(),
                        hotspot.hotspotRank(),
                        hotspot.hotspotScore(),
                        hotspot.coverageDays(),
                        hotspot.revenueImpactUsd()
                ));
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Regional drivers for ").append(productId).append(":\n");
        for (RegionDriverRow row : rows) {
            builder.append("- Rank ")
                    .append(row.hotspotRank())
                    .append(": ")
                    .append(row.warehouseName())
                    .append(" in ")
                    .append(row.countyName())
                    .append(" County, ")
                    .append(row.stateCode())
                    .append(" has hotspot score ")
                    .append(String.format("%.2f", row.hotspotScore()))
                    .append(", coverage ")
                    .append(String.format("%.1f", row.coverageDays()))
                    .append(" days, and revenue impact about $")
                    .append(String.format("%,.0f", row.revenueImpactUsd()))
                    .append(".\n");
        }
        if (includeFooter) {
            builder.append(buildFallbackFooter(reason));
        }
        return builder.toString().trim();
    }

    private String buildFallbackFooter(String reason) {
        return "This used a deterministic fallback over the demo tables because the live Select AI path was unavailable ("
                + reason
                + ").";
    }

    private static String normalizePrompt(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "Which products are at risk of stockouts next quarter?";
        }
        return userInput.trim();
    }

    private static boolean asksForRegions(String prompt) {
        return prompt.contains("region")
                || prompt.contains("warehouse")
                || prompt.contains("county")
                || prompt.contains("map")
                || prompt.contains("driving");
    }

    private static String determineAction(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("show sql") || lower.contains("showsql")) {
            return "showsql";
        }
        if (lower.contains("explain sql") || lower.contains("explainsql")) {
            return "explainsql";
        }
        if (lower.contains("chat")) {
            return "chat";
        }
        return "narrate";
    }

    private static String extractProductId(String prompt) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(prompt == null ? "" : prompt.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "SKU-500";
    }

    private PromptScope resolvePromptScope() {
        String configuredValue = firstNonBlank(
                environment.getProperty("SELECT_AI_PROMPT_SCOPE"),
                environment.getProperty("INVENTORY_SYSTEM_SELECT_AI_SCOPE"),
                "generic"
        );
        if ("inventory-demo".equalsIgnoreCase(configuredValue)
                || "inventory_demo".equalsIgnoreCase(configuredValue)
                || "demo".equalsIgnoreCase(configuredValue)) {
            return PromptScope.INVENTORY_DEMO;
        }
        return PromptScope.GENERIC_PROFILE;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String resolveOpenAiApiKey() {
        String configuredValue = firstNonBlank(
                environment.getProperty("OPENAI_API_KEY"),
                environment.getProperty("SELECT_AI_OPENAI_API_KEY")
        );
        if (!configuredValue.isBlank()) {
            return configuredValue;
        }

        String keyFilePath = firstNonBlank(
                environment.getProperty("OPENAI_API_KEY_FILE"),
                environment.getProperty("SELECT_AI_OPENAI_API_KEY_FILE")
        );
        if (keyFilePath.isBlank()) {
            return "";
        }

        try {
            return Files.readString(Path.of(keyFilePath)).trim();
        } catch (IOException exception) {
            return "";
        }
    }

    private static String trimForError(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240) + "...";
    }

    public record SelectAiResult(
            String responseText,
            String executionMode,
            String action,
            String sourceDetail
    ) {}

    private enum PromptScope {
        GENERIC_PROFILE,
        INVENTORY_DEMO
    }

    private record RegionDriverRow(
            String productId,
            String warehouseName,
            String countyName,
            String stateCode,
            int hotspotRank,
            double hotspotScore,
            double coverageDays,
            double revenueImpactUsd
    ) {}
}
