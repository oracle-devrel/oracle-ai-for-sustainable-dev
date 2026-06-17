#!/bin/bash

# Prompt user for the password
read -s -p "Enter frontendadmin password: " UI_PASSWORD
echo

# Base64 encode the password
BASE64_UI_PASSWORD=$(echo -n "$UI_PASSWORD" | base64)

# Create or update the Kubernetes secret
cat <<EOF | kubectl apply -n msdataworkshop -f -
apiVersion: v1
kind: Secret
metadata:
  name: frontendadmin
type: Opaque
data:
  password: "${BASE64_UI_PASSWORD}"
EOF

echo "✅ Secret 'frontendadmin' applied in namespace 'msdataworkshop'."