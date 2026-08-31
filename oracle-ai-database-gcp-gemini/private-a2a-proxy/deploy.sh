#!/usr/bin/env bash

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-adb-pm-prod}"
REGION="${REGION:-us-east4}"
SERVICE_NAME="${SERVICE_NAME:-oracle-private-a2a-relay}"
REPOSITORY="${REPOSITORY:-a2ui-agents}"
IMAGE_TAG="${IMAGE_TAG:-$(date -u +%Y%m%d%H%M%S)}"
PRIVATE_HOST_PREFIX="${PRIVATE_HOST_PREFIX:-gy5twnrd}"
OCI_REGION="${OCI_REGION:-us-ashburn-1}"
NETWORK="${NETWORK:-default}"
SUBNET="${SUBNET:-default}"
SERVICE_ACCOUNT="${SERVICE_ACCOUNT:-oracle-private-a2a-relay@${PROJECT_ID}.iam.gserviceaccount.com}"

: "${DB_OCID:?Set DB_OCID to the Autonomous AI Database OCID.}"

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/${SERVICE_NAME}:${IMAGE_TAG}"
PRIVATE_URL="https://${PRIVATE_HOST_PREFIX}.adb.${OCI_REGION}.oraclecloudapps.com/adb/a2a/v1/databases/${DB_OCID}/agents/oracle_ai_database_agent"

gcloud builds submit \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --tag="${IMAGE}" \
  .

gcloud run deploy "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --platform=managed \
  --image="${IMAGE}" \
  --service-account="${SERVICE_ACCOUNT}" \
  --network="${NETWORK}" \
  --subnet="${SUBNET}" \
  --vpc-egress=private-ranges-only \
  --ingress=all \
  --allow-unauthenticated \
  --set-env-vars="PRIVATE_ORACLE_A2A_URL=${PRIVATE_URL},PUBLIC_A2A_URL=https://pending.invalid" \
  --min-instances=0 \
  --max-instances=2 \
  --memory=512Mi \
  --cpu=1 \
  --timeout=180 \
  --concurrency=20 \
  --quiet

SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --format='value(status.url)')"

gcloud run services update "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --update-env-vars="PUBLIC_A2A_URL=${SERVICE_URL}" \
  --quiet

printf 'Relay deployed: %s\n' "${SERVICE_URL}"
