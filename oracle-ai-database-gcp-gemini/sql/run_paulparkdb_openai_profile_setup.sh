#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${1:-$REPO_ROOT/.env}"

# shellcheck disable=SC1091
source "$REPO_ROOT/load_env_defaults.sh"
load_env_defaults "$ENV_FILE" \
  DB_ADMIN_USERNAME \
  DB_ADMIN_PASSWORD \
  DB_USERNAME \
  DB_PASSWORD \
  DB_DSN \
  DB_WALLET_DIR \
  TNS_ADMIN \
  SQLCL_PATH \
  OPENAI_API_KEY

SQLCL_PATH="${SQLCL_PATH:-/opt/sqlcl/bin/sql}"
TNS_ADMIN="${TNS_ADMIN:-${DB_WALLET_DIR:-}}"

for name in DB_ADMIN_USERNAME DB_ADMIN_PASSWORD DB_USERNAME DB_PASSWORD DB_DSN OPENAI_API_KEY; do
  if [[ -z "${!name:-}" ]]; then
    echo "$name must be set in $ENV_FILE" >&2
    exit 1
  fi
done
if [[ -z "$TNS_ADMIN" || ! -d "$TNS_ADMIN" ]]; then
  echo "TNS_ADMIN/DB_WALLET_DIR is missing or is not a directory: $TNS_ADMIN" >&2
  exit 1
fi
if [[ ! -x "$SQLCL_PATH" ]]; then
  echo "SQLcl executable not found: $SQLCL_PATH" >&2
  exit 1
fi

export TNS_ADMIN

run_sql_script() {
  local username="$1"
  local password="$2"
  local script="$3"
  local script_argument="${4:-}"

  {
    printf 'set echo off verify off\n'
    printf 'whenever sqlerror exit failure rollback\n'
    printf 'connect %s/"%s"@%s\n' "$username" "$password" "$DB_DSN"
    if [[ -n "$script_argument" ]]; then
      printf '@"%s" "%s"\n' "$script" "$script_argument"
    else
      printf '@"%s"\n' "$script"
    fi
  } | "$SQLCL_PATH" -S /nolog
}

echo "Preparing FINANCIAL grants and provider network ACLs as $DB_ADMIN_USERNAME..."
run_sql_script \
  "$DB_ADMIN_USERNAME" \
  "$DB_ADMIN_PASSWORD" \
  "$SCRIPT_DIR/admin_prepare_paulparkdb_demo.sql"

echo "Creating encrypted FINANCIAL.OPENAI_CRED from standard input..."
run_sql_script \
  "$DB_USERNAME" \
  "$DB_PASSWORD" \
  "$SCRIPT_DIR/create_openai_select_ai_credential.sql" \
  "$OPENAI_API_KEY"

echo "Creating and testing PAULPARK_SUPPLY_CHAIN_OPENAI..."
run_sql_script \
  "$DB_USERNAME" \
  "$DB_PASSWORD" \
  "$SCRIPT_DIR/create_paulparkdb_openai_select_ai_profile.sql"
