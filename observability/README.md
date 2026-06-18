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
OpenTelemetry provider emits JDBC driver events into the same OpenTelemetry
context. Oracle Database 23.9 server-side OpenTelemetry can then export database
internals to the same collector when server-side tracing is enabled.

## Contents

- `oracle-db-otel-demo/`: Spring Boot demo app.
- `docker-compose.yaml`: local Jaeger backend with OTLP ports enabled.
- `sql/`: database-side setup scripts based on the attached Oracle notes.
- `RUNBOOK.md`: step-by-step setup, run, verification, and troubleshooting.
- `docs/BLOG.md`: working blog draft.
- `docs/RESEARCH.md`: source notes and links.

## Trace Modes

The demo can be run in two useful modes:

- App and JDBC tracing with the public Spring Boot demo dependencies.
- App-to-database-internals tracing when the database, network ACLs, collector,
  and Oracle JDBC driver support server-side OpenTelemetry propagation.

Start with the app/JDBC mode, then add database server-side tracing once the
collector is reachable from the database.

## Quick Start

Start a local Jaeger all-in-one backend:

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
```

Run the application:

```bash
cd observability/oracle-db-otel-demo
mvn spring-boot:run
```

Generate a traced database roundtrip:

```bash
curl http://localhost:8080/trace/roundtrip
```

Open Jaeger:

```text
http://localhost:16686
```

For full setup, build, DB-side tracing, and troubleshooting instructions, see
`RUNBOOK.md`.

## Database Server-Side Setup

The database must be able to reach the OTLP collector endpoint. If the database
is Autonomous Database or otherwise outside your laptop, `localhost:4318` will
not work for the database-side exporter. Use a reachable HTTPS collector endpoint
or a collector deployed in the same network.

Review and run:

```sql
@sql/enable_server_observability.sql
```

For a local demo, set `SQL_TRACE` at the session level. Avoid instance-wide
tracing in shared or production environments unless you have measured the cost.

The separate `db-observability-scenarios` material notes that some current
public driver/database combinations may show only app-side traces, or may need a
fixed/internal OJDBC build for full server-side OSCID propagation. That does not
invalidate the Spring Boot/Micrometer approach; it means the final database
span continuation depends on the driver and database feature path.

## References

- Oracle JDBC OpenTelemetry provider:
  https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-opentelemetry
- Spring Boot 3.3 tracing:
  https://docs.spring.io/spring-boot/3.3/reference/actuator/tracing.html
- OpenTelemetry Collector quick start:
  https://opentelemetry.io/docs/collector/quick-start/
- Jaeger getting started:
  https://www.jaegertracing.io/docs/latest/getting-started/
- Oracle `SQL_TRACE` parameter:
  https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html
