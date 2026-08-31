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
: "${DB_PASSWORD2:?Set DB_PASSWORD2 in .env for the APAC end user}"

DB_SCHEMA_OWNER="${DB_SCHEMA_OWNER:-FINANCIAL}"
DB_USERNAME_NA="${DB_USERNAME_NA:-SUPPLYCHAIN_NA_MGR}"
DB_USERNAME_APAC="${DB_USERNAME_APAC:-SUPPLYCHAIN_APAC_MGR}"
DB_PASSWORD_NA="${DB_PASSWORD_NA:-$DB_PASSWORD2}"
DB_PASSWORD_APAC="${DB_PASSWORD_APAC:-$DB_PASSWORD2}"
SQL_BIN="${SQLCL_PATH:-sql}"

for identifier in "$DB_SCHEMA_OWNER" "$DB_USERNAME_NA" "$DB_USERNAME_APAC"; do
  if [[ ! "$identifier" =~ ^[A-Za-z][A-Za-z0-9_]{0,29}$ ]]; then
    echo "Unsafe Oracle identifier: $identifier" >&2
    exit 1
  fi
done

for password in "$DB_ADMIN_PASSWORD" "$DB_PASSWORD_NA" "$DB_PASSWORD_APAC"; do
  if [[ "$password" == *'"'* || "$password" == *'&'* ]]; then
    echo 'Demo passwords must not contain double quotes or ampersands.' >&2
    exit 1
  fi
done

export TNS_ADMIN

echo "Installing regional inventory data and Deep Data Security policies..."
"$SQL_BIN" -s /nolog <<SQL
connect ${DB_ADMIN_USERNAME}/"${DB_ADMIN_PASSWORD}"@${DB_DSN}
@${SCRIPT_DIR}/setup_inventory_risk_deepsec_regions.sql \
  ${DB_SCHEMA_OWNER} ${DB_USERNAME_NA} ${DB_PASSWORD_NA} \
  ${DB_USERNAME_APAC} ${DB_PASSWORD_APAC}
SQL

echo "Verifying region isolation with both end-user identities..."
"$SQL_BIN" -s /nolog <<SQL
@${SCRIPT_DIR}/verify_inventory_risk_deepsec_regions.sql \
  ${DB_SCHEMA_OWNER} ${DB_USERNAME_NA} ${DB_PASSWORD_NA} \
  ${DB_USERNAME_APAC} ${DB_PASSWORD_APAC} ${DB_DSN}
SQL

echo "Verified: ${DB_USERNAME_NA} sees only NA and ${DB_USERNAME_APAC} sees only APAC."
