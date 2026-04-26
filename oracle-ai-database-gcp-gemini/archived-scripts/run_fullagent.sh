#!/bin/bash
# Run the Full ADK Agent with reasoning engine

# Load environment variables
if [ -f .env ]; then
    set -a
    source .env
    set +a
    echo "✓ Loaded environment variables from .env"
else
    echo "⚠️  Warning: .env file not found"
fi

# Set default API URL if not in .env
export ORACLE_RAG_API_URL=${ORACLE_RAG_API_URL:-"http://34.48.146.146:8501"}

echo "Starting Full ADK Agent..."
echo "API URL: $ORACLE_RAG_API_URL"
echo ""

# Run the full agent
python oracle_ai_database_adk_fullagent.py
