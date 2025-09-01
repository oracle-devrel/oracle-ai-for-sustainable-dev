#!/bin/bash

echo "=== Building and Running React Frontend Locally ==="

# Step 1: Source environment variables from financial/.env
echo "1. Sourcing environment variables from ../.env"
if [ -f "../.env" ]; then
    source ../.env
    echo "   Environment variables loaded successfully!"
    echo "   REACT_APP_MERN_SQL_ORACLE_SERVICE_URL: $REACT_APP_MERN_SQL_ORACLE_SERVICE_URL"
    echo "   REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL: $REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL"
    echo "   REACT_APP_MERN_MONGODB_SERVICE_URL: $REACT_APP_MERN_MONGODB_SERVICE_URL"
else
    echo "   Warning: ../.env file not found, using default configuration"
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

# Step 3: Build the React application
echo ""
echo "3. Building React application..."
npm run build

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "   Build failed! Exiting..."
    exit 1
fi

echo "   Build completed successfully!"

# Step 4: Start the React development server
echo ""
echo "4. Starting React development server..."
echo "   React app will be available at: http://localhost:3000"
echo "   Press Ctrl+C to stop the server"
echo ""

npm start
