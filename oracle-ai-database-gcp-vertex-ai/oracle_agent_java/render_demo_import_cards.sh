#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 PUBLIC_AGENT_HOST" >&2
  echo "Example: $0 34.186.79.96" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/demo-import"
PUBLIC_AGENT_HOST="$1"

PUBLIC_AGENT_HOST="${PUBLIC_AGENT_HOST#http://}"
PUBLIC_AGENT_HOST="${PUBLIC_AGENT_HOST#https://}"
PUBLIC_AGENT_HOST="${PUBLIC_AGENT_HOST%/}"

mkdir -p "$OUTPUT_DIR"

	for template in \
	  "$SCRIPT_DIR/agent-card-graph.json" \
	  "$SCRIPT_DIR/agent-card-spatial.json" \
	  "$SCRIPT_DIR/agent-card-select-ai.json" \
	  "$SCRIPT_DIR/agent-card-action.json" \
	  "$SCRIPT_DIR/agent-card-inventory-system.json"; do
	  output_file="$OUTPUT_DIR/$(basename "$template")"
	  sed "s|YOUR_PUBLIC_AGENT_HOST|$PUBLIC_AGENT_HOST|g" "$template" >"$output_file"
	done

echo "Rendered demo import cards to:"
echo "  $OUTPUT_DIR"
