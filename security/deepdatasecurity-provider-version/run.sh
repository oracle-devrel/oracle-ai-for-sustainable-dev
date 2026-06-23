#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

set -a
source .env
set +a

./install-ojdbc-provider-spring.sh

mvn -Dojdbc.provider.version="${OJDBC_PROVIDER_VERSION:-1.1.0}" spring-boot:run
