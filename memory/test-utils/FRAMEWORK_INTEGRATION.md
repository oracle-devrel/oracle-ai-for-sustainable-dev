# Framework Integration Notes

This examples project currently demonstrates direct standalone usage of
`ojdbc-agent-memory`. The same core library is intentionally built around
standard Java and JDBC types, especially `javax.sql.DataSource`, so it can be
used from standalone Java, Micronaut, Spring Boot, Spring AI, LangChain4j, and
other Java agent frameworks without changing the memory API.

Concrete documentation-only samples are available in:

- [framework-samples/spring](framework-samples/spring)
- [framework-samples/micronaut](framework-samples/micronaut)

They are intentionally outside `src/main/java`, so they do not add framework
dependencies or affect the current Maven build.

## Sample Integration vs Auto-Configuration

The first step for Micronaut and Spring should be sample-level integration:

- The application framework owns configuration loading.
- The application framework creates the `DataSource`.
- A small application-level factory/configuration class creates `Llm`,
  `Embedder`, and `OracleAgentMemory`.
- The sample proves that the core library works inside the framework without
  adding framework dependencies to `ojdbc-agent-memory`.

This is different from framework-native auto-configuration. Auto-configuration
should be a later step after the core API stabilizes.

## Micronaut Sample Integration

A Micronaut sample would use Micronaut configuration and dependency injection:

```java
@Factory
final class AgentMemoryFactory {
  @Singleton
  OracleAgentMemory oracleAgentMemory(DataSource dataSource, AgentMemorySettings settings) {
    return OracleAgentMemory.builder()
        .dataSource(dataSource)
        .extractMemories(settings.memory().extractMemories())
        .llm(...)
        .embedder(...)
        .build();
  }
}
```

The sample would use Micronaut `application.yml` for Oracle Database, embedding,
LLM, and memory settings, instead of the standalone
`ojdbc-agent-memory.properties` loader.

See [framework-samples/micronaut](framework-samples/micronaut) for the sample
factory and configuration files.

Next step for real Micronaut integration:

- Create a separate integration artifact, for example
  `ojdbc-agent-memory-micronaut`.
- Add Micronaut `@ConfigurationProperties` for memory settings.
- Add a Micronaut `@Factory` for conditional `OracleAgentMemory`, `Llm`, and
  `Embedder` beans.
- Use Micronaut `@Requires` conditions so beans are created only when the
  required classes/configuration are present.
- Keep the core `ojdbc-agent-memory` artifact free of Micronaut dependencies.

## Spring / Spring AI Sample Integration

A Spring Boot or Spring AI sample would use Spring configuration and bean
creation:

```java
@Configuration
@EnableConfigurationProperties(AgentMemoryProperties.class)
class AgentMemoryConfiguration {
  @Bean
  OracleAgentMemory oracleAgentMemory(DataSource dataSource, AgentMemoryProperties properties) {
    return OracleAgentMemory.builder()
        .dataSource(dataSource)
        .extractMemories(properties.memory().extractMemories())
        .llm(...)
        .embedder(...)
        .build();
  }
}
```

The sample would use `application.yml` or `application.properties` and the
framework-managed `DataSource`. For Spring AI, the sample can also show how to
retrieve memories before a model call and persist messages after the model
response.

See [framework-samples/spring](framework-samples/spring) for the sample
configuration class and properties files.

Next step for real Spring auto-configuration:

- Create a separate starter artifact, for example
  `ojdbc-agent-memory-spring-boot-starter`.
- Add `@AutoConfiguration` and `@ConfigurationProperties`.
- Add conditional beans with `@ConditionalOnClass`,
  `@ConditionalOnMissingBean`, and configuration property conditions.
- Register auto-configuration with
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Add Spring Boot configuration metadata so IDEs can autocomplete properties.
- Keep Spring-specific annotations and dependencies out of the core library.

## Project Direction

The recommended contribution path is:

1. Keep `ojdbc-agent-memory` as the framework-neutral core.
2. Maintain standalone examples for the basic API and local Oracle Database
   validation.
3. Add Micronaut and Spring sample projects that demonstrate manual framework
   wiring.
4. After the API stabilizes, create separate integration artifacts or
   contribute auto-configuration to the appropriate framework ecosystem.

This keeps the memory implementation reusable while allowing each Java
framework to expose a native developer experience.
