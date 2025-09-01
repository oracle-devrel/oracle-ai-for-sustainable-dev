#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

# Load environment variables from .env file if it exists
if [ -f "../.env" ]; then
    source ../.env
fi

# Set default ORDS URL if not provided
if [ -z "$REACT_APP_ORDS_BASE_URL" ]; then
    export REACT_APP_ORDS_BASE_URL="https://asdfmydbasdf-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords"
fi

echo "Using REACT_APP_ORDS_BASE_URL: $REACT_APP_ORDS_BASE_URL"

#kubectl apply -f  frontend-loadbalancer-service.yaml  -n financial
kubectl delete deployment frontend-react  -n financial

cp frontend-react-deployment_template.yaml frontend-react-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" frontend-react-deployment.yaml
sed -i '' "s|ORDS_BASE_URL_PLACEHOLDER|$REACT_APP_ORDS_BASE_URL|g" frontend-react-deployment.yaml
kubectl apply -f frontend-react-deployment.yaml -n financial

