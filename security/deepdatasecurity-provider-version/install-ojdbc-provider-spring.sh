#!/usr/bin/env bash
set -euo pipefail

PROVIDER_VERSION="${OJDBC_PROVIDER_VERSION:-1.1.0}"
PROVIDER_COORDINATE="com.oracle.database.jdbc:ojdbc-provider-spring:${PROVIDER_VERSION}"
EXTENSIONS_REPO_URL="${OJDBC_EXTENSIONS_REPO_URL:-https://github.com/oracle/ojdbc-extensions.git}"
EXTENSIONS_REF="${OJDBC_EXTENSIONS_REF:-main}"
EXTENSIONS_WORKDIR="${OJDBC_EXTENSIONS_WORKDIR:-${TMPDIR:-/tmp}/ojdbc-extensions-deepsec}"

if mvn -q dependency:get -Dartifact="${PROVIDER_COORDINATE}" -Dtransitive=false >/dev/null 2>&1; then
  exit 0
fi

echo "ojdbc-provider-spring ${PROVIDER_VERSION} was not found in Maven Central/local cache."
echo "Installing it from ${EXTENSIONS_REPO_URL} (${EXTENSIONS_REF}) into the local Maven cache."

GIT_PROXY_ARGS=()
if [[ "${OJDBC_EXTENSIONS_USE_CONFIGURED_GIT_PROXY:-false}" != "true" ]]; then
  GIT_PROXY_ARGS=(-c http.proxy= -c https.proxy=)
fi

if [[ -d "${EXTENSIONS_WORKDIR}/.git" ]]; then
  git "${GIT_PROXY_ARGS[@]}" -C "${EXTENSIONS_WORKDIR}" fetch --depth 1 origin "${EXTENSIONS_REF}"
  git "${GIT_PROXY_ARGS[@]}" -C "${EXTENSIONS_WORKDIR}" checkout FETCH_HEAD
else
  mkdir -p "$(dirname "${EXTENSIONS_WORKDIR}")"
  git "${GIT_PROXY_ARGS[@]}" clone --depth 1 --branch "${EXTENSIONS_REF}" \
    "${EXTENSIONS_REPO_URL}" "${EXTENSIONS_WORKDIR}"
fi

mvn -f "${EXTENSIONS_WORKDIR}/pom.xml" \
  -pl ojdbc-provider-spring \
  -am \
  -DskipTests \
  -Dmaven.javadoc.skip=true \
  install

mvn -q dependency:get -Dartifact="${PROVIDER_COORDINATE}" -Dtransitive=false >/dev/null
