#!/bin/bash

# Oracle Database RAG Agent using ADK CLI
# This uses the proper ADK CLI pattern that actually works

echo "Starting Oracle Database RAG Agent (ADK CLI)..."
echo "================================================"

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
    export RAG_API_URL="http://localhost:8501"
fi

# Set Google Cloud environment variables
export GOOGLE_CLOUD_PROJECT="${GOOGLE_CLOUD_PROJECT:-adb-pm-prod}"
export GOOGLE_CLOUD_LOCATION="${GOOGLE_CLOUD_LOCATION:-us-central1}"

echo "Project: $GOOGLE_CLOUD_PROJECT"
echo "Location: $GOOGLE_CLOUD_LOCATION"
echo "RAG API: $RAG_API_URL"
echo ""
echo "Running with ADK CLI (the proper way)..."
echo ""

# Run using ADK CLI - this is how the working examples do it
adk run rag
