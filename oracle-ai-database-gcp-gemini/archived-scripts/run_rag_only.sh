#!/bin/bash
# Run Oracle AI Database RAG-Only Agent
# Simple, reliable documentation search - no MCP, no database connections

echo "Starting Oracle RAG-Only Agent (Documentation Search)..."
echo ""

# Set environment variables
export ORACLE_RAG_API_URL="http://localhost:8501"
export GCP_PROJECT_ID="adb-pm-prod"
export GCP_REGION="us-central1"

# Activate Python environment if needed
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Run the agent
python oracle_ai_database_rag_only.py
