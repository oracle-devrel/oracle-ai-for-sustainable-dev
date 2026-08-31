package com.oracle.demo.interactiveai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

final class McpHttpClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void initializesAndCallsStandaloneStreamableHttpServer() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> respond(exchange, authorization));
        server.start();
        try (var client = new McpHttpClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/mcp"),
                "test-token", null, new char[0])) {
            client.initialize();
            assertEquals("oracle-db-mcp-toolkit", client.serverName());
            assertEquals("1.0.0", client.serverVersion());
            assertEquals(Map.of("find-stockout-transfer-recommendations", true),
                    client.listTools().stream().collect(
                            java.util.stream.Collectors.toMap(name -> name, name -> true)));
            McpHttpClient.ToolResult result = client.callTool(
                    "find-stockout-transfer-recommendations", Map.of("maximumRows", 1));
            assertEquals(1, result.structuredContent().path("rows").size());
            assertEquals("Bearer test-token", authorization.get());
        } finally {
            server.stop(0);
        }
    }

    private static void respond(
            HttpExchange exchange,
            AtomicReference<String> authorization) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        JsonNode request = MAPPER.readTree(exchange.getRequestBody());
        String method = request.path("method").asText();
        if ("notifications/initialized".equals(method)) {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
            return;
        }
        long id = request.path("id").asLong();
        String result = switch (method) {
            case "initialize" -> "{\"protocolVersion\":\"2025-03-26\","
                    + "\"serverInfo\":{\"name\":\"oracle-db-mcp-toolkit\",\"version\":\"1.0.0\"}}";
            case "tools/list" -> "{\"tools\":[{\"name\":\"find-stockout-transfer-recommendations\"}]}";
            case "tools/call" -> "{\"structuredContent\":{\"rows\":[{\"SKU\":\"TEST\"}]}}";
            default -> throw new IllegalArgumentException("Unexpected method " + method);
        };
        byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + result + "}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Mcp-Session-Id", "test-session");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
