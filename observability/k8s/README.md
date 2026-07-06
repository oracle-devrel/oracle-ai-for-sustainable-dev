# OKE Jaeger Endpoint

This manifest deploys Jaeger into the existing `financial` namespace and exposes
OTLP HTTP through the existing nginx ingress for the financial demo.

## Endpoint

Use this endpoint when the database or app must send traces to the OKE-hosted
collector:

```text
https://oracledev.ai/financial/otel/v1/traces
```

The ingress path is `/financial/otel/...` so it can use the same Cloudflare
routing as the financial app. Inside the cluster nginx rewrites that path to
Jaeger's OTLP HTTP endpoint, `/v1/traces`.

## Deploy

Use a kubeconfig for the financial OKE cluster:

```bash
kubectl apply -f observability/k8s/oke-jaeger.yaml
kubectl -n financial rollout status deployment/jaeger
```

## Verify

Check that Jaeger is running:

```bash
kubectl -n financial get deploy,svc,ingress jaeger jaeger-otlp-http
```

Check that the public OTLP path reaches Jaeger. An empty request should return
an error from the collector rather than a 404 from nginx:

```bash
curl -i -X POST https://oracledev.ai/financial/otel/v1/traces \
  -H 'Content-Type: application/json' \
  --data '{}'
```

Open the Jaeger UI with a local port-forward:

```bash
kubectl -n financial port-forward svc/jaeger 16686:16686
```

Then open:

```text
http://localhost:16686
```

## Spring Boot App

Run the demo app with the OKE endpoint:

```bash
export OTLP_TRACES_ENDPOINT='https://oracledev.ai/financial/otel/v1/traces'
```

The database-side exporter must also be configured to use the same URL through
`DBMS_OBSERVABILITY`.
