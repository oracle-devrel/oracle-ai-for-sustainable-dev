#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is required but was not found on PATH."
  exit 1
fi

cd "$SCRIPT_DIR"

echo "Building Oracle Graph Agent..."
exec mvn clean package
