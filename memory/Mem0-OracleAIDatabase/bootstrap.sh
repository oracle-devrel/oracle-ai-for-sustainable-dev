#!/usr/bin/env bash
set -euo pipefail

python_bin="${PYTHON_BIN:-python3}"

"${python_bin}" -c '
import sys
if sys.version_info < (3, 10):
    raise SystemExit(
        f"Mem0 requires Python 3.10 or later; found {sys.version.split()[0]}"
    )
'

"${python_bin}" -m venv .venv
.venv/bin/python -m pip install --upgrade pip
.venv/bin/python -m pip install -r requirements.txt

echo "Installed Mem0 with Oracle AI Vector Search support in .venv"

