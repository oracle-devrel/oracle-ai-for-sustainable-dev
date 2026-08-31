#!/usr/bin/env bash
set -euo pipefail

# Deploy a private, read-only MCP App server for a Gemini Enterprise connector.
: "${PROJECT_ID:?Set PROJECT_ID to the Google Cloud project ID}"
: "${DATABASE_SERVICE_NAME:?Set DATABASE_SERVICE_NAME to the Oracle Net service name}"
: "${DATABASE_USERNAME:?Set DATABASE_USERNAME to the least-privileged application schema}"
: "${WALLET_SECRET:?Set WALLET_SECRET to the Secret Manager wallet archive name}"
: "${DATABASE_PASSWORD_SECRET:?Set DATABASE_PASSWORD_SECRET to the password secret name}"

REGION="${REGION:-us-east4}"
SERVICE_NAME="${SERVICE_NAME:-oracle-supply-chain-mcp-gemini}"
REPOSITORY="${REPOSITORY:-a2ui-agents}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-a2ui-mcp-runner}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/$SERVICE_NAME:latest"
SERVICE_ACCOUNT_EMAIL="$RUNTIME_SERVICE_ACCOUNT@$PROJECT_ID.iam.gserviceaccount.com"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
DISCOVERY_ENGINE_SERVICE_AGENT="service-$PROJECT_NUMBER@gcp-sa-discoveryengine.iam.gserviceaccount.com"
RUNTIME_ENV="DB_SERVICE_NAME=$DATABASE_SERVICE_NAME,DB_USERNAME=$DATABASE_USERNAME,AGENT_PORT=8081,AGENT_SERVICE_URL=http://127.0.0.1:8081,MCP_BIND_HOST=0.0.0.0,MCP_WRITES_ENABLED=false"

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
  --no-allow-unauthenticated

if gcloud run services get-iam-policy "$SERVICE_NAME" \
    --project="$PROJECT_ID" --region="$REGION" \
    --flatten='bindings[].members' \
    --filter='bindings.role:roles/run.invoker AND bindings.members:allUsers' \
    --format='value(bindings.members)' | grep -q 'allUsers'; then
  gcloud run services remove-iam-policy-binding "$SERVICE_NAME" \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --member=allUsers \
    --role=roles/run.invoker >/dev/null
fi

gcloud run services add-iam-policy-binding "$SERVICE_NAME" \
  --project="$PROJECT_ID" \
  --region="$REGION" \
  --member="serviceAccount:$DISCOVERY_ENGINE_SERVICE_AGENT" \
  --role=roles/run.invoker >/dev/null

service_uri="$(gcloud run services describe "$SERVICE_NAME" \
  --project="$PROJECT_ID" --region="$REGION" --format='value(status.url)')"
echo "Gemini Enterprise private MCP service: $service_uri"
echo "Gemini Enterprise MCP endpoint: $service_uri/mcp"
echo "Discovery Engine invoker: $DISCOVERY_ENGINE_SERVICE_AGENT"
echo "Writes are disabled; only the dashboard tool is registered."
