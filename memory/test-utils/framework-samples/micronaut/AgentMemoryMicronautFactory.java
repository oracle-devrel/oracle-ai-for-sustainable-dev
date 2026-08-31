package com.example.agentmemory.micronaut;

import com.oracle.ojdbc.agentmemory.Embedder;
import com.oracle.ojdbc.agentmemory.Llm;
import com.oracle.ojdbc.agentmemory.OracleAgentMemory;
import com.oracle.ojdbc.agentmemory.internal.DatabaseEmbedder;
import com.oracle.ojdbc.agentmemory.internal.OllamaLlm;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import javax.sql.DataSource;

// Sample integration only.
// This is what a Micronaut application can write today without changing the
// framework-neutral ojdbc-agent-memory core library.
//
// Once a Micronaut integration artifact exists, this factory should move into a
// separate artifact such as ojdbc-agent-memory-micronaut and be guarded by
// @Requires conditions.
@Factory
final class AgentMemoryMicronautFactory {
  @Singleton
  OracleAgentMemory oracleAgentMemory(
      DataSource dataSource,
      AgentMemoryMicronautConfiguration configuration) {

    Embedder embedder = switch (configuration.embedding().provider()) {
      case "database" -> new DatabaseEmbedder(dataSource, configuration.embedding().model());
      case "none" -> null;
      default -> throw new IllegalArgumentException("Unsupported embedding provider: "
          + configuration.embedding().provider());
    };

    // The LLM is optional; when configured, it enables message-to-memory extraction flows.
    Llm llm = switch (configuration.llm().provider()) {
      case "ollama" -> new OllamaLlm(
          configuration.llm().baseUrl(),
          configuration.llm().model(),
          configuration.llm().temperature());
      case "none" -> null;
      default -> throw new IllegalArgumentException("Unsupported LLM provider: "
          + configuration.llm().provider());
    };

    return OracleAgentMemory.builder()
        .dataSource(dataSource)
        .tablePrefix(configuration.schema().tablePrefix())
        .autoCreateSchema(configuration.schema().autoCreate())
        .embedder(embedder)
        .llm(llm)
        .extractMemories(configuration.memory().extractMemories())
        .build();
  }
}

@ConfigurationProperties("ojdbc.agentmemory")
record AgentMemoryMicronautConfiguration(
    Schema schema,
    Embedding embedding,
    LlmConfig llm,
    MemoryConfig memory) {
  AgentMemoryMicronautConfiguration {
    schema = schema == null ? new Schema(true, "OAMJ_") : schema;
    embedding = embedding == null ? new Embedding("database", "allminilm") : embedding;
    llm = llm == null ? new LlmConfig("none", "http://localhost:11434", "llama3.2:latest", 0.0d) : llm;
    memory = memory == null ? new MemoryConfig(false) : memory;
  }
}

record Schema(boolean autoCreate, String tablePrefix) {}

record Embedding(String provider, String model) {}

record LlmConfig(String provider, String baseUrl, String model, double temperature) {}

record MemoryConfig(boolean extractMemories) {}
