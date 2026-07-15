# Oracle Database OpenTelemetry Observability

This folder contains a demo and blog draft for an end-to-end OpenTelemetry trace
from a Spring Boot application to Oracle Database internals.

The intended trace shape is:

```text
HTTP request
  -> Spring Boot server span
  -> Micrometer business observation
  -> Oracle JDBC roundtrip span
  -> Oracle Database server-side trace spans
  -> OTLP collector
  -> Jaeger, Grafana, or another OpenTelemetry backend
```

## Why this stack

Spring Boot Actuator provides Micrometer Tracing auto-configuration. Micrometer
bridges application observations into OpenTelemetry, and the Oracle JDBC
Observability provider emits JDBC driver events into the same OpenTelemetry
context. Oracle Database server-side OpenTelemetry can then export database
internals to the same collector when `DBMS_OBSERVABILITY` is enabled and the
database can reach the collector endpoint.

## Contents

- `springboot-oracle-db-otel-demo/`: Spring Boot demo app.
- `blog.html`: publishable blog walkthrough for GitHub Pages.
- `start-jaeger.sh` / `stop-jaeger.sh`: local Jaeger backend with OTLP ports enabled.
- `build.sh`, `run-app.sh`, `smoke-test.sh`: build, run, and verification helpers.
- `.env_example`: template for local database and OTLP settings. Copy to `.env`
  and keep credentials local.
- `sql/`: database-side setup scripts based on the attached Oracle notes.
- `RUNBOOK.md`: step-by-step setup, run, verification, and troubleshooting.
- `docs/`: supplemental material. `OCI_VM_BLOG.md` is the detailed verified
  single-VM OCI Linux transcript, `RESEARCH.md` captures source notes and
  links, and `testcase_podman_otel_local_failure.md` records the local Podman
  failure analysis.

## Trace Modes

The demo can be run in two useful modes:

- App and JDBC tracing with the public Spring Boot demo dependencies.
- App-to-database-internals tracing when the database, network ACLs, collector,
  and Oracle JDBC driver support server-side OpenTelemetry propagation.

Start with the app/JDBC mode, then add database server-side tracing once the
collector is reachable from the database.

## Quick Start

The public blog page is:

```text
https://paulparkinson.github.io/oracle-ai-for-sustainable-dev/observability/blog.html
```

Start a local Jaeger backend with Podman:

```bash
cd observability
./start-jaeger.sh
```

Configure the Spring Boot application:

```bash
cp .env_example .env
# Edit .env with DB_URL, DB_USERNAME, and DB_PASSWORD.
```

Build and run the application:

```bash
./build.sh
./run-app.sh
```

In another terminal, generate traces:

```bash
cd observability
./smoke-test.sh
```

Generate the agentic AI trace bridge demo:

```bash
curl 'http://localhost:8080/trace/agent-task?agentId=claims-investigator-agent&task=investigate_payment_anomalies'
```

Open the rendered browser view:

```text
http://localhost:8080/trace/agent-task/view?agentId=claims-investigator-agent&task=investigate_payment_anomalies
```

This endpoint is the most cohesive demo: Jaeger provides the visual trace, and
the response includes the same `traceId`, the agent id/task, the database
`SQL_ID`, full SQL text, application bind values, captured bind samples, a SQL
Monitor preview, and `DBMS_XPLAN` output for the SQL execution plan. The
rendered view embeds the Jaeger trace at the bottom after a short delay so the
visual trace and database diagnostics stay on one page. Use
`oracle.db.query.sql.id` on the Jaeger `oracle-db / DB Server` span as the bridge
from the visual trace to the database-internal SQL diagnostics.

Open Jaeger:

```text
http://localhost:16686
```

For full setup, build, DB-side tracing, and troubleshooting instructions, see
`RUNBOOK.md`.

## Verified Results

With local Jaeger, Oracle Database Free in Podman, and the Spring Boot app
running in the same Podman network, the demo has been verified to produce a
single Jaeger trace containing:

- the Spring Boot HTTP server span
- the explicit `oracle.demo.database-roundtrip` Micrometer observation
- Oracle JDBC provider spans such as authentication, ping, and `Execute query`

That proves the app-to-JDBC path. The local database also received the same
trace ids through JDBC server telemetry, but the tested public Oracle Database
Free build still archived those database operations to disk instead of exporting
an `oracle-db` service span to Jaeger. Treat database-internal spans as a
database image/exporter capability check, not an application instrumentation
problem.

For the local Oracle Free setup commands, see `RUNBOOK.md`.

On an OCI Linux x86_64 VM, the full end-to-end path has also been verified with
the Spring Boot app, Oracle Database Free, a local HTTPS OTLP proxy, and Jaeger
all running on the same instance. The successful Jaeger trace contains both
`springboot-oracle-db-otel-demo` and `oracle-db` services, including a `DB Server` span.
The app also sets database `MODULE`, `ACTION`, and `CLIENT_IDENTIFIER`, and sends
the Oracle JDBC `tracelevel` client context so the database span is easier to
recognize as server-side work. For the reproducible setup, see
`docs/OCI_VM_BLOG.md`.

## Database Server-Side Setup

The database must be able to reach the OTLP collector endpoint. If the database
is Autonomous Database or otherwise outside your laptop, `localhost:4318` will
not work for the database-side exporter. Use a reachable HTTPS collector
endpoint or a collector deployed in the same network.

For the financial OKE cluster, deploy the OKE-hosted Jaeger endpoint:

```bash
kubectl apply -f observability/k8s/oke-jaeger.yaml
```

Then use this OTLP endpoint for both the app and `DBMS_OBSERVABILITY`:

```text
https://oracledev.ai/financial/otel/v1/traces
```

Review and run:

```sql
@sql/grant_observability_privileges.sql
@sql/create_observability_acl.sql
@sql/enable_server_observability.sql
@sql/setup_local_deep_data_security.sql
```

For a local Oracle Free container, use:

```sql
@sql/setup_local_free_observability.sql
@sql/setup_local_deep_data_security.sql
```

The first script creates and seeds `FINANCIAL.AGENT_EVENT_LOG`. The second asks
for a private password and creates a local Deep Sec end user named
`"claims-investigator-agent"`, associates it with the `FINANCIAL` schema for
name resolution, grants it `AGENT_CLAIMS_INVESTIGATOR`, and gives that data role
the standard session/diagnostic role needed by this contained demo. Configure
the Spring Boot datasource with `DB_USERNAME='"claims-investigator-agent"'`
(the quotes preserve the case-sensitive Deep Sec end-user name) and its private
password, not the `FINANCIAL` schema-owner credentials.

For a local demo, set `SQL_TRACE` at the session level. Avoid instance-wide
tracing in shared or production environments unless you have measured the cost.

The separate `db-observability-scenarios` material notes that some older
driver/database combinations may show only app-side traces, or may need a fixed
driver build for full server-side trace propagation. The Spring Boot/Micrometer
approach is still the right app-side shape; the final database span continuation
depends on the database-side exporter being enabled and reachable.

The most useful demo view is the Jaeger trace detail page: blue
`springboot-oracle-db-otel-demo` spans come from the Java process, while the orange
`oracle-db / DB Server` span comes from Oracle Database server-side export.
For the agentic AI use case, open the `/trace/agent-task` trace in Jaeger and
expand the `DB Server` span whose `oracle.db.action` is `agent-workload-query`.
The app response then uses that span's `oracle.db.query.sql.id` as the bridge to
SQL Monitor and `DBMS_XPLAN`.

The browser view also includes a **Database Security Context** panel. It uses a
real password-authenticated local Deep Sec end user, so it does not require
Entra ID, OCI IAM, an OAuth token, or a JDBC `EndUserSecurityContext` call. The
panel correlates the trace with `ORA_END_USER_CONTEXT.username`, the current
schema, `MODULE`, `ACTION`, `CLIENT_IDENTIFIER`, enabled regular roles,
effective privileges, the granted Deep Sec data role, its data grant, and the
roles currently active in `V$END_USER_DATA_ROLE`. A row-filtering proof in the
same traced session counts the permitted agent rows and confirms that rows owned
by other agents are not visible.

The same panel also separates regular database roles from Deep Data Security
data roles. For example, `SELECT_CATALOG_ROLE` is a normal database role visible
in `SESSION_ROLES`, while `AGENT_CLAIMS_INVESTIGATOR` is a DDS data role visible
through `DBA_DATA_ROLES` and `DBA_DATA_GRANTS`. The local end user receives that
role through `GRANT DATA ROLE`, and direct password login activates it without
application-side security-context code. `FINANCIAL` remains only the owning
schema; a conventional database user cannot be the grantee of `GRANT DATA ROLE`.

## References

- Oracle JDBC Observability provider:
  https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-observability
- Oracle `DBMS_OBSERVABILITY`:
  https://docs.oracle.com/en/database/oracle/oracle-database/26/arpls/dbms_observability.html
- Spring Boot 3.3 tracing:
  https://docs.spring.io/spring-boot/3.3/reference/actuator/tracing.html
- OpenTelemetry Collector quick start:
  https://opentelemetry.io/docs/collector/quick-start/
- Jaeger getting started:
  https://www.jaegertracing.io/docs/latest/getting-started/
- Oracle `SQL_TRACE` parameter:
  https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html
