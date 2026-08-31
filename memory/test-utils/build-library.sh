#!/usr/bin/env bash
set -euo pipefail

EXAMPLES_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_LIBRARY_DIR="/Users/pparkins/src/orahub/ora-jdbc-dev/ojdbc-agent-memory"
LIBRARY_DIR="${OAM_LIBRARY_DIR:-${DEFAULT_LIBRARY_DIR}}"
MAVEN_BIN="${MAVEN_BIN:-/opt/homebrew/opt/maven/bin/mvn}"

if [[ ! -f "${LIBRARY_DIR}/pom.xml" ]]; then
  echo "ojdbc-agent-memory was not found at ${LIBRARY_DIR}." >&2
  echo "Set OAM_LIBRARY_DIR to its clone directory." >&2
  exit 1
fi

if [[ ! -x "${MAVEN_BIN}" ]]; then
  echo "Maven was not found at ${MAVEN_BIN}." >&2
  exit 1
fi

echo "Building ${LIBRARY_DIR}"
"${MAVEN_BIN}" -f "${LIBRARY_DIR}/pom.xml" clean install
echo "Installed com.oracle.database.jdbc:ojdbc-agent-memory:1.0-SNAPSHOT"
echo "Examples directory: ${EXAMPLES_DIR}"
