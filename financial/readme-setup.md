# Financial Application OKE Redeployment Plan

This is a planning document for rebuilding the `financial` application on a new
OKE cluster. It does not replace `README.md`; it captures the recommended setup
shape, the choices to expose to a new user, and the current repo issues to fix
before making the deployment fully repeatable.

## Current Scaffold Status

This setup scaffold now exists:

- `infra/terraform/`: Terraform wrapper for OKE plus optional Autonomous AI
  Database creation.
- `k8s/base/`: Kustomize base for the core services.
- `k8s/overlays/existing-db/`: overlay placeholder for reusing an existing DB.
- `k8s/overlays/full-create/`: overlay placeholder for full new environments.
- `setup/env.example`: copy to `setup/.env` for local deployment settings.
- `setup/build-images.sh`: builds and pushes the core images.
- `setup/create-secrets.sh`: creates the wallet and DB credential secrets.
- `setup/install-nginx-ingress.sh`: installs or upgrades ingress-nginx.
- `setup/setup-database.sh`: optional SQL setup runner.
- `setup/deploy.sh`: applies the generated Kustomize deployment.
- `setup/smoke-test.sh`: basic endpoint checks.
- `sql/setup-admin.sql`: parameterized admin/user setup alternative to the
  older hardcoded `sql/admin.sql`.

This is still a scaffold, not a fully tested one-click installer. The biggest
remaining app cleanup is converting hardcoded frontend/backend hostnames to
runtime configuration.

## Current Working Context

Last updated: 2026-06-22.

This document is the handoff point for future work. A later Codex session should
start here, then inspect the files under `infra/terraform`, `k8s`, `setup`, and
`sql`.

Current local/tooling facts:

- OCI CLI profile used for discovery: `DEFAULT`.
- Target region, compartment, OCIR namespace, cluster OCID, node pool OCID,
  public Kubernetes endpoint, ingress load balancer IP, database OCID, wallet
  path, OCIR username, OCIR auth token, and database password are stored in
  ignored local files, not in this checked-in document.
- OKE versions were checked during setup; choose a currently-supported OKE
  version when recreating the environment.
- Terraform is pinned in `infra/terraform/.terraform-version` to `1.13.5`.
- `TF_LOG=ERROR terraform validate` succeeds from `infra/terraform`. Plain
  `terraform validate` currently emits a misleading provider-schema startup
  error in this local shell.
- An ignored `infra/terraform/terraform.tfvars` exists locally with the
  chosen profile, compartment, region, public control plane access CIDR, OCI
  VCN-native pod networking, and database selection.
- An ignored `setup/.env` exists locally with deployment settings such as image
  registry, host, app base path, wallet path, OCIR login values, ingress
  external IP, and application database password. Do not commit it.
- Terraform apply completed for the OKE side. Two managed worker nodes are
  `Ready` in the current local deployment.
- CertManager is installed as an OKE managed add-on and is `ACTIVE`; its pods
  are running in the `cert-manager` namespace.
- Terraform now keeps the OCI Native Ingress Controller disabled because the
  tenancy hit `PolicyStatementsLimitPerCompartmentChainExceeded` while creating
  its least-privilege workload-identity IAM policy. This is intentional for the
  pragmatic path: use nginx ingress and avoid the OCI native ingress IAM policy.
- `ingress-nginx` is installed with Helm in namespace `ingress-nginx`.
  The controller service is a Kubernetes `LoadBalancer`; record its external IP
  in ignored `setup/.env` as `INGRESS_EXTERNAL_IP`.
- The Kustomize base and setup scripts now default to `ingressClassName: nginx`.
- Kubernetes secrets were created in namespace `financial` for the database
  wallet, application database credentials, and OCIR image pulls.
- Temporary kubeconfig for this session was written to
  `/private/tmp/financial-kubeconfig`.
- Podman Desktop is working after the update. The four core images build and
  push successfully for `linux/amd64` to the configured OCIR registry.
- OCIR login works with the Oracle SSO identity-domain format for this tenancy;
  the concrete username and auth token are kept only in ignored `setup/.env`.
- The core Kustomize deployment has been applied in namespace `financial`.
  `react-frontend`, `springboot-backend`, `microtx-account`, and
  `microtx-transfer` are all `1/1 Running`.

Current OCI discovery:

- The local deployment reuses an existing Autonomous Database. Its exact OCID
  and wallet path are kept in ignored local config.
- Database creation is disabled locally with `create_database=false`; OKE
  creation is enabled with `create_cluster=true`.

Do not commit local secrets. Keep `setup/.env`, `terraform.tfvars`, wallets,
TLS keys, and generated Kubernetes secrets ignored.

## Goals

- Provision or reuse an Oracle Autonomous AI Database.
- Provision or reuse an OKE cluster.
- Build and push the important microservice images.
- Deploy the React frontend and backend services with one predictable hostname.
- Keep database passwords and wallet files out of source-controlled manifests.
- Allow a user to choose whether database creation and database setup should run.
- Make the workflow simple enough for another developer to repeat.

## What This Directory Contains

The most important pieces for the current app are:

- `react-frontend/`: Create React App frontend served by nginx.
- `springboot-backend/`: the main Spring Boot backend for `/financial/**`, graph,
  Kafka/TxEventQ, stock, spatial, and related endpoints.
- `microtx-saga-lockless-bank-transfer/account/`: account service used by ATM
  and MicroTx flows.
- `microtx-saga-lockless-bank-transfer/transfer/`: transfer service used by
  MicroTx saga/LRA flows.
- `mongodb-mern-bank-account/mern-stack/`: optional MongoDB API / JSON duality
  backend.
- `sql/`: database setup scripts for the `FINANCIAL` schema and optional
  features.
- `k8s/`: shared ingress and helper scripts.

Historically, the deployment approach was mostly raw Kubernetes YAML plus
per-service shell scripts. The new scaffold adds Terraform under
`infra/terraform` and Kustomize under `k8s/`.

## Important Findings From The Scan

The React frontend needs public browser-reachable API paths. The setup now uses
one host with a base path, so the financial app can live at
`https://oracledev.ai/financial` while other apps can use sibling paths such as
`/digitaltwins`.

Because React runs in the user's browser, it cannot call Kubernetes `ClusterIP`
services directly. With the current code, a registered hostname is effectively
required unless the frontend is refactored to use relative paths everywhere.

The clean target is a single public host with path-based routing:

```text
https://oracledev.ai/financial
https://oracledev.ai/financial/api
https://oracledev.ai/financial/accounts-api
https://oracledev.ai/financial/transfer-api
```

The new Kustomize base uses `oracledev.ai` and `/financial/...` paths. Older
single-service manifests still exist for historical demos, but the repeatable
deployment path is the Kustomize base plus `setup/deploy.sh`.

The repo currently has hardcoded database values in several places:

- `DB_USER=financial`
- `DB_PASSWORD=<legacy-demo-password>`
- `jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/oraclefinancial/creds`

The setup should replace these with Kubernetes `Secret` and `ConfigMap` values.

There are a few manifest inconsistencies to clean up before automating:

- Some deployments declare `containerPort: 443` even though the apps listen on
  `8080` or `8081`.
- `transfer/Dockerfile` exposes `8080`, but the transfer app runs on `8081`.
- The MERN backend app defaults to `PORT=5001`, while its service manifests use
  `5000`.
- Some generated deployment YAML files are committed next to their templates.
- Some older optional demos have their own legacy manifests. Keep them out of
  the primary redeployment path until their ports, image names, and routes are
  aligned with the Kustomize setup.

## Recommended Approach

Use Terraform for OCI infrastructure and Kustomize for Kubernetes application
configuration.

Terraform is a good fit for:

- VCN and subnets.
- OKE cluster and node pool.
- OCIR repositories.
- Optional Autonomous AI Database creation.
- Optional DNS record.
- Optional OCI Vault secrets.

Kustomize is a good fit for:

- One base deployment layout for the app.
- Environment-specific overlays such as `dev`, `demo`, or `existing-db`.
- Image tag substitutions.
- Hostname substitutions.
- ConfigMap and Secret references.

Helm is still useful for third-party or platform components, such as ingress
controllers, cert-manager, external-dns, or MicroTx if using an official chart.
For this app's own services, Kustomize is simpler than writing a chart first.

## Proposed Repo Shape

The target shape is:

```text
financial/
  readme-setup.md
  setup/
    env.example
    setup.sh
    build-images.sh
    deploy.sh
    smoke-test.sh
  infra/
    terraform/
      main.tf
      variables.tf
      outputs.tf
      terraform.tfvars.example
  k8s/
    base/
      namespace.yaml
      configmap.yaml
      secrets.example.yaml
      ingress.yaml
      react-frontend.yaml
      springboot-backend.yaml
      microtx-account.yaml
      microtx-transfer.yaml
      kustomization.yaml
    overlays/
      dev/
      existing-db/
      full-create/
  sql/
    setup_financial.sql
    setup_core.sql
    setup_optional_features.sql
```

Keep real `.env`, wallet directories, generated secrets, and generated manifests
out of git.

## Setup Modes

The workflow should expose these switches:

```bash
CREATE_CLUSTER=true
CREATE_DATABASE=true
SETUP_DATABASE=true
DEPLOY_MICROTX=true
DEPLOY_OPTIONAL_SERVICES=false
```

Recommended mode combinations:

| Mode | CREATE_CLUSTER | CREATE_DATABASE | SETUP_DATABASE | Use Case |
| --- | --- | --- | --- | --- |
| Full demo | true | true | true | New environment from scratch |
| Existing database | true | false | true | Recreate OKE but reuse ADB |
| Existing initialized database | true | false | false | Recreate only cluster/app |
| App redeploy only | false | false | false | Existing OKE and database |

## Infrastructure Plan

1. Collect inputs in `financial/setup/env.example`.

   ```bash
   OCI_REGION=eu-frankfurt-1
   OCI_COMPARTMENT_OCID=ocid1.compartment...
   OKE_CLUSTER_NAME=financial-demo
   K8S_NAMESPACE=financial
   PUBLIC_HOSTNAME=oracledev.ai
   APP_BASE_PATH=/financial
   DOCKER_REGISTRY=fra.ocir.io/<tenancy-namespace>/financial
   DB_NAME=financialdb
   DB_SERVICE=financialdb_high
   DB_USER=financial
   DB_PASSWORD=<secret>
   WALLET_DIR=/path/to/Wallet_financialdb
   SETUP_DATABASE=true
   CREATE_DATABASE=false
   EXISTING_AUTONOMOUS_DATABASE_OCID=ocid1.autonomousdatabase...
   ```

   For public OKE control planes, also set
   `control_plane_allowed_cidrs` in `infra/terraform/terraform.tfvars` to the
   workstation or VPN CIDR that should reach the Kubernetes API, for example
   `["203.0.113.10/32"]`. Avoid `0.0.0.0/0` unless this is a throwaway demo.
   For this deployment, leave `enable_native_ingress_controller=false` and
   `create_native_ingress_controller_policy=false`, then install nginx ingress
   with `setup/install-nginx-ingress.sh`.

2. Use Terraform to create or discover OCI resources.

   - Create VCN/subnets/security rules when `CREATE_CLUSTER=true`.
   - Create an enhanced OKE cluster with managed AMD64 node pools.
   - Enable or install an ingress controller.
   - Create OCIR repositories for the app images.
   - Optionally create Autonomous AI Database when `CREATE_DATABASE=true`.
   - Output kubeconfig command, OCIR registry, database OCID, and public load
     balancer details.

   The scaffold is in `infra/terraform`:

   ```bash
   cd financial/infra/terraform
   tfenv install # optional, uses infra/terraform/.terraform-version
   cp terraform.tfvars.example terraform.tfvars
   # edit terraform.tfvars
   terraform init
   TF_LOG=ERROR terraform validate
   terraform plan
   terraform apply
   ```

   For this deployment, the cluster has already been created in Frankfurt. To
   recreate kubeconfig without exposing the sensitive Terraform output:

   ```bash
  oci ce cluster create-kubeconfig \
    --cluster-id "$OKE_CLUSTER_OCID" \
     --file ~/.kube/config-financial \
     --region eu-frankfurt-1 \
     --token-version 2.0.0 \
     --kube-endpoint PUBLIC_ENDPOINT
   export KUBECONFIG=~/.kube/config-financial
   kubectl get nodes
   ```

3. Use managed nodes first.

   The existing Dockerfiles build `linux/amd64` images and the services are
   ordinary Java/Node/nginx containers. Managed OKE nodes are the least
   surprising starting point. Virtual nodes can be evaluated later after the
   base deployment is stable.

4. Use nginx ingress.

   The pragmatic path for this tenancy is ingress-nginx. It avoids the OCI
   Native Ingress Controller IAM policy statement limit, works with standard
   Kubernetes `Ingress` resources, and exposes one OCI load balancer through the
   nginx controller `Service`.

   Install or upgrade it after kubeconfig is available:

   ```bash
   cd financial
   KUBECONFIG=/path/to/config-financial setup/install-nginx-ingress.sh
   kubectl -n ingress-nginx get svc ingress-nginx-controller
   ```

   Record the current financial ingress target IP in ignored `setup/.env` as
   `INGRESS_EXTERNAL_IP`.

## Database Plan

The setup should support both new and existing Autonomous Database.

### New Database

When `CREATE_DATABASE=true`:

1. Terraform creates an Autonomous AI Database.
2. Download or generate the wallet.
3. Create the application user.
4. Run the core setup SQL.
5. Run optional SQL for selected features.

### Existing Database

When `CREATE_DATABASE=false`:

1. Require database OCID or connection details.
2. Require wallet path or wallet secret.
3. Require `DB_SERVICE`, usually `financialdb_high`.
4. If `SETUP_DATABASE=true`, run the setup SQL.
5. If `SETUP_DATABASE=false`, only create Kubernetes secrets/config.

### SQL Setup Order

The current scripts should be wrapped in an idempotent setup script. Current
recommended ordering:

1. `sql/admin.sql` as ADMIN or privileged user.
2. `sql/financial.sql` as FINANCIAL or privileged user.
3. `sql/accounts_inserts.sql` as FINANCIAL.
4. Optional MicroTx reservation setup from `sql/lockfree-reservation.sql`.
5. Optional graph setup from `sql/graph.sql`.
6. Optional Kafka/TxEventQ setup from `sql/kafka-messaging.sql`.
7. Optional MongoDB API / JSON duality setup from
   `sql/mongodb-jsonduality.sql`.
8. Optional ORDS setup from `sql/ords-APIs.sql`.
9. Optional stock setup from `sql/truecache-stock.sql`.

Before automating this, `admin.sql` should be made parameterized and safer:

- Do not hardcode the legacy demo database password.
- Do not assume the user does not exist.
- Do not grant to `testuser` unless that user is part of the selected feature.
- Keep feature grants behind feature flags.

The scaffold adds `sql/setup-admin.sql`, which accepts the app username and
password as arguments and avoids the hardcoded legacy demo user creation
path:

```bash
cd financial
cp setup/env.example setup/.env
# edit setup/.env, set SETUP_DATABASE=true and DB_* connection values
setup/setup-database.sh
```

The setup runner currently executes:

1. `sql/setup-admin.sql`
2. `sql/financial.sql`
3. `sql/accounts_inserts.sql`
4. selected optional SQL scripts based on `RUN_*_SETUP` flags

Some optional SQL files are not yet fully idempotent, so run database setup
against disposable/demo schemas first.

## Kubernetes Plan

Create namespaces:

```bash
kubectl create namespace financial
kubectl create namespace otmm
```

Create a wallet secret:

```bash
kubectl create secret generic financialdb-wallet-secret \
  --namespace financial \
  --from-file="$WALLET_DIR"
```

Create database credentials:

```bash
kubectl create secret generic financialdb-credentials \
  --namespace financial \
  --from-literal=username="$DB_USER" \
  --from-literal=password="$DB_PASSWORD"
```

Create a shared ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: financial-config
  namespace: financial
data:
  DB_URL: jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/oraclefinancial/creds
  DB_USER: financial
  PUBLIC_BASE_URL: https://oracledev.ai/financial
```

The app deployments should consume:

- `DB_URL` from ConfigMap.
- `DB_USER` from ConfigMap or Secret.
- `DB_PASSWORD` from Secret.
- wallet files mounted at `/oraclefinancial/creds`.

With the new scaffold:

```bash
cd financial
cp setup/env.example setup/.env
# edit setup/.env
setup/create-secrets.sh
```

## MicroTx Plan

The account and transfer apps currently expect:

```text
http://otmm-tcs.otmm:9000/api/v1/lra-coordinator
```

So deploy the MicroTx coordinator into namespace `otmm` first. Then deploy:

- `account` service in namespace `financial` on port `8080`.
- `transfer` service in namespace `financial` on port `8081`.

The participant URLs in the app config currently use:

```text
http://account.financial:8080
http://transfer.financial:8081
```

Those are valid Kubernetes DNS names for services in the `financial` namespace.

## Frontend And Hostname Plan

Under the current code, yes, a registered hostname is needed for the deployed
browser app unless the frontend URLs are refactored.

Why:

- The browser cannot call Kubernetes `ClusterIP` service names.
- Create React App bakes `REACT_APP_*` values at build time.

Recommended final shape:

- Serve React and all APIs from the same hostname.
- Use path-based ingress routing.
- Convert frontend API calls to relative paths.
- Avoid rebuilding React just to change the hostname by adding a runtime
  `/env.js` generated by nginx or mounted ConfigMap.

Interim shape without frontend refactor:

- Build React with path-based `REACT_APP_*` values, for example
  `/financial/api`, `/financial/accounts-api`, and `/financial/transfer-api`.

The current `setup/build-images.sh` passes those path values into the React
build. The React router is also configured with `PUBLIC_URL=/financial` and
`REACT_APP_BASE_PATH=/financial`.

Ingress routes should look like:

```text
/financial               -> react-frontend:80
/financial/api           -> globallydistributeddatabase:8080
/financial/accounts-api  -> account:8080
/financial/transfer-api  -> transfer:8081
/financial/mern-backend  -> mern-backend:5000 or 5001, after port cleanup
/financial/grafana       -> grafana, optional
```

Use TLS for the public host. Options:

- OCI Load Balancer certificate.
- cert-manager with Let's Encrypt.
- Existing certificate imported as a Kubernetes TLS secret.

## Image Build Plan

The important images are:

```text
financial/react-frontend
financial/springboot-backend
financial/microtx-account
financial/microtx-transfer
```

Build and push with the new script:

```bash
cd financial
cp setup/env.example setup/.env
# edit setup/.env; set DOCKER_REGISTRY, TAG, PUBLIC_HOSTNAME, DB values, and OCIR values
setup/ocir-login.sh
setup/build-images.sh
```

For OCIR, the username is usually `<tenancy-namespace>/<oci-username>`. For
federated identity, Oracle documents the format as
`<tenancy-namespace>/<domain-name>/<username>`. For Oracle Identity Cloud
Service federation this commonly looks like
`<tenancy-namespace>/oracleidentitycloudservice/<username>`, while some tenancies
use an Oracle SSO domain such as `<tenancy-namespace>/oracle-sso/<username>`.
Use an OCI auth token as the password, not the Console password.
`setup/ocir-login.sh` reads these values from the ignored `setup/.env` and
passes the token via stdin.
`setup/create-secrets.sh` also uses those values to create `ocir-pull-secret`
and attach it to the namespace's default service account for private image
pulls.

Deploy the Kustomize base with the same image tag:

```bash
setup/create-secrets.sh
setup/deploy.sh
```

Preview the generated Kubernetes YAML without applying it:

```bash
DRY_RUN=true setup/deploy.sh
```

The deploy script creates a temporary Kustomize overlay that patches:

- image registry and tag;
- ingress class;
- public hostname;
- `DB_URL`;
- `DB_USER`;
- MicroTx coordinator URL.

The old per-service build/deploy scripts still exist, but the new scripts are
the intended path for repeatable redeployment.

Validation note: the Terraform module/provider download succeeded after
bypassing the broken `www-proxy.us.oracle.com` Git proxy in the local Git
config. Terraform is pinned to `1.13.5` in `infra/terraform/.terraform-version`,
and `TF_LOG=ERROR terraform validate` currently succeeds.

Current deployment checks:

- `kubectl get nodes -o wide` works through the public endpoint after adding a
  narrow workstation/VPN CIDR locally.
- `kubectl get pods -A` shows kube-system pods and CertManager pods running.
- OCI managed add-ons show CertManager, CoreDNS, KubeProxy,
  NodeProblemDetector, NvidiaGpuPlugin, ObservabilityAgent, and OciVcnIpNative
  as `ACTIVE`.
- `kubectl get ingressclass` now shows `nginx`.
- `kubectl -n ingress-nginx get svc ingress-nginx-controller` shows an external
  IP. Store it in ignored `setup/.env` as `INGRESS_EXTERNAL_IP`.
- `npm run build` succeeds for `react-frontend` with `PUBLIC_URL=/financial`
  and path-based API env vars.
- `setup/build-images.sh` succeeds with Podman and pushes the four core images
  to OCIR.
- `setup/create-secrets.sh` creates `financialdb-credentials`,
  `financialdb-wallet-secret`, `ocir-pull-secret`, and patches the default
  service account with the image pull secret.
- `setup/deploy.sh` applies the core Kustomize app successfully.
- `mvn -DskipTests package` succeeds for `springboot-backend`, MicroTx
  `account`, MicroTx `transfer`, `graph-circular-payments`,
  `true-cache-stock-ticker`, and `kafka-sql-inventory`.
- `kubectl kustomize financial/k8s/base` renders successfully.
- `bash -n` succeeds for the setup/helper scripts touched during this pass.
- Smoke checks through nginx using `Host: $PUBLIC_HOSTNAME` and
  `$INGRESS_EXTERNAL_IP` succeed:
  `/financial/` returns `200`, `/financial/api/test` returns `200` with body
  `test`, and `/financial/accounts-api/accounts` returns `200` JSON from ADB.
- `/financial` now redirects with `Location: /financial/`, without leaking the
  container's internal port.

## Smoke Test Plan

After deployment:

```bash
kubectl -n financial get pods
kubectl -n financial get svc
kubectl -n financial get ingress
```

Backend checks:

```bash
curl https://oracledev.ai/financial/api/test
curl https://oracledev.ai/financial/api/accounts
curl https://oracledev.ai/financial/accounts-api/accounts
```

MicroTx checks:

```bash
curl https://oracledev.ai/financial/accounts-api/accounts
curl -X POST "https://oracledev.ai/financial/transfer-api/transfer?fromAccount=497&toAccount=500&amount=1&sagaAction=commit&useLockFreeReservations=true&crashSimulation=false"
```

Frontend check:

```text
https://oracledev.ai/financial
```

The current frontend uses demo-only client-side login logic. Do not treat it as
real authentication for a public deployment.

Or use the script:

```bash
cd financial
setup/smoke-test.sh
```

## Remaining After App Deployment

1. Configure Cloudflare for `oracledev.ai/financial*` to reach the financial
   ingress IP stored as `INGRESS_EXTERNAL_IP` in ignored `setup/.env`. Plain DNS
   cannot route by URL path; this needs Cloudflare proxy/routing behavior in
   front of the origin. A separate
   `/digitaltwins*` route can point to a different origin IP.
2. Install/deploy the MicroTx coordinator in namespace `otmm` if the transfer
   flows are required.
3. Decide whether the MERN backend runs on `5000` or `5001`, then align the app,
   deployment, and service.

## Oracle Skills

The Oracle skills repository is here:

```text
https://github.com/oracle/skills
```

The repo describes root-level domains such as `db`, `oci`, `graal`, `apex`, and
`fusion`. For this project, the useful domains are:

- `db`: Autonomous Database, SQL, ORDS, JDBC, schema, and database guidance.
- `oci`: OKE, OCI infrastructure, Autonomous Database on OCI, and cloud setup
  guidance.

I installed these two domains for Codex on this machine:

```text
/Users/pparkins/.codex/skills/db
/Users/pparkins/.codex/skills/oci
```

Restart Codex to pick up newly installed skills.

For someone else using the generic skills CLI, the Oracle repo documents:

```bash
npx skills add oracle/skills/db
npx skills add oracle/skills/oci
```

For Claude Code, the repo documents the plugin marketplace flow:

```text
/plugin marketplace add oracle/skills
/plugin install db@oracle-skills
/plugin install oci@oracle-skills
```

These skills should not be vendored into this application repo. They are
agent/developer tooling, not application runtime dependencies.

## Reference Links

- Oracle Skills: https://github.com/oracle/skills
- OKE documentation: https://docs.oracle.com/en-us/iaas/Content/ContEng/home.htm
- OCI native ingress controller:
  https://docs.oracle.com/en-us/iaas/Content/ContEng/Tasks/contengsettingupnativeingresscontroller.htm
- ingress-nginx deployment guide:
  https://kubernetes.github.io/ingress-nginx/deploy/
- OCI Resource Manager:
  https://docs.oracle.com/en-us/iaas/Content/ResourceManager/home.htm
- OCI Container Registry image push and login:
  https://docs.oracle.com/en-us/iaas/Content/Registry/Tasks/registrypushingimagesusingthedockercli.htm
- OCI Terraform provider, Autonomous Database resource:
  https://github.com/oracle/terraform-provider-oci/blob/master/website/docs/r/database_autonomous_database.html.markdown
- OCI Terraform provider, OKE cluster resource:
  https://raw.githubusercontent.com/oracle/terraform-provider-oci/master/website/docs/r/containerengine_cluster.html.markdown
