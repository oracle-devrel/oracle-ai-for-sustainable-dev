package com.example.agentmemory.spring;

import com.oracle.ojdbc.agentmemory.Embedder;
import com.oracle.ojdbc.agentmemory.Llm;
import com.oracle.ojdbc.agentmemory.OracleAgentMemory;
import com.oracle.ojdbc.agentmemory.internal.DatabaseEmbedder;
import com.oracle.ojdbc.agentmemory.internal.OllamaLlm;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Sample integration only.
// This is what a Spring Boot application can write today without changing the
// framework-neutral ojdbc-agent-memory core library.
//
// Once a Spring Boot starter exists, this manual @Configuration class should be
// replaced by auto-configuration in a separate artifact such as
// ojdbc-agent-memory-spring-boot-starter.
@Configuration
@EnableConfigurationProperties(AgentMemorySpringConfiguration.AgentMemoryProperties.class)
class AgentMemorySpringConfiguration {
  @Bean
  OracleAgentMemory oracleAgentMemory(
      DataSource dataSource,
      AgentMemoryProperties properties) {

    Embedder embedder = switch (properties.embedding().provider()) {
      case "database" -> new DatabaseEmbedder(dataSource, properties.embedding().model());
      case "none" -> null;
      default -> throw new IllegalArgumentException("Unsupported embedding provider: "
          + properties.embedding().provider());
    };

    // The LLM is optional; when configured, it enables message-to-memory extraction flows.
    Llm llm = switch (properties.llm().provider()) {
      case "ollama" -> new OllamaLlm(
          properties.llm().baseUrl(),
          properties.llm().model(),
          properties.llm().temperature());
      case "none" -> null;
      default -> throw new IllegalArgumentException("Unsupported LLM provider: "
          + properties.llm().provider());
    };

    return OracleAgentMemory.builder()
        .dataSource(dataSource)
        .tablePrefix(properties.schema().tablePrefix())
        .autoCreateSchema(properties.schema().autoCreate())
        .embedder(embedder)
        .llm(llm)
        .extractMemories(properties.memory().extractMemories())
        .build();
  }

  @ConfigurationProperties("ojdbc.agentmemory")
  record AgentMemoryProperties(
      Schema schema,
      Embedding embedding,
      LlmConfig llm,
      MemoryConfig memory) {
    AgentMemoryProperties {
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
}
