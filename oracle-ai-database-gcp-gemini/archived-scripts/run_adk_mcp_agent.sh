#!/bin/bash
# Run Oracle AI Database ADK Agent with MCP Integration

# Set environment variables if not already set
export ORACLE_RAG_API_URL=${ORACLE_RAG_API_URL:-"http://34.48.146.146:8501"}
export GCP_PROJECT_ID=${GCP_PROJECT_ID:-"adb-pm-prod"}
export GCP_REGION=${GCP_REGION:-"us-central1"}
export SQLCL_PATH=${SQLCL_PATH:-"/opt/sqlcl/bin/sql"}
export TNS_ADMIN=${TNS_ADMIN:-"$HOME/wallet"}

echo "=========================================="
echo "Oracle AI Database ADK Agent with MCP"
echo "=========================================="
echo "RAG API: $ORACLE_RAG_API_URL"
echo "SQLcl: $SQLCL_PATH"
echo "Wallet: $TNS_ADMIN"
echo "MCP Connection: paulparkdb_mcp"
echo "=========================================="
echo ""

# Activate virtual environment if it exists
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Run the agent
python oracle_ai_database_adk_agent.py
