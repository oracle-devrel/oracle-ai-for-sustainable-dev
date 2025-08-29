#!/bin/bash

# deletepod.sh - Find and delete healthai-frontend pod(s) in the health namespace

echo "🔍 Finding healthai-frontend pods in health namespace..."

# Get pods matching healthai-frontend pattern in health namespace
PODS=$(kubectl get pods -n health --no-headers | grep "healthai-frontend" | awk '{print $1}')

if [ -z "$PODS" ]; then
    echo "❌ No healthai-frontend pods found in health namespace"
    exit 0
fi

echo "📋 Found healthai-frontend pods:"
echo "$PODS"
echo ""

# Delete each pod
for POD in $PODS; do
    echo "🗑️  Deleting pod: $POD"
    kubectl delete pod "$POD" -n health
    if [ $? -eq 0 ]; then
        echo "✅ Successfully deleted pod: $POD"
    else
        echo "❌ Failed to delete pod: $POD"
    fi
done

echo ""
echo "🔄 Checking remaining healthai-frontend pods..."
kubectl get pods -n health | grep "healthai-frontend" || echo "✅ No healthai-frontend pods remaining"

echo ""
echo "📊 Current pods in health namespace:"
kubectl get pods -n health

echo ""
echo "⏳ Waiting for new healthai-frontend pod to be created..."
sleep 5

# Wait for new pod to be created and get its name
NEW_POD=""
for i in {1..30}; do
    NEW_POD=$(kubectl get pods -n health --no-headers | grep "healthai-frontend" | grep "Running\|ContainerCreating" | head -1 | awk '{print $1}')
    if [ ! -z "$NEW_POD" ]; then
        echo "✅ Found new healthai-frontend pod: $NEW_POD"
        break
    fi
    echo "⏳ Waiting for pod creation... (attempt $i/30)"
    sleep 2
done

if [ ! -z "$NEW_POD" ]; then
    echo ""
    echo "📊 Updated pods in health namespace:"
    kubectl get pods -n health
    
    echo ""
    echo "📋 Getting logs from new pod: $NEW_POD"
    echo "----------------------------------------"
    kubectl logs "$NEW_POD" -n health --tail=50
    
    echo ""
    echo "🔍 Pod status details:"
    kubectl describe pod "$NEW_POD" -n health | head -20
else
    echo "❌ No new healthai-frontend pod found after waiting"
    echo "📊 Current pods in health namespace:"
    kubectl get pods -n health
fi
