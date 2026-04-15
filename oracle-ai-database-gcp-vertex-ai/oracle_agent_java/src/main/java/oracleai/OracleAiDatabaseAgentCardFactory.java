package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.a2a.spec.AuthorizationCodeOAuthFlow;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OAuthFlows;
import io.a2a.spec.SecurityScheme;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

final class OracleAiDatabaseAgentCardFactory {

    private static final String DEFAULT_AGENT_URL =
            "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/a2a/v1/agents/oracle_ai_database_agent";
    private static final String DEFAULT_AUTHORIZATION_URL =
            "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/authorize";
    private static final String DEFAULT_TOKEN_URL =
            "https://dataaccess.adb.us-ashburn-1.oraclecloudapps.com/adb/auth/v1/connect/token";

    private OracleAiDatabaseAgentCardFactory() {
    }

    static ObjectNode buildOracleAiDatabaseAgentCard(Environment environment, ObjectMapper objectMapper) {
        String agentUrl = resolveAgentUrl(environment);
        String authorizationUrl = resolveAuthorizationUrl(environment);
        String tokenUrl = resolveTokenUrl(environment);

        ObjectNode card = objectMapper.createObjectNode();
        card.put("name", "Oracle AI Database Agent");
        card.put(
                "description",
                "The Oracle AI Database Agent provides a seamless natural language interface for querying "
                        + "enterprise data stored in Oracle Database on Google Cloud."
        );
        card.put("url", agentUrl);
        card.put("version", "1.0");

        String iconUrl = environment.getProperty("ORACLE_AI_DATABASE_AGENT_ICON_URL");
        if (iconUrl != null && !iconUrl.isBlank()) {
            card.put("iconUrl", iconUrl.trim());
        }

        ObjectNode capabilities = card.putObject("capabilities");
        capabilities.put("streaming", false);
        capabilities.put("pushNotifications", false);
        capabilities.put("stateTransitionHistory", false);

        ArrayNode defaultInputModes = card.putArray("defaultInputModes");
        defaultInputModes.add("text/plain");

        ArrayNode defaultOutputModes = card.putArray("defaultOutputModes");
        defaultOutputModes.add("text/plain");

        ArrayNode skills = card.putArray("skills");
        addSkill(
                skills,
                "5",
                "SQL Query Executor",
                "Executes database queries based on natural language questions. Converts plain English requests "
                        + "into optimized SQL, runs them against your Oracle AI Database, and returns the results.",
                "SQL",
                "database",
                "query",
                "data access",
                "analysis"
        );
        addSkill(
                skills,
                "4",
                "Distinct Values Finder",
                "Returns all unique values from a specified database column, with optional pattern matching.",
                "distinct",
                "values",
                "data profiling",
                "database",
                "column analysis"
        );
        addSkill(
                skills,
                "6",
                "Range Values Checker",
                "Retrieves the minimum and maximum values for a numeric, date, or timestamp column.",
                "range",
                "min",
                "max",
                "numeric",
                "date",
                "database",
                "column analysis"
        );
        addSkill(
                skills,
                "7",
                "Chart Generator",
                "Creates charts, graphs, or visualizations based on your data and preferences.",
                "chart",
                "graph",
                "visualization",
                "data display",
                "reporting"
        );

        card.put("supportsAuthenticatedExtendedCard", false);

        ObjectNode securitySchemes = card.putObject("securitySchemes");
        ObjectNode oauth2 = securitySchemes.putObject("oauth2");
        oauth2.put("type", "oauth2");
        ObjectNode flows = oauth2.putObject("flows");
        ObjectNode authorizationCode = flows.putObject("authorizationCode");
        authorizationCode.put("authorizationUrl", authorizationUrl);
        authorizationCode.put("refreshUrl", tokenUrl);
        authorizationCode.put("tokenUrl", tokenUrl);
        authorizationCode.putObject("scopes").put("openid", "This is a mandatory scope.");

        ArrayNode security = card.putArray("security");
        security.addObject().putArray("oauth2");

        ArrayNode additionalInterfaces = card.putArray("additionalInterfaces");
        additionalInterfaces.addObject()
                .put("transport", "JSONRPC")
                .put("url", agentUrl);

        card.put("preferredTransport", "JSONRPC");
        card.put("protocolVersion", "0.3.0");

        ObjectNode provider = card.putObject("provider");
        provider.put("organization", "Oracle");
        provider.put("url", "https://www.oracle.com/cloud/google/oracle-database-at-google-cloud/");

        return card;
    }

    static String resolveAgentUrl(Environment environment) {
        return firstNonBlank(
                environment.getProperty("ORACLE_AI_DATABASE_AGENT_URL"),
                DEFAULT_AGENT_URL
        );
    }

    static String resolveAuthorizationUrl(Environment environment) {
        return firstNonBlank(
                environment.getProperty("ORACLE_AI_DATABASE_AGENT_AUTHORIZATION_URL"),
                DEFAULT_AUTHORIZATION_URL
        );
    }

    static String resolveTokenUrl(Environment environment) {
        return firstNonBlank(
                environment.getProperty("ORACLE_AI_DATABASE_AGENT_TOKEN_URL"),
                DEFAULT_TOKEN_URL
        );
    }

    static Map<String, SecurityScheme> buildOAuthSecuritySchemes(Environment environment) {
        AuthorizationCodeOAuthFlow authorizationCodeFlow = new AuthorizationCodeOAuthFlow(
                resolveAuthorizationUrl(environment),
                resolveTokenUrl(environment),
                Map.of("openid", "This is a mandatory scope."),
                resolveTokenUrl(environment)
        );
        OAuthFlows flows = new OAuthFlows.Builder()
                .authorizationCode(authorizationCodeFlow)
                .build();
        return Map.of(
                "oauth2",
                new OAuth2SecurityScheme.Builder()
                        .flows(flows)
                        .build()
        );
    }

    static List<Map<String, List<String>>> buildOAuthSecurityRequirements() {
        return List.of(Map.of("oauth2", List.of()));
    }

    private static void addSkill(
            ArrayNode skills,
            String id,
            String name,
            String description,
            String... tags
    ) {
        ObjectNode skill = skills.addObject();
        skill.put("id", id);
        skill.put("name", name);
        skill.put("description", description);

        ArrayNode tagArray = skill.putArray("tags");
        if (tags != null) {
            for (String tag : tags) {
                tagArray.add(tag);
            }
        }
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
