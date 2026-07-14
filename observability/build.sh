#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${SCRIPT_DIR}/springboot-oracle-db-otel-demo"

cd "$APP_DIR"
mvn -DskipTests package

echo "Built ${APP_DIR}/target/springboot-oracle-db-otel-demo-0.0.1-SNAPSHOT.jar"
