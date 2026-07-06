# testcase_podman_otel Local Failure Notes

Date: July 2, 2026

This note captures the result of running the supplied `testcase_podman_otel`
demo locally with Podman on Apple Silicon. No nginx proxy or Jaeger receiver was
used for this run. The database pushed directly to the testcase OpenTelemetry
Collector listener:

```text
https://testcase-otel-listener:4318/v1/traces
```

The listener is the supplied `otel-listener-config.yaml`: OTLP gRPC on `4317`,
OTLP HTTPS on `4318`, and debug exporters for traces/logs/metrics.

## Local Adaptations

The testcase was kept as close to the supplied run as possible, with these local
runtime adjustments:

- `TLS_CERT_DIR=/private/tmp/testcase-podman-otel-certs`
- `JDBC_CLIENT_WORKDIR=/private/tmp/testcase-podman-otel-jdbc`
- `OTEL_JAVAAGENT=/private/tmp/opentelemetry-javaagent.jar`
- host ports changed to avoid conflicts: `1523`, `14319`, `14320`
- `LISTENER_TEST_WAIT_SECONDS=120`
- collector pinned to the documented successful version:
  `docker.io/otel/opentelemetry-collector-contrib:0.153.0`

The script also needed a local macOS Bash 3.2 compatibility tweak in the ignored
`testcase_podman_otel` directory:

```bash
case "$(printf '%s' "$TLS_WALLET_RESTART_DB" | tr '[:upper:]' '[:lower:]')" in
```

instead of Bash 4 `${TLS_WALLET_RESTART_DB,,}`.

## Command

```bash
TLS_CERT_DIR=/private/tmp/testcase-podman-otel-certs \
JDBC_CLIENT_WORKDIR=/private/tmp/testcase-podman-otel-jdbc \
OTEL_JAVAAGENT=/private/tmp/opentelemetry-javaagent.jar \
OTEL_IMAGE=docker.io/otel/opentelemetry-collector-contrib:0.153.0 \
HOST_DB_PORT=1523 \
HOST_OTLP_HTTP_PORT=14319 \
HOST_OTLP_GRPC_PORT=14320 \
LISTENER_TEST_WAIT_SECONDS=120 \
bash observability/testcase_podman_otel/testcase.sh run-jdbc-local
```

The same failure occurred with the script default Java 17 image and with
`JDBC_CLIENT_IMAGE=docker.io/library/eclipse-temurin:25-jdk`.

## Result

The testcase still failed after 120 seconds waiting for database spans:

```text
TESTCASE_RESULT=FAILED
reason=missing_listener_span db_span=NO jdbc_span=YES
```

The final matrix was:

```text
testcase-otel-listener | otlp     | jdbc | push   | traces YES (10)
testcase-otel-listener | otlp     | db   | push   | traces NO
testcase-otel-scraper  | oracledb | db   | scrape | metrics YES (84)
```

## Setup Reported By Testcase

```text
database_image=container-registry.oracle.com/database/free:23.26.2.0
database_version=Oracle AI Database 26ai Free Release 23.26.2.0.0 - Develop, Learn, and Run for Free
database_build_label=RDBMS_23.26.2.0.0DBRU_LINUX.ARM64_260417
jdbc_source=local ./ojdbc11.jar
jdbc_manifest:
  Implementation-Version=26.1.0.24.0
  Repository-Id=JAVAVM_MAIN_LINUX.X64_260419
tls_cert_key_type=ec
tls_trust_mode=wallet
tls_wallet_root=/opt/oracle/wallets
tls_wallet_component=disttrc
otel_image=docker.io/otel/opentelemetry-collector-contrib:0.153.0
otel_version=0.153.0
```

The documented successful sample reported an x64 database build:

```text
RDBMS_23.26.2.0.0DBRU_LINUX.X64_260427
```

## What Worked

The database can reach the Collector listener over HTTPS using the testcase
wallet:

```text
HTTPS_PING_RESULT=SUCCESS status_code=404 reason="Not Found"
url=https://testcase-otel-listener:4318/ wallet=YES
```

The JDBC client can toggle database runtime telemetry:

```text
DBMS_OBSERVABILITY runtime=Traces:1 Logs:1 CaptureTraces:1 container=Traces:1 Logs:1 CaptureTraces:1
```

The Collector received JDBC client spans from the Java agent:

```text
resource spans: 1, spans: 10
service.name: Str(jdbc-client)
InstrumentationScope io.opentelemetry.jdbc
```

## What Failed

No DB-originated OTLP signal arrived at the testcase listener. Raw listener logs
showed only JDBC client spans, all from `service.name=jdbc-client`.

Database diagnostic trace files show the fixed testcase trace id and the
configured OTLP listener host/port, but the KSTRC operation is still archived to
the local trace file:

```text
KSTRC: Operation [traceid-4bf92f3577b34da6a3ce929d0e0e4736 ...] archived to: ksu_ops_FREE.trc
Trace Bucket Dump Begin: 4bf92f3577b34da6a3ce929d0e0e4736
value="testcase-otel-listener"
value=4318
ALTER SYSTEM SET _kstrc_archiver='otel_traces://https://testcase-otel-listener:4318/v1/traces' SCOPE=BOTH;
```

## KSTRC Diagnostic Run

After the testcase owner requested deeper KSTRC/KSTRCB logging, the Oracle Free
container was restarted from a pfile with these additional parameters:

```text
_kstrc_service_mask=8
event="trace[KSTRC] disk highest"
event="trace[KSTRCB] disk highest"
```

For this local Oracle Free container the pfile path was:

```text
/opt/oracle/product/26ai/dbhomeFree/dbs/initFREE_kstrc_diag.ora
```

The alert log confirmed the diagnostic settings were active:

```text
event                    = "trace[KSTRC] disk highest"
event                    = "trace[KSTRCB] disk highest"
_kstrc_service_mask      = 8
_kstrc_archiver          = "otel_traces://https://testcase-otel-listener:4318/v1/traces"
```

After rerunning the JDBC workload, the most useful trace line was:

```text
KSTRCB: Failed ACL check and will not archive on URI 3630598141602376716
```

The same trace file also shows KSTRC attempting to archive the propagated trace:

```text
KSTRC: Archiving [traceid-4bf92f3577b34da6a3ce929d0e0e4736:parentid-0000000000000002]
```

The database then falls back to local operation trace output:

```text
KSTRC: Operation [traceid-4bf92f3577b34da6a3ce929d0e0e4736:parentid-0000000000000003] 0x81799c38 archived to: ksu_ops_FREE.trc
```

The collected diagnostic bundle is local-only at:

```text
/private/tmp/kstrc_diag_logs_20260702.tar.gz
```

Key files in that bundle:

```text
initFREE_kstrc_diag.ora
jdbc-client.log
collector/testcase-otel-listener.log
db_trace/alert_FREE.log
db_trace/FREE_ora_1395.trc
db_trace/FREE_dt01_878.trc
```

After the testcase owner clarified that the ACL should be granted to the user
executing the SQL session, a direct HTTPS check was run as the JDBC workload
user. It succeeded:

```text
session_user=JDBCCLIENT
current_user=JDBCCLIENT
HTTPS_PING_RESULT=SUCCESS status_code=404 reason="Not Found"
```

The PDB ACL metadata also shows the listener host grant for `JDBCCLIENT`:

```text
HOST                    PRINCIPAL   PRIVILEGE
testcase-otel-listener  JDBCCLIENT  CONNECT
testcase-otel-listener  JDBCCLIENT  HTTP
testcase-otel-listener  JDBCCLIENT  HTTP_PROXY
```

Additional local-only ACL experiments were tried in the disposable Podman DB:

- added `RESOLVE` for `JDBCCLIENT` on `testcase-otel-listener`
- added a port-specific `4318` grant for `JDBCCLIENT`
- added a port-specific grant for the resolved listener container IP
  `10.89.1.4`
- added diagnostic `PUBLIC` grants in `FREEPDB1`
- added diagnostic `PUBLIC` grants in `CDB$ROOT`

None of those changed the testcase result. JDBC spans still arrived, but DB
spans did not:

```text
db_span=NO jdbc_span=YES
db   traces=NO logs=NO metrics=NO
```

One additional detail: after starting from the diagnostic pfile,
`DBMS_OBSERVABILITY` container options showed `CaptureTraces:1`, but the runtime
option printed by the JDBC testcase still showed `CaptureTraces:0`:

```text
DBMS_OBSERVABILITY runtime=Traces:1 Logs:1 CaptureTraces:0 container=Traces:1 Logs:1 CaptureTraces:1
```

## Question For Testcase Owner

The supplied testcase successfully enables DBMS_OBSERVABILITY, configures the
PDB wallet, verifies database HTTPS reachability to the collector, and receives
JDBC spans. The database also receives the trace id and KSTRC writes trace
buckets locally.

The KSTRCB diagnostics now show an ACL failure:

```text
KSTRCB: Failed ACL check and will not archive on URI 3630598141602376716
```

Which database principal should be granted the outbound network ACL for the
KSTRCB/background archive path? The testcase grants the listener ACL to
`JDBCCLIENT`, and a direct SQL session as `JDBCCLIENT` can reach the listener,
but KSTRCB still reports an ACL failure when archiving the DB trace.
