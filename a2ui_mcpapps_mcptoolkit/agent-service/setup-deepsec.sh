#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# shellcheck disable=SC1091
source ./load-database-env.sh

if [ -z "${DB_ADMIN_PASSWORD:-}" ]; then
  shared_env="../../oracle-ai-database-gcp-gemini/.env"
  if [ -f "$shared_env" ]; then
    admin_username="$(
      unset DB_ADMIN_USERNAME
      # shellcheck disable=SC1090
      source "$shared_env"
      printf '%s' "${DB_ADMIN_USERNAME:-ADMIN}"
    )"
    admin_password="$(
      unset DB_ADMIN_PASSWORD
      # shellcheck disable=SC1090
      source "$shared_env"
      printf '%s' "${DB_ADMIN_PASSWORD:-}"
    )"
    export DB_ADMIN_USERNAME="${DB_ADMIN_USERNAME:-$admin_username}"
    export DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-$admin_password}"
  fi
fi

: "${DB_USERNAME2:?Set DB_USERNAME2 in ../.env}"
: "${DB_PASSWORD2:?Set DB_PASSWORD2 in ../.env}"
: "${DB_ADMIN_PASSWORD:?Set DB_ADMIN_PASSWORD in ../.env or oracle-ai-database-gcp-gemini/.env}"

if command -v mvn >/dev/null 2>&1; then
  maven_command="mvn"
elif [ -x /opt/homebrew/opt/maven/bin/mvn ]; then
  maven_command="/opt/homebrew/opt/maven/bin/mvn"
else
  echo "Maven 3.9+ is required." >&2
  exit 1
fi

"$maven_command" -q -DskipTests package
java -cp target/interactive-ai-agent-service-0.1.0-SNAPSHOT.jar \
  com.oracle.demo.interactiveai.DeepSecSetup
