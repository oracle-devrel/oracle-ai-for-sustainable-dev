#!/bin/bash

# Financial Services Environment Setup Script
# This script copies the main .env file to all locations where it's needed

echo "Setting up environment files for Financial Services..."

# Main .env file location
MAIN_ENV_FILE="./.env"

if [ ! -f "$MAIN_ENV_FILE" ]; then
    echo "❌ Main .env file not found at $MAIN_ENV_FILE"
    echo "Please copy .env.example to .env and configure it first."
    exit 1
fi

echo "✅ Found main .env file"

# Copy to React frontend
if [ -d "./react-frontend" ]; then
    cp "$MAIN_ENV_FILE" "./react-frontend/.env"
    echo "✅ Copied .env to react-frontend/"
fi

# Copy to other microservices that might need it
for service_dir in */; do
    if [ -f "$service_dir/package.json" ] || [ -f "$service_dir/pom.xml" ] || [ -f "$service_dir/requirements.txt" ]; then
        if [ "$service_dir" != "react-frontend/" ]; then
            cp "$MAIN_ENV_FILE" "$service_dir/.env"
            echo "✅ Copied .env to $service_dir"
        fi
    fi
done

echo ""
echo "🎉 Environment setup complete!"
echo "All microservices now have access to the centralized environment configuration."
echo ""
echo "To start the React frontend:"
echo "  cd react-frontend && npm start"
echo ""
echo "To update environment variables, edit the main .env file and run this script again."
