#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Shutting down MCP Toolbox server...${NC}"
    if [ ! -z "$TOOLBOX_PID" ]; then
        kill $TOOLBOX_PID 2>/dev/null || true
        wait $TOOLBOX_PID 2>/dev/null || true
    fi
    echo -e "${GREEN}Cleanup complete${NC}"
}

trap cleanup EXIT INT TERM

# Navigate to parent directory (where tools.yaml and .env are located)
cd "$(dirname "$0")/.."

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env file not found${NC}"
    exit 1
fi

# Activate virtual environment
if [ -d "venv" ]; then
    source venv/bin/activate
elif [ -d ".venv" ]; then
    source .venv/bin/activate
else
    echo -e "${RED}Error: Virtual environment not found${NC}"
    exit 1
fi

# Check if toolbox binary exists
TOOLBOX_BIN="./toolbox"
if [ ! -f "$TOOLBOX_BIN" ]; then
    echo -e "${YELLOW}MCP Toolbox binary not found. Downloading...${NC}"
    VERSION="0.24.0"
    
    # Detect OS and architecture
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case "$OS" in
        linux)
            case "$ARCH" in
                x86_64) ARCH="amd64" ;;
                aarch64) ARCH="arm64" ;;
                *) echo -e "${RED}Unsupported architecture: $ARCH${NC}"; exit 1 ;;
            esac
            ;;
        darwin)
            case "$ARCH" in
                x86_64) ARCH="amd64" ;;
                arm64) ARCH="arm64" ;;
                *) echo -e "${RED}Unsupported architecture: $ARCH${NC}"; exit 1 ;;
            esac
            ;;
        *)
            echo -e "${RED}Unsupported OS: $OS${NC}"
            exit 1
            ;;
    esac
    
    URL="https://storage.googleapis.com/genai-toolbox/v${VERSION}/${OS}/${ARCH}/toolbox"
    echo "Downloading from: $URL"
    curl -L -o "$TOOLBOX_BIN" "$URL"
    chmod +x "$TOOLBOX_BIN"
    echo -e "${GREEN}MCP Toolbox downloaded successfully${NC}"
fi

# Check if tools.yaml exists
if [ ! -f "tools.yaml" ]; then
    echo -e "${RED}Error: tools.yaml not found in $(pwd)${NC}"
    exit 1
fi

# Start MCP Toolbox server in background
echo -e "${GREEN}Starting MCP Toolbox server...${NC}"
echo "Running: $TOOLBOX_BIN serve --tools-file tools.yaml --port 5000"
$TOOLBOX_BIN serve --tools-file tools.yaml --port 5000 > toolbox.log 2>&1 &
TOOLBOX_PID=$!
echo "Toolbox PID: $TOOLBOX_PID"

# Give the process a moment to fail if there's an immediate error
sleep 2

# Check if process is still running
if ! kill -0 $TOOLBOX_PID 2>/dev/null; then
    echo -e "${RED}Error: MCP Toolbox server process died immediately${NC}"
    echo "Toolbox log contents:"
    cat toolbox.log 2>/dev/null || echo "No log file created"
    exit 1
fi

# Wait for server to start
echo "Waiting for MCP Toolbox server to start..."
for i in {1..30}; do
    if curl -s http://127.0.0.1:5000/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ MCP Toolbox server is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}Error: MCP Toolbox server failed to start${NC}"
        echo "Toolbox log contents:"
        cat toolbox.log 2>/dev/null || echo "No log file created"
        exit 1
    fi
    sleep 1
done

# Run the ADK agent
echo -e "${GREEN}Starting ADK Agent...${NC}"
cd python
python oracle_ai_database_adk_mcp_agent.py "$@"
