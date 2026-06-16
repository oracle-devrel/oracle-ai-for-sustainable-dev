#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
ENV_HELPER="$REPO_ROOT/load_env_defaults.sh"

if [[ -f "$ENV_HELPER" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_HELPER"
  load_env_defaults "$ENV_FILE" \
    INVENTORY_SYSTEM_AGENT_URL \
    GRAPH_AGENT_URL \
    A2A_URL \
    PUBLIC_PROTOCOL \
    PUBLIC_HOST \
    GRAPH_AGENT_PORT \
    PORT
elif [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

BASE_URL="${INVENTORY_SYSTEM_AGENT_URL:-${GRAPH_AGENT_URL:-${A2A_URL:-${PUBLIC_PROTOCOL:-http}://${PUBLIC_HOST:-localhost}:${GRAPH_AGENT_PORT:-${PORT:-8080}}}}}"
if [[ "${BASE_URL%/}" == */inventory-system ]]; then
  TARGET_URL="${BASE_URL%/}"
else
  TARGET_URL="${BASE_URL%/}/inventory-system"
fi

PROMPT="${1:-Which products are at risk of stockouts next quarter, and which regions are driving that risk?}"
TEST_OUTPUT_DIR="${TEST_OUTPUT_DIR:-$SCRIPT_DIR/test-output}"

mkdir -p "$TEST_OUTPUT_DIR"

echo "-----------------------------------------------"
echo "STEP 1: Testing Discovery"
echo "-----------------------------------------------"
curl -sS "$TARGET_URL/.well-known/agent-card.json" | python3 -m json.tool
echo -e "\n"

echo "-----------------------------------------------"
echo "STEP 2: Testing Inventory-System Router (A2A JSON-RPC)"
echo "-----------------------------------------------"
RESPONSE="$(curl -sS -X POST "$TARGET_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"method\": \"message/send\",
    \"params\": {
      \"message\": {
        \"kind\": \"message\",
        \"messageId\": \"inventory-system-test-1\",
        \"role\": \"user\",
        \"parts\": [
          {
            \"kind\": \"text\",
            \"text\": \"$PROMPT\"
          }
        ]
      }
    },
    \"id\": 1
  }")"

printf '%s\n' "$RESPONSE" | TEST_OUTPUT_DIR="$TEST_OUTPUT_DIR" python3 -c '
import base64
import json
import os
from pathlib import Path
import sys

payload = json.load(sys.stdin)
if "error" in payload:
    print(json.dumps(payload, indent=2))
    raise SystemExit(0)

output_dir = Path(os.environ["TEST_OUTPUT_DIR"]).resolve()
output_dir.mkdir(parents=True, exist_ok=True)

result = payload.get("result", {})
summary = {
    "contextId": result.get("contextId"),
    "status": (result.get("status") or {}).get("state"),
    "metadata": result.get("metadata", {}),
}

status_message = (result.get("status") or {}).get("message") or {}
if status_message.get("parts"):
    summary["statusMessageParts"] = status_message["parts"]

saved_files = []
artifacts = []

for artifact in result.get("artifacts", []):
    parts = []
    for part in artifact.get("parts", []):
        part_summary = {"kind": part.get("kind")}
        file_info = part.get("file")
        if isinstance(file_info, dict):
            part_summary["mimeType"] = file_info.get("mimeType")
            part_summary["name"] = file_info.get("name")
            raw_bytes = file_info.get("bytes")
            if raw_bytes:
                filename = file_info.get("name") or f"{artifact.get('name') or 'artifact'}.bin"
                save_path = output_dir / filename
                if save_path.exists():
                    stem = save_path.stem
                    suffix = save_path.suffix
                    index = 1
                    while True:
                        candidate = output_dir / f"{stem}-{index}{suffix}"
                        if not candidate.exists():
                            save_path = candidate
                            break
                        index += 1
                save_path.write_bytes(base64.b64decode(raw_bytes))
                part_summary["savedPath"] = str(save_path)
                saved_files.append(str(save_path))
        if part.get("text"):
            part_summary["text"] = part.get("text")
        parts.append(part_summary)
    artifacts.append({
        "artifactId": artifact.get("artifactId"),
        "name": artifact.get("name"),
        "parts": parts,
    })

if artifacts:
    summary["artifacts"] = artifacts
if saved_files:
    summary["savedFiles"] = saved_files

print(json.dumps(summary, indent=2))
'
echo
