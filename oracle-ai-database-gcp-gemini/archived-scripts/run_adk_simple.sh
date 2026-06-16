#!/bin/bash

# Oracle Database ADK RAG Agent - Simple Version Runner
# Requires: gcloud auth application-default login

echo "Starting Oracle Database RAG Agent (ADK Simplified)..."
echo "======================================================="

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please create .env file with RAG_API_URL=http://localhost:8501"
    exit 1
fi

# Load environment variables
source .env

# Check if RAG API URL is set
if [ -z "$RAG_API_URL" ]; then
    echo "Warning: RAG_API_URL not set in .env, using default http://localhost:8501"
fi

# Set Google Cloud environment variables
export GOOGLE_CLOUD_PROJECT="${GOOGLE_CLOUD_PROJECT:-adb-pm-prod}"
export GOOGLE_CLOUD_LOCATION="${GOOGLE_CLOUD_LOCATION:-us-central1}"

echo "Project: $GOOGLE_CLOUD_PROJECT"
echo "Location: $GOOGLE_CLOUD_LOCATION"
echo "RAG API: ${RAG_API_URL:-http://localhost:8501}"
echo ""

# Run the agent
python oracle_adk_rag_simple.py
