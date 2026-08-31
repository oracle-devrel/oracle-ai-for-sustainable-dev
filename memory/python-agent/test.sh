#!/usr/bin/env bash
set -euo pipefail

PYTHON_APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MEMORY_DIR="$(cd -- "${PYTHON_APP_DIR}/.." && pwd)"
VENV_DIR="${MEMORY_DIR}/.venv"

if [[ ! -x "${VENV_DIR}/bin/python" ]]; then
  echo "Run memory/python-agent/run.sh once to create the virtual environment." >&2
  exit 1
fi

"${VENV_DIR}/bin/python" -m unittest discover \
  -s "${PYTHON_APP_DIR}/tests" \
  -p "test_*.py"
"${VENV_DIR}/bin/python" -m pip check
