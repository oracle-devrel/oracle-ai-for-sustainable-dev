#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

INGRESS_NGINX_NAMESPACE="${INGRESS_NGINX_NAMESPACE:-ingress-nginx}"
INGRESS_NGINX_RELEASE="${INGRESS_NGINX_RELEASE:-ingress-nginx}"
INGRESS_NGINX_REPLICAS="${INGRESS_NGINX_REPLICAS:-2}"

helm upgrade --install "${INGRESS_NGINX_RELEASE}" ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace "${INGRESS_NGINX_NAMESPACE}" \
  --create-namespace \
  --set "controller.replicaCount=${INGRESS_NGINX_REPLICAS}" \
  --set "controller.config.use-forwarded-headers=true"

kubectl wait --namespace "${INGRESS_NGINX_NAMESPACE}" \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

kubectl get service ingress-nginx-controller --namespace "${INGRESS_NGINX_NAMESPACE}"
