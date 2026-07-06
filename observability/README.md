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
- `docker-compose.yaml`: local Jaeger backend with OTLP ports enabled.
- `sql/`: database-side setup scripts based on the attached Oracle notes.
- `RUNBOOK.md`: step-by-step setup, run, verification, and troubleshooting.
- `docs/BLOG.md`: working blog draft.
- `docs/OCI_VM_BLOG.md`: verified single-VM OCI Linux walkthrough with
  database-internal spans in Jaeger.
- `docs/RESEARCH.md`: source notes and links.

## Trace Modes

The demo can be run in two useful modes:

- App and JDBC tracing with the public Spring Boot demo dependencies.
- App-to-database-internals tracing when the database, network ACLs, collector,
  and Oracle JDBC driver support server-side OpenTelemetry propagation.

Start with the app/JDBC mode, then add database server-side tracing once the
collector is reachable from the database.

## Quick Start

Start a local Jaeger backend with Podman:

```bash
cd observability
podman run -d --name oracle-db-otel-jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  cr.jaegertracing.io/jaegertracing/jaeger:2.19.0
```

Or use Docker Compose:

```bash
cd observability
docker compose up -d
```

Configure the Spring Boot application:

```bash
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'
export DB_USERNAME='financial'
export DB_PASSWORD='<database-password>'
export OTLP_TRACES_ENDPOINT='http://localhost:4318/v1/traces'
unset DEBUG
```

Run the application:

```bash
cd observability/springboot-oracle-db-otel-demo
export OTEL_SEMCONV_STABILITY_OPT_IN=database/dup
mvn spring-boot:run
```

Generate a traced database roundtrip:

```bash
curl http://localhost:8080/trace/roundtrip
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
```

For a local Oracle Free container, use:

```sql
@sql/setup_local_free_observability.sql
```

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
