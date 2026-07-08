#!/usr/bin/env bash
set -euo pipefail

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

java -jar target/springboot-oracle-lakehouse-iceberg-demo-0.0.1-SNAPSHOT.jar
