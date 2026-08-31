#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing ignored environment file: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${DB_ADMIN_USERNAME:?Set DB_ADMIN_USERNAME in .env}"
: "${DB_ADMIN_PASSWORD:?Set DB_ADMIN_PASSWORD in .env}"
: "${DB_DSN:?Set DB_DSN in .env}"
: "${TNS_ADMIN:?Set TNS_ADMIN in .env}"
: "${DB_PASSWORD_NA:?Set DB_PASSWORD_NA in .env}"
: "${DB_PASSWORD_APAC:?Set DB_PASSWORD_APAC in .env}"

DB_SCHEMA_OWNER="${DB_SCHEMA_OWNER:-FINANCIAL}"
DB_USERNAME_NA="${DB_USERNAME_NA:-SUPPLYCHAIN_NA_MGR}"
DB_USERNAME_APAC="${DB_USERNAME_APAC:-SUPPLYCHAIN_APAC_MGR}"
SELECT_AI_CREDENTIAL="${SUPPLY_CHAIN_MANAGER_SELECT_AI_CREDENTIAL:-OPENAI_CRED}"
SELECT_AI_PROFILE="${SUPPLY_CHAIN_MANAGER_SELECT_AI_PROFILE:-PAULPARK_SUPPLY_CHAIN_OPENAI}"
SQL_BIN="${SQLCL_PATH:-sql}"

if [[ ! -x "$SQL_BIN" ]] && ! command -v "$SQL_BIN" >/dev/null 2>&1; then
  echo "SQLcl executable not found: $SQL_BIN" >&2
  exit 1
fi

for identifier in \
  "$DB_SCHEMA_OWNER" "$DB_USERNAME_NA" "$DB_USERNAME_APAC" \
  "$SELECT_AI_CREDENTIAL" "$SELECT_AI_PROFILE"; do
  if [[ ! "$identifier" =~ ^[A-Za-z][A-Za-z0-9_]{0,127}$ ]]; then
    echo "Unsafe Oracle identifier: $identifier" >&2
    exit 1
  fi
done

for secret in \
  "$DB_ADMIN_PASSWORD" "$DB_PASSWORD_NA" "$DB_PASSWORD_APAC"; do
  if [[ "$secret" == *'"'* || "$secret" == *'&'* || "$secret" == *"'"* ]]; then
    echo "Passwords and API keys must not contain quotes or ampersands for this SQLcl runner." >&2
    exit 1
  fi
done

export TNS_ADMIN

echo "Replacing Deep Sec end users with ordinary schema users..."
"$SQL_BIN" -s /nolog <<SQL
connect ${DB_ADMIN_USERNAME}/"${DB_ADMIN_PASSWORD}"@${DB_DSN}
@${SCRIPT_DIR}/migrate_supply_chain_managers_to_schema_users.sql \
  ${DB_SCHEMA_OWNER} ${DB_USERNAME_NA} ${DB_PASSWORD_NA} \
  ${DB_USERNAME_APAC} ${DB_PASSWORD_APAC}
SQL

configure_profile() {
  local username="$1"
  local password="$2"

  echo "Configuring Select AI profile for ${username}..."
  "$SQL_BIN" -s /nolog <<SQL
set echo off verify off
connect ${username}/"${password}"@${DB_DSN}
@${SCRIPT_DIR}/configure_supply_chain_manager_select_ai.sql \
  ${DB_SCHEMA_OWNER} ${SELECT_AI_CREDENTIAL} ${SELECT_AI_PROFILE}
SQL
}

configure_profile "$DB_USERNAME_NA" "$DB_PASSWORD_NA"
configure_profile "$DB_USERNAME_APAC" "$DB_PASSWORD_APAC"

OFFICIAL_DIR="$($SCRIPT_DIR/fetch_official_oracle_ai_database_agent.sh | tail -n 1)"
OFFICIAL_TOOL="$OFFICIAL_DIR/oracle_ai_database_agent_tool.sql"
OFFICIAL_AGENT="$OFFICIAL_DIR/oracle_ai_database_agent.sql"

install_agent() {
  local username="$1"

  echo "Installing Oracle AI Database Agent tools and team in ${username}..."
  # Oracle's two official installers terminate their SQLcl sessions, so invoke
  # them separately instead of placing both in one SQLcl input stream.
  "$SQL_BIN" -s /nolog <<SQL
set echo off verify off
connect ${DB_ADMIN_USERNAME}/"${DB_ADMIN_PASSWORD}"@${DB_DSN}
define SCHEMA_NAME = '${username}'
@${OFFICIAL_TOOL}
SQL

  "$SQL_BIN" -s /nolog <<SQL
set echo off verify off
connect ${DB_ADMIN_USERNAME}/"${DB_ADMIN_PASSWORD}"@${DB_DSN}
define SCHEMA_NAME = '${username}'
define AI_PROFILE_NAME = '${SELECT_AI_PROFILE}'
@${OFFICIAL_AGENT}
SQL
}

install_agent "$DB_USERNAME_NA"
install_agent "$DB_USERNAME_APAC"

echo "Schema-user and Select AI agent setup complete."
