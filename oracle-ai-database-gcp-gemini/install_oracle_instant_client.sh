#!/bin/bash
set -e

echo "Installing Oracle Instant Client for MCP Toolbox..."

# Download Oracle Instant Client Basic
cd /tmp
echo "Downloading Oracle Instant Client 23.5..."
wget -q https://download.oracle.com/otn_software/linux/instantclient/2350000/instantclient-basic-linux.x64-23.5.0.24.07.zip

echo "Extracting..."
unzip -q instantclient-basic-linux.x64-23.5.0.24.07.zip

# Move to standard location
echo "Installing to /opt/oracle..."
sudo mkdir -p /opt/oracle
sudo mv instantclient_23_5 /opt/oracle/
sudo ln -sf /opt/oracle/instantclient_23_5 /opt/oracle/instantclient

# Install required library
echo "Installing libaio1..."
sudo apt-get update -qq
sudo apt-get install -y libaio1

# Configure library path
echo "Configuring library path..."
echo /opt/oracle/instantclient | sudo tee /etc/ld.so.conf.d/oracle-instantclient.conf > /dev/null
sudo ldconfig

# Verify installation
echo ""
echo "✅ Oracle Instant Client installed successfully!"
echo "Location: /opt/oracle/instantclient"
ls -la /opt/oracle/instantclient/

# Cleanup
rm -f /tmp/instantclient-basic-linux.x64-23.5.0.24.07.zip

echo ""
echo "You can now run Option 3 (ADK Agent with MCP Toolbox)"
