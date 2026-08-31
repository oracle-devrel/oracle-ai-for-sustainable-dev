#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${APP_DIR}/server"
DEFAULT_LIBRARY_DIR="/Users/pparkins/src/orahub/aire-dev/memory/java/ojdbc-agent-memory"
LIBRARY_DIR="${OAM_LIBRARY_DIR:-${DEFAULT_LIBRARY_DIR}}"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi

MAVEN_BIN="${MAVEN_BIN:-mvn}"
if ! command -v "${MAVEN_BIN}" >/dev/null 2>&1 \
    && [[ -x /opt/homebrew/opt/maven/bin/mvn ]]; then
  MAVEN_BIN="/opt/homebrew/opt/maven/bin/mvn"
fi

if [[ ! -f "${LIBRARY_DIR}/pom.xml" ]]; then
  echo "Oracle Agent Memory Java library not found at ${LIBRARY_DIR}." >&2
  echo "Set OAM_LIBRARY_DIR to the java/ojdbc-agent-memory directory from the memory MR branch." >&2
  exit 1
fi
cd "${LIBRARY_DIR}"
"${MAVEN_BIN}" -q -DskipTests install
cd "${SERVER_DIR}"
"${MAVEN_BIN}" test
