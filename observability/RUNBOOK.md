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
| App and JDBC tracing | Spring Boot HTTP spans, the explicit Micrometer observation, and Oracle JDBC roundtrip spans | Public demo dependencies, Oracle JDBC Observability provider, an OTLP backend |
| App to database internals | The app/JDBC spans plus Oracle Database server-side spans in the same trace | Oracle Database server-side OpenTelemetry, reachable OTLP endpoint from the database, correct ACLs, server telemetry enabled on the session, and an Oracle JDBC/database combination that propagates the trace context correctly |

The `db-observability-scenarios` material notes that some older
driver/database combinations may only produce app-side traces, or may require a
fixed driver build for full server-side propagation. Treat DB-side spans as a
database/driver/exporter capability check, not as proof that the Spring
Boot/Micrometer approach is wrong.

## Prerequisites

- JDK 25.
- Maven 3.9 or newer.
- Podman or another OTLP-compatible tracing backend.
- An Oracle Database reachable from the Spring Boot app.
- For DB-side spans, an Oracle Database version with `DBMS_OBSERVABILITY` and
  server-side OpenTelemetry support.
- For DB-side spans, a collector endpoint that the database can reach. A remote
  database cannot export to your laptop's `localhost`.

If your host JDK is older than 25, build from a Java 25 Maven container:

```bash
podman run --rm \
  -v "$PWD/observability/springboot-oracle-db-otel-demo:/workspace:z" \
  -w /workspace \
  docker.io/library/maven:3.9.11-eclipse-temurin-25 \
  mvn -DskipTests package
```

## 1. Start A Local Trace Backend

For local app/JDBC traces, Jaeger all-in-one is enough:

```bash
cd observability
./start-jaeger.sh
```

Open the UI after the app emits traces:

```text
http://localhost:16686
```

The helper script exposes OTLP HTTP on `4318`, OTLP gRPC on `4317`, and the
Jaeger UI on `16686`.

## 2. Configure The Spring Boot App

Copy the local template and set the JDBC URL and credentials. For Autonomous
Database with a wallet, include `TNS_ADMIN` in the JDBC URL:

```bash
cd observability
cp .env_example .env
vi .env
```

For app/JDBC-only tracing, it is fine for the collector endpoint to be on
localhost because the Spring Boot process is exporting the spans.

## 3. Build And Run

```bash
cd observability
./build.sh
./run-app.sh
```

If you want a packaged build instead:

```bash
cd observability/springboot-oracle-db-otel-demo
mvn package
java -jar target/springboot-oracle-db-otel-demo-0.0.1-SNAPSHOT.jar
```

## 4. Generate A Trace

In another terminal:

```bash
cd observability
./smoke-test.sh
```

The smoke test calls health, `/trace/roundtrip`, and `/trace/agent-task`.
The trace responses include the current trace id and span id from the Spring
request.

In Jaeger, select the `springboot-oracle-db-otel-demo` service and search for recent
traces. In app/JDBC mode, expect the HTTP span, the
`oracle.demo.database-roundtrip` observation, and Oracle JDBC roundtrip spans.

You can also verify with the Jaeger API:

```bash
TRACE_ID='<trace-id-from-the-response>'
curl -s "http://localhost:16686/api/traces/${TRACE_ID}"
```

A healthy app/JDBC trace includes operations such as:

- `http get /trace/roundtrip`
- `oracle.demo.database-roundtrip`
- `DB Server` from service `oracle-db` when database server-side export is working
- `Get the session key`
- `Generic authentication call`
- `Ping`
- `Execute query`

The `/trace/roundtrip` response also reports the database session context that
the app set before running the workload. Look for `MODULE_NAME`,
`ACTION_NAME`, `CLIENT_IDENTIFIER`, `INSTANCE_NAME`, and `SERVER_HOST` in the
JSON response. In Jaeger, the database-exported span should show tags such as
`oracle.db.module`, `oracle.db.action`, `oracle.db.session.id`,
`oracle.db.pdb`, and, for some SQL statements, `oracle.db.query.sql.id`.

For the more polished agentic AI demo, use:

```bash
curl 'http://localhost:8080/trace/agent-task?agentId=claims-investigator-agent&task=investigate_payment_anomalies'
```

For a browser-friendly page instead of raw JSON, open:

```text
http://localhost:8080/trace/agent-task/view?agentId=claims-investigator-agent&task=investigate_payment_anomalies
```

The view includes full SQL text from `V$SQL.SQL_FULLTEXT`, application bind
values, captured bind samples from `V$SQL_BIND_CAPTURE`, SQL Monitor output,
`DBMS_XPLAN` for the SQL execution plan, and the Jaeger trace embedded at the
bottom. The embedded trace loads after a short delay so completed Spring Boot,
JDBC, and database spans have time to flush. Oracle captures bind samples rather
than every bind value for every execution, so the app bind values and
`V$SQL_BIND_CAPTURE` samples are shown separately.

The agent endpoint uses Oracle JDBC `setEndToEndMetrics(...)` to set Oracle
`MODULE` to `agent:<agent-id>`, `ACTION` to the current phase, and
`CLIENT_IDENTIFIER` to the trace id. The app still appears as
`springboot-oracle-db-otel-demo` in Jaeger, but the database-exported span
carries the agent identity in `oracle.db.module`.

This creates an `oracle.demo.agent-database-investigation` span and a named
database operation for the agent task. The response includes the agent id, task,
trace id, workload summary, canonical `SQL_ID`, SQL Monitor report preview, and
`DBMS_XPLAN.DISPLAY_CURSOR` lines. In Jaeger, expand the
`oracle-db / DB Server` span with `oracle.db.action=agent-workload-query`. Its
`oracle.db.query.sql.id` attribute is the bridge from the visual trace to the
database-internal SQL Monitor and DBMS_XPLAN diagnostics.

Depending on the database exporter build, the Jaeger tag may display the full
SQL ID or the same leading SQL ID prefix. Treat the endpoint response as the
canonical SQL diagnostic record and the Jaeger tag as the visual pointer from
the trace to that database work.

The SQL Monitor bridge requires demo diagnostic privileges:

```sql
grant execute on sys.dbms_sql_monitor to <app_user>;
grant execute on sys.dbms_xplan to <app_user>;
grant select_catalog_role to <app_user>;
```

Use those broad grants only in a contained demo environment. For a shared
environment, replace them with a reviewed diagnostic role and confirm the
Diagnostics Pack / Tuning Pack licensing posture before using SQL Monitor.

`DBMS_SQL_MONITOR.BEGIN_OPERATION` is used only to give the agent task a named
database operation in SQL Monitor. The trace, JDBC spans, database `DB Server`
spans, and SQL-ID bridge still work without that call, because the workload SQL
itself is monitored and the app can query SQL diagnostics by SQL ID. Removing
the call would reduce privileges and one database round trip, but it would also
remove the explicit database-operation id and attributes for the agent task.

## 4A. Fully Local Podman Path

This path runs Jaeger, Oracle Database Free, and the Spring Boot app locally.
It is useful for proving the app/JDBC trace path without depending on the OKE
cluster or Autonomous Database.

Create a shared Podman network:

```bash
podman network create oracle-db-otel
```

Start Jaeger on that network. If you want this script-free and attached to the
Podman network, use:

```bash
podman run -d --name oracle-db-otel-jaeger \
  --network oracle-db-otel \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  cr.jaegertracing.io/jaegertracing/jaeger:2.19.0
```

Start Oracle Database Free on the same network:

```bash
podman run -d --name oracledb-free \
  --network oracle-db-otel \
  -p 1521:1521 \
  -p 5500:5500 \
  -v oracle-data:/opt/oracle/oradata \
  -e ORACLE_PASSWORD='<sys-password>' \
  container-registry.oracle.com/database/free:latest
```

Wait until the container is healthy:

```bash
podman ps --filter name=oracledb-free
```

Copy and run the local database setup helper:

```bash
podman cp observability/sql/setup_local_free_observability.sql \
  oracledb-free:/tmp/setup_local_free_observability.sql

podman exec -it oracledb-free \
  sqlplus / as sysdba @/tmp/setup_local_free_observability.sql
```

Use these prompt values for the all-local setup:

```text
PDB name: FREEPDB1
Application DB user: FINANCIAL
Collector host: oracle-db-otel-jaeger
Collector OTLP HTTP port: 4318
```

Copy and run the local Deep Sec direct-login setup. Supply a private password
for the local end user when prompted:

```bash
podman cp observability/sql/setup_local_deep_data_security.sql \
  oracledb-free:/tmp/setup_local_deep_data_security.sql

podman exec -it oracledb-free \
  sqlplus / as sysdba @/tmp/setup_local_deep_data_security.sql
```

Use `FREEPDB1`, `FINANCIAL`, `claims-investigator-agent`, and the collector host
used above. This creates a local Deep Sec `END USER`; it does not create another
schema and does not use Entra ID, OCI IAM, or a JDBC security-context callback.

Build the app:

```bash
cd observability/springboot-oracle-db-otel-demo
mvn -DskipTests package
```

Run the app as a container on the same network:

```bash
podman run -d --name springboot-oracle-db-otel-demo-app \
  --network oracle-db-otel \
  -p 8080:8080 \
  -v "$PWD/target/springboot-oracle-db-otel-demo-0.0.1-SNAPSHOT.jar:/app/app.jar:ro" \
  -e DB_URL='jdbc:oracle:thin:@//oracledb-free:1521/FREEPDB1' \
  -e DB_USERNAME='"claims-investigator-agent"' \
  -e DB_PASSWORD='<local-deep-sec-end-user-password>' \
  -e OTLP_TRACES_ENDPOINT='http://oracle-db-otel-jaeger:4318/v1/traces' \
  -e OTEL_SEMCONV_STABILITY_OPT_IN='database/dup' \
  -e ORACLE_JDBC_SERVER_TELEMETRY_TRACES_ENABLED='true' \
  -e ORACLE_JDBC_TRACELEVEL_CLIENT_INFO_ENABLED='true' \
  docker.io/library/eclipse-temurin:25-jre \
  java -jar /app/app.jar
```

Trigger a trace:

```bash
curl http://localhost:8080/trace/roundtrip
```

Then open `http://localhost:16686` in a browser.

Verified local result on July 2, 2026:

- Spring Boot and Oracle JDBC spans reached Jaeger in one trace.
- The database received the same trace ids; DT trace files showed `KSTRC`
  operations for the app trace ids.
- The tested public Oracle Database Free build still archived those operations
  to disk and did not emit an `oracle-db` / `DB Server` service span to Jaeger.

So the current local public-image result proves propagation into the database,
but not database-internal span export. When a fixed Oracle Free image or
database-side exporter path is available, the same local setup should show an
additional `oracle-db` service in Jaeger.

## 4B. Verified OCI Linux VM Path

The full server-side export path is verified on an OCI Linux x86_64 VM with
Oracle Database Free, Jaeger, and the Spring Boot app all on the same instance.
The required differences from the simple local path are:

- expose an HTTPS OTLP endpoint to the database, even if Jaeger itself receives
  plain OTLP HTTP
- enable HTTP/2 on that HTTPS front door
- create the trusted Oracle wallet under `WALLET_ROOT/<PDB_GUID>/disttrc`
- enable `dbms_observability.show_extra_metadata` where supported
- have the app set `MODULE`, `ACTION`, `CLIENT_IDENTIFIER`, `tracectx`, and
  `tracelevel`

On a fresh Oracle Database Free 23.26.2.0 VM verification, database server-side
spans exported successfully while `_kstrc_service_mask` remained `0` and
`_kstrc_archiver` remained `disk://localhost`. Earlier troubleshooting used
those hidden KSTRC parameters to diagnose a local export failure, but they are
not part of the normal verified setup.

The verified Jaeger trace contains both services:

```text
springboot-oracle-db-otel-demo
oracle-db
```

The `oracle-db / DB Server` span is the database-exported span. For the complete
reproducible setup, use `docs/OCI_VM_BLOG.md`.

## 5. Optional Database Server-Side Trace Setup

Only do this in a test database or a controlled demo environment.

For the financial OKE cluster, deploy Jaeger first:

```bash
kubectl apply -f observability/k8s/oke-jaeger.yaml
kubectl -n financial rollout status deployment/jaeger
```

The OKE-hosted OTLP endpoint is:

```text
https://oracledev.ai/financial/otel/v1/traces
```

Use that same URL for the Spring Boot app and for `DBMS_OBSERVABILITY`.

Grant the app user access to the observability package:

```sql
@observability/sql/grant_observability_privileges.sql
```

The database user that runs the JDBC session must also be allowed to reach the
collector host:

```sql
@observability/sql/create_observability_acl.sql
```

Then register and enable the OKE-hosted OTLP traces endpoint:

```sql
@observability/sql/enable_server_observability.sql
```

Before running the scripts, edit the endpoint, host, port, and principal values.
The ACL principal must match the database user used by the Spring Boot app.

For a remote database, use a collector endpoint reachable from the database,
such as the OKE-hosted endpoint above.

Do not use `http://localhost:4318/v1/traces` for a remote database; that points
at the database host, not your laptop.

The Podman testcase notes also found that the database push path should use
HTTPS. If you want the database to export to a trace backend running on your
laptop, put an HTTPS tunnel or collector endpoint in front of local Jaeger and
register that public HTTPS URL with `DBMS_OBSERVABILITY`.

To inspect the OKE-hosted Jaeger UI:

```bash
kubectl -n financial port-forward svc/jaeger 16686:16686
```

Then open `http://localhost:16686`.

## 6. Verify Database Observability

Run:

```sql
@observability/sql/show_server_observability.sql
```

Check that the service is enabled and that an `otel_traces` endpoint is
registered. If this query returns `ORA-00904` or another package visibility
error, the connected database/user does not expose the required
`DBMS_OBSERVABILITY` status surface yet; fix that before expecting
server-originated spans.

If the database can export server-side spans and the driver propagates the
context correctly, the database spans should appear in the same trace as the
Spring Boot request.

## Known Current Limitations

- The public Spring Boot demo is expected to show app and JDBC spans first.
- DB-side spans depend on the Oracle Database server-side OpenTelemetry feature,
  network ACLs, a reachable collector, and trace context propagation from the
  JDBC driver into the database session.
- The separate `db-observability-scenarios` material indicates that some older
  driver/database combinations do not yet complete the DB-side span path, and
  that a fixed driver build may be required for full propagation.
- The current SQL scripts are intentionally minimal. For a more complete setup,
  add CDB/PDB-aware enablement, `otel_logs` if log export is needed, collector
  credentials if required, and database trust/wallet setup for HTTPS.

## Troubleshooting

If no traces appear, confirm that `OTLP_TRACES_ENDPOINT` points to a running
collector and that `management.tracing.sampling.probability` is `1.0`.

If only HTTP spans appear, confirm that the Oracle JDBC Observability provider
dependency is present and that Hikari receives this property:

```yaml
oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider
```

Also confirm that the Spring Boot `OpenTelemetry` instance is registered as
`GlobalOpenTelemetry`; the Oracle JDBC provider reads the global OpenTelemetry
instance.

If app and JDBC spans appear but no database spans appear, check:

- The database can reach the OTLP endpoint.
- The ACL principal is the same user used by the JDBC connection.
- `DBMS_OBSERVABILITY` is enabled in the right container or PDB.
- A traces endpoint is registered.
- The JDBC/database version supports the required server-side propagation path.
- The database DT trace files are not saying the operation was archived only to
  `ksu_ops_<db>.trc`; that indicates the trace context reached the database but
  the server-side exporter did not push to OTLP.

If Maven cannot download dependencies, retry on a working network or use an
existing local Maven cache. A dependency download failure is separate from the
demo code path.
