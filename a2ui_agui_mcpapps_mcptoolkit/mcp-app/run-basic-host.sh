#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

ext_apps_commit="2ca6a59d2f493b227a83a2e3ce0396db4705621a"
checkout_root="../.runtime/ext-apps-basic-host"
compatibility_patch="$(pwd)/patches/basic-host-sandbox-static.patch"

command -v node >/dev/null 2>&1 || { echo "Node.js 20+ is required." >&2; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "npm is required." >&2; exit 1; }

if [ ! -d "$checkout_root/.git" ]; then
  echo "Downloading the pinned official MCP Apps basic host..." >&2
  if ! git clone --quiet https://github.com/modelcontextprotocol/ext-apps.git "$checkout_root"; then
    echo "Configured Git proxy failed; retrying GitHub directly." >&2
    rmdir "$checkout_root" 2>/dev/null || true
    git -c http.proxy= -c https.proxy= clone --quiet \
      https://github.com/modelcontextprotocol/ext-apps.git "$checkout_root"
  fi
fi

if [ "$(git -C "$checkout_root" rev-parse HEAD)" != "$ext_apps_commit" ]; then
  git -C "$checkout_root" fetch --quiet origin "$ext_apps_commit" \
    || git -c http.proxy= -c https.proxy= -C "$checkout_root" \
      fetch --quiet origin "$ext_apps_commit"
  git -C "$checkout_root" checkout --quiet --detach "$ext_apps_commit"
fi

if ! git -C "$checkout_root" apply --reverse --check \
    "$compatibility_patch" >/dev/null 2>&1; then
  git -C "$checkout_root" apply --check "$compatibility_patch"
  git -C "$checkout_root" apply "$compatibility_patch"
fi

if [ ! -d "$checkout_root/node_modules" ]; then
  echo "Installing the official basic-host dependencies..." >&2
  npm --prefix "$checkout_root" ci
fi

echo "Building the pinned official MCP Apps basic host..." >&2
npm --prefix "$checkout_root" run build \
  --workspace @modelcontextprotocol/ext-apps-basic-host

echo "Opening the MCP Apps basic host at http://127.0.0.1:8082" >&2
echo "The isolated sandbox is served separately at http://127.0.0.1:8081" >&2

# The pinned example's package script uses Bun. Express sendFile can return
# NotFoundError for the generated sandbox under that runtime on macOS. Run the
# same official TypeScript server with its installed tsx/Node toolchain.
cd "$checkout_root/examples/basic-host"
HOST_PORT=8082 \
SANDBOX_PORT=8081 \
SERVERS='["http://127.0.0.1:3001/mcp"]' \
  exec ../../node_modules/.bin/tsx serve.ts
