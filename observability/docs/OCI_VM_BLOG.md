# End-to-End OpenTelemetry Tracing from Spring Boot to Oracle Database on OCI

This walkthrough builds a reproducible Oracle Database OpenTelemetry demo on a
single OCI Linux x86_64 instance. The finished trace appears in Jaeger as one
continuous trace with both application spans and Oracle Database server-side
spans.

The working shape is:

```text
Browser or curl
  -> Spring Boot HTTP span
  -> Micrometer observation
  -> Oracle JDBC provider spans
  -> Oracle Database DB Server span
  -> HTTPS OTLP endpoint
  -> Jaeger
```

The important details are:

- Use an x86_64 Oracle Linux instance. The database server-side export path did
  not work reliably on macOS ARM with the public database image.
- Jaeger exposes OTLP over plain HTTP, but Oracle Database server-side trace
  export needs an HTTPS OTLP endpoint.
- The database distributed trace exporter looks for its trust wallet under
  `WALLET_ROOT/<PDB_GUID>/disttrc`, not just `WALLET_ROOT/<PDB_GUID>/tls`.
- On Oracle Database Free 23.26.2.0, the verified setup did not require hidden
  KSTRC parameters; leave `_kstrc_service_mask` and `_kstrc_archiver` at their
  defaults unless Oracle Support or a specific diagnostic exercise asks you to
  change them.
- Set database `MODULE`, `ACTION`, and `CLIENT_IDENTIFIER` from the app so the
  DB span visibly carries database-session context, not just a generic
  `DB Server` label.

## OCI Instance

Create an Oracle Linux x86_64 VM with enough room for the database image and
Maven dependencies. The verified demo used:

- Oracle Linux 9.x
- `VM.Standard.E5.Flex`
- 4 OCPUs
- 32 GB memory
- 200 GB boot volume

Open only the ports you need from your workstation IP:

- `22/tcp` for SSH
- `8080/tcp` for the Spring Boot app
- `16686/tcp` for Jaeger

Keep Oracle Database listener port `1521` private to the instance.

## Install Host Packages

SSH to the VM and install the runtime tools:

```bash
sudo dnf install -y podman git java-25-openjdk java-25-openjdk-devel maven jq curl openssl firewalld
sudo systemctl enable --now firewalld
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=16686/tcp
sudo firewall-cmd --reload
```

Use Java 25 for the app build and service:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## Start Jaeger and Oracle Database

Create a Podman network:

```bash
sudo podman network create oracle-db-otel
```

Start Jaeger:

```bash
sudo podman run -d --name oracle-db-otel-jaeger \
  --network oracle-db-otel \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  cr.jaegertracing.io/jaegertracing/jaeger:2.19.0
```

Start Oracle Database Free:

```bash
sudo podman volume create oracle-free-data
sudo podman run -d --name oracledb-free \
  --network oracle-db-otel \
  -p 127.0.0.1:1521:1521 \
  -p 127.0.0.1:5500:5500 \
  -e ORACLE_PASSWORD='<admin-password>' \
  -v oracle-free-data:/opt/oracle/oradata \
  container-registry.oracle.com/database/free:23.26.2.0
```

Wait until the container is healthy:

```bash
sudo podman inspect -f '{{.State.Health.Status}}' oracledb-free
```

## Add an HTTPS OTLP Front Door for the Database

Jaeger receives OTLP HTTP on port `4318`, but the database trace push path needs
HTTPS. Add a small NGINX TLS proxy on the same Podman network. It accepts HTTPS
from the database and forwards to Jaeger's plain OTLP HTTP endpoint.

Generate a local CA and server certificate for `otel-tls-proxy`:

```bash
mkdir -p /opt/observability-demo/local/tls-proxy
cd /opt/observability-demo/local/tls-proxy

cat > server.ext <<'EOF'
subjectAltName=DNS:otel-tls-proxy,DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth
EOF

openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
  -subj "/CN=Oracle DB OTel Demo Local CA" -out ca.crt
openssl genrsa -out server.key 2048
openssl req -new -key server.key -subj "/CN=otel-tls-proxy" -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt -days 825 -sha256 -extfile server.ext
```

Create the NGINX config:

```bash
cat > nginx.conf <<'EOF'
events {}
http {
  server {
    listen 4318 ssl http2;
    server_name otel-tls-proxy;
    ssl_certificate /etc/nginx/certs/server.crt;
    ssl_certificate_key /etc/nginx/certs/server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    client_max_body_size 0;

    location / {
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Forwarded-Proto https;
      proxy_pass http://oracle-db-otel-jaeger:4318;
    }
  }
}
EOF
```

Start the proxy:

```bash
sudo podman run -d --name otel-tls-proxy \
  --network oracle-db-otel \
  -p 9443:4318 \
  -v /opt/observability-demo/local/tls-proxy/nginx.conf:/etc/nginx/nginx.conf:ro,z \
  -v /opt/observability-demo/local/tls-proxy:/etc/nginx/certs:ro,z \
  docker.io/library/nginx:1.29-alpine
```

## Configure the Database

Create an application user and configure DBMS_OBSERVABILITY in `FREEPDB1`.
Generate and store the application password outside the repo.

```bash
APP_USER=FINANCIAL
APP_PASSWORD='<app-user-password>'
```

Run the SQL setup as SYSDBA:

```bash
sudo podman exec -i oracledb-free bash -lc 'sqlplus -s / as sysdba' <<SQL
set echo off feedback on serveroutput on size unlimited

alter system set local_listener='(ADDRESS=(PROTOCOL=tcp)(HOST=0.0.0.0)(PORT=1521))' scope=both;
alter system register;
alter system set wallet_root='/opt/oracle/wallets' scope=spfile;

alter session set container=FREEPDB1;

declare
  l_count number;
begin
  select count(*) into l_count from dba_users where username = '${APP_USER}';
  if l_count = 0 then
    execute immediate 'create user ${APP_USER} identified by "${APP_PASSWORD}"';
  else
    execute immediate 'alter user ${APP_USER} identified by "${APP_PASSWORD}" account unlock';
  end if;
end;
/

grant create session to ${APP_USER};
grant create table to ${APP_USER};
grant create sequence to ${APP_USER};
grant alter session to ${APP_USER};
grant execute on sys.dbms_observability to ${APP_USER};
grant execute on sys.dbms_sql_monitor to ${APP_USER};
grant execute on sys.dbms_xplan to ${APP_USER};
grant execute on sys.utl_http to ${APP_USER};
grant select_catalog_role to ${APP_USER};
alter user ${APP_USER} quota unlimited on users;
SQL
```

Restart the database container so `wallet_root` takes effect:

```bash
sudo podman restart oracledb-free
```

Wait for the database to become healthy again.

Create Oracle wallets for the PDB. The `disttrc` component is the critical one
for database server-side trace export. The `tls` component is useful for
UTL_HTTP tests.

```bash
sudo podman cp /opt/observability-demo/local/tls-proxy/ca.crt \
  oracledb-free:/tmp/otel-tls-proxy-ca.crt

PDB_GUID=$(sudo podman exec -i oracledb-free bash -lc 'sqlplus -s / as sysdba' <<'SQL' | awk 'NF {print $1; exit}'
set heading off feedback off pages 0 verify off
select rawtohex(guid) from v$pdbs where name='FREEPDB1';
SQL
)

WALLET_ROOT=/opt/oracle/wallets
WALLET_PASSWORD='<wallet-password>'

for COMPONENT in disttrc tls; do
  WALLET_DIR="${WALLET_ROOT}/${PDB_GUID}/${COMPONENT}"
  sudo podman exec --user root \
    -e WALLET_ROOT="$WALLET_ROOT" \
    -e WALLET_DIR="$WALLET_DIR" \
    oracledb-free bash -lc \
    'mkdir -p "$WALLET_ROOT" "$WALLET_DIR" && chown -R oracle:oinstall "$WALLET_ROOT"'

  sudo podman exec \
    -e WALLET_DIR="$WALLET_DIR" \
    -e WALLET_PASSWORD="$WALLET_PASSWORD" \
    oracledb-free bash -lc '
      rm -f "$WALLET_DIR"/* &&
      "$ORACLE_HOME/bin/orapki" wallet create -wallet "$WALLET_DIR" -pwd "$WALLET_PASSWORD" -auto_login &&
      "$ORACLE_HOME/bin/orapki" wallet add -wallet "$WALLET_DIR" -pwd "$WALLET_PASSWORD" \
        -alias otel-tls-proxy -trusted_cert -cert /tmp/otel-tls-proxy-ca.crt &&
      "$ORACLE_HOME/bin/orapki" wallet display -wallet "$WALLET_DIR" -details >/dev/null'
done
```

Register the HTTPS OTLP endpoint and enable tracing:

```bash
sudo podman exec -i oracledb-free bash -lc 'sqlplus -s / as sysdba' <<'SQL'
set echo off feedback on serveroutput on size unlimited lines 240 pages 100

alter session set container=FREEPDB1;

begin
  dbms_network_acl_admin.append_host_ace(
    host => 'otel-tls-proxy',
    ace => xs$ace_type(
      privilege_list => xs$name_list('connect', 'resolve', 'http', 'http_proxy'),
      principal_name => 'FINANCIAL',
      principal_type => xs_acl.ptype_db
    )
  );
exception
  when others then
    if sqlcode not in (-24243, -46385) then
      raise;
    end if;
end;
/

declare
begin
  dbms_observability.add_endpoint(
    endpoint_type => dbms_observability.otel_traces,
    endpoint => 'https://otel-tls-proxy:4318/v1/traces',
    credential_name => null);
exception
  when others then
    if sqlcode != -20000 then
      raise;
    end if;
end;
/

begin
  dbms_observability.enable_endpoint('https://otel-tls-proxy:4318/v1/traces');
  dbms_observability.enable_service_option(dbms_observability.capture_traces);
  dbms_observability.enable_service_option(dbms_observability.show_extra_metadata);
  dbms_observability.enable_service(dbms_observability.all_services);
end;
/

select dbms_observability.show_service_status from dual;
SQL
```

## Build and Run the Spring Boot App

Build the demo app:

```bash
cd /opt/observability-demo/springboot-oracle-db-otel-demo
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
mvn -DskipTests package
```

Create a VM-local environment file. Do not commit this file:

```bash
cat > /opt/observability-demo/.env <<EOF
DB_URL=jdbc:oracle:thin:@//127.0.0.1:1521/FREEPDB1
DB_USERNAME=FINANCIAL
DB_PASSWORD=<app-user-password>
OTLP_TRACES_ENDPOINT=http://127.0.0.1:4318/v1/traces
TRACE_SAMPLE_PROBABILITY=1.0
ORACLE_JDBC_SERVER_TELEMETRY_TRACES_ENABLED=true
ORACLE_JDBC_SERVER_TELEMETRY_LOGGING_ENABLED=false
ORACLE_JDBC_TRACEPARENT_CLIENT_INFO_ENABLED=true
ORACLE_JDBC_TRACELEVEL_CLIENT_INFO_ENABLED=true
JAVA_HOME=/usr/lib/jvm/java-25-openjdk
EOF
chmod 600 /opt/observability-demo/.env
```

Install the app as a service:

```bash
sudo tee /etc/systemd/system/springboot-oracle-db-otel-demo.service >/dev/null <<'EOF'
[Unit]
Description=Oracle DB OpenTelemetry Demo
After=network-online.target podman.service
Wants=network-online.target

[Service]
Type=simple
User=opc
WorkingDirectory=/opt/observability-demo/springboot-oracle-db-otel-demo
EnvironmentFile=/opt/observability-demo/.env
Environment=JAVA_HOME=/usr/lib/jvm/java-25-openjdk
Environment=PATH=/usr/lib/jvm/java-25-openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin
ExecStart=/usr/lib/jvm/java-25-openjdk/bin/java -jar /opt/observability-demo/springboot-oracle-db-otel-demo/target/springboot-oracle-db-otel-demo-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now springboot-oracle-db-otel-demo.service
```

## Verify the End-to-End Trace

Generate a traced request:

```bash
curl -sS http://127.0.0.1:8080/trace/roundtrip | jq .
```

The response includes the trace id and the database-side session context that
the app set before running the workload:

```json
{
  "database": {
    "MODULE_NAME": "springboot-oracle-db-otel-demo",
    "ACTION_NAME": "context-query",
    "CLIENT_IDENTIFIER": "traceId=<trace-id>",
    "SESSION_ID": "..."
  },
  "workload": {
    "syntheticRows": 5000,
    "dictionaryObjectsVisible": 1000,
    "serverMetadata": {
      "MODULE_NAME": "springboot-oracle-db-otel-demo",
      "ACTION_NAME": "server-metadata-query",
      "INSTANCE_NAME": "FREE",
      "SERVER_HOST": "..."
    }
  }
}
```

Capture the returned `trace.traceId`, then query Jaeger:

```bash
TRACE_ID=<trace-id-from-response>
curl -sS "http://127.0.0.1:16686/api/traces/${TRACE_ID}" | jq \
  '{services: ([.data[].processes[]?.serviceName] | unique),
    ops: ([.data[].spans[].operationName] | unique),
    spanCount: ([.data[].spans[]] | length)}'
```

A successful result includes both services and a `DB Server` operation:

```json
{
  "services": [
    "oracle-db",
    "springboot-oracle-db-otel-demo"
  ],
  "ops": [
    "DB Server",
    "Execute query",
    "Generic authentication call",
    "Get the session key",
    "Ping",
    "http get /trace/roundtrip",
    "oracle.demo.database-roundtrip"
  ],
  "spanCount": 19
}
```

In the Jaeger UI, expand the orange `oracle-db / DB Server` span. The DB span is
exported by the database and should include database-only fields such as:

- `oracle.db.instance.name`
- `oracle.db.name`
- `oracle.db.pdb`
- `oracle.db.service`
- `oracle.db.session.id`
- `oracle.db.module`
- `oracle.db.action`
- `oracle.db.connection.id`
- `oracle.db.query.sql.id` on SQL statements where the database reports one
- `db.response.status_code`

The client-side JDBC spans are useful too, but they are emitted by the Java
process. The `oracle-db` service and `DB Server` span prove that the trace crossed
into the database server-side exporter.

The TLS proxy should also show the database export:

```bash
sudo podman logs --since 5m otel-tls-proxy
```

Expected line:

```text
"POST /v1/traces HTTP/2.0" 200
```

Open Jaeger from your workstation:

```text
http://<vm-public-ip>:16686
```

Search for service `springboot-oracle-db-otel-demo` and open the trace. The same trace
should include the `oracle-db` service and a `DB Server` span.

## Agentic AI Trace With SQL Monitor Bridge

The most complete demo endpoint is `/trace/agent-task`. It frames the database
work as an agentic AI task, then keeps the whole story anchored in one Jaeger
trace:

```text
agent task
  -> Spring Boot HTTP span
  -> oracle.demo.agent-database-investigation span
  -> Oracle JDBC spans
  -> Oracle Database DB Server span
  -> oracle.db.query.sql.id
  -> SQL Monitor and DBMS_XPLAN details returned by the same API call
```

Generate an agent trace:

```bash
curl -sS \
  'http://127.0.0.1:8080/trace/agent-task?agentId=claims-investigator-agent&task=investigate_payment_anomalies' \
  | jq .
```

For a browser-friendly rendering of the same demo, open:

```text
http://127.0.0.1:8080/trace/agent-task/view?agentId=claims-investigator-agent&task=investigate_payment_anomalies
```

The rendered page includes the agent/task context, trace bridge, SQL runtime
stats, full SQL text from `V$SQL.SQL_FULLTEXT`, application bind values,
captured bind samples from `V$SQL_BIND_CAPTURE`, the SQL Monitor report preview,
`DBMS_XPLAN` for the SQL execution plan, and an embedded Jaeger trace at the
bottom. The embedded trace loads after a short delay so the Spring Boot HTTP,
Micrometer, JDBC, and database spans have time to flush to Jaeger. The button at
the top still opens Jaeger in a separate tab if your browser blocks embedding.

For the agent endpoint, the app uses Oracle JDBC `setEndToEndMetrics(...)` to
set Oracle `MODULE` to `agent:<agent-id>`, set `ACTION` to the current phase
such as `agent-workload-query`, and set `CLIENT_IDENTIFIER` to the trace id.
The application remains visible as the `springboot-oracle-db-otel-demo` service
in Jaeger, but the database-exported span now carries the agent identity in
`oracle.db.module`. That is the field we will reuse when we correlate traces
with database security policy and audit evidence.

The agent workload uses an app-owned `AGENT_EVENT_LOG` table and a prepared
statement with bind variables for risk threshold, agent id, event type, and
minimum amount. The page shows the exact application bind values and any
database-captured bind samples. Oracle captures bind samples, not every bind
value from every execution, so `V$SQL_BIND_CAPTURE` should be treated as
database evidence samples rather than a complete parameter history.

The response keeps the visual trace and the database diagnostics together:

```json
{
  "agent": {
    "id": "claims-investigator-agent",
    "task": "investigate_payment_anomalies",
    "databaseOperation": "agent-claims-investigator-agen",
    "databaseOperationExecutionId": 1
  },
  "trace": {
    "traceId": "fc53ed01bfd8530a3b8740010a6086f8"
  },
  "sqlBridge": {
    "sqlId": "ff7xbtj1ku8ja",
    "description": "Use oracle.db.query.sql.id on the Jaeger DB Server span as the bridge from the visual trace to these Oracle SQL Monitor and DBMS_XPLAN details."
  },
  "workload": {
    "eventsScanned": 1782,
    "flaggedEvents": 368,
    "highestRiskScore": 100,
    "highestAmount": 6712.67
  },
  "bindValues": {
    "riskThreshold": 80,
    "agentId": "claims-investigator-agent",
    "eventType": "payment",
    "minimumAmount": 1000.0
  },
  "sqlStats": {
    "available": true,
    "SQL_ID": "ff7xbtj1ku8ja",
    "EXECUTIONS": 3,
    "ROWS_PROCESSED": 3
  },
  "sqlMonitor": {
    "available": true,
    "type": "TEXT",
    "sqlId": "ff7xbtj1ku8ja"
  },
  "dbmsXplan": {
    "available": true,
    "format": "ALLSTATS LAST +IOSTATS +MEMSTATS"
  }
}
```

In Jaeger, search for the returned `trace.traceId` and expand the
`oracle-db / DB Server` span whose `oracle.db.action` is
`agent-workload-query`. That database-exported span contains
`oracle.db.query.sql.id`, `oracle.db.module`, `oracle.db.action`, and
`oracle.db.session.id`.

That `oracle.db.query.sql.id` attribute is the bridge: it connects the visual
Jaeger trace to the SQL Monitor report and `DBMS_XPLAN.DISPLAY_CURSOR` output in
the response. In the verified VM run, `DBMS_XPLAN` and SQL Monitor returned the
canonical 13-character SQL ID, while the database-exported Jaeger tag displayed
the same SQL ID prefix. Use the response as the canonical SQL diagnostic record
and the Jaeger attribute as the visual pointer from trace to database work. The
app runs the workload SQL with:

```sql
select /*+ monitor gather_plan_statistics agentic_ai_trace_bridge */ ...
```

The `MONITOR` hint makes the SQL eligible for Real-Time SQL Monitoring, while
`gather_plan_statistics` allows `DBMS_XPLAN.DISPLAY_CURSOR` to show actual
runtime stats with `ALLSTATS LAST +IOSTATS +MEMSTATS`.

The endpoint also starts a named composite database operation with
`DBMS_SQL_MONITOR.BEGIN_OPERATION`, using the agent id and trace id as operation
attributes. That gives the database a server-side name for the agent task, not
just an anonymous SQL execution.

The demo user needs extra diagnostic privileges for this endpoint:

```sql
grant execute on sys.dbms_sql_monitor to FINANCIAL;
grant execute on sys.dbms_xplan to FINANCIAL;
grant select_catalog_role to FINANCIAL;
```

These grants are intentionally broad for a contained demo VM. For a shared or
production environment, create a narrower diagnostic role and review Oracle
Database Diagnostics Pack and Tuning Pack licensing before using SQL Monitor.

Verified OCI VM result:

```json
{
  "traceId": "fc53ed01bfd8530a3b8740010a6086f8",
  "services": [
    "oracle-db",
    "springboot-oracle-db-otel-demo"
  ],
  "ops": [
    "DB Server",
    "Execute query",
    "Fetch a row",
    "http get /trace/agent-task",
    "oracle.demo.agent-database-investigation"
  ],
  "spanCount": 27
}
```

The response included a canonical SQL Monitor/DBMS_XPLAN SQL ID of
`ff7xbtj1ku8ja`, SQL text with binds such as `:1`, `:2`, `:3`, and `:4`,
application bind values, database-captured bind samples, SQL Monitor
availability, and server-side metrics such as elapsed time, CPU time, buffer
gets, and disk reads.

## Troubleshooting Notes

If Jaeger only shows `springboot-oracle-db-otel-demo`, check the database trace files:

```bash
sudo podman exec oracledb-free bash -lc \
  "grep -R -i 'KSTRC\\|IPCLNT\\|OpenWallet\\|otel-tls-proxy' /opt/oracle/diag/rdbms/free/FREE/trace/FREE_dt*.trc | tail -80"
```

Useful failures:

- `nzos_OpenWallet() failed ... /disttrc/`: create the trusted wallet under
  `WALLET_ROOT/<PDB_GUID>/disttrc`.
- no `POST /v1/traces` in proxy logs: the database did not reach the collector.
- only disk archive lines such as `archived to: ksu_ops_FREE.trc`: verify the
  `DBMS_OBSERVABILITY` endpoint, endpoint HTTPS, ACLs, and the
  `WALLET_ROOT/<PDB_GUID>/disttrc` trust wallet. Use KSTRC diagnostics only when
  you are deliberately troubleshooting database trace export internals.
- `DBMS_OBSERVABILITY runtime=Traces:0`: ensure the JDBC app toggles
  `OracleConnection.ServerTelemetry.Traces` and that
  `dbms_observability.enable_service(dbms_observability.all_services)` is set in
  the PDB.
- generic `oracle.db.module` such as `JarLauncher`: make sure the app calls
  Oracle JDBC `setEndToEndMetrics(...)` before the traced SQL.
- missing richer database tags: enable
  `dbms_observability.enable_service_option(dbms_observability.show_extra_metadata)`
  where the database version supports it.

## Next Step: OKE

The same pattern can move to OKE:

- Run Jaeger in the cluster.
- Expose an internal HTTPS OTLP endpoint for the database server-side exporter.
- Deploy this Spring Boot app as a microservice.
- Point the app and the database to the same collector.
- Use the financial Autonomous Database once the required `DBMS_OBSERVABILITY`
  endpoint, ACL, and wallet/trust configuration are available there.

## Next Step: Security And Audit Correlation

The trace now carries the right join keys for the security story:

- `trace.traceId` identifies the distributed request.
- `agent.id` identifies the AI actor in the application span.
- `oracle.db.module=agent:<agent-id>` identifies the same actor inside the
  database session and DB Server span.
- `oracle.db.action` identifies the phase, such as `agent-workload-query`.
- `oracle.db.query.sql.id` bridges the visual trace to SQL text, SQL Monitor,
  `DBMS_XPLAN`, and later audit records.
- `CLIENT_IDENTIFIER=traceId=<trace-id>` gives database security and audit
  policy a compact trace correlation value.

The security correlation path should build on the Deep Data Security work:

1. Add a security-aware agent endpoint that authenticates as a real application
   principal, then uses Oracle JDBC end-to-end metrics to set `MODULE`,
   `ACTION`, and `CLIENT_IDENTIFIER` before every database call.
2. Run a query against a table protected by database security policy, such as
   grants, roles, VPD/OLS-style predicates, or Deep Data Security token-based
   authorization.
3. Return a security evidence panel in the same rendered page: effective
   database user, enabled roles, object privileges checked, policy decision,
   rows allowed/filtered, and any denied-operation example.
4. Keep `oracle.db.query.sql.id` as the bridge from the Jaeger DB Server span to
   the SQL text and security evidence.
5. Add an audit phase after the security phase: enable a scoped audit policy,
   run the same agent workload, and show audit rows correlated by
   `CLIENT_IDENTIFIER`, database user, `MODULE`, `ACTION`, SQL ID, and timestamp.

The goal is a single page where the reader can see: this agent made this
request, this trace crossed into Oracle Database, this SQL ran under these
database privileges/policies, and the database audit trail recorded the same
actor and trace context.
