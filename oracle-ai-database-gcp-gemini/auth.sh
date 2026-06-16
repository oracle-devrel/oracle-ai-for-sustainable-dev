#!/bin/bash
set -euo pipefail

PROJECT_ID="${GCP_PROJECT_ID:-adb-pm-prod}"

echo "Refreshing Google Application Default Credentials (ADC)..."
echo "Project: ${PROJECT_ID}"
echo

gcloud config set project "${PROJECT_ID}"
gcloud auth application-default login --no-launch-browser
gcloud auth application-default set-quota-project "${PROJECT_ID}"

echo
echo "ADC authentication complete."