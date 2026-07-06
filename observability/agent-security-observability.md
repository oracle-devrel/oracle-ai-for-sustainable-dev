# Agent Security Observability With Oracle AI Database

This note captures a follow-on direction for the Oracle Database OpenTelemetry demo: combine database server-side tracing with Deep Data Security concepts so an AI agent trace shows not only what happened, but which identity, role, grant, and policy allowed it to happen.

## Core Idea

Use the distributed trace as the visual spine, then enrich it with Oracle Database security context.

Observability answers:

- Which agent task ran?
- Which service, JDBC call, and database session participated?
- Which SQL statement ran?
- How long did each app, JDBC, and database span take?

Deep Data Security answers:

- Which human or workload identity was propagated to the database?
- Which database user, external identity, or mapped application principal was used?
- Which roles and privileges were active?
- Which objects and rows were visible to that identity?
- Which access was allowed or denied by database policy?

Together, this creates an identity-aware trace for agentic AI.

## Example Flow

An agent task starts with application context:

- `agent.id=claims-investigator-agent`
- `agent.task=investigate_payment_anomalies`
- `human.user=paul.parkinson@oracle.com`
- `trace_id=<otel-trace-id>`
- `span_id=<otel-span-id>`

The Spring Boot service connects through JDBC and propagates OpenTelemetry context to Oracle Database. The database exports its own server-side span into the same trace.

At the same time, the app and database set useful session context:

- `DBMS_APPLICATION_INFO.SET_MODULE('agent:claims-investigator-agent')`
- `DBMS_APPLICATION_INFO.SET_ACTION('investigate_payment_anomalies')`
- `CLIENT_IDENTIFIER='paul.parkinson@oracle.com'`
- Application context values such as `agent_id`, `task_id`, `purpose`, `tenant`, or `risk_score`

The result is a trace that can say:

> Agent `claims-investigator-agent` ran task `investigate_payment_anomalies` on behalf of `paul.parkinson@oracle.com`. Oracle Database authenticated the propagated identity, enabled only the mapped roles, executed SQL_ID `abc123`, returned permitted rows, and produced database-side telemetry and audit records.

## What To Show In The Demo UI

The observability page should eventually show one cohesive view:

- Jaeger trace for the end-to-end app, JDBC, and database server-side spans
- Agent task metadata: agent id, task name, task purpose, risk score
- Database identity: database user, external/OAuth subject, client identifier
- Authorization context: enabled roles, object privileges, and relevant grants
- SQL bridge: `oracle.db.query.sql.id`, full SQL text, and captured bind samples
- Execution detail: SQL Monitor / `DBMS_XPLAN` plan information
- Security result: whether access was allowed, denied, filtered, redacted, or audited
- Audit references: unified audit or fine-grained audit rows that correspond to the same user/session/action

## Why `oracle.db.query.sql.id` Matters

The `oracle.db.query.sql.id` tag is the bridge between the visual trace and the database internals.

From Jaeger, the SQL_ID can be used to find:

- SQL text in `V$SQL`
- Bind samples in `V$SQL_BIND_CAPTURE`
- Execution plan details through `DBMS_XPLAN`
- SQL Monitor details where available
- Related session details in `V$SESSION`
- Audit rows that include matching SQL text, object access, user, client identifier, module, or action

That makes the trace actionable: a reviewer can start from an agent task and drill into the exact database work that task caused.

## Is This Auditing?

The trace itself is not auditing.

Tracing is observability. It is excellent for debugging, performance analysis, causality, and visual correlation, but traces are commonly sampled, mutable, collector-controlled, and retained for shorter periods.

Auditing requires a durable security record created by the database or a trusted audit service.

For Oracle Database, that means mechanisms such as:

- Unified Auditing
- Fine-Grained Auditing
- Audit records in `UNIFIED_AUDIT_TRAIL`
- Auditing of logon/logoff, privilege use, role changes, object access, grants, DDL, and policy violations
- Optional forwarding to OCI Data Safe, AVDF, SIEM, or other controlled retention systems

The best story is:

> Observability explains the agent's execution path. Auditing proves the security-relevant facts.

## What Would Constitute Auditing

A complete agentic AI database audit story would record:

- Who initiated the task: human user, workload identity, or service principal
- Which agent acted: agent id, version, deployment, and task
- Which database identity was used: database user, external identity, schema, proxy user, or mapped role
- Which privileges were active: role, grant, or policy basis
- Which object was accessed: owner, table, view, package, or procedure
- What operation occurred: select, insert, update, delete, execute, grant, revoke, or DDL
- Whether it succeeded or failed: return code, error, or policy denial
- Which SQL was involved: SQL text or SQL_ID, with bind values where appropriate and safe
- Which session context was present: client identifier, module, action, trace id, span id
- When and where it happened: timestamp, host, service, database, PDB, session id

This should be retained outside the application trace pipeline, because audit evidence has a different trust and retention model than telemetry.

## Good Oracle AI Database Security Hooks

Potential security and audit features to tie into the demo:

- Deep Data Security with OAuth access tokens and database-side identity mapping
- Database roles mapped to external identities or application principals
- Object grants and least-privilege role design
- `CLIENT_IDENTIFIER`, `MODULE`, and `ACTION` for session attribution
- Application contexts for agent id, task id, purpose, and tenant
- Fine-Grained Auditing for sensitive tables or columns
- Unified Auditing for durable security events
- Virtual Private Database for row-level policy enforcement
- Data Redaction for sensitive result values
- SQL Firewall for permitted SQL shape enforcement
- OCI Data Safe or AVDF for centralized audit reporting

## Recommended Demo Progression

1. Start with the current OpenTelemetry demo.
   Show app, JDBC, and Oracle Database server-side spans in one Jaeger trace.

2. Add agent metadata.
   Show `agent.id`, `agent.task`, and task purpose in the page and as span attributes.

3. Add database session attribution.
   Set module, action, and client identifier so the database span and database views show the agent and user context.

4. Add Deep Data Security identity.
   Authenticate the app/database access through OAuth-backed identity rather than a shared technical password.

5. Add role/grant visibility.
   Show which database roles and object privileges are active for the propagated identity.

6. Add an allowed/denied comparison.
   Run the same agent task as two different users or roles and show that the database returns different data or denies access.

7. Add auditing.
   Enable Unified Auditing and/or Fine-Grained Auditing and show audit rows correlated to the trace using user, client identifier, module, action, SQL_ID, and timestamp.

8. Add the governance narrative.
   Present the trace and audit view as an explainable control plane for AI agents accessing Oracle AI Database.

## One-Sentence Positioning

This turns Oracle Database observability into an agentic AI security story: a Jaeger trace shows what the agent did, while Oracle Database identity, grants, roles, policies, and audit records explain why it was allowed and prove what happened.
