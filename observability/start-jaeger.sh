#!/usr/bin/env bash
set -euo pipefail

JAEGER_CONTAINER_NAME="${JAEGER_CONTAINER_NAME:-oracle-db-otel-jaeger}"
JAEGER_IMAGE="${JAEGER_IMAGE:-cr.jaegertracing.io/jaegertracing/jaeger:2.19.0}"
JAEGER_UI_PORT="${JAEGER_UI_PORT:-16686}"
OTLP_GRPC_PORT="${OTLP_GRPC_PORT:-4317}"
OTLP_HTTP_PORT="${OTLP_HTTP_PORT:-4318}"

if podman container exists "$JAEGER_CONTAINER_NAME"; then
  if [ "$(podman inspect -f '{{.State.Running}}' "$JAEGER_CONTAINER_NAME")" = "true" ]; then
    echo "Jaeger is already running: http://localhost:${JAEGER_UI_PORT}"
    exit 0
  fi
  podman start "$JAEGER_CONTAINER_NAME" >/dev/null
else
  podman run -d --name "$JAEGER_CONTAINER_NAME" \
    -p "${JAEGER_UI_PORT}:16686" \
    -p "${OTLP_GRPC_PORT}:4317" \
    -p "${OTLP_HTTP_PORT}:4318" \
    "$JAEGER_IMAGE" >/dev/null
fi

echo "Jaeger UI: http://localhost:${JAEGER_UI_PORT}"
echo "OTLP HTTP endpoint: http://localhost:${OTLP_HTTP_PORT}/v1/traces"
