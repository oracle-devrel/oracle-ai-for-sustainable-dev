#!/usr/bin/env bash
set -euo pipefail

EXAMPLES_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MEMORY_DIR="$(cd -- "${EXAMPLES_DIR}/.." && pwd)"
REPO_DIR="$(cd -- "${MEMORY_DIR}/.." && pwd)"
MAVEN_BIN="${MAVEN_BIN:-/opt/homebrew/opt/maven/bin/mvn}"
MODE="${OAM_MODE:-minimal}"
MAIN_CLASS="${OAM_MAIN_CLASS:-com.oracle.ojdbc.agentmemory.examples.UcpConnectionPoolExample}"

if [[ -f "${MEMORY_DIR}/.env" ]]; then
  ENV_FILE="${MEMORY_DIR}/.env"
elif [[ -f "${REPO_DIR}/financial/setup/.env" ]]; then
  ENV_FILE="${REPO_DIR}/financial/setup/.env"
else
  echo "Create memory/.env or configure financial/setup/.env." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

DB_USERNAME="${DB_USERNAME:-${DB_USER:-}}"
DB_SERVICE="${DB_SERVICE:-financialdb_high}"
TNS_ADMIN="${TNS_ADMIN:-${WALLET_DIR:-}}"

if [[ -z "${DB_USERNAME}" || -z "${DB_PASSWORD:-}" || -z "${TNS_ADMIN}" ]]; then
  echo "DB_USERNAME or DB_USER, DB_PASSWORD, and TNS_ADMIN or WALLET_DIR are required." >&2
  exit 1
fi

if [[ ! -x "${MAVEN_BIN}" ]]; then
  echo "Maven was not found at ${MAVEN_BIN}." >&2
  exit 1
fi

CONFIG_FILE="$(mktemp "${TMPDIR:-/tmp}/ojdbc-agent-memory.XXXXXX.properties")"
chmod 600 "${CONFIG_FILE}"
trap 'rm -f "${CONFIG_FILE}"' EXIT

case "${MODE}" in
  minimal)
    EMBEDDING_PROVIDER="none"
    LLM_PROVIDER="none"
    EXTRACT_MEMORIES="false"
    ;;
  full)
    EMBEDDING_PROVIDER="database"
    LLM_PROVIDER="ollama"
    EXTRACT_MEMORIES="true"
    ;;
  *)
    echo "OAM_MODE must be minimal or full." >&2
    exit 1
    ;;
esac

{
  printf '%s\n' "ojdbc.agentmemory.database.url=jdbc:oracle:thin:@${DB_SERVICE}?TNS_ADMIN=${TNS_ADMIN}"
  printf '%s\n' "ojdbc.agentmemory.database.user=${DB_USERNAME}"
  printf '%s\n' "ojdbc.agentmemory.database.password=${DB_PASSWORD}"
  printf '%s\n' ""
  printf '%s\n' "ojdbc.agentmemory.pool.enabled=true"
  printf '%s\n' "ojdbc.agentmemory.pool.name=OAMJ_TEST_POOL"
  printf '%s\n' "ojdbc.agentmemory.pool.connection-factory-class-name=oracle.jdbc.pool.OracleDataSource"
  printf '%s\n' "ojdbc.agentmemory.pool.initial-size=1"
  printf '%s\n' "ojdbc.agentmemory.pool.min-size=1"
  printf '%s\n' "ojdbc.agentmemory.pool.max-size=3"
  printf '%s\n' ""
  printf '%s\n' "ojdbc.agentmemory.schema.auto-create=true"
  printf '%s\n' "ojdbc.agentmemory.schema.table-prefix=OAMJ_TEST_"
  printf '%s\n' ""
  printf '%s\n' "ojdbc.agentmemory.embedding.provider=${EMBEDDING_PROVIDER}"
  printf '%s\n' "ojdbc.agentmemory.embedding.model=${OAM_EMBEDDING_MODEL:-allminilm}"
  printf '%s\n' ""
  printf '%s\n' "ojdbc.agentmemory.llm.provider=${LLM_PROVIDER}"
  printf '%s\n' "ojdbc.agentmemory.llm.base-url=${OLLAMA_BASE_URL:-http://localhost:11434}"
  printf '%s\n' "ojdbc.agentmemory.llm.model=${OLLAMA_MODEL:-llama3.2:latest}"
  printf '%s\n' "ojdbc.agentmemory.llm.temperature=0.0"
  printf '%s\n' ""
  printf '%s\n' "ojdbc.agentmemory.memory.extract-memories=${EXTRACT_MEMORIES}"
} > "${CONFIG_FILE}"

echo "Running ${MAIN_CLASS} in ${MODE} mode"
"${MAVEN_BIN}" -q \
  -Doracle.net.tns_admin="${TNS_ADMIN}" \
  -Dojdbc.agentmemory.config="${CONFIG_FILE}" \
  -Dexec.mainClass="${MAIN_CLASS}" \
  compile exec:java
