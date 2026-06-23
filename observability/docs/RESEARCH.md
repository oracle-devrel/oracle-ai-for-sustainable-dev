# Research Notes

## Application instrumentation

Spring Boot 3.3 Actuator auto-configures Micrometer Tracing. For OpenTelemetry
with OTLP, Spring documents the dependency set as:

- `spring-boot-starter-actuator`
- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`

It also documents `management.tracing.sampling.probability`; this demo sets it
to `1.0` so each request is visible while developing.

Source: https://docs.spring.io/spring-boot/3.3/reference/actuator/tracing.html

## Oracle JDBC driver instrumentation

Oracle's `ojdbc-provider-opentelemetry` implements the Oracle JDBC
`TraceEventListener` interface. The README says it publishes driver events into
OpenTelemetry, including roundtrips to the database server, Application
Continuity begin/success, and VIP-down events.

The provider is enabled with this connection property:

```text
oracle.jdbc.provider.traceEventListener=open-telemetry-trace-event-listener-provider
```

The provider is enabled by default once configured. Sensitive attributes such as
SQL text and connection URL are disabled by default.

Source: https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-opentelemetry

## Database server-side OpenTelemetry

The attached "Enable server side Open Telemetry" notes describe Oracle Database
23.9 server-side OpenTelemetry support. The notes say that `SQL_TRACE` is the
session or system switch and that the JDBC thin driver can request server-side
telemetry using connection properties such as:

```text
oracle.jdbc.serverTelemetryTracesEnabled
oracle.jdbc.serverTelemetryMetricsEnabled
oracle.jdbc.serverTelemetryLoggingEnabled
```

The attached "RDBMS MAIN End-to-End Config for OpenTelemetry (WIP)" notes
describe `DBMS_OBSERVABILITY` setup:

- `dbms_observability.enable_service`
- `dbms_observability.add_endpoint`
- optional credentials with `dbms_observability.create_credential`
- network ACLs with `DBMS_NETWORK_ACL_ADMIN`

Public Oracle docs for `SQL_TRACE` say it is modifiable with `ALTER SESSION`
and `ALTER SYSTEM`, but also warn that instance-wide SQL tracing can have severe
performance impact and that `SQL_TRACE` is deprecated for traditional SQL trace
use cases.

Source: https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html

## Collector and trace backend

OpenTelemetry Collector receives telemetry, processes it, and forwards it to
backends. The quick start exposes OTLP gRPC on `4317` and OTLP HTTP on `4318`.

Source: https://opentelemetry.io/docs/collector/quick-start/

Jaeger all-in-one can receive OTLP on ports `4317` and `4318` and exposes the UI
on `16686`.

Source: https://www.jaegertracing.io/docs/latest/getting-started/
