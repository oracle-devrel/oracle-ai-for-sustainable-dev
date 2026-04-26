package oracleai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.a2a.spec.Artifact;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class OracleAiDatabaseAgentClient {

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile CachedAccessToken cachedAccessToken;

    public OracleAiDatabaseAgentClient(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.cachedAccessToken = null;
    }

    public RemoteDatabaseResult answer(String userInput) throws Exception {
        return answer(userInput, null);
    }

    public RemoteDatabaseResult answer(String userInput, String callerAuthorizationHeader) throws Exception {
        String normalizedInput = userInput == null || userInput.isBlank()
                ? "Summarize the most important current inventory risks in the Oracle database."
                : userInput.trim();
        String agentUrl = OracleAiDatabaseAgentCardFactory.resolveAgentUrl(environment);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("method", "message/send");
        payload.put("id", UUID.randomUUID().toString());

        ObjectNode params = payload.putObject("params");
        ObjectNode message = params.putObject("message");
        message.put("kind", "message");
        message.put("messageId", UUID.randomUUID().toString());
        message.put("role", "user");
        ArrayNode parts = message.putArray("parts");
        parts.addObject()
                .put("kind", "text")
                .put("text", normalizedInput);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(agentUrl))
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        String authorizationHeader = resolveAuthorizationHeader(callerAuthorizationHeader);
        if (authorizationHeader.isBlank()) {
            throw new IllegalStateException(
                    "No Oracle OAuth bearer token was available for delegation. "
                            + "Import the inventory-system gateway with Oracle OAuth enabled so Gemini Enterprise "
                            + "can send the caller's Authorization header, or configure "
                            + "ORACLE_AI_DATABASE_AGENT_AUTHORIZATION_HEADER / "
                            + "ORACLE_AI_DATABASE_AGENT_BEARER_TOKEN on the gateway. "
                            + "For a demo service identity, you can also configure "
                            + "ORACLE_AI_DATABASE_AGENT_CLIENT_ID / "
                            + "ORACLE_AI_DATABASE_AGENT_CLIENT_SECRET / "
                            + "ORACLE_AI_DATABASE_AGENT_REFRESH_TOKEN on the gateway."
            );
        }
        requestBuilder.header("Authorization", authorizationHeader);

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Oracle AI Database agent returned HTTP "
                    + response.statusCode() + ": " + trimForError(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.hasNonNull("error")) {
            throw new IllegalStateException("Oracle AI Database agent returned JSON-RPC error: "
                    + trimForError(root.get("error").toString()));
        }

        JsonNode taskNode = root.path("result");
        if (taskNode.isMissingNode() || taskNode.isNull()) {
            throw new IllegalStateException("Oracle AI Database agent response did not include a result task.");
        }
        taskNode = awaitTaskCompletion(agentUrl, authorizationHeader, taskNode);

        String taskState = taskNode.path("status").path("state").asText("");
        if ("failed".equalsIgnoreCase(taskState)) {
            throw new IllegalStateException("Oracle AI Database agent task failed: "
                    + firstNonBlank(extractResponseText(taskNode), trimForError(response.body())));
        }

        List<Artifact> artifacts = parseArtifacts(taskNode.path("artifacts"));
        String responseText = extractResponseText(taskNode);
        if (responseText.isBlank() && !artifacts.isEmpty()) {
            responseText = "Oracle AI Database agent completed and returned "
                    + artifacts.size() + " artifact(s).";
        }
        if (responseText.isBlank()) {
            throw new IllegalStateException("Oracle AI Database agent returned no message text or artifacts.");
        }

        JsonNode metadata = extractMetadata(taskNode);
        return new RemoteDatabaseResult(
                responseText,
                firstNonBlank(textValue(metadata, "executionMode"), "remote-a2a"),
                firstNonBlank(
                        textValue(metadata, "sourceDetail"),
                        "Oracle AI Database agent at " + agentUrl
                ),
                firstNonBlank(
                        textValue(metadata, "action"),
                        "delegate-oracle-ai-database-agent"
                ),
                artifacts
        );
    }

    private JsonNode awaitTaskCompletion(
            String agentUrl,
            String authorizationHeader,
            JsonNode initialTaskNode
    ) throws Exception {
        String taskId = firstNonBlank(
                textValue(initialTaskNode, "id"),
                textValue(initialTaskNode, "taskId"),
                textValue(initialTaskNode.path("status").path("message"), "taskId")
        );
        if (taskId.isBlank()) {
            return initialTaskNode;
        }

        JsonNode currentTask = initialTaskNode;
        String currentState = textValue(currentTask.path("status"), "state");
        String currentText = extractResponseText(currentTask);
        if (!shouldPollForTaskCompletion(currentState, currentText, currentTask)) {
            return currentTask;
        }

        int maxAttempts = resolveTaskPollAttempts();
        long pollIntervalMillis = resolveTaskPollIntervalMillis();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            sleepBeforePoll(pollIntervalMillis);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("jsonrpc", "2.0");
            payload.put("method", "tasks/get");
            payload.put("id", UUID.randomUUID().toString());
            ObjectNode params = payload.putObject("params");
            params.put("id", taskId);
            params.put("taskId", taskId);

            HttpRequest request = HttpRequest.newBuilder(URI.create(agentUrl))
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorizationHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Oracle AI Database task polling failed with HTTP "
                        + response.statusCode() + ": " + trimForError(response.body()));
            }

            JsonNode pollRoot = objectMapper.readTree(response.body());
            if (pollRoot.hasNonNull("error")) {
                throw new IllegalStateException("Oracle AI Database task polling returned JSON-RPC error: "
                        + trimForError(pollRoot.get("error").toString()));
            }
            currentTask = pollRoot.hasNonNull("result") ? pollRoot.path("result") : pollRoot;
            if (currentTask.isMissingNode() || currentTask.isNull()) {
                throw new IllegalStateException("Oracle AI Database task polling returned no task payload.");
            }

            currentState = textValue(currentTask.path("status"), "state");
            currentText = extractResponseText(currentTask);
            if (!shouldPollForTaskCompletion(currentState, currentText, currentTask)) {
                return currentTask;
            }
        }

        throw new IllegalStateException("Oracle AI Database agent accepted the task but did not finish within "
                + (maxAttempts * pollIntervalMillis) + " ms.");
    }

    private String resolveAuthorizationHeader(String callerAuthorizationHeader) {
        String explicitHeader = environment.getProperty("ORACLE_AI_DATABASE_AGENT_AUTHORIZATION_HEADER");
        if (explicitHeader != null && !explicitHeader.isBlank()) {
            return explicitHeader.trim();
        }

        String bearerToken = environment.getProperty("ORACLE_AI_DATABASE_AGENT_BEARER_TOKEN");
        if (bearerToken != null && !bearerToken.isBlank()) {
            return "Bearer " + bearerToken.trim();
        }

        String refreshToken = environment.getProperty("ORACLE_AI_DATABASE_AGENT_REFRESH_TOKEN");
        String clientId = environment.getProperty("ORACLE_AI_DATABASE_AGENT_CLIENT_ID");
        String clientSecret = environment.getProperty("ORACLE_AI_DATABASE_AGENT_CLIENT_SECRET");
        if (isNonBlank(refreshToken) && isNonBlank(clientId) && isNonBlank(clientSecret)) {
            return "Bearer " + resolveAccessTokenFromRefreshToken(
                    clientId.trim(),
                    clientSecret.trim(),
                    refreshToken.trim()
            );
        }

        if (callerAuthorizationHeader != null && !callerAuthorizationHeader.isBlank()) {
            return callerAuthorizationHeader.trim();
        }

        return "";
    }

    private synchronized String resolveAccessTokenFromRefreshToken(
            String clientId,
            String clientSecret,
            String refreshToken
    ) {
        CachedAccessToken current = cachedAccessToken;
        if (current != null && current.isUsable()) {
            return current.accessToken();
        }

        try {
            String form = "grant_type=refresh_token"
                    + "&refresh_token=" + urlEncode(refreshToken)
                    + "&client_id=" + urlEncode(clientId)
                    + "&client_secret=" + urlEncode(clientSecret);

            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(OracleAiDatabaseAgentCardFactory.resolveTokenUrl(environment))
                    )
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Oracle OAuth token refresh failed with HTTP "
                        + response.statusCode() + ": " + trimForError(response.body()));
            }

            JsonNode tokenResponse = objectMapper.readTree(response.body());
            String accessToken = tokenResponse.path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new IllegalStateException("Oracle OAuth token refresh did not return an access_token.");
            }

            long expiresInSeconds = tokenResponse.path("expires_in").asLong(300L);
            Instant expiresAt = Instant.now().plusSeconds(Math.max(60L, expiresInSeconds - 60L));
            cachedAccessToken = new CachedAccessToken(accessToken.trim(), expiresAt);
            return cachedAccessToken.accessToken();
        } catch (Exception exception) {
            throw new IllegalStateException("Oracle OAuth token refresh failed: " + exception.getMessage(), exception);
        }
    }

    private int resolveTimeoutSeconds() {
        String configured = environment.getProperty("ORACLE_AI_DATABASE_AGENT_TIMEOUT_SECONDS");
        if (configured == null || configured.isBlank()) {
            return 45;
        }
        try {
            return Integer.parseInt(configured.trim());
        } catch (NumberFormatException exception) {
            return 45;
        }
    }

    private int resolveTaskPollAttempts() {
        String configured = environment.getProperty("ORACLE_AI_DATABASE_AGENT_TASK_POLL_ATTEMPTS");
        if (configured == null || configured.isBlank()) {
            return 60;
        }
        try {
            return Math.max(1, Integer.parseInt(configured.trim()));
        } catch (NumberFormatException exception) {
            return 60;
        }
    }

    private long resolveTaskPollIntervalMillis() {
        String configured = environment.getProperty("ORACLE_AI_DATABASE_AGENT_TASK_POLL_INTERVAL_MILLIS");
        if (configured == null || configured.isBlank()) {
            return 1500L;
        }
        try {
            return Math.max(250L, Long.parseLong(configured.trim()));
        } catch (NumberFormatException exception) {
            return 1500L;
        }
    }

    private List<Artifact> parseArtifacts(JsonNode artifactsNode) {
        if (artifactsNode == null || !artifactsNode.isArray()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(artifactsNode, new TypeReference<List<Artifact>>() {});
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private JsonNode extractMetadata(JsonNode taskNode) {
        JsonNode statusMetadata = taskNode.path("status").path("message").path("metadata");
        if (statusMetadata != null && statusMetadata.isObject()) {
            return statusMetadata;
        }
        JsonNode taskMetadata = taskNode.path("metadata");
        if (taskMetadata != null && taskMetadata.isObject()) {
            return taskMetadata;
        }
        return objectMapper.createObjectNode();
    }

    private String extractResponseText(JsonNode taskNode) {
        String statusMessageText = extractTextFromParts(taskNode.path("status").path("message").path("parts"));
        if (!statusMessageText.isBlank()) {
            return statusMessageText;
        }

        JsonNode history = taskNode.path("history");
        if (history.isArray()) {
            for (int index = history.size() - 1; index >= 0; index--) {
                JsonNode message = history.get(index);
                String role = message.path("role").asText("");
                if (!role.isBlank() && !"agent".equalsIgnoreCase(role)) {
                    continue;
                }
                String candidate = extractTextFromParts(message.path("parts"));
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        String artifactText = extractTextFromArtifactParts(taskNode.path("artifacts"));
        if (!artifactText.isBlank()) {
            return artifactText;
        }

        return "";
    }

    private static String extractTextFromParts(JsonNode partsNode) {
        if (partsNode == null || !partsNode.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode partNode : partsNode) {
            String text = partNode.path("text").asText("");
            if (text == null || text.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(text.trim());
        }
        return builder.toString();
    }

    private static String extractTextFromArtifactParts(JsonNode artifactsNode) {
        if (artifactsNode == null || !artifactsNode.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode artifactNode : artifactsNode) {
            String candidate = extractTextFromParts(artifactNode.path("parts"));
            if (candidate.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(candidate.trim());
        }
        return builder.toString();
    }

    private static String textValue(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private static boolean shouldPollForTaskCompletion(String taskState, String responseText, JsonNode taskNode) {
        if (isPendingTaskState(taskState)) {
            return true;
        }
        if (looksLikeSubmittedMessage(responseText) && parseArtifactCount(taskNode) == 0) {
            return true;
        }
        return false;
    }

    private static boolean isPendingTaskState(String taskState) {
        if (taskState == null || taskState.isBlank()) {
            return false;
        }
        String normalized = taskState.trim().toLowerCase();
        return normalized.equals("submitted")
                || normalized.equals("working")
                || normalized.equals("running")
                || normalized.equals("in_progress")
                || normalized.equals("pending");
    }

    private static boolean looksLikeSubmittedMessage(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return false;
        }
        String normalized = responseText.trim().toLowerCase();
        return normalized.equals("task has been submitted.")
                || normalized.equals("task submitted.")
                || normalized.equals("submitted");
    }

    private static int parseArtifactCount(JsonNode taskNode) {
        JsonNode artifactsNode = taskNode == null ? null : taskNode.path("artifacts");
        return artifactsNode != null && artifactsNode.isArray() ? artifactsNode.size() : 0;
    }

    private static void sleepBeforePoll(long pollIntervalMillis) {
        try {
            Thread.sleep(pollIntervalMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Oracle AI Database task completion.", exception);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimForError(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= 280) {
            return trimmed;
        }
        return trimmed.substring(0, 280) + "...";
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

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record CachedAccessToken(String accessToken, Instant expiresAt) {
        boolean isUsable() {
            return accessToken != null
                    && !accessToken.isBlank()
                    && expiresAt != null
                    && Instant.now().isBefore(expiresAt);
        }
    }

    public record RemoteDatabaseResult(
            String responseText,
            String executionMode,
            String sourceDetail,
            String action,
            List<Artifact> artifacts
    ) {}
}
