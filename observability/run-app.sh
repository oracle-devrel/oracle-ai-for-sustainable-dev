#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${SCRIPT_DIR}/springboot-oracle-db-otel-demo"
ENV_FILE="${OBSERVABILITY_ENV_FILE:-${SCRIPT_DIR}/.env}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${OTEL_SEMCONV_STABILITY_OPT_IN:=database/dup}"
: "${OTLP_TRACES_ENDPOINT:=http://localhost:4318/v1/traces}"
export OTEL_SEMCONV_STABILITY_OPT_IN OTLP_TRACES_ENDPOINT

if [ -z "${DB_URL:-}" ] || [ -z "${DB_USERNAME:-}" ] || [ -z "${DB_PASSWORD:-}" ]; then
  cat >&2 <<'EOF'
Missing required database settings.

Create observability/.env, or export these variables before running:

  DB_URL='jdbc:oracle:thin:@//host:1521/service'
  DB_USERNAME='"claims-investigator-agent"'
  DB_PASSWORD='<local-deep-sec-end-user-password>'
  OTLP_TRACES_ENDPOINT='http://localhost:4318/v1/traces'

EOF
  exit 1
fi

cd "$APP_DIR"
exec mvn spring-boot:run
