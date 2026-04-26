#!/bin/bash
# Run the Oracle AI Database ADK Agent

# Load environment variables
set -a
source .env
set +a

# Set API URL (defaults to GCP server)
export ORACLE_RAG_API_URL="${ORACLE_RAG_API_URL:-http://34.48.146.146:8501}"

echo "Starting Oracle Database AI Agent..."
echo "API URL: $ORACLE_RAG_API_URL"
echo ""

python oracle_ai_database_adk_agent.py
