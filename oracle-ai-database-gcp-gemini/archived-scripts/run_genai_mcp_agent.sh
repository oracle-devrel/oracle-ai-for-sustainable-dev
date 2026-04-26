#!/bin/bash
# Run Oracle AI Database Agent with GenerativeModel + Manual MCP
# This version bypasses ADK's buggy McpToolset and uses manual MCP integration

echo "Starting Oracle AI Database Agent (GenerativeModel + Manual MCP)..."
echo ""

# Set environment variables
export ORACLE_RAG_API_URL="http://34.48.146.146:8501"
export GCP_PROJECT_ID="adb-pm-prod"
export GCP_REGION="us-central1"
export SQLCL_PATH="/opt/sqlcl/bin/sql"
export TNS_ADMIN="$HOME/wallet"

# Activate Python environment if needed
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Run the agent
python oracle_ai_database_genai_mcp.py
