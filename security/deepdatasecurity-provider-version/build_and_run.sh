#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

ENV_FILE="${DEEPSEC_ENV_FILE:-.env}"
if [[ ! -f "${ENV_FILE}" && -f "../deepdatasecurity-api-version/.env" ]]; then
  ENV_FILE="../deepdatasecurity-api-version/.env"
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "No .env file found. Create security/deepdatasecurity-provider-version/.env or set DEEPSEC_ENV_FILE." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

export DEEPSEC_PORT="${DEEPSEC_PORT:-18080}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-entraid}"
export DEEPSEC_BROWSER_MODE="${DEEPSEC_BROWSER_MODE:-true}"

if [[ "${DEEPSEC_ENTRA_DATABASE_SCOPE:-}" == */user_impersonation ]]; then
  export DEEPSEC_ENTRA_DATABASE_SCOPE="${DEEPSEC_ENTRA_DATABASE_SCOPE%/user_impersonation}/.default"
fi

if [[ "${DEEPSEC_BROWSER_MODE}" != "true" && -n "${DEEPSEC_ENTRA_TENANT_ID:-}" ]]; then
  export DEEPSEC_ENTRA_JWT_ISSUER_URI="${DEEPSEC_ENTRA_JWT_ISSUER_URI:-https://sts.windows.net/${DEEPSEC_ENTRA_TENANT_ID}/}"
  export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI="${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI:-https://login.microsoftonline.com/${DEEPSEC_ENTRA_TENANT_ID}/discovery/keys}"
fi

APP_SCOPE="$(
  printf '%s' "${DEEPSEC_ENTRA_APPLICATION_SCOPE:-}" |
    tr ',' '\n' |
    sed 's/^ *//; s/ *$//' |
    awk '/^api:\/\// {print; exit}'
)"

echo "Building deepdatasecurity-provider-version..."
mvn test

cat <<EOF

Starting deepdatasecurity-provider-version on http://localhost:${DEEPSEC_PORT}
Spring profile: ${SPRING_PROFILES_ACTIVE}

Open this in a browser:

  http://localhost:${DEEPSEC_PORT}/

The browser page signs in with Entra when SPRING_PROFILES_ACTIVE=entraid, obtains an access token, and calls the protected endpoints with Authorization: Bearer.
For SPRING_PROFILES_ACTIVE=oci-iam, use curl or another client that can supply an OCI IAM bearer token.
If sign-in fails with a redirect/client error, add http://localhost:${DEEPSEC_PORT} as a SPA redirect URI on the Entra app registration used by DEEPSEC_ENTRA_BROWSER_CLIENT_ID.

Optional CLI bearer-token test:

  cd ${SCRIPT_DIR}
  set -a
  source ${ENV_FILE}
  set +a
  export DEEPSEC_BROWSER_MODE=false
  APP_SCOPE="${APP_SCOPE}"
  az login --tenant "\$DEEPSEC_ENTRA_TENANT_ID" --allow-no-subscriptions
  TOKEN="\$(az account get-access-token --tenant "\$DEEPSEC_ENTRA_TENANT_ID" --scope "\$APP_SCOPE" --query accessToken -o tsv)"
  curl http://localhost:${DEEPSEC_PORT}/deepsec/health
  curl -i http://localhost:${DEEPSEC_PORT}/deepsec/query
  curl -H "Authorization: Bearer \$TOKEN" http://localhost:${DEEPSEC_PORT}/deepsec/whoami
  curl -H "Authorization: Bearer \$TOKEN" http://localhost:${DEEPSEC_PORT}/deepsec/query

EOF

mvn spring-boot:run
