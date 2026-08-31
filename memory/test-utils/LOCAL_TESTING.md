# Local testing

These files are copied from the internal `ojdbc-agent-memory-examples`
repository. The local helpers in this directory keep credentials out of the
copied properties file, reuse `memory/.env` or `financial/setup/.env`, and pass
the wallet directory to Oracle JDBC as `oracle.net.tns_admin`.

## Prerequisites

- Java 21
- Maven
- `ojdbc-agent-memory` cloned at
  `/Users/pparkins/src/orahub/ora-jdbc-dev/ojdbc-agent-memory`, or set
  `OAM_LIBRARY_DIR`
- An Oracle Database account allowed to create the isolated
  `OAMJ_TEST_MESSAGE` and `OAMJ_TEST_RECORDS` objects

The copied Maven project adds Oracle PKI at the same release as the JDBC driver.
This companion library is required when the example connects with an Oracle
Wallet.

## 1. Build and install the Java library

```bash
cd memory/test-utils
./build-library.sh
```

This runs the library tests and installs
`com.oracle.database.jdbc:ojdbc-agent-memory:1.0-SNAPSHOT` in the local Maven
repository.

## 2. Run the baseline example

```bash
cd memory/test-utils
./run-local.sh
```

The default is `minimal` mode. It needs neither Ollama nor an embedding model.
It tests:

- Oracle UCP connectivity
- automatic creation of isolated `OAMJ_TEST_*` objects
- user, agent, and thread scope
- stored messages
- application-controlled durable memory
- scoped lexical retrieval

The expected output contains `Thread records using UCP` and
`Search results using UCP`, including the orange-juice memory. Full mode also
extracts grounded preference and guideline records from the example's explicit
brevity and bullet-point instructions.

The local UCP example explicitly enables auto-commit for each newly created
physical connection. The current proof-of-concept library performs individual
updates but does not commit them itself, so this keeps writes visible when a
later operation borrows another connection from the pool.

Verify the committed rows through a new database connection:

```bash
OAM_MAIN_CLASS=com.oracle.ojdbc.agentmemory.examples.DatabaseVerificationExample \
  ./run-local.sh
```

Minimal mode produces two rows in `OAMJ_TEST_MESSAGE` and one row in
`OAMJ_TEST_RECORDS`. Full mode produces two message rows plus the explicit
memory and any grounded records extracted by Ollama. Repeating either mode
replaces the example thread, so counts do not accumulate.

Run the non-UCP entry point if desired:

```bash
OAM_MAIN_CLASS=com.oracle.ojdbc.agentmemory.examples.BasicMemoryExample \
  ./run-local.sh
```

`BasicMemoryExample` calls `clearAll()` for the configured prefix. The local
runner uses the isolated `OAMJ_TEST_` prefix, so it does not touch the blog
demo's `AIM_DEMO_*` or Python SDK objects.

## 3. Install and start Ollama

On macOS with Homebrew:

```bash
/opt/homebrew/bin/brew install ollama
/opt/homebrew/bin/brew services start ollama
/opt/homebrew/opt/ollama/bin/ollama pull llama3.2:latest
curl -sS http://127.0.0.1:11434/api/version
```

The Homebrew service starts Ollama at login and listens on
`http://localhost:11434`. Stop it when it is no longer needed:

```bash
/opt/homebrew/bin/brew services stop ollama
```

## 4. Load and verify the database embedding model

```bash
cd memory/test-utils
./load-allminilm.sh
```

The loader is idempotent. It loads Oracle's public
`all_MiniLM_L12_v2.onnx` object into the current schema as `ALLMINILM` only
when the model is absent, then calls `VECTOR_EMBEDDING` and confirms 384
dimensions. The model is a persistent database object and is about 127 MB.

## 5. Run full extraction and vector search

```bash
OAM_MODE=full ./run-local.sh
```

Full mode causes `addMessages()` to ask Ollama to map turns into typed records.
The database generates 384-dimensional embeddings for those records and ranks
the semantic search with `VECTOR_DISTANCE`.

Override names or endpoints when necessary:

```bash
OAM_MODE=full \
OAM_EMBEDDING_MODEL=allminilm \
OLLAMA_BASE_URL=http://localhost:11434 \
OLLAMA_MODEL=llama3.2:latest \
./run-local.sh
```

Verify the committed full-mode records and model through a separate connection:

```bash
OAM_MAIN_CLASS=com.oracle.ojdbc.agentmemory.examples.DatabaseVerificationExample \
  ./run-local.sh
```

The verified example produced two message rows, three durable records, and one
`ALLMINILM` model.

## 6. Inspect the database

After the run, execute `check-database.sql` with SQLcl. The first query confirms
the connected database, the second checks for `allminilm`, and the third lists
the isolated example tables.
