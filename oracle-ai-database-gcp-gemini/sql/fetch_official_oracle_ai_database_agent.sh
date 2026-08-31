#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_REPOSITORY="https://github.com/oracle-devrel/oracle-autonomous-database-samples.git"
SOURCE_COMMIT="7c61fc86ffb7f0f548bdba32ae53ce46ea876fa2"
SOURCE_SUBDIR="google-gemini-marketplace-agents/oracle_ai_database_agent"
DESTINATION="$REPO_ROOT/.runtime/oracle-ai-database-agent-official-${SOURCE_COMMIT:0:12}"

command -v git >/dev/null 2>&1 || {
  echo "git is required." >&2
  exit 1
}
command -v sha256sum >/dev/null 2>&1 || {
  echo "sha256sum is required." >&2
  exit 1
}

if [[ ! -d "$DESTINATION/.git" ]]; then
  if [[ -e "$DESTINATION" ]]; then
    echo "Refusing to overwrite non-repository path: $DESTINATION" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$DESTINATION")"
  git clone --filter=blob:none --no-checkout "$SOURCE_REPOSITORY" "$DESTINATION"
  git -C "$DESTINATION" sparse-checkout init --cone
  git -C "$DESTINATION" sparse-checkout set "$SOURCE_SUBDIR"
  git -C "$DESTINATION" checkout --detach "$SOURCE_COMMIT"
fi

if [[ "$(git -C "$DESTINATION" rev-parse HEAD)" != "$SOURCE_COMMIT" ]]; then
  echo "Existing official-sample checkout is not at the pinned commit." >&2
  exit 1
fi

SOURCE_DIR="$DESTINATION/$SOURCE_SUBDIR"
(
  cd "$SOURCE_DIR"
  printf '%s  %s\n' \
    '206bc7384eb41df08881894130c16f0c19ff393c954255d9c14731ba2f8b4699' \
    'oracle_ai_database_agent.sql' \
    '9b28880ac6fea442b4f343a5504ebd19b9ba39cc47e6f3d093d415e67e83aaa3' \
    'oracle_ai_database_agent_tool.sql' \
    'b884c14f87949d54cc33054f8efe94cd85bd985d02250d865217c749865b23cb' \
    'README.md' | sha256sum -c -
)

echo "$SOURCE_DIR"
