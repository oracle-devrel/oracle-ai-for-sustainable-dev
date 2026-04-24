#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
VENV_DIR="$SCRIPT_DIR/.venv"
VENV_PYTHON="$VENV_DIR/bin/python"
SHELL_PATH="${SHELL:-/bin/bash}"

if [[ ! -x "$VENV_PYTHON" ]]; then
  echo "No virtual environment found at $VENV_DIR"
  echo "Run $SCRIPT_DIR/setup_venv.sh first."
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

export VIRTUAL_ENV="$VENV_DIR"
export PATH="$VENV_DIR/bin:$PATH"
export PORT="${ACTION_AGENT_PORT:-${PORT:-8080}}"
unset PYTHONHOME || true

hash -r 2>/dev/null || true

cd "$SCRIPT_DIR"

if [[ "$SHELL_PATH" == *"zsh" ]]; then
  SHELL_ARGS=(-dfi)
else
  SHELL_PATH="/bin/bash"
  SHELL_ARGS=(--noprofile --norc -i)
fi

echo "Activated virtual environment for $SCRIPT_DIR"
echo "Python: $(command -v python)"
echo "Port: ${PORT:-8080}"
echo "Type 'exit' to leave it."

exec "$SHELL_PATH" "${SHELL_ARGS[@]}"
