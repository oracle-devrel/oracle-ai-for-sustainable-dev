# Python Oracle AI Agent Memory Demo

This implementation uses the latest public `oracleagentmemory==26.6.0` package and Oracle AI Database. It mirrors the Flynn's Theme Park Java demo's seven memory proofs and its Memory Quest graph, spatial, vector, transaction, audit, and gamification flow.

It intentionally uses:

- `SearchStrategy.KEYWORD` with Oracle Text and `SearchIndexSyncMode.ON_COMMIT`;
- no external embedding or LLM key;
- explicit durable memory writes;
- exact user and agent scopes;
- threads and persisted messages;
- fact, preference, memory, and guideline record types;
- whole-record updates through `update_memory()`;
- persisted-message updates through `update_message()`;
- per-record time-to-live;
- bounded retention through `MemoryRetentionConfig`;
- metadata-filtered retrieval; and
- a separately scoped, human-approved guideline.
- a live Oracle Deep Data Security comparison using separate Ava organizer and
  Leo participant data roles on the managed memory table.

Compared with the current Java merge-request branch, the Python package still
provides the more complete lifecycle surface: named memory/message update and
delete APIs, working TTL anchors and retention limits, async variants, executed
background extraction, Oracle Text keyword and true hybrid search with index
synchronization controls, schema-owner options, and explicit thread updates.
The Java branch now shares the six-table model, vector search, exact scope,
metadata-filter objects, schema policies, extraction configuration, and context
cards, but some declared facilities remain partial. In particular, Java's
`EXPIRES_AT` columns are not populated by its current JDBC writers, its
`BACKGROUND` mode only records a pending flag, and its `HYBRID`/`KEYWORD`
strategies currently use lexical fallback rather than Python's managed Oracle
Text/hybrid implementation. This app exercises the Python update, TTL,
retention, metadata-filter, keyword-search, and index-sync capabilities directly.
See `/api/capabilities` for the runtime inventory.

Run:

```bash
cd memory/python-agent
./run.sh
```

Open <http://127.0.0.1:8092>. In a second terminal:

```bash
cd memory/python-agent
./smoke-test.sh
```

For the database-enforced Ava versus Leo proof, run the one-time setup in
`memory/deep-data-security` first. The browser's **Deep Data Security proof**
panel executes the same memory query as each local end user and shows the rows
returned by Oracle AI Database. The Leo cross-user probe must return zero.

The same page includes an optional AR simulator. It starts a consent-scoped
session, sends “remember this” observations through Oracle Agent Memory, and
indexes an opted-in media transcript with a 384-dimensional Oracle AI Database
vector. The corresponding Lens Studio 5.15.4 source kit is in
`memory/spectacles-lens`. Actual ASR, camera, permission, tracking, comfort, and
outdoor testing require Spectacles hardware; the repository does not claim that
those device-only paths have been validated.

The runner reuses `financial/setup/.env` when `memory/.env` is absent. It never prints the database or wallet passwords.

The Memory Quest application tables are shared with the Java demo. If they do not yet exist in the selected schema, run `memory/java-agent/run.sh` once to initialize the `AIM_DEMO_*` and `AIM_PARK_*` objects, stop it, then start this Python app. The Oracle Agent Memory `MAGIC_PY_*` tables are created and managed by the public Python SDK itself.

## Package status

- Oracle Agent Memory: `26.6.0`, published July 7, 2026.
- Supported interpreter used here: Python 3.12.
- LiteLLM is pinned to `1.84.10`, inside the SDK's supported `>=1.84.0,<2` range. Newer LiteLLM 1.95 attempted a local Rust build on this Mac and required a newer Rust compiler; the pin uses its published pure-Python wheel.
- Search is deliberately `KEYWORD` so the demo needs no external embedding/model key. The Memory Quest GraphRAG lane still uses the database-resident `ALLMINILM` model, matching the Java demo.
- In 26.6.0, `update_memory()` can persist a timestamp/TTL combination that makes a record immediately expired and then raise `KeyError` while reloading it through the SDK's active-only read path. The demo treats that specific post-update condition as success and verifies that search excludes the row; the database inspector still shows its expired timestamp.
