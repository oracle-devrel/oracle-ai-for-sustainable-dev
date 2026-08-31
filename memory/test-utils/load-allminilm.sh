#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

OAM_MODE=minimal \
OAM_MAIN_CLASS=com.oracle.ojdbc.agentmemory.examples.AllMiniLmInstaller \
  "${SCRIPT_DIR}/run-local.sh"
