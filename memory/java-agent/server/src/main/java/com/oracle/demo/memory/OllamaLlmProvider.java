package com.oracle.demo.memory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import oracle.jdbc.agentmemory.internal.SimpleJson;
import oracle.jdbc.agentmemory.spi.LlmProvider;
import oracle.jdbc.agentmemory.spi.LlmRequest;
import oracle.jdbc.agentmemory.spi.LlmResponse;

final class OllamaLlmProvider implements LlmProvider {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String model;
    private final double temperature;

    OllamaLlmProvider(String baseUrl, String model, double temperature) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "http://localhost:11434"
                : baseUrl;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public String providerName() {
        return "ollama-local";
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String prompt = request.systemPrompt() == null || request.systemPrompt().isBlank()
                ? request.prompt()
                : request.systemPrompt() + "\n\n" + request.prompt();
        String body = "{"
                + "\"model\":" + SimpleJson.stringify(model) + ","
                + "\"prompt\":" + SimpleJson.stringify(prompt) + ","
                + "\"stream\":false,"
                + "\"options\":{\"temperature\":" + temperature + "}"
                + "}";
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = client.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Ollama request failed: HTTP " + response.statusCode());
            }
            Object parsed = SimpleJson.parse(response.body());
            if (parsed instanceof Map<?, ?> map) {
                Object text = map.get("response");
                return LlmResponse.of(text == null ? "" : String.valueOf(text));
            }
            return LlmResponse.of("");
        } catch (IOException exception) {
            throw new IllegalStateException("Ollama request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama request interrupted", exception);
        }
    }
}
