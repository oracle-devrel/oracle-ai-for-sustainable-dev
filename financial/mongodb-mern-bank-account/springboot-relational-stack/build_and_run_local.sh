#!/bin/bash

echo "=== Building and Running Account Service Locally ==="

# Step 1: Source environment variables from financial/.env
echo "1. Sourcing environment variables from ../../.env"
if [ -f "../../.env" ]; then
    source ../../.env
    echo "   Environment variables loaded successfully!"
else
    echo "   Warning: ../../.env file not found, using default configuration"
fi

# Step 2: Clean and package with Maven
echo ""
echo "2. Building project with Maven..."
mvn clean package -DskipTests

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "   Build failed! Exiting..."
    exit 1
fi

echo "   Build completed successfully!"

# Step 3: Run the generated JAR
echo ""
echo "3. Starting Account Service..."
echo "   Running: java -jar target/account-1.0.0-SNAPSHOT.jar"
echo ""

java -jar target/account-1.0.0-SNAPSHOT.jar
