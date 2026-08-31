#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${1:-$REPO_ROOT/.env}"

# shellcheck disable=SC1091
source "$REPO_ROOT/load_env_defaults.sh"
load_env_defaults "$ENV_FILE" \
  DB_USERNAME \
  DB_PASSWORD \
  DB_DSN \
  DB_WALLET_DIR \
  TNS_ADMIN \
  SQLCL_PATH \
  DB_SCHEMA_OWNER \
  INVENTORY_SCHEMA_OWNER \
  SELECT_AI_OBJECT_OWNER

SQLCL_PATH="${SQLCL_PATH:-/opt/sqlcl/bin/sql}"
TNS_ADMIN="${TNS_ADMIN:-${DB_WALLET_DIR:-}}"
DEMO_OWNER="${INVENTORY_SCHEMA_OWNER:-${SELECT_AI_OBJECT_OWNER:-${DB_SCHEMA_OWNER:-${DB_USERNAME:-}}}}"

if [[ -z "${DB_USERNAME:-}" || -z "${DB_PASSWORD:-}" || -z "${DB_DSN:-}" ]]; then
  echo "DB_USERNAME, DB_PASSWORD, and DB_DSN must be set in $ENV_FILE" >&2
  exit 1
fi
if [[ -z "$TNS_ADMIN" || ! -d "$TNS_ADMIN" ]]; then
  echo "TNS_ADMIN/DB_WALLET_DIR is missing or is not a directory: $TNS_ADMIN" >&2
  exit 1
fi
if [[ ! -x "$SQLCL_PATH" ]]; then
  echo "SQLcl executable not found: $SQLCL_PATH" >&2
  exit 1
fi
if [[ ! "$DEMO_OWNER" =~ ^[A-Za-z][A-Za-z0-9_$#]{0,127}$ ]]; then
  echo "Invalid DEMO_OWNER derived from the environment." >&2
  exit 1
fi

export TNS_ADMIN
exec "$SQLCL_PATH" -S "${DB_USERNAME}/${DB_PASSWORD}@${DB_DSN}" \
  @"$SCRIPT_DIR/audit_paulparkdb_demo.sql" "${DEMO_OWNER^^}"
