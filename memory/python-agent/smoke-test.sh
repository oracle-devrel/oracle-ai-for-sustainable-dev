#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${MEMORY_PYTHON_BASE_URL:-http://127.0.0.1:${MEMORY_PYTHON_PORT:-8092}}"

curl -fsS "${BASE_URL}/api/health"
curl -fsS "${BASE_URL}/api/capabilities"
for action in reset retain recall correct expire dream approve next-day; do
  curl -fsS \
    -H 'Content-Type: application/json' \
    -d '{}' \
    "${BASE_URL}/api/actions/${action}"
done
curl -fsS "${BASE_URL}/api/state"
curl -fsS "${BASE_URL}/api/security/state"
curl -fsS "${BASE_URL}/api/park/state"
for action in reset plan start complete-step complete-step complete-step graphrag; do
  curl -fsS -H 'Content-Type: application/json' -d '{}' \
    "${BASE_URL}/api/park/actions/${action}"
done
curl -fsS "${BASE_URL}/api/database/tables"
ar_session="$(curl -fsS -H 'Content-Type: application/json' -d \
  '{"guestId":"AVA","cameraSensing":true,"mediaRecording":true,"locationSharing":false,"retentionDays":7}' \
  "${BASE_URL}/api/ar/session/start")"
ar_session_id="$(printf '%s' "${ar_session}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["sessionId"])')"
ar_session_token="$(printf '%s' "${ar_session}" | python3 -c 'import json,sys; print(json.load(sys.stdin)["sessionToken"])')"
curl -fsS -H 'Content-Type: application/json' -d \
  "{\"sessionId\":\"${ar_session_id}\",\"sessionToken\":\"${ar_session_token}\",\"text\":\"Remember that I enjoyed the quiet Lantern Garden entrance.\",\"source\":\"browser\"}" \
  "${BASE_URL}/api/ar/remember"
curl -fsS -H 'Content-Type: application/json' -d \
  "{\"sessionId\":\"${ar_session_id}\",\"sessionToken\":\"${ar_session_token}\",\"transcript\":\"A covered constellation mosaic appears beside the accessible atrium entrance.\",\"mediaType\":\"video-transcript\"}" \
  "${BASE_URL}/api/ar/media"
curl -fsS -H 'Content-Type: application/json' -d \
  "{\"sessionId\":\"${ar_session_id}\",\"sessionToken\":\"${ar_session_token}\",\"query\":\"Where was the accessible constellation artwork?\"}" \
  "${BASE_URL}/api/ar/search"
curl -fsS "${BASE_URL}/api/ar/state"
printf '\nPython Oracle AI Agent Memory smoke test passed.\n'
