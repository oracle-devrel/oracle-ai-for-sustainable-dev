#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FINANCIAL_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

K8S_NAMESPACE="${K8S_NAMESPACE:-financial}"
DOCKER_REGISTRY="${DOCKER_REGISTRY:?Set DOCKER_REGISTRY in ${ENV_FILE} or the environment}"
TAG="${TAG:?Set TAG to the image tag to deploy}"
PUBLIC_HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
APP_BASE_PATH="${APP_BASE_PATH:-/financial}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-https://${PUBLIC_HOSTNAME}${APP_BASE_PATH}}"
INGRESS_CLASS_NAME="${INGRESS_CLASS_NAME:-nginx}"
INGRESS_BASIC_AUTH_ENABLED="${INGRESS_BASIC_AUTH_ENABLED:-false}"
INGRESS_BASIC_AUTH_SECRET_NAME="${INGRESS_BASIC_AUTH_SECRET_NAME:-financial-basic-auth}"
INGRESS_BASIC_AUTH_REALM="${INGRESS_BASIC_AUTH_REALM:-Financial}"
DB_SERVICE="${DB_SERVICE:-financialdb_high}"
DB_USER="${DB_USER:-financial}"
DB_URL="${DB_URL:-jdbc:oracle:thin:@${DB_SERVICE}?TNS_ADMIN=/oraclefinancial/creds}"
OTMM_COORDINATOR_URL="${OTMM_COORDINATOR_URL:-http://otmm-tcs.otmm:9000/api/v1/lra-coordinator}"

REACT_FRONTEND_IMAGE_NAME="${REACT_FRONTEND_IMAGE_NAME:-react-frontend}"
SPRINGBOOT_BACKEND_IMAGE_NAME="${SPRINGBOOT_BACKEND_IMAGE_NAME:-springboot-backend}"
MICROTX_ACCOUNT_IMAGE_NAME="${MICROTX_ACCOUNT_IMAGE_NAME:-microtx-account}"
MICROTX_TRANSFER_IMAGE_NAME="${MICROTX_TRANSFER_IMAGE_NAME:-microtx-transfer}"

TMP_ROOT="${FINANCIAL_DIR}/.tmp"
mkdir -p "${TMP_ROOT}"
TMP_DIR="$(mktemp -d "${TMP_ROOT}/kustomize.XXXXXX")"
trap 'rm -rf "${TMP_DIR}"' EXIT

cat > "${TMP_DIR}/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../../k8s/base
namespace: ${K8S_NAMESPACE}
images:
  - name: financial/react-frontend
    newName: ${DOCKER_REGISTRY}/${REACT_FRONTEND_IMAGE_NAME}
    newTag: ${TAG}
  - name: financial/springboot-backend
    newName: ${DOCKER_REGISTRY}/${SPRINGBOOT_BACKEND_IMAGE_NAME}
    newTag: ${TAG}
  - name: financial/microtx-account
    newName: ${DOCKER_REGISTRY}/${MICROTX_ACCOUNT_IMAGE_NAME}
    newTag: ${TAG}
  - name: financial/microtx-transfer
    newName: ${DOCKER_REGISTRY}/${MICROTX_TRANSFER_IMAGE_NAME}
    newTag: ${TAG}
patches:
  - target:
      kind: Namespace
      name: financial
    patch: |-
      - op: replace
        path: /metadata/name
        value: "${K8S_NAMESPACE}"
  - target:
      kind: Ingress
      name: financial
    patch: |-
      - op: replace
        path: /spec/ingressClassName
        value: "${INGRESS_CLASS_NAME}"
      - op: replace
        path: /spec/rules/0/host
        value: "${PUBLIC_HOSTNAME}"
  - target:
      kind: ConfigMap
      name: financial-config
    patch: |-
      - op: replace
        path: /data/DB_URL
        value: "${DB_URL}"
      - op: replace
        path: /data/DB_USER
        value: "${DB_USER}"
      - op: replace
        path: /data/PUBLIC_BASE_URL
        value: "${PUBLIC_BASE_URL}"
      - op: replace
        path: /data/OTMM_COORDINATOR_URL
        value: "${OTMM_COORDINATOR_URL}"
EOF

if [[ "${INGRESS_BASIC_AUTH_ENABLED}" == "true" ]]; then
  cat >> "${TMP_DIR}/kustomization.yaml" <<EOF
  - target:
      kind: Ingress
      name: financial
    patch: |-
      - op: add
        path: /metadata/annotations
        value:
          nginx.ingress.kubernetes.io/auth-type: "basic"
          nginx.ingress.kubernetes.io/auth-secret: "${INGRESS_BASIC_AUTH_SECRET_NAME}"
          nginx.ingress.kubernetes.io/auth-realm: "${INGRESS_BASIC_AUTH_REALM}"
EOF
fi

if [[ "${DRY_RUN:-false}" == "true" ]]; then
  kubectl kustomize "${TMP_DIR}"
  exit 0
fi

kubectl create namespace "${K8S_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -k "${TMP_DIR}"

cat <<EOF

Applied financial core services.

Namespace: ${K8S_NAMESPACE}
Host:      ${PUBLIC_HOSTNAME}
Images:    ${DOCKER_REGISTRY}/*:${TAG}
Auth:      ${INGRESS_BASIC_AUTH_ENABLED}

Next:
  kubectl -n ${K8S_NAMESPACE} get pods
  kubectl -n ${K8S_NAMESPACE} get ingress
EOF
