# Financial OCI Infrastructure

This Terraform scaffold is for the OCI infrastructure around the financial
microservices demo.

It can:

- create an OKE cluster using Oracle's public OKE Terraform module;
- optionally create a new Autonomous AI Database;
- output the values needed by the Kubernetes deployment scripts.

It does not run database schema setup and does not deploy the application.

Terraform `1.5.0` or newer is required. This directory includes a
`.terraform-version` pin for `tfenv`; run `tfenv install` from this directory if
your local Terraform version is older.

## Quick Start

```bash
cd financial/infra/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`. By default this scaffold reads credentials from
`~/.oci/config` using `config_file_profile = "DEFAULT"`.

For a public OKE control plane, set `control_plane_allowed_cidrs` to the
workstation or VPN CIDR allowed to reach the Kubernetes API on port `6443`.
Prefer a narrow `/32` for local work, for example:

```hcl
control_plane_allowed_cidrs = ["203.0.113.10/32"]
```

For the pragmatic nginx ingress path, use:

```hcl
enable_cert_manager                      = true
enable_native_ingress_controller         = false
create_native_ingress_controller_policy  = false
```

The Kustomize ingress manifests under `financial/k8s` default to
`ingressClassName: nginx`.

The native ingress add-on also needs IAM permissions. This scaffold creates a
workload-identity policy for the controller service account when
`create_native_ingress_controller_policy = true`. OCI documents those
permissions as separate policy statements; if the tenancy is near the policy
statement limit, Terraform can fail with
`PolicyStatementsLimitPerCompartmentChainExceeded`. In that case, free policy
statement capacity, request a limit increase, provide equivalent pre-existing
permissions and set `create_native_ingress_controller_policy = false`, or keep
using nginx ingress.

Then run:

```bash
tfenv install # optional, only when using tfenv
terraform init
TF_LOG=ERROR terraform validate
terraform plan
terraform apply
```

On this macOS environment, plain `terraform validate` can emit a misleading
provider-schema startup error even though the providers load correctly. Running
with `TF_LOG=ERROR` reaches the real validation path and currently succeeds.

If `create_cluster = true`, create kubeconfig with OCI CLI:

```bash
oci ce cluster create-kubeconfig \
  --cluster-id "$(terraform output -raw cluster_id)" \
  --file ~/.kube/config-financial \
  --region <region> \
  --token-version 2.0.0 \
  --kube-endpoint PUBLIC_ENDPOINT
export KUBECONFIG=~/.kube/config-financial
kubectl get nodes
```

You can also use the sensitive `cluster_kubeconfig` Terraform output, but avoid
printing it into shared logs.

Install or upgrade ingress-nginx after kubeconfig is available:

```bash
cd ../../
KUBECONFIG=~/.kube/config-financial setup/install-nginx-ingress.sh
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

Record the nginx controller load balancer IP in the ignored
`financial/setup/.env` as `INGRESS_EXTERNAL_IP`.

If `create_database = false`, set `existing_autonomous_database_ocid` and use
an existing wallet with the scripts under `financial/setup`.
