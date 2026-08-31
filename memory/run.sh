#!/usr/bin/env bash
set -euo pipefail

MEMORY_APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec "${MEMORY_APP_DIR}/python-agent/run.sh"
