#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${MEMORY_BASE_URL:-http://127.0.0.1:${MEMORY_PORT:-8091}}"

curl -fsS "${BASE_URL}/api/health"
curl -fsS "${BASE_URL}/api/database/tables"
curl -fsS -H 'Content-Type: application/json' -d '{}' \
  "${BASE_URL}/api/library/actions/reset"
curl -fsS -H 'Content-Type: application/json' -d '{}' \
  "${BASE_URL}/api/library/actions/retain"
curl -fsS -H 'Content-Type: application/json' \
  -d '{"query":"Plan an accessible quiet morning and rainy evening"}' \
  "${BASE_URL}/api/library/actions/recall"
curl -fsS "${BASE_URL}/api/library/state"
for action in reset retain recall correct expire dream approve next-day; do
  if [[ "${action}" == "recall" ]]; then
    body='{"guestId":"AVA","query":"Build a rain-safe evening plan"}'
  elif [[ "${action}" == "approve" ]]; then
    body='{"approver":"demo.presenter@example.com"}'
  else
    body='{}'
  fi
  curl -fsS \
    -H 'Content-Type: application/json' \
    -d "${body}" \
    "${BASE_URL}/api/actions/${action}"
done
curl -fsS "${BASE_URL}/api/state"
for action in reset plan start complete-step complete-step complete-step graphrag; do
  if [[ "${action}" == "graphrag" ]]; then
    body='{"query":"Find a quiet accessible rainy route with founder stories"}'
  else
    body='{}'
  fi
  curl -fsS \
    -H 'Content-Type: application/json' \
    -d "${body}" \
    "${BASE_URL}/api/park/actions/${action}"
done
curl -fsS "${BASE_URL}/api/park/state"
printf '\nMemory demo smoke test passed.\n'
