#!/bin/bash

echo "=== Building and Running MERN Stack Node.js Backend Locally ==="

# Step 1: Source environment variables from financial/.env
echo "1. Sourcing environment variables from ../../.env"
if [ -f "../../.env" ]; then
    source ../../.env
    echo "   Environment variables loaded successfully!"
    echo "   MONGO_URI: $MONGO_URI"
else
    echo "   Warning: ../../.env file not found, using default configuration"
fi

# Step 2: Install dependencies if node_modules doesn't exist
echo ""
echo "2. Checking dependencies..."
if [ ! -d "node_modules" ]; then
    echo "   Installing npm dependencies..."
    npm install
    if [ $? -ne 0 ]; then
        echo "   npm install failed! Exiting..."
        exit 1
    fi
    echo "   Dependencies installed successfully!"
else
    echo "   Dependencies already installed, skipping npm install"
fi

# Step 3: Start the Node.js server
echo ""
echo "3. Starting MERN Stack Node.js Backend..."
echo "   Server will be available at: http://localhost:5001"
echo "   MongoDB API endpoints:"
echo "   - POST/GET /api/accounts"
echo "   - GET/PUT/DELETE /api/accounts/:id"
echo "   Press Ctrl+C to stop the server"
echo ""

node app.js
