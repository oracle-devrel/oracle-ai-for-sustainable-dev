# Financial Application Setup

This document is the setup map for the full `financial` area. It covers the
live OKE deployment path, the optional feature services that the React app can
link to, and the standalone/lab demos that live under the same directory.

It intentionally does not replace the scenario-specific READMEs in each demo
folder. Use this file as the top-level runbook, then drop into the individual
folder README when enabling a feature that has extra setup.

## Scope

The current live app path is the React frontend plus the backend services it
calls through ingress. That is not the same thing as every directory under
`financial`.

Use these tiers when planning a deployment:

- Core public app: the services required for the main browser workflow.
- Feature services: backend demos that appear in the React UI but may be
  enabled one at a time.
- Standalone labs: examples that are useful for workshops but are not currently
  required for the public `/financial` app.
- Local/private setup: ignored files that hold operator-specific values,
  wallets, Terraform state, kubeconfig, and secrets.

The public app is normally exposed at a path such as:

```text
https://oracledev.ai/financial/
```

Because React runs in the user's browser, all browser calls must go through a
public hostname or path-based ingress route. The browser cannot call Kubernetes
`ClusterIP` service names directly.

## Full App Inventory

| Area | Role | Deployment status |
| --- | --- | --- |
| `react-frontend` | Browser shell for the financial workshop. | Core OKE service; must be rebuilt with the public path and backend route values. |
| `springboot-backend` | Main Java backend for financial APIs such as accounts, payments, spatial, graph-related endpoints, true cache, and stock/investment demos. | Core or feature service depending on page; should use wallet and DB password secrets. |
| `microtx-saga-lockless-bank-transfer/account` | Account service for the MicroTx transfer flow. | Core service for transfer/ATM pages. |
| `microtx-saga-lockless-bank-transfer/transfer` | Transfer service for the MicroTx transfer flow. | Core service for transfer/ATM pages. |
| `mongodb-mern-bank-account/mern-stack` | MERN/Oracle JSON duality account backend. | Feature service behind the React accounts page. |
| `mongodb-mern-bank-account/springboot-relational-stack` | Alternative Spring Boot account backend. | Optional alternative to the MERN account backend. |
| `graph-circular-payments` | Circular payment and graph demo. | Feature service; route through ingress when enabled. |
| `true-cache-stock-ticker` | True Cache stock ticker demo. | Feature service; route through ingress when enabled. |
| `globally-distributed-database-purchases` | Globally distributed database/spatial purchase flow. | Feature service; route through ingress when enabled. |
| `kafka-txeventq-txoutbox-nft-purchase/kafka-sql-inventory` | Kafka/TXEventQ transactional outbox demo service. | Feature service; requires Kafka/TXEventQ setup. |
| `select-ai-speak-with-data` | Select AI and speech/query demo. | Feature service; requires wallet, DB setup, and OCI AI configuration. |
| `vector-mcp-ai-agents` | Agentic AI/vector/MCP demo. | Feature service or standalone lab; keep real config in ignored files. |
| `observability-opentelemetry` | OpenTelemetry/observability notes and demo assets. | Optional platform/lab add-on; typically pairs with Grafana/collector setup. |
| `spatial-oml-suspicious-purchases` | Spatial and OML suspicious purchase demo. | Mostly notebook/script flow; database setup may be reused by the app. |
| `polyglot-atm` | ATM/inventory examples across Java, Go, Node.js, Python, .NET, Quarkus, Helidon, Micronaut, PL/SQL, and others. | Standalone lab family; deploy only the language variants needed for a workshop. |
| `bank-transfer-microtx-saga-lockless` | Older/alternate MicroTx bank transfer sample. | Standalone or legacy reference; not part of the main public path by default. |
| `mongodb-kafka-postgres` | MongoDB, Kafka, and PostgreSQL support stack examples. | Standalone support infrastructure for selected labs. |
| `react-graphql` | React GraphQL experiment. | Standalone lab unless wired into the main frontend. |
| `spring-data-jpa-graphql-ucp-oracle` | Spring Data JPA/GraphQL/UCP Oracle sample. | Standalone backend lab. |
| `sql` | Database setup and demo SQL. | Shared database setup source; review before running because some files are scenario-specific. |
| `k8s` | Shared ingress/TLS/wallet helper manifests. | Shared Kubernetes assets; current ingress needs host/path modernization for `oracledev.ai/financial`. |

Local-only directories such as Terraform working directories or generated setup
files may exist on a developer machine but should stay ignored unless they are
sanitized and intentionally added.

## Target Architecture

Use one OKE namespace for the public app unless a demo requires isolation:

```text
browser
  -> Cloudflare DNS/TLS
  -> OCI Load Balancer for nginx ingress
  -> nginx ingress Basic Auth and path routing
  -> React frontend and backend services
  -> Autonomous AI Database using wallet and DB secrets
```

Recommended platform pieces:

- OCI Kubernetes Engine with private worker nodes.
- nginx ingress controller for path-based routing under `oracledev.ai`.
- One Autonomous AI Database, or an existing Autonomous Database supplied by
  the operator.
- Wallet secret mounted into database-backed pods.
- Database password secret referenced by pods rather than literal manifest
  values.
- OCIR image-pull secret if images are private.
- cert-manager or a Cloudflare origin certificate for TLS when the ingress
  terminates HTTPS.
- Optional Grafana/OpenTelemetry stack for observability demos.

## Local Configuration

Keep real deployment values in an ignored local file:

```text
financial/setup/.env
```

Typical values include:

```bash
K8S_NAMESPACE=financial
PUBLIC_HOSTNAME=oracledev.ai
APP_BASE_PATH=/financial
PUBLIC_BASE_URL=https://oracledev.ai/financial

OCI_REGION=<oci-region>
OCI_COMPARTMENT_OCID=<compartment-ocid>
OCIR_REGISTRY=<region-key>.ocir.io
OCIR_NAMESPACE=<tenancy-namespace>
OCIR_USERNAME=<tenancy-or-oracle-sso-user>
OCIR_AUTH_TOKEN=<auth-token>
IMAGE_REPOSITORY=<ocir-repo-prefix>
IMAGE_TAG=<image-tag>

DB_MODE=existing
ADB_OCID=<autonomous-database-ocid>
DB_SERVICE=financialdb_high
DB_USER=financial
DB_PASSWORD=<database-password>
WALLET_DIR=/path/to/Wallet_financialdb
ORDS_BASE_URL=https://<adb-host>/ords

INGRESS_BASIC_AUTH_ENABLED=true
INGRESS_BASIC_AUTH_SECRET_NAME=financial-basic-auth
INGRESS_BASIC_AUTH_REALM=Financial
INGRESS_BASIC_AUTH_USERNAME=financial
INGRESS_BASIC_AUTH_PASSWORD=<frontend-password>

REACT_APP_AI_AGENTS_BACKEND_URL=/financial/aiagent
REACT_APP_ORDS_BASE_URL=${ORDS_BASE_URL}
GRAFANA_PUBLIC_PATH=/financial/grafana
```

Do not commit `setup/.env`, wallet directories, Terraform state, kubeconfigs,
generated Kubernetes secret manifests, TLS private keys, OCIR tokens, or live IP
addresses that are specific to one operator's deployment.

## Terraform Infrastructure Commands

`financial/infra/terraform` is the intended location for the OKE and optional
Autonomous Database Terraform stack.

Current repository status: a fresh clone does not yet contain committed
Terraform `.tf` files under `financial/infra/terraform`. This workspace may have
ignored local `terraform.tfvars` and `terraform.tfstate` files, but those are
operator-specific and must not be committed. Before another user can provision
from scratch, commit a sanitized Terraform stack plus
`terraform.tfvars.example` with placeholders.

Once the Terraform stack is present, the expected command flow is:

```bash
cd financial/infra/terraform

# One-time local setup from the committed example.
cp terraform.tfvars.example terraform.tfvars
vi terraform.tfvars

# Validate and review before creating or changing cloud resources.
terraform fmt -recursive
terraform init
terraform validate
terraform plan -out=tfplan

# Apply only after reviewing the plan.
terraform apply tfplan

# Capture useful outputs without committing generated files.
terraform output
```

For an existing Autonomous Database, set values like these in the local,
ignored `terraform.tfvars`:

```hcl
create_database                   = false
existing_autonomous_database_ocid = "<autonomous-database-ocid>"
```

For a new Autonomous Database, set `create_database = true` and provide only
placeholder-safe defaults in committed examples. Put the real admin password in
an ignored local file or secret manager, not in git.

After Terraform creates or locates the OKE cluster, configure `kubectl` using
the cluster OCID from Terraform output or the OCI Console:

```bash
oci ce cluster create-kubeconfig \
  --cluster-id "<cluster-ocid>" \
  --file "$HOME/.kube/config" \
  --region "<oci-region>" \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT

kubectl cluster-info
kubectl get nodes
```

Use `PRIVATE_ENDPOINT` instead of `PUBLIC_ENDPOINT` when your workstation can
reach the private control-plane endpoint. Scope public control-plane access to
known operator IP ranges if you use a public endpoint.

## Secrets

The full app needs these Kubernetes secrets when the corresponding services are
enabled:

| Secret | Purpose |
| --- | --- |
| `financialdb-wallet-secret` | Autonomous Database wallet files mounted by most Java/Node services. |
| `dbuser` or a newer `financial-db-credentials` secret | Database password and related database credential keys. Prefer secret refs over literal manifest values. |
| `financial-basic-auth` | nginx ingress Basic Auth htpasswd hash for the public app. |
| OCIR image-pull secret | Pulls private images from OCIR. |
| TLS secret or cert-manager certificate | HTTPS termination for the ingress, if not fully terminated at Cloudflare. |
| Feature-specific secrets | OCI config, AI service settings, Hugging Face tokens, Grafana credentials, or other demo-specific values. |

The current raw manifests still contain some older demo defaults and hardcoded
hostnames. Before using a manifest as part of the reproducible full deployment,
template it so secrets come from Kubernetes Secrets and public URLs come from the
local setup file.

## Frontend Password Storage

The financial frontend password is not stored in React and is not read by the
microservices.

It is an nginx ingress Basic Auth password. The flow is:

```text
browser -> nginx ingress Basic Auth -> React frontend and backend routes
```

Storage locations:

1. Local source of truth:

   ```text
   financial/setup/.env
   ```

   ```bash
   INGRESS_BASIC_AUTH_USERNAME=financial
   INGRESS_BASIC_AUTH_PASSWORD=<frontend-password>
   INGRESS_BASIC_AUTH_SECRET_NAME=financial-basic-auth
   ```

2. Kubernetes Secret:

   ```text
   namespace: financial
   secret: financial-basic-auth
   key: auth
   ```

   The `auth` key stores an htpasswd-formatted value:

   ```text
   financial:<hashed-password>
   ```

3. nginx ingress annotations:

   ```yaml
   nginx.ingress.kubernetes.io/auth-type: basic
   nginx.ingress.kubernetes.io/auth-secret: financial-basic-auth
   nginx.ingress.kubernetes.io/auth-realm: Financial
   ```

The plaintext frontend password should only exist in local operator config or a
secret manager. Kubernetes receives the htpasswd hash.

## Create Or Update The Basic Auth Secret

If the setup scripts are present, use the project helper that reads
`financial/setup/.env` and creates the secret:

```bash
cd financial
setup/create-secrets.sh
```

If applying it manually, use:

```bash
cd financial
set -a
source setup/.env
set +a

mkdir -p /tmp/financial-auth
htpasswd -nbB \
  "${INGRESS_BASIC_AUTH_USERNAME}" \
  "${INGRESS_BASIC_AUTH_PASSWORD}" > /tmp/financial-auth/auth

kubectl create namespace "${K8S_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "${K8S_NAMESPACE}" create secret generic "${INGRESS_BASIC_AUTH_SECRET_NAME}" \
  --from-file=auth=/tmp/financial-auth/auth \
  --dry-run=client -o yaml | kubectl apply -f -

rm -rf /tmp/financial-auth
```

Verify without printing the hash:

```bash
kubectl -n financial get secret financial-basic-auth \
  -o jsonpath='{.metadata.name}{"\n"}{.type}{"\n"}'
```

## Ingress And Route Map

The full public app should use one nginx ingress with Basic Auth annotations
when `INGRESS_BASIC_AUTH_ENABLED=true`.

For `oracledev.ai/financial`, prefer these public paths:

| Public path | Service |
| --- | --- |
| `/financial/` | `react-frontend` |
| `/financial/api` | main Spring Boot backend |
| `/financial/accounts-api` | MicroTx account service |
| `/financial/transfer-api` | MicroTx transfer service |
| `/financial/mern-backend` | MERN/JSON duality backend |
| `/financial/graph` | graph service, when enabled |
| `/financial/truecache` | True Cache service, when enabled |
| `/financial/kafka` | Kafka/TXEventQ service, when enabled |
| `/financial/aiagent` | vector/MCP/AI agents service, when enabled |
| `/financial/selectai` | Select AI service, when enabled |
| `/financial/grafana` | Grafana endpoint, when enabled |

The existing checked-in ingress at `financial/k8s/frontend-react-ingress.yaml`
is an older root-host layout. For the current domain model, rewrite or overlay
it so the host is `oracledev.ai` and all app paths are scoped under
`/financial`.

For path-based hosting, the React build must use the same base path values that
the ingress uses:

```bash
PUBLIC_URL=/financial
REACT_APP_BASE_PATH=/financial
REACT_APP_API_BASE_PATH=/financial/api
REACT_APP_MICROTX_ACCOUNT_SERVICE_URL=/financial/accounts-api
REACT_APP_MICROTX_TRANSFER_SERVICE_URL=/financial/transfer-api
REACT_APP_MERN_BACKEND_SERVICE_URL=/financial/mern-backend
REACT_APP_GRAPH_LAUNDERING_SERVICE_URL=/financial/graph
REACT_APP_TRUECACHE_STOCK_SERVICE_URL=/financial/truecache
REACT_APP_KAFKA_TXEVENTQ_SERVICE_URL=/financial/kafka
REACT_APP_AI_AGENTS_BACKEND_URL=/financial/aiagent
REACT_APP_SPEECH_SELECTAI_QUERY_SERVICE_URL=/financial/selectai
REACT_APP_ORDS_BASE_URL=<ords-base-url>
```

Several current frontend pages still have older hardcoded public URLs. Refactor
those to use the `REACT_APP_*` values before calling the deployment fully
portable.

## DNS And TLS

For Cloudflare in front of the OKE ingress:

- Point `oracledev.ai` or a subdomain to the OCI load balancer address.
- Use Cloudflare proxying if desired, but keep the ingress host and certificate
  names aligned with the hostname sent by the browser.
- Prefer Cloudflare `Full (strict)` mode with a valid origin certificate at the
  ingress, or use cert-manager with a public ACME certificate.
- Do not commit certificate private keys or generated TLS secrets.

## Database Setup

Support both database modes:

- Existing database: use an existing Autonomous Database OCID, service name, and
  wallet path in `financial/setup/.env`.
- New database: provision Autonomous AI Database through Terraform or OCI
  Resource Manager, then write only non-secret outputs to docs and keep
  `terraform.tfvars` and state ignored.

Database setup should be idempotent where possible:

1. Create or confirm the application user.
2. Apply base tables and data from `financial/sql`.
3. Apply optional objects for each feature service, such as graph, spatial,
   True Cache, Kafka/TXEventQ, Select AI, OML, or ORDS.
4. Enable ORDS only if the APIs page or browser demos require it.
5. Record which feature SQL scripts were applied in an ignored local notes file
   or in your deployment run log.

Review SQL before running it against a shared database. Some scripts are
scenario-specific and may reset demo data.

## Build And Deploy

The target automated flow below assumes the Terraform stack and setup scripts
have been committed. Today, treat it as the desired runbook/checklist and use
the existing per-service `build.sh` and `deploy.sh` scripts where a profile
script does not exist yet.

```bash
cd financial

# 1. Load local values.
set -a
source setup/.env
set +a

# 2. Provision or connect to infrastructure.
cd infra/terraform
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
cd ../..

# 3. Configure kubectl for the OKE cluster.
oci ce cluster create-kubeconfig \
  --cluster-id "<cluster-ocid>" \
  --file "$HOME/.kube/config" \
  --region "${OCI_REGION}" \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT

# 4. Authenticate to OCIR if needed.
setup/ocir-login.sh

# 5. Build and push images for the selected deployment profile.
setup/build-images.sh core
setup/build-images.sh features

# 6. Create/update namespace and Kubernetes secrets.
setup/create-secrets.sh

# 7. Install or confirm nginx ingress and optional cert-manager.
setup/install-ingress.sh

# 8. Apply database setup if requested.
setup/setup-database.sh

# 9. Deploy core app and selected feature services.
setup/deploy.sh core
setup/deploy.sh features

# 10. Smoke test.
setup/smoke-test.sh
```

If those setup scripts are not present on your branch, apply the same pieces
manually:

- Namespace.
- Wallet secret.
- Database credential secret.
- Optional OCIR image-pull secret.
- nginx Basic Auth secret.
- TLS certificate or cert-manager configuration.
- React frontend deployment and service.
- Core backend deployments and services.
- Optional feature service deployments and services.
- Ingress with `/financial/...` path routing and Basic Auth annotations.

Do not deploy every lab by default. Build the core app first, then enable
feature services one by one so failures are easy to isolate.

## Suggested Deployment Profiles

Use profiles so another user can reproduce only the parts they need:

| Profile | Includes |
| --- | --- |
| `core` | `react-frontend`, `springboot-backend`, MicroTx account, MicroTx transfer, wallet/DB/basic-auth secrets, nginx ingress. |
| `accounts` | MERN account backend and optional Spring relational account backend. |
| `analytics` | Graph, spatial, OML, globally distributed database, and True Cache demos. |
| `messaging` | Kafka/TXEventQ transactional outbox demos and required support services. |
| `ai` | Select AI, vector/MCP agents, ORDS, and required OCI AI settings. |
| `observability` | Grafana/OpenTelemetry add-ons and service routes. |
| `labs` | Polyglot ATM, GraphQL, MongoDB/Kafka/Postgres support stack, and legacy/alternate samples. |

## Smoke Tests

Unauthenticated requests should be challenged by nginx:

```bash
curl -I https://oracledev.ai/financial/
```

Expected: `401`.

Authenticated requests should reach the app:

```bash
curl -u financial:<frontend-password> -I https://oracledev.ai/financial/
```

Expected: `200` or an HTTP redirect/asset response from the frontend.

Then verify core and enabled feature routes:

```bash
kubectl -n financial get pods
kubectl -n financial get ingress
kubectl -n financial get secret financial-basic-auth \
  -o jsonpath='{.metadata.name}{"\n"}'

curl -u financial:<frontend-password> -I https://oracledev.ai/financial/api/
curl -u financial:<frontend-password> -I https://oracledev.ai/financial/accounts-api/
curl -u financial:<frontend-password> -I https://oracledev.ai/financial/transfer-api/
```

For optional feature services, test only the routes you enabled.

The React app should show the financial UI directly after the Basic Auth gate.
It should not contain a baked-in username/password login form.

## Rotating The Frontend Password

1. Update only the ignored local value:

   ```bash
   INGRESS_BASIC_AUTH_PASSWORD=<new-frontend-password>
   ```

2. Recreate the `financial-basic-auth` Kubernetes Secret with
   `setup/create-secrets.sh` or the manual `htpasswd` command above.

3. Reapply or reload the ingress if needed.

4. Smoke test:

   ```bash
   curl -I https://oracledev.ai/financial/
   curl -u financial:<new-frontend-password> -I https://oracledev.ai/financial/
   ```

No React image rebuild is required when only the ingress Basic Auth password
changes.

## Oracle Skills Repository

The `oracle/skills` repository is useful as operator/developer guidance, but it
is not required at runtime by the financial app.

Recommended options:

- Keep it out of the app image and link to it from docs when only humans need
  it.
- Add it as a git submodule under a clearly named docs or vendor path only if
  reviewers agree that vendoring the content is acceptable.
- Do not copy private local skill config, tokens, or machine-specific MCP
  settings into this repository.

## Security Notes

- Do not put frontend Basic Auth credentials in React source or Kubernetes
  ConfigMaps.
- Do not commit `financial/setup/.env`; it contains OCIR, database, and ingress
  secrets.
- The frontend Basic Auth password protects access to the demo app. It is not a
  database password and is not consumed by backend microservices.
- Replace hardcoded demo passwords and hostnames in older manifests before
  using those manifests for a reproducible public deployment.
- Store production-grade secrets in a managed secret store and generate
  Kubernetes Secrets from that source during deployment.
- Keep Terraform state, `terraform.tfvars`, wallet zip/directories, kubeconfig,
  TLS keys, and generated Kubernetes Secret YAML files out of git.

## Current Gaps To Close

To make this a true full-app one-command setup, the next implementation work is:

- Commit a sanitized Terraform stack under `financial/infra/terraform` and a
  placeholder-only `terraform.tfvars.example`.
- Add committed setup script templates or Kustomize overlays for the profiles
  above.
- Convert raw manifests to use secret refs and image/path placeholders.
- Refactor remaining React hardcoded public URLs to `REACT_APP_*` values.
- Normalize services to `/financial/...` paths under `oracledev.ai`.
- Split feature database setup into idempotent scripts that can be enabled per
  profile.
- Add smoke tests for each profile so a future contributor can verify the full
  app without reverse engineering the folder tree.
