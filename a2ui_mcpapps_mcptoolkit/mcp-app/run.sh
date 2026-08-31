#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

node_is_compatible() {
  "$1" -e '
    const [major, minor] = process.versions.node.split(".").map(Number);
    process.exit(
      (major === 20 && minor >= 19) ||
      (major === 22 && minor >= 12) ||
      major > 22 ? 0 : 1
    );
  ' >/dev/null 2>&1
}

if ! command -v node >/dev/null 2>&1 || ! node_is_compatible "$(command -v node)"; then
  compatible_node_dir=""
  nvm_root="${NVM_DIR:-${HOME}/.nvm}"
  for node_candidate in \
      /opt/homebrew/bin/node \
      /usr/local/bin/node \
      "$nvm_root"/versions/node/*/bin/node; do
    if [ -x "$node_candidate" ] && node_is_compatible "$node_candidate"; then
      compatible_node_dir="$(dirname "$node_candidate")"
      break
    fi
  done
  if [ -n "$compatible_node_dir" ]; then
    export PATH="$compatible_node_dir:$PATH"
  fi
fi

command -v node >/dev/null 2>&1 || { echo "Node.js 20.19+ or 22.12+ is required." >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "npm is required." >&2; exit 1; }

if ! node_is_compatible "$(command -v node)"; then
  echo "Node.js 20.19+ or 22.12+ is required; found $(node --version)." >&2
  exit 1
fi

echo "Installing pinned MCP App dependencies..." >&2
npm install --no-package-lock --no-audit --no-fund
npm run typecheck
npm run build

echo "Starting the MCP App at http://127.0.0.1:3001/mcp" >&2
echo "The governed-data adapter defaults to ${AGENT_SERVICE_URL:-http://127.0.0.1:8080}." >&2
exec npm run serve
