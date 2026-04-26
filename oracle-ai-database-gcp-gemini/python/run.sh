#!/bin/bash

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Activate virtual environment if it exists
if [ -d ".venv" ]; then
    source .venv/bin/activate
elif [ -d "../.venv" ]; then
    source ../.venv/bin/activate
fi

show_menu() {
    clear
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}Agentic AI with Oracle AI Database and Vertex AI + Gemini${NC}         ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}Application Launcher${NC}                                              ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  PREREQUISITE:${NC} Run ${CYAN}oracle_ai_database_gemini_rag.ipynb${NC} in Jupyter/VS Code first"
    echo -e "    to create the required database tables and vector indexes."
    echo ""
    echo "  1) Streamlit Gemini RAG UI (LangChain + Vertex AI)"
    echo "     → oracle_ai_database_langchain_streamlit.py"
    echo ""
    echo "  2) ADK Agent with Gemini RAG - direct OracleVS"
    echo "     → oracle_ai_database_adk_agent.py"
    echo ""
    echo "  3) ADK Agent with Google MCP Toolbox - requires supported platform, like AMD64"
    echo "     → oracle_ai_database_adk_mcp_agent.py"
    echo ""
    echo "  4) Vertex AI + Oracle MCP - requires SQLcl (manual MCP)"
    echo "     → oracle_ai_database_genai_mcp.py"
    echo ""
    echo "  5) ADK Agent with SQLcl MCP (McpToolset) - requires SQLcl + Java"
    echo "     → oracle_ai_database_adk_sqlcl_mcp_agent.py"
    echo ""
    echo -e "${YELLOW}Other Options:${NC}"
    echo "  6) Show environment configuration"
    echo "  7) Refresh Google ADC authentication"
    echo "     → ../auth.sh"
    echo "  0) Exit"
    echo ""
    echo -e "${BLUE}────────────────────────────────────────────────────────────────────${NC}"
}

run_streamlit() {
    echo -e "${CYAN}Starting Streamlit RAG Application...${NC}"
    echo "Server will be available at: http://localhost:8502"
    echo ""
    streamlit run oracle_ai_database_langchain_streamlit.py --server.port 8502
}

run_adk_agent() {
    echo -e "${CYAN}Starting ADK Agent with Custom BaseTool...${NC}"
    echo ""
    python oracle_ai_database_adk_agent.py
}

run_adk_mcp_agent() {
    echo -e "${CYAN}Starting ADK Agent with MCP Toolbox...${NC}"
    echo "Note: Requires AMD64 platform or Docker"
    echo ""
    bash run_oracle_ai_database_adk_mcp_agent.sh
}

run_adk_mcp_docker() {
    echo -e "${CYAN}Starting ADK Agent with MCP Toolbox (Docker)...${NC}"
    echo ""
    bash run_oracle_ai_database_adk_mcp_agent_docker.sh
}

run_genai_mcp() {
    echo -e "${CYAN}Starting GenerativeModel + Oracle SQLcl MCP...${NC}"
    echo -e "${YELLOW}Note: Requires SQLcl with MCP support AND Java${NC}"
    echo ""
    python oracle_ai_database_genai_mcp.py
}

run_adk_sqlcl_mcp() {
    echo -e "${CYAN}Starting ADK Agent with SQLcl MCP (McpToolset)...${NC}"
    echo -e "${YELLOW}Note: Requires SQLcl with MCP support, Java, and google-adk>=1.25.1${NC}"
    echo ""
    python oracle_ai_database_adk_sqlcl_mcp_agent.py
}

run_auth() {
    echo -e "${CYAN}Refreshing Google ADC authentication...${NC}"
    echo ""
    bash ../auth.sh
}

show_config() {
    echo -e "${CYAN}Environment Configuration:${NC}"
    echo ""
    echo "Database:"
    echo "  DB_USERNAME: ${DB_USERNAME:-<not set>}"
    echo "  DB_DSN: ${DB_DSN:-<not set>}"
    echo "  DB_WALLET_DIR: ${DB_WALLET_DIR:-<not set>}"
    echo ""
    echo "Google Cloud:"
    echo "  GCP_PROJECT_ID: ${GCP_PROJECT_ID:-<not set>}"
    echo "  GCP_REGION: ${GCP_REGION:-<not set>}"
    echo ""
    echo "Python Environment:"
    python --version 2>/dev/null || echo "  Python: Not found"
    echo "  Virtual Env: ${VIRTUAL_ENV:-<not activated>}"
    echo ""
    echo "Platform:"
    echo "  OS: $(uname -s)"
    echo "  Architecture: $(uname -m)"
    echo ""
    echo "Press Enter to continue..."
    read
}

# Main loop
while true; do
    show_menu
    echo -n "Enter your choice [0-7]: "
    read choice
    echo ""

    case $choice in
        1)
            run_streamlit
            ;;
        2)
            run_adk_agent
            ;;
        3)
            run_adk_mcp_agent
            ;;
        4)
            run_genai_mcp
            ;;
        5)
            run_adk_sqlcl_mcp
            ;;
        6)
            show_config
            ;;
        7)
            run_auth
            ;;
        0)
            echo -e "${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${YELLOW}Invalid option. Press Enter to continue...${NC}"
            read
            ;;
    esac

    # After running a command, wait for user
    if [ "$choice" != "6" ] && [ "$choice" != "0" ]; then
        echo ""
        echo -e "${YELLOW}Press Enter to return to menu...${NC}"
        read
    fi
done
