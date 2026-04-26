#!/bin/bash
# Start the Oracle AI Database RAG API

# Load environment variables
set -a
source .env
set +a

# Start FastAPI server with uvicorn
echo "Starting Oracle AI Database RAG API on http://0.0.0.0:8501"
echo "OpenAPI docs available at: http://0.0.0.0:8501/docs"
echo "Alternative docs at: http://0.0.0.0:8501/redoc"

# Use --log-level warning to suppress INFO logs for health checks
uvicorn oracle_ai_database_rag:app --host 0.0.0.0 --port 8501 --reload --log-level warning --access-log
