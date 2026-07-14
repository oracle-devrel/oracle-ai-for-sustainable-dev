#!/usr/bin/env bash
set -euo pipefail

APP_BASE_URL="${APP_BASE_URL:-http://localhost:8080}"
AGENT_ID="${AGENT_ID:-claims-investigator-agent}"
TASK="${TASK:-investigate_payment_anomalies}"

echo "Health:"
curl -fsS "${APP_BASE_URL}/actuator/health"
echo
echo

echo "Roundtrip trace:"
curl -fsS "${APP_BASE_URL}/trace/roundtrip"
echo
echo

echo "Agent task trace:"
curl -fsS "${APP_BASE_URL}/trace/agent-task?agentId=${AGENT_ID}&task=${TASK}"
echo
echo

echo "Browser view:"
echo "${APP_BASE_URL}/trace/agent-task/view?agentId=${AGENT_ID}&task=${TASK}"
