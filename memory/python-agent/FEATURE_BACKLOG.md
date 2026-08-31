# Oracle Agent Memory Python Feature Backlog

This backlog tracks public Python Oracle Agent Memory capabilities that the
theme park demo does not yet exercise. The existing app already demonstrates
durable memories and messages, exact identity scope, updates, reset/deletion,
TTL and expiration, retention configuration, metadata filtering, Oracle Text
keyword search, and search-index synchronization.

## Recommended next additions

| Priority | Capability | Theme park use | Proof to add |
|---|---|---|---|
| 1 | LLM-driven memory extraction | Extract a guest preference, accessibility requirement, visit outcome, and reusable guideline from a natural conversation. | Show the source messages beside the extracted records and database changes. |
| 2 | True hybrid search | Combine semantic similarity with exact terms such as attraction names, dietary restrictions, accessibility features, and closure codes. | Compare keyword, vector, and hybrid rankings for the same request. |
| 3 | Executed background extraction | Keep the concierge responsive while durable memories are extracted after the conversation turn. | Display queued, processing, completed, and failed extraction states. |
| 4 | Async APIs | Handle multiple party members, live park events, and memory retrieval without blocking request threads. | Add concurrent Ava and Leo requests using the SDK's async methods. |
| 5 | Richer metadata operators | Filter by consent, sensitivity, visit date, park zone, confidence, source, and approval status. | Add nested, array, range, and combined filter examples where supported. |
| 6 | Explicit thread reconfiguration | Change extraction frequency, context limits, or instructions for a trip without losing durable guest memory. | Reconfigure a thread and show which persisted records remain reusable. |
| 7 | Retention cleanup at scale | Apply different retention policies to temporary closures, visit episodes, preferences, and approved procedures. | Seed batches, run cleanup, and report retained versus expired records. |

## Useful but lower-priority additions

| Capability | Why it is lower priority |
|---|---|
| Schema-owner separation | Important for production security and operations, but less visible in a short user-facing demonstration. |
| Framework integrations | LangGraph and agent SDK adapters improve reuse, but the memory behavior should be demonstrated clearly before adding orchestration frameworks. |
| Additional model and embedding providers | Valuable for portability and comparison, but not necessary to prove the Oracle AI Database memory lifecycle. |

## Implemented optional AR experience

AR should remain a client of the same governed memory, spatial, graph, quest,
and transaction services rather than becoming a separate memory implementation.
For example, smart glasses or a phone camera could overlay:

- accessible and covered route arrows;
- attraction, wait-time, and closure information;
- personalized clues that do not reveal another guest's private memory;
- party regrouping points allowed by current consent;
- quest checkpoints, points, and earned badges; and
- a confirmation control before any reservation, transfer, or purchase.

The ordinary browser map remains the accessible fallback and development
harness. An AR client can be added later using WebXR or a native AR framework
without changing the Oracle Agent Memory or Oracle AI Database contracts.

The browser simulator, Python HTTPS contract, Oracle Agent Memory write,
consent-specific session state, TTL, database vector search, and audit trail are
implemented and database-tested. The Lens Studio 5.15.4 source kit maps the
same contract to Spectacles ASR and overlays. Lens Studio import plus physical
device testing remain required before describing the experience as a working
Spectacles Lens.

## AR safeguards

- Minimize overlays to avoid distraction and visual overload.
- Never expose sensitive or inferred guest attributes in public view.
- Require consent for party-location sharing and make revocation immediate.
- Mark inferred or stale information and show the source/update time.
- Use human confirmation for consequential transactions.
- Provide audio, high-contrast, reduced-motion, and non-AR alternatives.
- Avoid biometric identification unless it is independently justified,
  consented to, secured, and legally reviewed.
