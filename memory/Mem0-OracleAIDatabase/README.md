# Mem0 with Oracle AI Database

This directory starts a runnable integration that uses Oracle AI Database as the vector and JSON payload store for Mem0 OSS.

## Current status

As of July 30, 2026, this no longer requires a custom vector-store adapter. Mem0 merged the native `oracledb` provider on July 23, 2026 in commit [`d6d89c9`](https://github.com/mem0ai/mem0/commit/d6d89c987bddf580870db14c69db974edfc5263c). The provider is present on Mem0's `main` branch, but the latest PyPI release is still `mem0ai 2.0.12`, published July 13, 2026. This starter therefore pins the merge commit until a release containing the provider is available.

The provider class is:

```text
mem0.vector_stores.oracledb.OracleAIVectorSearch
```

It implements Mem0's `VectorStoreBase` contract:

- `create_col()`
- `insert()`
- `search()`
- `get()`
- `update()`
- `delete()`
- `list()`
- `list_cols()`
- `col_info()`
- `delete_col()`
- `reset()`

## What Oracle backs

| Mem0 responsibility | Storage in this starter | Status |
| --- | --- | --- |
| Memory embeddings | Oracle native `VECTOR` column | Supported |
| Memory payload and scope metadata | Oracle `JSON` column | Supported |
| Semantic retrieval | `VECTOR_DISTANCE` with relational and JSON filters | Supported |
| Approximate vector index | Oracle HNSW or IVF vector index | Supported |
| Mem0 operation history | Local SQLite through Mem0's `SQLiteManager` | Not yet Oracle-backed |
| Optional graph memory | A separate Mem0-supported graph provider | Not yet Oracle-backed |

Calling Oracle "the Mem0 backend" is accurate for the vector-store component. Making every Mem0 persistence component Oracle-backed would also require:

1. A pluggable Mem0 history-store interface and Oracle implementation to replace the currently hard-coded `SQLiteManager`.
2. An Oracle graph-store adapter if Mem0 Graph Memory is required.
3. Transaction coordination or an outbox pattern if vector, history, and graph writes must commit as one business operation.

## Why Oracle AI Database is a useful Mem0 vector store

- Native `VECTOR` and `JSON` keep embeddings, memory text, identity scope, and business metadata together.
- SQL and JSON predicates restrict the candidate set before or alongside semantic ranking.
- Oracle connection pooling, TLS, wallets, auditing, authorization, backup, and high availability apply to the memory store.
- HNSW and IVF indexes support approximate nearest-neighbor retrieval at scale.
- The application can join governed memory with transactional Oracle data without copying that data into a separate vector database.

## Directory contents

```text
Mem0-OracleAIDatabase/
├── .env.example
├── README.md
├── bootstrap.sh
├── config.py
├── demo.py
├── requirements.txt
├── vector_store_smoke.py
├── sql/
│   └── inspect.sql
└── tests/
    └── test_config.py
```

## Prerequisites

- Python 3.10 or later
- Oracle Database 23.4 or later with AI Vector Search
- A database user allowed to create a table and, when enabled, a vector index
- Network access or an Oracle wallet for the database
- An OpenAI API key for the end-to-end Mem0 extraction example

The database-only smoke test does not call an LLM or embedding service. It uses fixed three-dimensional vectors to validate Oracle CRUD, JSON filtering, and vector ranking.

## Configure

```bash
cd memory/Mem0-OracleAIDatabase
cp .env.example .env
```

Edit `.env`:

```dotenv
ORACLE_USER=mem0_user
ORACLE_PASSWORD=replace-me
ORACLE_DSN=localhost:1521/FREEPDB1
```

For Autonomous Database with a wallet:

```dotenv
ORACLE_DSN=mydb_high
ORACLE_CONFIG_DIR=/absolute/path/to/wallet
ORACLE_WALLET_LOCATION=/absolute/path/to/wallet
ORACLE_WALLET_PASSWORD=replace-if-required
```

Do not commit `.env`. The repository ignores it.

## Install

```bash
./bootstrap.sh
```

The script refuses Python versions older than 3.10, creates `.venv`, and installs Mem0 from the exact Oracle-provider merge commit.

## Test the Oracle vector-store contract

```bash
set -a
source .env
set +a
.venv/bin/python vector_store_smoke.py
```

The test:

1. Creates a uniquely named temporary Oracle table.
2. Inserts three fixed vectors and JSON payloads.
3. Searches with cosine distance and a `user_id` filter.
4. Verifies `get()`, `update()`, `list()`, and `delete()`.
5. Drops only the temporary table it created.

Use `--keep` to retain the table for inspection:

```bash
.venv/bin/python vector_store_smoke.py --keep
```

## Run end to end through Mem0

Add an OpenAI API key to `.env`, then run:

```bash
set -a
source .env
set +a
.venv/bin/python demo.py
```

The example calls:

```python
memory = Memory.from_config(config)
memory.add(messages, user_id="ava", metadata={"domain": "theme-park-concierge"})
memory.search("What should the concierge remember?", user_id="ava")
```

Mem0 uses the configured LLM to extract memory, the configured embedder to create a vector, and `OracleAIVectorSearch` to persist and retrieve it.

## Oracle objects

The provider creates one collection table:

```sql
CREATE TABLE <collection_name> (
    id      VARCHAR2(36) PRIMARY KEY,
    vector  VECTOR(<embedding_dimensions>),
    payload JSON
);
```

With indexing enabled, it also creates an HNSW or IVF vector index. The default collection in this starter is `MEM0_ORACLE_MEMORIES`.

Use [sql/inspect.sql](sql/inspect.sql) from SQLcl after setting its substitution variable:

```sql
DEFINE collection_name = MEM0_ORACLE_MEMORIES
@sql/inspect.sql
```

## Production work still required

- Replace the Git commit pin with the first released Mem0 version containing `oracledb`.
- Use a dedicated schema, least-privilege runtime account, and a separate migration identity if runtime DDL is prohibited.
- Decide whether Mem0 may auto-create tables and indexes or whether deployment migrations own them.
- Size the connection pool and vector index from measured concurrency, row count, dimensions, latency, and recall.
- Enforce tenant and user scope in every search filter. Add Oracle VPD or application contexts when defense in depth is required.
- Establish deletion, retention, backup, restore, and audit policies for memory payloads.
- Add failure recovery for Mem0's separate vector and history writes.
- Run Mem0's upstream Oracle provider test suite against the target Oracle release.

## Research sources

- [Mem0 Oracle provider merge commit](https://github.com/mem0ai/mem0/commit/d6d89c987bddf580870db14c69db974edfc5263c)
- [Mem0 `VectorStoreBase`](https://github.com/mem0ai/mem0/blob/main/mem0/vector_stores/base.py)
- [Mem0 Oracle AI Vector Search provider](https://github.com/mem0ai/mem0/blob/main/mem0/vector_stores/oracledb.py)
- [Mem0 vector-store overview](https://docs.mem0.ai/components/vectordbs/overview)
- [python-oracledb vector documentation](https://python-oracledb.readthedocs.io/en/latest/user_guide/vector_data_type.html)
- [Oracle AI Vector Search documentation](https://docs.oracle.com/en/database/oracle/oracle-database/26/vecse/)

