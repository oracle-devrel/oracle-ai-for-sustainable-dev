#!/bin/bash
# Start code-server on the GCP VM (port 8080)
# Run this on the remote VM after a reboot/reset

if command -v code-server &> /dev/null; then
    echo "Starting code-server..."
    nohup code-server > ~/code-server.log 2>&1 &
    sleep 2
    if ss -tlnp | grep -q 8080; then
        echo "✓ code-server is running on port 8080"
    else
        echo "✗ code-server failed to start. Check ~/code-server.log"
    fi
else
    echo "✗ code-server is not installed"
fi
