# Oracle Database OpenTelemetry Demo Runbook

This runbook starts with the path that works with the public Spring Boot demo,
then shows the extra database-side requirements for a continuous trace that
extends into Oracle Database internals.

## Should This Use Micrometer?

Yes. For a Spring Boot application, Micrometer is still the right default.
Spring Boot Actuator creates the HTTP/server spans, Micrometer gives the app a
portable observation API, and the OpenTelemetry bridge exports those spans to an
OTLP backend.

The OpenTelemetry Java agent is useful for broader auto-instrumentation or for a
standalone scenario lab, but it is not required for this Spring Boot demo. The
database-side continuation is mainly controlled by the Oracle JDBC driver,
session settings, `DBMS_OBSERVABILITY`, and whether the database can reach the
collector.

## Trace Modes

Use these modes to set expectations before testing.

| Mode | What You Should See | Requirements |
| --- | --- | --- |
| App and JDBC tracing | Spring Boot HTTP spans, the explicit Micrometer observation, and Oracle JDBC roundtrip spans | Public demo dependencies, Oracle JDBC OpenTelemetry provider, an OTLP backend |
| App to database internals | The app/JDBC spans plus Oracle Database server-side spans in the same trace | Oracle Database server-side OpenTelemetry, reachable OTLP endpoint from the database, correct ACLs, server telemetry enabled on the session, and an Oracle JDBC/database combination that propagates the trace context correctly |

The `db-observability-scenarios` material notes a current bug/path where the
public setup may only produce app-side traces, or may require a fixed/internal
OJDBC build for server-side OSCID propagation. Treat DB-side spans as a
database/driver capability check, not as proof that the Spring Boot/Micrometer
approach is wrong.

## Prerequisites

- JDK 17.
- Maven 3.9 or newer.
- Docker Compose, Podman Compose, or another OTLP-compatible tracing backend.
- An Oracle Database reachable from the Spring Boot app.
- For DB-side spans, an Oracle Database version with `DBMS_OBSERVABILITY` and
  server-side OpenTelemetry support.
- For DB-side spans, a collector endpoint that the database can reach. A remote
  database cannot export to your laptop's `localhost`.

## 1. Start A Local Trace Backend

For local app/JDBC traces, Jaeger all-in-one is enough:

```bash
cd observability
docker compose up -d
```

Open the UI after the app emits traces:

```text
http://localhost:16686
```

The compose file exposes OTLP HTTP on `4318` and OTLP gRPC on `4317`.

## 2. Configure The Spring Boot App

Set the JDBC URL and credentials. For Autonomous Database with a wallet, include
`TNS_ADMIN` in the JDBC URL:

```bash
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'
export DB_USERNAME='financial'
export DB_PASSWORD='<database-password>'
export OTLP_TRACES_ENDPOINT='http://localhost:4318/v1/traces'
```

For app/JDBC-only tracing, it is fine for the collector endpoint to be on
localhost because the Spring Boot process is exporting the spans.

## 3. Build And Run

```bash
cd observability/oracle-db-otel-demo
mvn spring-boot:run
```

If you want a packaged build instead:

```bash
cd observability/oracle-db-otel-demo
mvn package
java -jar target/oracle-db-otel-demo-0.0.1-SNAPSHOT.jar
```

## 4. Generate A Trace

In another terminal:

```bash
curl http://localhost:8080/trace/roundtrip
```

The response includes the current trace id and span id from the Spring request.

In Jaeger, select the `oracle-db-otel-demo` service and search for recent
traces. In app/JDBC mode, expect the HTTP span, the
`oracle.demo.database-roundtrip` observation, and Oracle JDBC roundtrip spans.

## 5. Optional Database Server-Side Trace Setup

Only do this in a test database or a controlled demo environment.

The database needs an OTLP traces endpoint:

```sql
@observability/sql/enable_server_observability.sql
```

The database user that runs the JDBC session must also be allowed to reach the
collector host:

```sql
@observability/sql/create_observability_acl.sql
```

Before running the scripts, edit the endpoint, host, port, and principal values.
The ACL principal must match the database user used by the Spring Boot app.

For a remote database, use a collector endpoint reachable from the database,
for example:

```text
https://collector.example.com/v1/traces
```

Do not use `http://localhost:4318/v1/traces` for a remote database; that points
at the database host, not your laptop.

## 6. Verify Database Observability

Run:

```sql
@observability/sql/show_server_observability.sql
```

Check that the service is enabled and that an `otel_traces` endpoint is
registered.

If the database can export server-side spans and the driver propagates the
context correctly, the database spans should appear in the same trace as the
Spring Boot request.

## Known Current Limitations

- The public Spring Boot demo is expected to show app and JDBC spans first.
- DB-side spans depend on the Oracle Database server-side OpenTelemetry feature,
  network ACLs, a reachable collector, and trace context propagation from the
  JDBC driver into the database session.
- The separate `db-observability-scenarios` material indicates that some current
  public driver/database combinations do not yet complete the DB-side span path,
  and that a fixed or internal OJDBC build may be required for full OSCID
  propagation.
- If using an affected JDBC provider build, a null-safe
  `TraceEventListenerProvider` wrapper may be needed. The current Spring Boot
  demo does not include that wrapper yet.
- The current SQL scripts are intentionally minimal. For a more complete setup,
  add CDB/PDB-aware enablement, `otel_logs` if log export is needed, and ACL
  privileges for `connect`, `resolve`, `http`, and `http_proxy`.

## Troubleshooting

If no traces appear, confirm that `OTLP_TRACES_ENDPOINT` points to a running
collector and that `management.tracing.sampling.probability` is `1.0`.

If only HTTP spans appear, confirm that the Oracle JDBC OpenTelemetry provider
dependency is present and that Hikari receives this property:

```yaml
oracle.jdbc.provider.traceEventListener=open-telemetry-trace-event-listener-provider
```

If app and JDBC spans appear but no database spans appear, check:

- The database can reach the OTLP endpoint.
- The ACL principal is the same user used by the JDBC connection.
- `DBMS_OBSERVABILITY` is enabled in the right container or PDB.
- A traces endpoint is registered.
- The JDBC/database version supports the required server-side propagation path.

If Maven cannot download dependencies, retry on a working network or use an
existing local Maven cache. A dependency download failure is separate from the
demo code path.
