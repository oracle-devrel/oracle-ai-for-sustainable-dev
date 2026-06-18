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
driver-level spans through the `ojdbc-provider-opentelemetry` provider. The
driver can also enable server-side telemetry on the database session, allowing
the database to export trace data to the same collector.

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

Oracle's OpenTelemetry provider for JDBC is the piece that makes the database
client visible. Add the provider:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-opentelemetry</artifactId>
  <version>1.0.6</version>
</dependency>
```

Then set the connection property:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        "[oracle.jdbc.provider.traceEventListener]": open-telemetry-trace-event-listener-provider
```

The provider emits events such as database roundtrips. Sensitive details such as
SQL text are disabled by default, which is the right default for a blog demo.

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

There is one practical caveat: the public app can prove the Spring, Micrometer,
and JDBC span path first, while the final database-internals spans depend on a
database and OJDBC combination that supports the server-side propagation path.
Some current scenario material notes that a fixed or internal OJDBC build may
be required for full OSCID propagation into Oracle Database server spans.

## Enabling Database Export

The database also needs an OTLP endpoint. The attached Oracle notes show
`DBMS_OBSERVABILITY` as the package for enabling the service and registering
endpoints:

```sql
begin
  dbms_observability.enable_service;
end;
/

begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_traces,
    endpoint => 'https://collector.example.com/v1/traces',
    credential_name => null
  );
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

## Running The Demo

The short path is below. The full setup, build, DB-side configuration, and
troubleshooting steps are in `../RUNBOOK.md`.

```bash
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'
export DB_USERNAME='financial'
export DB_PASSWORD='<database-password>'
export OTLP_TRACES_ENDPOINT='http://localhost:4318/v1/traces'

cd oracle-db-otel-demo
mvn spring-boot:run
```

Trigger a roundtrip:

```bash
curl http://localhost:8080/trace/roundtrip
```

In Jaeger, search for `oracle-db-otel-demo`. The first version of the trace
should show HTTP and app spans plus Oracle JDBC roundtrip spans. Once the
database endpoint and server-side telemetry are enabled, database-side spans
should join the same trace.

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
- Oracle JDBC OpenTelemetry provider: https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-opentelemetry
- OpenTelemetry Collector quick start: https://opentelemetry.io/docs/collector/quick-start/
- Jaeger getting started: https://www.jaegertracing.io/docs/latest/getting-started/
- Oracle `SQL_TRACE`: https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html
