#!/bin/bash

# Prompt for namespace
read -p "Enter namespace (Eg msdataworkshop): " NAMESPACE

# Prompt for the password (hidden input)
read -s -p "Enter database password (eg Welcome12345): " DB_PASSWORD
echo

# Base64 encode the password
BASE64_PWD=$(echo -n "$DB_PASSWORD" | base64)

# Apply the secret manifest
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: dbuser
  namespace: ${NAMESPACE}
type: Opaque
data:
  dbpassword: ${BASE64_PWD}
EOF

echo "✅ Kubernetes secret 'dbuser' created/updated in namespace '${NAMESPACE}'."