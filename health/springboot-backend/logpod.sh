#!/bin/bash

# logpod.sh - Show logs from healthai-backend-springboot pod(s) in the health namespace

echo "🔍 Finding healthai-backend-springboot pods in health namespace..."

# Get pods matching healthai-backend-springboot pattern in health namespace
PODS=$(kubectl get pods -n health --no-headers | grep "healthai-backend-springboot" | awk '{print $1}')

if [ -z "$PODS" ]; then
    echo "❌ No healthai-backend-springboot pods found in health namespace"
    echo ""
    echo "📊 Current pods in health namespace:"
    kubectl get pods -n health
    exit 0
fi

echo "📋 Found healthai-backend-springboot pods:"
echo "$PODS"
echo ""

# Show logs from each pod
for POD in $PODS; do
    echo "📄 Logs from pod: $POD"
    echo "========================================"
    
    # Get pod status first
    POD_STATUS=$(kubectl get pod "$POD" -n health --no-headers | awk '{print $3}')
    echo "Pod Status: $POD_STATUS"
    echo ""
    
    # Show recent logs (last 100 lines)
    echo "Recent logs (last 100 lines):"
    echo "----------------------------------------"
    kubectl logs "$POD" -n health --tail=100
    
    echo ""
    echo "🔍 Pod details and events:"
    echo "----------------------------------------"
    kubectl describe pod "$POD" -n health | tail -20
    
    echo ""
    echo "========================================"
    echo ""
done

echo "📊 Current health namespace pod status:"
kubectl get pods -n health

echo ""
echo "💡 To follow logs in real-time, use:"
for POD in $PODS; do
    echo "   kubectl logs -f $POD -n health"
    kubectl logs -f $POD -n health
done
