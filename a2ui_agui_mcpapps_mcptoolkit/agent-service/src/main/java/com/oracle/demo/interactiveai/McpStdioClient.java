package com.oracle.demo.interactiveai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Minimal synchronous MCP stdio client used to invoke the separately running Oracle toolkit. */
final class McpStdioClient implements AutoCloseable {
    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Process process;
    private final BufferedReader stdout;
    private final BufferedWriter stdin;
    private final ExecutorService reader = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("mcp-stdio-reader").factory());
    private long requestId;
    private String serverName;
    private String serverVersion;

    McpStdioClient(List<String> command, Map<String, String> childEnvironment) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Map<String, String> environment = builder.environment();
            String path = environment.get("PATH");
            String home = environment.get("HOME");
            environment.clear();
            if (path != null) environment.put("PATH", path);
            if (home != null) environment.put("HOME", home);
            environment.putAll(childEnvironment);
            process = builder.start();
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            Thread.ofVirtual().name("oracle-db-mcp-toolkit-stderr").start(() -> {
                try (BufferedReader errors = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    for (String line; (line = errors.readLine()) != null; ) {
                        System.err.println("[oracle-db-mcp-toolkit] " + line);
                    }
                } catch (IOException ignored) {
                    // The stream closes when the child process is stopped.
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start Oracle Database MCP Java Toolkit", exception);
        }
    }

    void initialize() {
        // The toolkit registers YAML tools immediately after constructing its MCP server.
        // Avoid racing initialization against those synchronous registrations.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while Oracle Database MCP Toolkit started", exception);
        }
        if (!process.isAlive()) throw new IllegalStateException("Oracle Database MCP Toolkit exited during startup");
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
        ObjectNode message = mapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("method", method);
        message.set("params", mapper.valueToTree(params));
        write(message);

        while (true) {
            String line = readLine();
            if (line == null) throw new IllegalStateException("Oracle Database MCP Toolkit exited unexpectedly");
            try {
                JsonNode response = mapper.readTree(line);
                if (!response.path("id").canConvertToLong() || response.path("id").asLong() != id) continue;
                if (response.hasNonNull("error")) {
                    throw new IllegalStateException("MCP " + method + " failed: " + response.path("error").toString());
                }
                return response.path("result");
            } catch (IOException exception) {
                throw new IllegalStateException("Oracle Database MCP Toolkit returned invalid JSON", exception);
            }
        }
    }

    private synchronized void notify(String method, Map<String, ?> params) {
        ObjectNode message = mapper.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        message.set("params", mapper.valueToTree(params));
        write(message);
    }

    private void write(JsonNode message) {
        try {
            stdin.write(mapper.writeValueAsString(message));
            stdin.newLine();
            stdin.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write to Oracle Database MCP Toolkit", exception);
        }
    }

    private String readLine() {
        try {
            return reader.submit(stdout::readLine).get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            close();
            throw new IllegalStateException("Oracle Database MCP Toolkit timed out", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Oracle Database MCP Toolkit", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to read from Oracle Database MCP Toolkit", exception.getCause());
        }
    }

    @Override
    public void close() {
        try {
            stdin.close();
        } catch (IOException ignored) {
        }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        reader.shutdownNow();
    }

    record ToolResult(JsonNode structuredContent) {
    }
}
