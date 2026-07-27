package com.oracle.demo.interactiveai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main {
    private final SupplyChainRepository repository;
    private final ApprovalService approvals;
    private final AguiRunService runs;
    private final String actor =
            System.getenv().getOrDefault(
                    "REQUESTED_BY",
                    "supply.planner@example.com");
    private final Path webRoot;

    private Main(Path webRoot, SupplyChainRepository repository) {
        this.webRoot = webRoot;
        this.repository = repository;
        this.approvals = new ApprovalService();
        this.runs = new AguiRunService(repository, approvals);
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(
                System.getenv().getOrDefault("AGENT_PORT", "8080"));
        Path webRoot = Path.of(
                System.getProperty("web.root", "../web-client"));
        SupplyChainRepository repository =
                McpToolkitSupplyChainRepository.fromEnvironment(System.getenv());
        if (repository instanceof AutoCloseable closeable) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> closeQuietly(closeable),
                            "repository-shutdown"));
        }
        new Main(webRoot, repository).start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", port),
                0);
        server.createContext("/api/runs", this::run);
        server.createContext("/api/recommendations", this::recommendations);
        server.createContext("/api/approve", this::approve);
        server.createContext("/api/reject", this::reject);
        server.createContext(
                "/api/health",
                exchange -> respond(
                        exchange,
                        200,
                        "application/json",
                        Json.value(Map.of(
                                "status", "UP",
                                "mode", "mcp",
                                "backend",
                                "oracle-db-mcp-java-toolkit",
                                "useCase",
                                "supply-chain-inventory-exchange"))));
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println(
                "Supply-chain inventory exchange listening on http://127.0.0.1:"
                        + port);
    }

    private void recommendations(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "GET required");
            return;
        }
        try {
            Map<String, String> query = query(exchange);
            double minimumStockoutRisk = Double.parseDouble(
                    query.getOrDefault("minimumStockoutRisk", "70"));
            int maximumRows = Integer.parseInt(
                    query.getOrDefault("maximumRows", "10"));
            InputValidation.minimumStockoutRisk(minimumStockoutRisk);
            InputValidation.maximumRows(maximumRows);
            List<TransferRecommendation> recommendations =
                    repository.findTransferRecommendations(
                            minimumStockoutRisk,
                            maximumRows);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            respond(
                    exchange,
                    200,
                    "application/json",
                    recommendationsJson(recommendations));
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void run(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            double minimumStockoutRisk = Double.parseDouble(
                    form.getOrDefault("minimumStockoutRisk", "70"));
            int maximumRows = Integer.parseInt(
                    form.getOrDefault("maximumRows", "10"));
            InputValidation.minimumStockoutRisk(minimumStockoutRisk);
            InputValidation.maximumRows(maximumRows);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                runs.stream(
                        output,
                        minimumStockoutRisk,
                        maximumRows,
                        actor);
            }
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void approve(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            String recommendationId = required(form, "recommendationId");
            String approvalId = required(form, "approvalId");
            String notes = required(form, "approvalNotes");
            TransferRecommendation recommendation =
                    approvals.consume(approvalId, recommendationId, actor);
            InputValidation.approval(recommendation, notes, actor);
            TransferResult result =
                    repository.approveTransfer(recommendation, notes, actor);
            System.out.printf(
                    "audit tool=approve-inventory-transfer actor=%s "
                            + "recommendationId=%s result=APPROVED transferId=%d%n",
                    actor,
                    recommendationId,
                    result.transferId());
            respond(
                    exchange,
                    200,
                    "application/json",
                    Json.value(Map.of(
                            "transferId", result.transferId(),
                            "recommendationId", result.recommendationId(),
                            "transferQuantity", result.transferQuantity(),
                            "status", result.status())));
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        } catch (IllegalStateException exception) {
            respond(
                    exchange,
                    500,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void reject(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "POST required");
            return;
        }
        try {
            Map<String, String> form = form(exchange);
            approvals.reject(required(form, "approvalId"), actor);
            System.out.printf(
                    "audit tool=approve-inventory-transfer actor=%s "
                            + "result=REJECTED%n",
                    actor);
            respond(
                    exchange,
                    200,
                    "application/json",
                    "{\"status\":\"REJECTED\"}");
        } catch (IllegalArgumentException exception) {
            respond(
                    exchange,
                    400,
                    "application/json",
                    Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void staticFile(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.equals("/")
                ? "index.html"
                : requestPath.substring(1);
        if (!filename.matches("[A-Za-z0-9._/-]+")
                || filename.contains("..")) {
            respond(exchange, 400, "text/plain", "Invalid path");
            return;
        }
        Path file = webRoot.resolve(filename).normalize();
        if (!file.startsWith(webRoot.normalize())
                || !Files.isRegularFile(file)) {
            respond(exchange, 404, "text/plain", "Not found");
            return;
        }
        String type = filename.endsWith(".js")
                ? "text/javascript"
                : filename.endsWith(".css")
                        ? "text/css"
                        : "text/html";
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set(
                "Content-Type",
                type + "; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static Map<String, String> form(HttpExchange exchange)
            throws IOException {
        return pairs(new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    private static Map<String, String> query(HttpExchange exchange) {
        return pairs(exchange.getRequestURI().getRawQuery());
    }

    private static Map<String, String> pairs(String encoded) {
        Map<String, String> result = new HashMap<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String pair : encoded.split("&")) {
            if (pair.isBlank()) continue;
            String[] pieces = pair.split("=", 2);
            result.put(
                    URLDecoder.decode(pieces[0], StandardCharsets.UTF_8),
                    pieces.length == 2
                            ? URLDecoder.decode(
                                    pieces[1],
                                    StandardCharsets.UTF_8)
                            : "");
        }
        return result;
    }

    private static String required(
            Map<String, String> values,
            String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    static String recommendationsJson(
            List<TransferRecommendation> recommendations) {
        return Json.value(Map.of(
                "source", "oracle-db-mcp-java-toolkit",
                "recommendations",
                recommendations.stream()
                        .map(Json::recommendationMap)
                        .toList()));
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception exception) {
            System.err.println(
                    "Unable to close MCP repository: "
                            + exception.getMessage());
        }
    }
}
