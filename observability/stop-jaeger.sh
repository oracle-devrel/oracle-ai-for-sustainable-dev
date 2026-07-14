#!/usr/bin/env bash
set -euo pipefail

JAEGER_CONTAINER_NAME="${JAEGER_CONTAINER_NAME:-oracle-db-otel-jaeger}"

if podman container exists "$JAEGER_CONTAINER_NAME"; then
  podman stop "$JAEGER_CONTAINER_NAME" >/dev/null
  echo "Stopped ${JAEGER_CONTAINER_NAME}."
else
  echo "${JAEGER_CONTAINER_NAME} does not exist."
fi
