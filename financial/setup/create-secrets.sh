#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

K8S_NAMESPACE="${K8S_NAMESPACE:-financial}"
DB_USER="${DB_USER:-financial}"
DB_PASSWORD="${DB_PASSWORD:?Set DB_PASSWORD in ${ENV_FILE} or the environment}"
WALLET_DIR="${WALLET_DIR:?Set WALLET_DIR in ${ENV_FILE} or the environment}"
OCIR_REGISTRY="${OCIR_REGISTRY:-${DOCKER_REGISTRY%%/*}}"
OCIR_PULL_SECRET_NAME="${OCIR_PULL_SECRET_NAME:-ocir-pull-secret}"
INGRESS_BASIC_AUTH_ENABLED="${INGRESS_BASIC_AUTH_ENABLED:-false}"
INGRESS_BASIC_AUTH_SECRET_NAME="${INGRESS_BASIC_AUTH_SECRET_NAME:-financial-basic-auth}"
INGRESS_BASIC_AUTH_USERNAME="${INGRESS_BASIC_AUTH_USERNAME:-financial}"

kubectl create namespace "${K8S_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic financialdb-credentials \
  --namespace "${K8S_NAMESPACE}" \
  --from-literal=username="${DB_USER}" \
  --from-literal=password="${DB_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic financialdb-wallet-secret \
  --namespace "${K8S_NAMESPACE}" \
  --from-file="${WALLET_DIR}" \
  --dry-run=client -o yaml | kubectl apply -f -

if [[ -n "${OCIR_USERNAME:-}" && -n "${OCIR_AUTH_TOKEN:-}" ]]; then
  kubectl create secret docker-registry "${OCIR_PULL_SECRET_NAME}" \
    --namespace "${K8S_NAMESPACE}" \
    --docker-server="${OCIR_REGISTRY}" \
    --docker-username="${OCIR_USERNAME}" \
    --docker-password="${OCIR_AUTH_TOKEN}" \
    --docker-email="${OCI_USERNAME:-${OCIR_USERNAME}}" \
    --dry-run=client -o yaml | kubectl apply -f -

  kubectl patch serviceaccount default \
    --namespace "${K8S_NAMESPACE}" \
    --type merge \
    --patch "{\"imagePullSecrets\":[{\"name\":\"${OCIR_PULL_SECRET_NAME}\"}]}"
fi

if [[ "${INGRESS_BASIC_AUTH_ENABLED}" == "true" ]]; then
  : "${INGRESS_BASIC_AUTH_PASSWORD:?Set INGRESS_BASIC_AUTH_PASSWORD in ${ENV_FILE}}"

  AUTH_FILE="$(mktemp)"
  trap 'rm -f "${AUTH_FILE}"' EXIT

  if command -v htpasswd >/dev/null 2>&1; then
    htpasswd -nbB "${INGRESS_BASIC_AUTH_USERNAME}" "${INGRESS_BASIC_AUTH_PASSWORD}" > "${AUTH_FILE}"
  elif command -v openssl >/dev/null 2>&1; then
    HASH="$(openssl passwd -apr1 "${INGRESS_BASIC_AUTH_PASSWORD}")"
    printf '%s:%s\n' "${INGRESS_BASIC_AUTH_USERNAME}" "${HASH}" > "${AUTH_FILE}"
  else
    echo "Install htpasswd or openssl to create nginx basic auth credentials." >&2
    exit 1
  fi

  kubectl create secret generic "${INGRESS_BASIC_AUTH_SECRET_NAME}" \
    --namespace "${K8S_NAMESPACE}" \
    --from-file=auth="${AUTH_FILE}" \
    --dry-run=client -o yaml | kubectl apply -f -
fi

echo "Created or updated database, wallet, optional OCIR, and optional ingress auth secrets in namespace ${K8S_NAMESPACE}."
