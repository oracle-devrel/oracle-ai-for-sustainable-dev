#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
VENV_DIR="$SCRIPT_DIR/.venv"
VENV_PYTHON="$VENV_DIR/bin/python"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ ! -d "$VENV_DIR" ]]; then
  python3 -m venv "$VENV_DIR"
fi

PIP_TIMEOUT="${PIP_DEFAULT_TIMEOUT:-60}"

"$VENV_PYTHON" -m pip install \
  --disable-pip-version-check \
  --default-timeout "$PIP_TIMEOUT" \
  -r "$SCRIPT_DIR/requirements.txt"

echo
echo "Virtual environment is ready at: $VENV_DIR"
echo "Enter it with: $SCRIPT_DIR/enter_venv.sh"
