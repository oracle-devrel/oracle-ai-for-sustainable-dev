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

Oracle's `ojdbc-provider-observability` implements the Oracle JDBC
`TraceEventListener` interface. The README says it publishes driver events into
OpenTelemetry, including roundtrips to the database server, Application
Continuity begin/success, and VIP-down events. The published Maven coordinate
is:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-observability</artifactId>
  <version>1.1.0</version>
</dependency>
```

The provider is enabled with this connection property:

```text
oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider
```

The older `open-telemetry-trace-event-listener-provider` value is still
documented as a backward-compatible OTEL-only path, but the demo uses the new
observability provider. Sensitive attributes such as SQL text and connection URL
are disabled by default.

Source: https://github.com/oracle/ojdbc-extensions/tree/main/ojdbc-provider-observability

## Database server-side OpenTelemetry

Oracle public docs say `DBMS_OBSERVABILITY` manages ADBS Native Observability
and emits database OpenTelemetry signals to a collector. The attached "Enable
server side Open Telemetry" notes describe Oracle Database server-side
OpenTelemetry support. The notes say that `SQL_TRACE` is the session or system
switch and that the JDBC thin driver can request server-side telemetry using
connection properties such as:

```text
oracle.jdbc.serverTelemetryTracesEnabled
oracle.jdbc.serverTelemetryMetricsEnabled
oracle.jdbc.serverTelemetryLoggingEnabled
```

The attached `DBMS_OBSERVABILITY` Podman testcase describes this setup shape:

- `dbms_observability.enable_service`
- `dbms_observability.add_endpoint`
- `dbms_observability.enable_endpoint`
- optional credentials with `dbms_observability.create_credential`
- network ACLs with `DBMS_NETWORK_ACL_ADMIN`
- HTTPS for the database push path

Public Oracle docs for `SQL_TRACE` say it is modifiable with `ALTER SESSION`
and `ALTER SYSTEM`, but also warn that instance-wide SQL tracing can have severe
performance impact and that `SQL_TRACE` is deprecated for traditional SQL trace
use cases.

Sources:

- https://docs.oracle.com/en/database/oracle/oracle-database/26/arpls/dbms_observability.html
- https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/SQL_TRACE.html

## Local verification notes

Local Jaeger with the Spring Boot app and Oracle Database Free in Podman
produced a trace containing the Spring HTTP server span, the explicit
`oracle.demo.database-roundtrip` observation, and Oracle JDBC spans including
ping and `Execute query`.

The local database container used for these tests reported Oracle AI Database
Free `23.26.1.0.0`. The private Podman testcase notes describe a successful
database-server-span run on an Oracle Database Free `23.26.2.0.0` build, so the
local container may be behind the fixed database-side behavior.

Verified local trace ids:

- App/JDBC baseline: `ff7863c73fa0fde848ec7952e806255a`
- Public JDBC jar with server logging enabled:
  `fbe347663500f57a596c5fa566b52c46`
- Patched JDBC jar test with server logging enabled:
  `c7cc76bc04ce577643ca4f38ced90048`

The local database setup also verified these points:

- `DBMS_OBSERVABILITY` is installed and valid in `FREEPDB1`.
- The `FINANCIAL` user can reach the local Jaeger OTLP HTTP endpoint from SQL.
- The public `ojdbc11-23.26.2.0.0` jar contains the OSCID handoff class.
- The database DT trace files received the Spring trace ids and logged `KSTRC`
  operations for them.
- The app explicitly unsets and then sets `OracleConnection.setServerTelemetry`
  for each request. This forces a state change so the telemetry request is
  piggybacked to the database even when the requested flags already appear to
  be enabled.
- Enabling `oracle.jdbc.serverTelemetryLoggingEnabled` caused the DB trace
  files to include trace bucket dump entries for the matching trace ids.

What did not work on the tested public Oracle Database Free image:

- Jaeger services were only `jaeger` and `springboot-oracle-db-otel-demo`.
- The trace contained five app/JDBC spans and no `oracle-db` / `DB Server` span.
- DT trace files logged the operations as archived to `ksu_ops_FREE.trc`, even
  after `DBMS_OBSERVABILITY` showed the OTLP traces endpoint enabled.
- Replacing the Maven Central JDBC jar with the local patched jar did not change
  Jaeger's visible result in this local setup: the database still received the
  trace id and dumped buckets, but no database-internal span appeared in Jaeger.
  The tested patched jar manifest reported `Implementation-Version:
  26.1.0.24.0` and `Repository-Id: JAVAVM_MAIN_LINUX.X64_260419`.

The refreshed OCR `database/free:latest` image was pulled on July 2, 2026
(`Created` 2026-05-27), but starting a second DB container from it overloaded
Podman Desktop during first boot and was cleaned up before the end-to-end test
could complete. Re-test with that newer image or a fixed database build before
claiming database-internal spans are generally available in the public local
container path.

## Latest local DB retest

After restarting Podman Desktop cleanly and increasing the Podman VM memory to
8 GiB, a clean `container-registry.oracle.com/database/free:latest` container
started successfully and reported Oracle AI Database Free `23.26.2.0.0`.

Retests against that database:

- Public Maven Central JDBC jar, server traces/logging enabled:
  `2bde5b3a18871e4feee645b873eefe14`
- Local patched JDBC jar, server traces/logging enabled:
  `611afd6c0bbf5a280e6a9c812ab201cc`
- Local patched JDBC jar with HTTPS proxy + trusted local CA:
  `283ad860aae247ee507c8c3efdefb89c`
- Local patched JDBC jar with disk + HTTPS endpoints enabled:
  `a6f7be6bd6eed4898db58b5e82f263e5`
- Local patched JDBC jar with diagnostic in-memory `_kstrc_archiver` override:
  `0aa56c5e2cc20829058a1325b943d9d5`

The app/JDBC side worked in each case: Jaeger showed the Spring HTTP span, the
explicit Micrometer observation, and Oracle JDBC `Ping` / `Execute query`
client spans under service `springboot-oracle-db-otel-demo`.

The database also received the propagated trace ids in each case and wrote
`KSTRC: Operation` plus trace bucket dump entries to the DB diagnostic trace
files. However, Jaeger still did not show a DB server service/span, and an
HTTPS reverse proxy in front of Jaeger saw no database archive-worker POSTs
except the manual `curl` probe from the DB container. Even with the diagnostic
`_kstrc_archiver` value set in memory, operations were still archived to
`ksu_ops_FREE.trc`.

## Original Podman testcase retest

The original `testcase_podman_otel` reference testcase was also run locally on
July 2, 2026 with:

- `JdbcServerTelemetryTest.java`
- Java 25 client image: `docker.io/library/eclipse-temurin:25-jdk`
- local testcase JDBC jar: `ojdbc11.jar`
- OpenTelemetry Java agent `2.29.0`
- testcase-managed OpenTelemetry Collector listener
- testcase wallet trust flow with `wallet_root=/opt/oracle/wallets`
- Oracle Database Free image `container-registry.oracle.com/database/free:23.26.2.0`

The direct Java 25 `sql_trace` and `piggyback` runs succeeded and showed
`DBMS_OBSERVABILITY` runtime traces/logging toggling on during the request. The
database trace files contained the testcase trace id
`4bf92f3577b34da6a3ce929d0e0e4736` and trace bucket dumps.

The full testcase, using its own collector and wallet setup, still ended with:

```text
TESTCASE_RESULT=FAILED
reason=missing_listener_span db_span=NO jdbc_span=YES
```

It did prove JDBC client spans and scraped database metrics:

```text
testcase-otel-listener | otlp     | jdbc | push   | traces YES (10)
testcase-otel-listener | otlp     | db   | push   | traces NO
testcase-otel-scraper  | oracledb | db   | scrape | metrics YES (70)
```

The most important difference from the successful reference notes is the
database build. On this Apple Silicon laptop, Podman pulled the ARM64 database
build:

```text
RDBMS_23.26.2.0.0DBRU_LINUX.ARM64_260417
```

The feature should not conceptually depend on CPU architecture, but this result
makes the exact DB build and platform a real variable for server-side span
export.

## OCI Linux x86_64 verification

On July 3, 2026, the full app-to-database-internals path was verified on an OCI
Oracle Linux x86_64 VM with Oracle Database Free `23.26.2.0.0`, Jaeger, and the
Spring Boot demo on the same host.

The working differences from the failed local ARM tests were:

- Jaeger still received plain OTLP HTTP, but the database exported through a
  local HTTPS/HTTP2 proxy named `otel-tls-proxy`.
- `_kstrc_archiver` was set to
  `otel_traces://https://otel-tls-proxy:4318/v1/traces`.
- `_kstrc_service_mask` was set to `8`.
- The database trust wallet was created under both
  `WALLET_ROOT/<PDB_GUID>/tls` and `WALLET_ROOT/<PDB_GUID>/disttrc`; `disttrc`
  was the key path used by the KSTRC exporter.
- `dbms_observability.show_extra_metadata` was enabled.
- The Spring app set database `MODULE`, `ACTION`, and `CLIENT_IDENTIFIER`, and
  propagated both `clientcontext.ora$opentelem$tracectx` and
  `clientcontext.ora$opentelem$tracelevel`.

Verified trace:

```text
trace id: 3b38cf5d48a818187d6c6dcb0bc30963
services: oracle-db, springboot-oracle-db-otel-demo
span count: 19
```

The database-exported `oracle-db / DB Server` spans included database-only tags
such as `oracle.db.pdb`, `oracle.db.service`, `oracle.db.session.id`,
`oracle.db.module`, `oracle.db.action`, and `oracle.db.query.sql.id` on the
dictionary workload query.

## Collector and trace backend

OpenTelemetry Collector receives telemetry, processes it, and forwards it to
backends. The quick start exposes OTLP gRPC on `4317` and OTLP HTTP on `4318`.

Source: https://opentelemetry.io/docs/collector/quick-start/

Jaeger all-in-one can receive OTLP on ports `4317` and `4318` and exposes the UI
on `16686`.

Source: https://www.jaegertracing.io/docs/latest/getting-started/
