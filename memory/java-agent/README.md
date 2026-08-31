# Flynn's Theme Park Java Memory Demo

This browser app has three complementary lanes:

- a live `ojdbc-agent-memory` flow from the Java merge-request branch using Ollama
  extraction, Oracle AI Database `ALLMINILM` embeddings, exact identity scope,
  semantic search, and context-card assembly;
- a repeatable presenter flow with fixed Java and SQL actions for correction, TTL, experience traces,
  human-approved procedural learning, and cross-guest isolation.
- a Memory Quest flow that combines SQL Property Graph, Oracle Spatial,
  AI Vector Search, GraphRAG, a consent-bounded party, an accessible 2D park
  map, transactional checkpoints, points, badges, and reward audit.

## Prerequisites

- Java 21
- Maven
- a working Oracle Database connection in `financial/setup/.env` or
  `memory/.env`
- Ollama running locally with `llama3.2:latest`
- `ALLMINILM` loaded in the connected Oracle Database schema
- the `feature/ojdbc-agent-memory-java` branch of the memory repository, with
  `java/ojdbc-agent-memory` built locally

One-time model setup:

```bash
brew install ollama
brew services start ollama
ollama pull llama3.2

cd memory/test-utils
./load-allminilm.sh
```

## Test and run

```bash
cd memory/java-agent
export OAM_LIBRARY_DIR=/path/to/memory/java/ojdbc-agent-memory
./test.sh
./run.sh
```

Open <http://127.0.0.1:8091>. In another terminal, run the complete API proof:

```bash
cd memory/java-agent
./smoke-test.sh
```

The live library controls are intended to be used in order:

1. **Reset library lane**
2. **Retain Ava's conversation**
3. **Recall and build context**

On the first launch after upgrading from the earlier two-table prototype, the
app detects its incompatible `OAMJ_CONCIERGE_RECORDS`/`MESSAGE` schema and
recreates only those demo-prefixed managed objects in the six-table MR format.

Then use the seven numbered presenter controls to show the governed memory
lifecycle and continual-learning concepts.

Finally, run the Memory Quest controls in order:

1. **Reset quest lane**
2. **Plan accessible route** to traverse `AIM_PARK_GRAPH` and compare the
   route with `SDO_GEOM.SDO_DISTANCE`
3. **Start quest** to create Ava's transactional progress and audit rows
4. **Complete next checkpoint** three times to finish the quest, earn 400
   total points, and receive the Lantern Pathfinder badge
5. **Retrieve + expand graph** to rank `AIM_PARK_KNOWLEDGE` vectors and add
   connected places and quest context through SQL Property Graph

At the bottom of the page, the database inspector shows current contents from
the six Python-parity `OAMJ_CONCIERGE_*` managed library tables, five `AIM_DEMO_*` lifecycle
tables, and the principal `AIM_PARK_*` graph, spatial, vector, progress,
badge, and reward tables. It refreshes automatically after every action. **Refresh table
contents** performs the same check manually without reloading the page. Added,
changed, and removed rows remain highlighted until the next refresh.

The Memory Quest schema is created idempotently by `ParkDatabaseSetup`. Its
main objects are:

- `AIM_PARK_GRAPH`, a SQL property graph over guests, parties, places, paths,
  quests, steps, badges, and earned-badge edges;
- `AIM_PARK_PLACES.LOCATION`, an `SDO_GEOMETRY` point for each place;
- `AIM_PARK_KNOWLEDGE.EMBEDDING`, a native `VECTOR(384, FLOAT32)` generated
  with the database-resident `ALLMINILM` model;
- transactional `AIM_PARK_PROGRESS`, `AIM_PARK_GUEST_BADGES`, and
  `AIM_PARK_REWARD_AUDIT` records.
