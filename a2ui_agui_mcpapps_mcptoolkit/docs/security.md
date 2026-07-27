# Security notes

## Trust boundaries

The browser, model output, A2UI envelopes, MCP App content, and tool arguments are untrusted. The agent service is the policy point for approval and exact tool allowlisting. Oracle AI Database is the final feasibility, locking, transaction, and audit enforcement point.

## Controls implemented

- Server validation for risk threshold, result limit, recommendation ID, notes, and actor
- Deterministic recommendations produced from governed Oracle views
- Short-lived, actor-bound, single-use approval nonce bound to the exact returned recommendations
- Browser approval contains no client-selected source, target, product, or quantity
- Rejection path that never calls a write tool
- Fixed A2UI version, catalog, surface, message, and component allowlists
- DOM construction with `textContent`; no generated scripts or HTML
- Bind placeholders in Toolkit YAML
- Tool allowlist limited to five supply-chain exchange operations
- Stored-procedure validation, deterministic source/target row locking, and current-stock revalidation
- One database statement for the audit insert and source-inventory reservation
- Placeholder-only environment template

## Controls required for a deployment

- TLS on the agent and MCP endpoints
- OAuth 2.0 rather than the Toolkit's development token mode
- Least-privileged Oracle database user with execute/select grants only
- MCP client authentication and authorization mapped to the authenticated actor
- Persistent, hashed idempotency and approval records rather than process memory
- Server-side session binding and CSRF protection for browser actions
- Explicit CORS and MCP App CSP/permission declarations
- Database or centralized audit trail for every tool outcome
- Timeouts, cancellation propagation, rate limits, and bounded response sizes
- Redaction rules for logs and model context
- Policy checks for location, product, quantity, value, and requester authority
- Integration tests that prove rollback and lock behavior under concurrent approvals
