# Oracle AI Database Lakehouse + Iceberg JDBC Demo

This Spring Boot sample shows how a Java service can query Oracle lakehouse and
Apache Iceberg-backed SQL objects through Oracle JDBC. The database owns the
lakehouse/Iceberg integration; the application uses normal JDBC and SQL once the
table or view is available in Oracle AI Database.

## What is in this folder?

- `pom.xml` - Spring Boot 3.5 app using Java 25, `ojdbc17`, and `ucp17`.
- `src/main/java` - REST endpoints for database metadata and Iceberg table samples.
- `src/main/resources/application.yaml` - environment-driven Oracle UCP/JDBC and demo settings.
- `.env_example` - local configuration template.
- `sql/show_lakehouse_metadata.sql` - helper queries for checking visible SQL objects.
- `blog.html` - draft blog post explaining the example and the lakehouse context.

## Prerequisites

- JDK 25.
- Maven 3.9 or later.
- Oracle AI Database or Autonomous Database with an Oracle SQL object that
  exposes Iceberg/lakehouse data.
- Network access from the app to the database.

The app uses the Oracle JDBC 17-plus driver line through `ojdbc17` and `ucp17`.
The app sets `java.version` to `25` as requested.

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
curl http://localhost:8080/api/lakehouse/summary
curl 'http://localhost:8080/api/lakehouse/sample?tableName=LAKEHOUSE_DEMO.SALES_ICEBERG&limit=10'
curl 'http://localhost:8080/api/lakehouse/columns?tableName=LAKEHOUSE_DEMO.SALES_ICEBERG'
```

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

## Notes

- Keep the database user least-privileged: grant only the SQL objects the app
  needs.
- Use a wallet or secret manager for production credentials.
- If the app returns an empty sample, first verify that `ICEBERG_TABLE_NAME` is
  visible to the configured database user.
