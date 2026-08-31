#!/usr/bin/env bash
set -euo pipefail

# Deploy the MCP App server to Cloud Run for ChatGPT or another remote MCP host.
# Supply project-specific values through environment variables, never source control.
: "${PROJECT_ID:?Set PROJECT_ID to the Google Cloud project ID}"
: "${DATABASE_SERVICE_NAME:?Set DATABASE_SERVICE_NAME to the Oracle Net service name}"
: "${DATABASE_USERNAME:?Set DATABASE_USERNAME to the least-privileged application schema}"
: "${WALLET_SECRET:?Set WALLET_SECRET to the Secret Manager wallet archive name}"
: "${DATABASE_PASSWORD_SECRET:?Set DATABASE_PASSWORD_SECRET to the password secret name}"

REGION="${REGION:-us-east4}"
SERVICE_NAME="${SERVICE_NAME:-oracle-supply-chain-mcp-app}"
REPOSITORY="${REPOSITORY:-a2ui-agents}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-a2ui-mcp-runner}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
ENABLE_WRITE_ACTIONS="${ENABLE_WRITE_ACTIONS:-false}"
ALLOW_UNAUTHENTICATED="${ALLOW_UNAUTHENTICATED:-false}"

if [[ "$ENABLE_WRITE_ACTIONS" != "true" && "$ENABLE_WRITE_ACTIONS" != "false" ]]; then
  echo "ENABLE_WRITE_ACTIONS must be true or false." >&2
  exit 2
fi
if [[ "$ALLOW_UNAUTHENTICATED" != "true" && "$ALLOW_UNAUTHENTICATED" != "false" ]]; then
  echo "ALLOW_UNAUTHENTICATED must be true or false." >&2
  exit 2
fi
if [[ "$ENABLE_WRITE_ACTIONS" == "true" && "$ALLOW_UNAUTHENTICATED" == "true" ]]; then
  echo "Refusing to expose Oracle-backed write actions anonymously. Configure OAuth first." >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/$SERVICE_NAME:latest"
SERVICE_ACCOUNT_EMAIL="$RUNTIME_SERVICE_ACCOUNT@$PROJECT_ID.iam.gserviceaccount.com"
RUNTIME_ENV="DB_SERVICE_NAME=$DATABASE_SERVICE_NAME,DB_USERNAME=$DATABASE_USERNAME,AGENT_PORT=8081,AGENT_SERVICE_URL=http://127.0.0.1:8081,MCP_BIND_HOST=0.0.0.0,MCP_WRITES_ENABLED=$ENABLE_WRITE_ACTIONS"

gcloud config set project "$PROJECT_ID"
gcloud artifacts repositories describe "$REPOSITORY" --project="$PROJECT_ID" --location="$REGION" >/dev/null
gcloud iam service-accounts describe "$SERVICE_ACCOUNT_EMAIL" --project="$PROJECT_ID" >/dev/null

for secret in "$WALLET_SECRET" "$DATABASE_PASSWORD_SECRET"; do
  gcloud secrets versions describe latest --secret="$secret" --project="$PROJECT_ID" >/dev/null
  gcloud secrets add-iam-policy-binding "$secret" \
    --project="$PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
    --role=roles/secretmanager.secretAccessor >/dev/null
done

gcloud builds submit "$PROJECT_ROOT" \
  --project="$PROJECT_ID" \
  --config="$PROJECT_ROOT/deploy/gcp/cloudbuild.mcp-app.yaml" \
  --substitutions="_IMAGE=$IMAGE"

access_flag="--no-allow-unauthenticated"
if [[ "$ALLOW_UNAUTHENTICATED" == "true" ]]; then
  access_flag="--allow-unauthenticated"
fi

gcloud run deploy "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --platform=managed \
  --image="$IMAGE" \
  --service-account="$SERVICE_ACCOUNT_EMAIL" \
  --cpu=2 \
  --memory=2Gi \
  --concurrency=10 \
  --min-instances="$MIN_INSTANCES" \
  --max-instances=1 \
  --timeout=300 \
  --port=8080 \
  --network=default \
  --subnet=default \
  --vpc-egress=private-ranges-only \
  --set-env-vars="$RUNTIME_ENV" \
  --set-secrets="/var/run/secrets/oracle-wallet/wallet.zip=$WALLET_SECRET:latest,DB_PASSWORD=$DATABASE_PASSWORD_SECRET:latest" \
  "$access_flag"

service_uri="$(gcloud run services describe "$SERVICE_NAME" \
  --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')"
echo "Cloud Run MCP service: $service_uri"
echo "Health endpoint: $service_uri/health"
echo "Remote MCP endpoint: $service_uri/mcp"

