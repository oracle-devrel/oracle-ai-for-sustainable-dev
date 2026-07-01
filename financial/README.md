# Financial Demo Application

This financial application and workshop are aimed at both financial line of
business experts and developers. The goal is to give both audiences a shared
view of the business workflows, application architecture, Oracle Database
features, AI integrations, scaling patterns, and deployment tradeoffs.

Each lab or application area describes topics such as:

- Business process.
- Developer architecture and implementation details.
- Companies or industries that use similar patterns.
- Oracle differentiators.
- Code, tests, and comparisons with other approaches.
- Short walkthrough material.

For the full setup map, including OKE redeployment, Terraform, ingress,
database setup, secrets, deployment profiles, and frontend Basic Auth, see
[`readme-setup.md`](readme-setup.md).

## Directory Shape

The main public app is the React frontend plus the backend services it calls
through ingress. The broader `financial` directory also contains optional
feature services and standalone workshop labs.

Start with these areas for the live app:

- `react-frontend`
- `springboot-backend`
- `microtx-saga-lockless-bank-transfer/account`
- `microtx-saga-lockless-bank-transfer/transfer`
- `mongodb-mern-bank-account/mern-stack`

Then enable optional services such as graph, True Cache, Kafka/TXEventQ,
Select AI, vector/MCP agents, spatial/OML, observability, or polyglot ATM demos
as needed.

## Running A Standalone Java Service

Most Java services can be run outside Kubernetes from their own directory. For
example:

```bash
cd financial/springboot-backend

export DB_USER=financial
export DB_PASSWORD=<database-password>
export DB_URL='jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/path/to/Wallet_financialdb'

mvn package
java -jar target/*.jar
```

PowerShell equivalent:

```powershell
cd financial/springboot-backend

$env:DB_USER = "financial"
$env:DB_PASSWORD = "<database-password>"
$env:DB_URL = "jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=C:\path\to\Wallet_financialdb"

mvn package
java -jar (Get-ChildItem target\*.jar | Select-Object -First 1).FullName
```

Use the service-specific README when a demo requires additional environment
variables, OCI AI configuration, Kafka/TXEventQ setup, ORDS, or feature-specific
database objects.

## Security

Do not commit database passwords, wallet files, OCIR auth tokens, Terraform
state, kubeconfigs, TLS private keys, or generated Kubernetes Secret manifests.
Keep local operator values in ignored files such as `financial/setup/.env`.
