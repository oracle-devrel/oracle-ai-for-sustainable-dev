#!/usr/bin/env bash
set -euo pipefail

base_url="${AGENT_SERVICE_URL:-http://127.0.0.1:8080}"
run_file="$(mktemp)"
reject_file="$(mktemp)"
trap 'rm -f "$run_file" "$reject_file"' EXIT

curl --fail --silent --show-error -X POST "$base_url/api/runs" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data 'minimumStockoutRisk=70&maximumRows=5' > "$run_file"

approval_id="$(sed -n 's/.*"approvalId":"\([^"]*\)".*/\1/p' "$run_file" | head -1)"
recommendation_id="$(sed -n 's/.*"recommendationId":"\([^"]*\)".*/\1/p' "$run_file" | head -1)"

test -n "$approval_id"
test -n "$recommendation_id"
grep -q 'find-stockout-transfer-recommendations' "$run_file"

curl --fail --silent --show-error -X POST "$base_url/api/approve" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "approvalId=$approval_id" \
  --data-urlencode "recommendationId=$recommendation_id" \
  --data-urlencode 'approvalNotes=Approve governed inventory rebalancing recommendation' \
  | grep -q '"status":"APPROVED"'

curl --fail --silent --show-error -X POST "$base_url/api/runs" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data 'minimumStockoutRisk=70&maximumRows=2' > "$reject_file"
approval_id="$(sed -n 's/.*"approvalId":"\([^"]*\)".*/\1/p' "$reject_file" | head -1)"
test -n "$approval_id"

curl --fail --silent --show-error -X POST "$base_url/api/reject" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "approvalId=$approval_id" \
  | grep -q '"status":"REJECTED"'

echo "Supply-chain exchange approval and rejection smoke tests passed."
