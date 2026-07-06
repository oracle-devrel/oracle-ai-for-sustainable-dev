# One Trace From Spring Boot to Oracle Database Internals

Distributed tracing usually stops at the database call. You get a nice HTTP
server span, maybe a service span, and then a generic "JDBC query" block. Useful,
but the most interesting part of the latency is often hidden behind that block.

Oracle Database and the Oracle JDBC driver can go deeper. With Spring Boot,
Micrometer Tracing, Oracle JDBC OpenTelemetry events, and Oracle Database
server-side OpenTelemetry export, the trace can continue from the application
thread into the database engine.

## The Shape Of The Demo

The flow is:

```text
Browser or curl
  -> Spring Boot HTTP endpoint
  -> Micrometer observation around business logic
  -> Oracle JDBC driver roundtrip events
  -> Oracle Database server-side trace export
  -> OTLP collector
  -> Jaeger or Grafana
```

The reason this works is that every layer speaks OpenTelemetry. Spring Boot
bridges Micrometer observations to OpenTelemetry. Oracle JDBC contributes
driver-level spans through the `ojdbc-provider-observability` provider. The
driver can also enable server-side telemetry on the database session, allowing
the database to export trace data to the same collector when
`DBMS_OBSERVABILITY` is configured.

## Why Spring Boot And Micrometer

Spring Boot Actuator already knows how to create HTTP spans, correlate logs, and
export traces through Micrometer. In Spring Boot 3.3, OpenTelemetry with OTLP
uses these dependencies:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

For a demo, sample every request:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

This is the right default for a Spring Boot application. The OpenTelemetry Java
agent is a good option for broader auto-instrumentation, but the important
database-side handoff is controlled by the Oracle JDBC driver, the database
session, `DBMS_OBSERVABILITY`, and the collector endpoint.

## Adding Oracle JDBC Spans

Oracle's Observability provider for JDBC is the piece that makes the database
client visible. Add the provider:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-observability</artifactId>
  <version>1.1.0</version>
</dependency>
```

Then set the connection property:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        "[oracle.jdbc.provider.traceEventListener]": observability-trace-event-listener-provider
        "[oracle.jdbc.provider.traceEventListener.unique_identifier]": springboot-oracle-db-otel-demo
```

The provider emits events such as database roundtrips. Sensitive details such as
full SQL text are disabled by default, which is the right default for a blog
demo. For stable OpenTelemetry database semantic conventions during the
migration period, run the app with:

```bash
export OTEL_SEMCONV_STABILITY_OPT_IN=database/dup
```

One Spring-specific detail matters: the Oracle JDBC provider reads the
OpenTelemetry SDK from `GlobalOpenTelemetry`, while Spring Boot wires an
`OpenTelemetry` bean. The sample bridges those by registering the Spring bean at
startup:

```java
@Bean
ApplicationRunner registerGlobalOpenTelemetry(OpenTelemetry openTelemetry) {
  return args -> {
    try {
      GlobalOpenTelemetry.set(openTelemetry);
    } catch (IllegalStateException alreadyInitialized) {
      // Another OpenTelemetry setup, such as the Java agent, got there first.
    }
  };
}
```

## Asking The Driver To Enable Server Telemetry

The server-side OpenTelemetry notes for Oracle Database 23.9 describe JDBC thin
connection properties for server telemetry:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        "[oracle.jdbc.serverTelemetryTracesEnabled]": true
        "[oracle.jdbc.serverTelemetryMetricsEnabled]": false
        "[oracle.jdbc.serverTelemetryLoggingEnabled]": false
```

The important one for this demo is `serverTelemetryTracesEnabled`. The goal is
for the same trace context that exists in Spring to be propagated through the
JDBC driver and into the database session.

The demo also sets the W3C trace context into the client-info slot used by the
database-side telemetry path, plus the tracelevel client-info slot used by the
reference testcase:

```java
oracleConnection.setClientInfo(
    "clientcontext.ora$opentelem$tracectx",
    "traceparent: 00-" + traceId + "-" + spanId + "-01\r\n");
oracleConnection.setClientInfo(
    "clientcontext.ora$opentelem$tracelevel",
    "tracelevel: 0x01");
```

It also sets normal Oracle Database session metadata before running the SQL:

```sql
begin
  dbms_application_info.set_module('springboot-oracle-db-otel-demo', 'context-query');
  dbms_session.set_identifier('traceId=<trace-id>');
end;
/
```

There is one practical caveat: the public app can prove the Spring, Micrometer,
and JDBC span path first, while the final database-internals spans depend on a
database and driver combination that supports the server-side propagation path
and a database-side exporter that can reach the collector.

## Enabling Database Export

The database also needs an OTLP endpoint. The attached Oracle notes show
`DBMS_OBSERVABILITY` as the package for enabling the service and registering
endpoints:

```sql
grant execute on sys.dbms_observability to FINANCIAL;
```

```sql
begin
  dbms_observability.enable_service;
end;
/

begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_traces,
    endpoint => 'https://oracledev.ai/financial/otel/v1/traces',
    credential_name => null
  );
end;
/

begin
  dbms_observability.enable_endpoint('https://oracledev.ai/financial/otel/v1/traces');
  dbms_observability.enable_service(dbms_observability.all_services);
end;
/
```

For a session-scoped demo:

```sql
alter session set sql_trace = true;
```

Be careful with `ALTER SYSTEM SET SQL_TRACE = true`. Public Oracle docs warn
that instance-wide SQL tracing can have a severe performance impact. Keep this
demo scoped to a session or test database unless you have measured the load.

## Local Backend

Jaeger all-in-one is enough to prove the app-side shape:

```bash
docker compose up -d
```

The compose file in this folder exposes:

- `4317` for OTLP gRPC
- `4318` for OTLP HTTP
- `16686` for the Jaeger UI

If the database is remote, it cannot export to your laptop's `localhost`. Use a
collector endpoint the database can reach, usually HTTPS with the required ACL
and credential setup.

For the financial OKE demo cluster, I deploy Jaeger behind the existing nginx
ingress and use:

```text
https://oracledev.ai/financial/otel/v1/traces
```

The ingress keeps the public path under `/financial` for Cloudflare routing and
rewrites it to Jaeger's internal `/v1/traces` OTLP endpoint.

For a local laptop demo with Podman:

```bash
podman run -d --name oracle-db-otel-jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  cr.jaegertracing.io/jaegertracing/jaeger:2.19.0
```

## Running The Demo

The short path is below. The full setup, build, DB-side configuration, and
troubleshooting steps are in `../RUNBOOK.md`.

```bash
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'
export DB_USERNAME='financial'
export DB_PASSWORD='<database-password>'
export OTLP_TRACES_ENDPOINT='http://localhost:4318/v1/traces'
export OTEL_SEMCONV_STABILITY_OPT_IN='database/dup'

cd springboot-oracle-db-otel-demo
mvn spring-boot:run
```

Trigger a roundtrip:

```bash
curl http://localhost:8080/trace/roundtrip
```

In Jaeger, search for `springboot-oracle-db-otel-demo`. The app/JDBC trace should show:

- `http get /trace/roundtrip`
- `oracle.demo.database-roundtrip`
- `DB Server` from the `oracle-db` service when the database exporter is enabled
- `Get the session key`
- `Generic authentication call`
- `Ping`
- `Execute query`

The application also sets Oracle Database session context with
`DBMS_APPLICATION_INFO.SET_MODULE`, `SET_ACTION`, and
`DBMS_SESSION.SET_IDENTIFIER`, and propagates both
`clientcontext.ora$opentelem$tracectx` and
`clientcontext.ora$opentelem$tracelevel`. That makes the database-exported span
much easier to identify in Jaeger because it can carry fields such as
`oracle.db.module`, `oracle.db.action`, `oracle.db.session.id`, and sometimes
`oracle.db.query.sql.id`.

For the local Podman run I verified the Spring Boot and JDBC spans and confirmed
that Oracle Database Free received the same trace ids through the JDBC/server
telemetry path. The public database image I tested still archived the database
operations to disk rather than exporting an `oracle-db` span to Jaeger, so I am
keeping the blog language precise: the application shape is ready, while the
database-internal spans depend on a database image/exporter path that actually
pushes the server-side spans to OTLP.

On OCI Linux x86_64, the full flow is verified. With Oracle Database Free,
Jaeger, and the Spring Boot app on one VM, plus a local HTTPS/HTTP2 OTLP proxy,
Jaeger shows a single trace containing both `springboot-oracle-db-otel-demo` and
`oracle-db`. The `oracle-db / DB Server` span is exported by the database and
shows database-only metadata including PDB, service, session id, module, action,
connection id, and SQL id on the richer workload query. See
`docs/OCI_VM_BLOG.md` for the reproducible setup.

## Agentic AI: SQL ID As The Bridge

For an agentic AI use case, I use a second endpoint:

```bash
curl 'http://localhost:8080/trace/agent-task?agentId=claims-investigator-agent&task=investigate_payment_anomalies'
```

The same endpoint also has a browser-friendly view:

```text
http://localhost:8080/trace/agent-task/view?agentId=claims-investigator-agent&task=investigate_payment_anomalies
```

The browser view keeps the trace and database diagnostics on one page. It shows
the full SQL text from `V$SQL.SQL_FULLTEXT`, application bind values, captured
bind samples from `V$SQL_BIND_CAPTURE`, SQL Monitor, `DBMS_XPLAN` for the SQL
execution plan, and an embedded Jaeger trace at the bottom. The embedded trace
loads after a short delay so the completed Spring Boot, JDBC, and database spans
have time to flush. Oracle captures bind samples rather than every bind value
from every execution, so the application bind values and database-captured
samples are shown separately.

This endpoint creates an `oracle.demo.agent-database-investigation` span, starts
a named Oracle Database operation for the agent task with
`DBMS_SQL_MONITOR.BEGIN_OPERATION`, and runs a synthetic investigation query
with:

```sql
select /*+ monitor gather_plan_statistics agentic_ai_trace_bridge */ ...
```

The response includes the same trace id you search for in Jaeger, the agent id,
the agent task, the database `SQL_ID`, a SQL Monitor preview, and
`DBMS_XPLAN.DISPLAY_CURSOR` output using `ALLSTATS LAST +IOSTATS +MEMSTATS`.

That is the key pattern: Jaeger remains the visual experience, and
`oracle.db.query.sql.id` on the `oracle-db / DB Server` span becomes the bridge
from the distributed trace to database internals. The trace answers "which agent
did this, in which request, and which database server span did it create?" The
SQL Monitor and DBMS_XPLAN output answer "what did the database actually do for
that agent task?"

In some database exporter builds, the Jaeger tag may display the canonical SQL
ID or the same leading SQL ID prefix. The endpoint response is the canonical SQL
diagnostic record; the Jaeger attribute is the visual pointer from the trace to
that database work.

For the verified OCI VM run, the agent endpoint returned SQL ID
`ff7xbtj1ku8ja`, SQL text with bind placeholders, application bind values,
database-captured bind samples, SQL Monitor availability, and SQL stats
including elapsed time, CPU time, buffer gets, and disk reads. The matching
Jaeger trace had both `springboot-oracle-db-otel-demo` and `oracle-db` services, including
a database-exported `DB Server` span with `oracle.db.action=agent-workload-query`.

The SQL Monitor bridge requires extra diagnostic privileges in the demo
database:

```sql
grant execute on sys.dbms_sql_monitor to FINANCIAL;
grant execute on sys.dbms_xplan to FINANCIAL;
grant select_catalog_role to FINANCIAL;
```

Use those broad grants only in a contained demo environment. In a shared
environment, replace them with a reviewed diagnostic role and confirm your
Diagnostics Pack and Tuning Pack licensing posture before using SQL Monitor.

## Troubleshooting

If you see only Spring spans, confirm the JDBC connection property is applied to
the actual pool.

If you see Spring and JDBC spans but no database spans, check that the database
can reach the OTLP endpoint and that `DBMS_OBSERVABILITY` has an enabled traces
endpoint.

If trace volume is too high, lower `management.tracing.sampling.probability`.

If SQL text is missing from JDBC spans, that is expected. The Oracle provider
keeps sensitive data disabled by default.

## Sources

- Spring Boot 3.3 tracing: https://docs.spring.io/spring-boot/3.3/reference/actuator/tracing.html
- Oracle JDBC Observability provider: https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-observability
- Oracle `DBMS_OBSERVABILITY`: https://docs.oracle.com/en/database/oracle/oracle-database/26/arpls/dbms_observability.html
- Oracle Real-Time SQL Monitoring: https://docs.oracle.com/en/database/oracle/oracle-database/26/tgsql/monitoring-database-operations.html
- Oracle `DBMS_SQL_MONITOR`: https://docs.oracle.com/en/database/oracle/oracle-database/26/arpls/DBMS_SQL_MONITOR.html
- Oracle `DBMS_XPLAN`: https://docs.oracle.com/en/database/oracle/oracle-database/26/arpls/DBMS_XPLAN.html
- OpenTelemetry Collector quick start: https://opentelemetry.io/docs/collector/quick-start/
- Jaeger getting started: https://www.jaegertracing.io/docs/latest/getting-started/
- Oracle `SQL_TRACE`: https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html
