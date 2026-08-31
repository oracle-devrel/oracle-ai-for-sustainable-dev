package com.oracle.demo.interactiveai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Synchronous Streamable HTTP MCP client for a separately managed Toolkit service. */
final class McpHttpClient implements AutoCloseable {
    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;
    private final URI endpoint;
    private final String bearerToken;
    private long requestId;
    private String sessionId;
    private String serverName;
    private String serverVersion;

    McpHttpClient(URI endpoint, String bearerToken, Path trustStore, char[] trustStorePassword) {
        this.endpoint = endpoint;
        this.bearerToken = bearerToken;
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT);
        if (trustStore != null) builder.sslContext(sslContext(trustStore, trustStorePassword));
        this.http = builder.build();
    }

    void initialize() {
        JsonNode result = request("initialize", Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "interactive-ai-agent-service", "version", "0.1.0")));
        serverName = result.path("serverInfo").path("name").asText();
        serverVersion = result.path("serverInfo").path("version").asText();
        notify("notifications/initialized", Map.of());
    }

    Set<String> listTools() {
        JsonNode tools = request("tools/list", Map.of()).path("tools");
        if (!tools.isArray()) throw new IllegalStateException("MCP tools/list did not return an array");
        Set<String> names = new LinkedHashSet<>();
        tools.forEach(tool -> names.add(tool.path("name").asText()));
        return Set.copyOf(names);
    }

    ToolResult callTool(String name, Map<String, Object> arguments) {
        JsonNode result = request("tools/call", Map.of("name", name, "arguments", arguments));
        if (result.path("isError").asBoolean(false)) {
            String detail = result.path("content").isArray() && !result.path("content").isEmpty()
                    ? result.path("content").get(0).path("text").asText("unknown toolkit error")
                    : "unknown toolkit error";
            throw new IllegalStateException("Oracle Database MCP tool failed: " + name + " (" + detail + ")");
        }
        JsonNode structured = result.path("structuredContent");
        if (!structured.isObject()) {
            throw new IllegalStateException("Oracle Database MCP tool did not return structuredContent: " + name);
        }
        return new ToolResult(structured);
    }

    String serverName() {
        return serverName;
    }

    String serverVersion() {
        return serverVersion;
    }

    private synchronized JsonNode request(String method, Map<String, ?> params) {
        long id = ++requestId;
        ObjectNode message = message(method, params);
        message.put("id", id);
        JsonNode payload = responsePayload(send(message).body());
        if (!payload.path("id").canConvertToLong() || payload.path("id").asLong() != id) {
            throw new IllegalStateException("MCP " + method + " returned an unexpected response id");
        }
        if (payload.hasNonNull("error")) {
            throw new IllegalStateException("MCP " + method + " failed: " + payload.path("error"));
        }
        return payload.path("result");
    }

    private synchronized void notify(String method, Map<String, ?> params) {
        send(message(method, params));
    }

    private ObjectNode message(String method, Map<String, ?> params) {
        ObjectNode message = mapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        message.set("params", mapper.valueToTree(params));
        return message;
    }

    private HttpResponse<String> send(JsonNode message) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json, text/event-stream")
                    .header("Content-Type", "application/json")
                    .header("MCP-Protocol-Version", PROTOCOL_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(message)));
            if (sessionId != null) request.header("Mcp-Session-Id", sessionId);
            if (bearerToken != null && !bearerToken.isBlank()) {
                request.header("Authorization", "Bearer " + bearerToken);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Oracle Database MCP Toolkit returned HTTP "
                        + response.statusCode() + ": " + response.body());
            }
            response.headers().firstValue("Mcp-Session-Id").ifPresent(value -> sessionId = value);
            return response;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to call Oracle Database MCP Toolkit at " + endpoint, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Oracle Database MCP Toolkit", exception);
        }
    }

    private JsonNode responsePayload(String body) {
        try {
            String trimmed = body == null ? "" : body.trim();
            if (trimmed.isEmpty()) return mapper.createObjectNode();
            if (trimmed.startsWith("{")) return mapper.readTree(trimmed);
            for (String line : trimmed.split("\\R")) {
                if (line.startsWith("data:")) {
                    String data = line.substring("data:".length()).trim();
                    if (!data.isEmpty()) return mapper.readTree(data);
                }
            }
            throw new IllegalStateException("Oracle Database MCP Toolkit returned an unsupported response");
        } catch (IOException exception) {
            throw new IllegalStateException("Oracle Database MCP Toolkit returned invalid JSON", exception);
        }
    }

    private static SSLContext sslContext(Path trustStore, char[] password) {
        if (!Files.isRegularFile(trustStore)) {
            throw new IllegalArgumentException("ORACLE_MCP_TRUSTSTORE does not exist: " + trustStore);
        }
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(trustStore)) {
                keyStore.load(input, password);
            }
            TrustManagerFactory managers = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            managers.init(keyStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, managers.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Unable to load Oracle MCP TLS truststore", exception);
        }
    }

    @Override
    public void close() {
        // The standalone Toolkit service has an independent lifecycle.
    }

    record ToolResult(JsonNode structuredContent) {
    }
}
