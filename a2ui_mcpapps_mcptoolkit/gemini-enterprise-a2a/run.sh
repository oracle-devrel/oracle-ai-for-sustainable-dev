#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

command -v python3 >/dev/null 2>&1 || {
  echo "Python 3.10 through 3.13 is required." >&2
  exit 1
}

if [ ! -d .venv ]; then
  python3 -m venv .venv
fi

. .venv/bin/activate
python -m pip install --disable-pip-version-check -r requirements.txt
python -m unittest discover -s tests

echo "Starting Gemini Enterprise A2A/A2UI adapter on port ${GEMINI_ENTERPRISE_A2A_PORT:-3002}." >&2
exec python main.py
