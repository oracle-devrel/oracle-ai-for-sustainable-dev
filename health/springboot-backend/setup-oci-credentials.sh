#!/bin/bash

# setup-oci-credentials.sh - Create Kubernetes secrets and configmaps for OCI authentication

echo "🔧 Setting up OCI credentials for Kubernetes deployment..."

# Check if files exist
if [ ! -f ~/.oci/config ]; then
    echo "❌ Error: ~/.oci/config file not found"
    echo "Please ensure your OCI config file exists at ~/.oci/config"
    exit 1
fi

if [ ! -f ~/.ssh/oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem ]; then
    echo "❌ Error: Private key file not found at ~/.ssh/oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem"
    echo "Please ensure your OCI private key file exists"
    exit 1
fi

echo "✅ Found required OCI credential files"

# Create namespace if it doesn't exist
# kubectl create namespace health --dry-run=client -o yaml | kubectl apply -f -

# Delete existing secrets/configmaps if they exist (to update them)
echo "🧹 Cleaning up existing OCI credentials..."
kubectl delete configmap oci-config -n health --ignore-not-found=true
kubectl delete secret oci-private-key -n health --ignore-not-found=true

# Create ConfigMap for OCI config file
echo "📝 Creating OCI config ConfigMap..."
kubectl create configmap oci-config \
    --from-file=config="$HOME/.oci/config" \
    -n health

if [ $? -eq 0 ]; then
    echo "✅ Successfully created oci-config ConfigMap"
else
    echo "❌ Failed to create oci-config ConfigMap"
    exit 1
fi

# Create Secret for private key file
echo "🔐 Creating OCI private key Secret..."
kubectl create secret generic oci-private-key \
    --from-file=oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem="$HOME/.ssh/oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem" \
    -n health

if [ $? -eq 0 ]; then
    echo "✅ Successfully created oci-private-key Secret"
else
    echo "❌ Failed to create oci-private-key Secret"
    exit 1
fi

echo ""
echo "🎉 OCI credentials setup complete!"
echo ""
echo "📋 Created resources:"
echo "  • ConfigMap: oci-config (contains ~/.oci/config)"
echo "  • Secret: oci-private-key (contains private key file)"
echo ""
echo "💡 Your deployment template is now configured to mount these at:"
echo "  • /root/.oci/config (OCI configuration)"
echo "  • /root/.ssh/oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem (private key)"
echo ""
echo "🚀 You can now deploy your application with:"
echo "  ./deploy.sh"
echo ""

# Verify the setup
echo "🔍 Verifying setup..."
echo "ConfigMaps in health namespace:"
kubectl get configmaps -n health | grep oci-config || echo "❌ oci-config not found"

echo "Secrets in health namespace:"
kubectl get secrets -n health | grep oci-private-key || echo "❌ oci-private-key not found"

echo ""
echo "✅ Setup verification complete!"
