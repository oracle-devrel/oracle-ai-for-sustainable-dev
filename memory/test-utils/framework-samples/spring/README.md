# Spring / Spring AI Sample Integration

These files are documentation samples only. They are intentionally outside
`src/main/java`, so the current examples Maven project does not compile them or
require Spring dependencies.

The sample shows the integration an application can write today:

- Spring Boot owns `DataSource` creation through `spring.datasource.*`.
- Application properties bind Oracle Agent Memory settings.
- A small `@Configuration` class creates `OracleAgentMemory`.
- The core `ojdbc-agent-memory` library remains framework-neutral.

Once the API stabilizes, the next step would be a separate Spring Boot starter,
for example `ojdbc-agent-memory-spring-boot-starter`. That starter would provide
real auto-configuration so applications would not need to copy this
configuration class.

## Files

- `AgentMemorySpringConfiguration.java`: manual Spring wiring sample.
- `application.properties`: sample application properties for manual wiring.
- `application-auto-configuration.properties`: sample property shape once a
  future Spring Boot starter exists.
