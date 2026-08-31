#!/usr/bin/env bash

project_env="../.env"
financial_env="../../financial/setup/.env"

if [ -f "$project_env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$project_env"
  set +a
elif [ -f "$financial_env" ]; then
  set -a
  # Reuse the ignored financial demo credentials without copying the secret.
  # shellcheck disable=SC1091
  source "$financial_env"
  set +a
  export DB_USERNAME="${DB_USERNAME:-${DB_USER:-FINANCIAL}}"
  export TNS_ADMIN="${TNS_ADMIN:-${WALLET_DIR:-}}"
  # The financial deployment URL contains its container-only wallet path.
  unset DB_URL
fi

# Optionally reuse credentials from another ignored local environment file
# while keeping all non-secret connection and application settings here.
if [ -n "${DB_CREDENTIALS_ENV:-}" ] && [ -f "$DB_CREDENTIALS_ENV" ]; then
  shared_database_password="$(
    unset DB_PASSWORD
    # shellcheck disable=SC1090
    source "$DB_CREDENTIALS_ENV"
    printf '%s' "${DB_PASSWORD:-}"
  )"
  if [ -n "$shared_database_password" ]; then
    export DB_PASSWORD="$shared_database_password"
  fi
  shared_database_username2="$(
    unset DB_USERNAME2
    # shellcheck disable=SC1090
    source "$DB_CREDENTIALS_ENV"
    printf '%s' "${DB_USERNAME2:-}"
  )"
  shared_database_password2="$(
    unset DB_PASSWORD2
    # shellcheck disable=SC1090
    source "$DB_CREDENTIALS_ENV"
    printf '%s' "${DB_PASSWORD2:-}"
  )"
  if [ -n "$shared_database_username2" ]; then
    export DB_USERNAME2="$shared_database_username2"
  fi
  if [ -n "$shared_database_password2" ]; then
    export DB_PASSWORD2="$shared_database_password2"
  fi
fi

export DB_PASSWORD2="${DB_PASSWORD2:-${DB_PASSWORD:-}}"

# The MCP Toolkit child needs a JDBC URL that points at the local wallet.
# DB_SERVICE_NAME makes the runtime portable across Autonomous Databases while
# retaining the financial demo as the local default.
if [ -z "${DB_URL:-}" ] && [ -n "${TNS_ADMIN:-}" ]; then
  export DB_URL="jdbc:oracle:thin:@${DB_SERVICE_NAME:-financialdb_high}?TNS_ADMIN=${TNS_ADMIN}"
fi
