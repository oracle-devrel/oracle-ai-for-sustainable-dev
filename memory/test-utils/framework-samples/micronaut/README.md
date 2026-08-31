# Micronaut Sample Integration

These files are documentation samples only. They are intentionally outside
`src/main/java`, so the current examples Maven project does not compile them or
require Micronaut dependencies.

The sample shows the integration an application can write today:

- Micronaut owns `DataSource` creation through `datasources.default.*`.
- Micronaut binds Oracle Agent Memory settings with `@ConfigurationProperties`.
- A small `@Factory` creates `OracleAgentMemory`.
- The core `ojdbc-agent-memory` library remains framework-neutral.

Once the API stabilizes, the next step would be a separate Micronaut integration
artifact, for example `ojdbc-agent-memory-micronaut`. That artifact would
provide conditional beans so applications would not need to copy this factory.

## Files

- `AgentMemoryMicronautFactory.java`: manual Micronaut wiring sample.
- `application.yml`: sample application YAML for manual wiring.
- `application-auto-configuration.yml`: sample configuration shape once a
  future Micronaut integration artifact exists.
