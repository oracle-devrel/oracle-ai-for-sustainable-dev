# ojdbc-agent-memory-examples

Runnable examples for `ojdbc-agent-memory`.

For this repository's financial-database wallet setup, Ollama installation,
ONNX model loader, full-mode runner, and verified output, see
[LOCAL_TESTING.md](LOCAL_TESTING.md).

Framework integration notes for Micronaut and Spring are in
[FRAMEWORK_INTEGRATION.md](FRAMEWORK_INTEGRATION.md).

Documentation-only framework samples are in:

- [framework-samples/spring](framework-samples/spring)
- [framework-samples/micronaut](framework-samples/micronaut)

Those files are intentionally outside `src/main/java`; they show how an
application would wire the core library without adding Spring or Micronaut
dependencies to this examples build.

The default `src/main/resources/ojdbc-agent-memory.properties` targets:

- Oracle Database: `localhost:1521/freepdb1`, `scott/tiger`
- UCP connection pool enabled
- DB embedding model: `allminilm`
- Ollama LLM: `http://localhost:11434`, model `llama3.2:latest`

Run from this directory after installing the library:

```bash
cd ../ojdbc-agent-memory
mvn -q clean install

cd ../ojdbc-agent-memory-examples
mvn -q exec:java -Dexec.mainClass=com.oracle.ojdbc.agentmemory.examples.BasicMemoryExample
```

Run the explicit UCP connection-pool example:

```bash
mvn -q exec:java -Dexec.mainClass=com.oracle.ojdbc.agentmemory.examples.UcpConnectionPoolExample
```

Override config with:

```bash
mvn -q exec:java \
  -Dojdbc.agentmemory.config=/path/to/ojdbc-agent-memory.properties \
  -Dexec.mainClass=com.oracle.ojdbc.agentmemory.examples.BasicMemoryExample
```
