# Memories Are the Magic

This directory contains two articles, two runnable Oracle AI Database demos, and
a subtitle-led walkthrough video:

- [`blog.html`](blog.html): presenter-ready article and walkthrough based on the “Memories Are the Magic” presentation.
- [`comparisons.html`](comparisons.html): comparison of commonly evaluated AI agent-memory platforms.
- [`python-agent/`](python-agent/): public Oracle AI Agent Memory 26.6 SDK reference
  and browser UI.
- [`java-agent/`](java-agent/): Java 21, the `ojdbc-agent-memory` proof-of-concept library,
  Oracle JDBC/UCP, explicit lifecycle mechanics, and browser UI.
- [`database/`](database/): reference schema and SQLcl inspection queries.
- [`video/`](video/): reproducible silent MP4, poster, SRT, WebVTT, and build source.

## What the demo proves

The Flynn's Theme Park concierge demonstrates:

1. working, episodic, semantic, operational, and procedural state;
2. Retain, Recall, Reuse, and Refine;
3. guest-and-agent scope before recall;
4. transactional correction with auditable versions;
5. expiration of short-lived state;
6. experience traces as learning material;
7. human approval of an induced shared skill; and
8. reuse of that skill by a second guest with zero private-memory leakage.

The numbered presenter flow uses fixed Java and SQL extraction and skill
induction steps so a live presentation is reproducible. The Oracle Database operations are
real. The Python version uses the public Oracle AI Agent Memory SDK. The Java
version has a live library lane that uses Ollama extraction, in-database
`ALLMINILM` embeddings, exact scope, semantic search, and context cards. Its
second lane exposes lifecycle and transaction mechanics for teaching.

## Run

The runners reuse `financial/setup/.env` if it exists. Otherwise:

```bash
cp memory/.env.example memory/.env
# Edit memory/.env. Do not commit it.
```

Run the public Python SDK reference first:

```bash
cd memory/python-agent
./test.sh
./run.sh
```

Open <http://127.0.0.1:8092>. Its end-to-end API test is:

```bash
cd memory/python-agent
./smoke-test.sh
```

Prepare the Java reference with Java 21, Maven, Ollama, and a local
`ojdbc-agent-memory` proof-of-concept library clone:

```bash
brew install ollama
brew services start ollama
ollama pull llama3.2

cd memory/test-utils
./load-allminilm.sh
```

Then build and run it. Set `OAM_LIBRARY_DIR` when the clone is not at the
default path printed by `memory/test-utils/build-library.sh`:

```bash
cd memory/java-agent
export OAM_LIBRARY_DIR=/path/to/ojdbc-agent-memory
./test.sh
./run.sh
```

Open <http://127.0.0.1:8091>. In a second terminal, run:

```bash
cd memory/java-agent
./smoke-test.sh
```

In the browser, use the live Java library controls in this order:

1. **Reset library lane**
2. **Retain Ava's conversation**
3. **Recall and build context**

The result shows extracted typed memory, 384-dimensional database embeddings,
semantic ranking, a generated context card, and an exact-scope check that
returns zero Ava records for Leo. Continue with the seven numbered controls to
present correction, TTL, trace induction, human approval, and safe reuse.
The database inspector at the bottom refreshes after each action and highlights
added, changed, and removed rows across all seven demo tables.

The Java service creates only missing `OAMJ_CONCIERGE_*` and `AIM_DEMO_*`
objects. Reset actions delete rows only from their respective demo objects;
they do not drop schema objects. The Python service uses SDK-managed objects
with the `MAGIC_PY` store ID.

## Inspect the database

After running the browser walkthrough, execute [`database/inspect_demo.sql`](database/inspect_demo.sql) with SQLcl to see memory versions, recall audit records, traces, and skill approval state.
