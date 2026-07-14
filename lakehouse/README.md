# Oracle AI Database Lakehouse + Iceberg JDBC Demo

This Spring Boot sample shows how a Java service can query Oracle lakehouse and
Apache Iceberg-backed SQL objects through Oracle JDBC. The database owns the
lakehouse/Iceberg integration; the application uses normal JDBC and SQL once the
table or view is available in Oracle AI Database.

## What is in this folder?

- `pom.xml` - Spring Boot 3.5 app using Java 25, `ojdbc17-production`,
  and Oracle UCP.
- `src/main/java` - REST endpoints for database metadata and Iceberg table samples.
- `src/main/resources/application.yaml` - environment-driven Oracle UCP/JDBC and demo settings.
- `.env_example` - local configuration template.
- `sql/show_lakehouse_metadata.sql` - helper queries for checking visible SQL objects.
- `sample-iceberg/metadata.json` - illustrative Iceberg metadata file shape for the direct metadata setup path.
- `blog.html` - draft blog post explaining the example and the lakehouse context.

## Prerequisites

- JDK 25.
- Maven 3.9 or later.
- Oracle AI Database or Autonomous Database with an Oracle SQL object that
  exposes Iceberg/lakehouse data.
- Network access from the app to the database.

The app uses the Oracle JDBC production dependency to keep the driver,
wallet/security libraries, and related runtime artifacts aligned. It also
depends directly on `ucp17` because the code creates an Oracle UCP connection
pool explicitly. The app sets `java.version` to `25` as requested.

## Configure

```bash
cd lakehouse
cp .env_example .env
vi .env
```

Set:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ICEBERG_TABLE_NAME`

`ICEBERG_TABLE_NAME` should be a table or view visible to the database user,
for example `LAKEHOUSE_DEMO.SALES_ICEBERG`.

## Build and Run

```bash
./build.sh
./run.sh
```

Then try:

```bash
curl http://localhost:8080/api/database
curl 'http://localhost:8080/api/lakehouse/objects?nameLike=ICEBERG'
curl http://localhost:8080/api/lakehouse/summary
curl 'http://localhost:8080/api/lakehouse/sample?tableName=LAKEHOUSE_DEMO.SALES_ICEBERG&limit=10'
curl 'http://localhost:8080/api/lakehouse/columns?tableName=LAKEHOUSE_DEMO.SALES_ICEBERG'
```

## Financial Database Smoke Test

The app can be smoke-tested against the existing financial Autonomous Database
even before you create a real Iceberg-backed table. Use the financial wallet
and point `ICEBERG_TABLE_NAME` at the setup table created by:

```bash
sql financial/<password>@financialdb_high @lakehouse/sql/setup_financial_smoke_test.sql
```

Then run:

```bash
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'
export DB_USERNAME=financial
export DB_PASSWORD='<financial-password>'
export ICEBERG_TABLE_NAME=LAKEHOUSE_JDBC_SMOKE_TEST
./build.sh
./run.sh
```

This verifies wallet connectivity, Oracle JDBC 17, Oracle UCP, endpoint
behavior, table metadata, and sample-row queries. It is not a substitute for a
real Iceberg-backed object; switch `ICEBERG_TABLE_NAME` to the Oracle SQL object
that exposes Iceberg table data when lakehouse setup is complete.

## How the Demo Relates to Iceberg

Apache Iceberg is an open table format for large analytic tables. It gives data
lake files table-like metadata, schema evolution, snapshots, partition pruning,
and multi-engine access. A lakehouse uses that kind of table format to combine
low-cost object storage with data-warehouse-style SQL access.

In this demo, Oracle AI Database is the SQL engine and governance point. Oracle
database-side lakehouse support discovers or exposes the Iceberg table; Oracle
JDBC connects the Spring Boot application to that SQL object. The Java code does
not parse Iceberg metadata itself and does not read Parquet files directly.

That is the important application pattern for Oracle JDBC Iceberg table support:
after the database exposes the Iceberg table as a SQL object, the Java service
uses normal JDBC APIs, normal SQL, and normal result-set handling.

The included `sample-iceberg/metadata.json` is a readable example of the
Iceberg metadata file referenced by the direct metadata setup path. A real
external table requires a complete Iceberg table in object storage, including
the metadata file, manifest list, manifests, and data files referenced by that
metadata.

## Notes

- Keep the database user least-privileged: grant only the SQL objects the app
  needs.
- Use a wallet or secret manager for production credentials.
- If the app returns an empty sample, first verify that `ICEBERG_TABLE_NAME` is
  visible to the configured database user.
