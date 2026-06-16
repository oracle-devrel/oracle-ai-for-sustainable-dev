# Oracle ADK Agent with MCP Integration

## Overview

Two versions of the Oracle AI Database ADK Agent:

1. **oracle_ai_database_adk_agent.py** (NEW - MCP Enabled)
   - Integrates Oracle MCP Server for direct database queries
   - Uses Vector RAG for documentation search
   - Full ADK agent with multi-tool capabilities

2. **oracle_ai_database_adk_agent_ragonlynomcp.py** (Original - Backup)
   - RAG only (no MCP)
   - Simpler implementation for reference

3. **oracle_ai_database_adk_fullagent.py** (Alternative Implementation)
   - Uses direct Vertex AI Gemini (not ADK framework)
   - Function calling approach
   - Different architecture

## Key Features (MCP Version)

### Dual Tool Approach

**1. Oracle RAG Knowledge Base (query_oracle_rag_kb)**
- Search Oracle Database documentation
- Vector similarity search
- Best for "what is" and "how to" questions

**2. Oracle MCP Database Server**
- Direct database operations via SQLcl MCP server
- Tools available:
  - `list-connections` - List saved database connections
  - `connect` - Connect to database
  - `run-sql` - Execute SQL queries
  - `run-sqlcl` - Execute SQLcl commands
  - `schema-information` - Get detailed schema metadata
  - `disconnect` - Close database connection

## Architecture

```
┌─────────────────────────────────────────────────┐
│         ADK Agent (Gemini 2.0 Flash)           │
│  Orchestrates tools and conversation flow       │
└─────────────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│   RAG API Tool   │    │  MCP Toolset     │
│  (HTTP/FastAPI)  │    │  (stdio/SQLcl)   │
└──────────────────┘    └──────────────────┘
        │                       │
        ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│  Vector Search   │    │ Oracle Database  │
│  (Oracle 26ai)   │    │   paulparkdb     │
│  428 doc chunks  │    │  via paulparkdb_ │
└──────────────────┘    │  mcp connection  │
                        └──────────────────┘
```

## Configuration

### Environment Variables

```bash
# RAG API Configuration
export ORACLE_RAG_API_URL="http://YOUR_PUBLIC_AGENT_HOST:8501"

# GCP Configuration
export GCP_PROJECT_ID="adb-pm-prod"
export GCP_REGION="us-central1"

# MCP Server Configuration
export SQLCL_PATH="/opt/sqlcl/bin/sql"
export TNS_ADMIN="$HOME/wallet"
```

### MCP Server Setup

The agent connects to Oracle MCP server using:

```json
{
  "command": "/opt/sqlcl/bin/sql",
  "args": ["-mcp"],
  "env": {
    "TNS_ADMIN": "/path/to/wallet"
  }
}
```

**Database Connection**: `paulparkdb_mcp`
- Pre-configured in SQLcl using: 
  ```bash
  conn -save YOUR_DB_CONN_NAME -savepwd YOUR_DB_USERNAME/YOUR_DB_PASSWORD@YOUR_DB_DSN
  ```

## Installation

```bash
# Install dependencies
pip install -r requirements-adk.txt

# Key packages:
# - google-adk>=1.4.2 (Agent Development Kit)
# - google-genai>=1.0.0 (Gemini SDK)
# - mcp>=0.9.0 (Model Context Protocol)
```

## Usage

### Run the Agent

**Linux/Mac:**
```bash
chmod +x run_adk_mcp_agent.sh
./run_adk_mcp_agent.sh
```

**Windows:**
```powershell
.\run_adk_mcp_agent.ps1
```

**Direct Python:**
```bash
python oracle_ai_database_adk_agent.py
```

### Example Interactions

**Documentation Questions (Uses RAG):**
```
You: What are the new spatial features in Oracle 26ai?
Agent: [Searches RAG knowledge base for documentation]
```

**Database Queries (Uses MCP):**
```
You: Show me the most recently created tables in the database
Agent: [Uses MCP to connect and run SQL query]
```

**Combined Queries:**
```
You: Explain vector indexes and show me any vector indexes in the database
Agent: [Uses RAG for explanation, MCP for database inspection]
```

## Agent Instructions

The agent is configured to intelligently choose the right tool:

- **RAG Knowledge Base**: For conceptual/documentation questions
  - "What is..."
  - "How to..."
  - Features, capabilities, syntax

- **MCP Database**: For operational queries
  - "Show me data..."
  - "List tables..."
  - "Run this SQL..."
  - Schema inspection

## Implementation Details

### MCP Integration

```python
# Configure MCP server
oracle_mcp_params = StdioServerParameters(
    command=self.sqlcl_path,
    args=["-mcp"],
    env={"TNS_ADMIN": self.wallet_path}
)

# Create ADK agent with MCP toolset
agent = LlmAgent(
    model="gemini-2.0-flash-exp",
    name="oracle_ai_assistant",
    instruction=instructions,
    tools=[
        MCPToolset(connection_params=oracle_mcp_params),
        self._create_rag_tool()
    ]
)
```

### Custom RAG Tool

The RAG API is wrapped as a Python function for ADK:

```python
def query_oracle_rag_kb(query: str, top_k: int = 5) -> str:
    """Search the Oracle Database knowledge base"""
    result = self.query_oracle_rag(query, top_k)
    return f"{result['answer']}\n\n[Source: {chunks} chunks]"
```

## Comparison: MCP vs Non-MCP Versions

| Feature | MCP Version | Non-MCP Version |
|---------|-------------|-----------------|
| Documentation Search | ✅ Yes (RAG) | ✅ Yes (RAG) |
| Direct Database Queries | ✅ Yes (MCP) | ❌ No |
| SQL Execution | ✅ Yes | ❌ No |
| Schema Inspection | ✅ Yes | ❌ No |
| Connection Management | ✅ Yes | ❌ No |
| Async Support | ✅ Yes | ⚠️ Limited |
| Tool Count | 6+ tools | 2 tools |
| Complexity | Higher | Lower |

## Troubleshooting

### MCP Connection Issues

1. **Check SQLcl installation:**
   ```bash
   /opt/sqlcl/bin/sql -version
   ```

2. **Verify wallet path:**
   ```bash
   ls -la $TNS_ADMIN
   # Should show: tnsnames.ora, sqlnet.ora, ewallet.p12, etc.
   ```

3. **Test MCP connection:**
   ```bash
   export TNS_ADMIN=/path/to/wallet
   /opt/sqlcl/bin/sql -mcp
   ```

4. **Verify saved connection:**
   ```bash
   sql /nolog
   SQL> show connections
   ```

### Fallback Mode

If MCP initialization fails, the agent automatically falls back to RAG-only mode:

```
❌ Failed to initialize agent: [error details]

Falling back to simple RAG-only mode...
```

## References

- [Google ADK Documentation](https://google.github.io/adk-docs/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Oracle SQLcl MCP Server](https://www.oracle.com/database/sqlcl/)
- [ADK MCP Examples](https://github.com/GoogleCloudPlatform/generative-ai/tree/main/gemini/mcp/adk_mcp_app)

## Files

- `oracle_ai_database_adk_agent.py` - Main agent with MCP
- `oracle_ai_database_adk_agent_ragonlynomcp.py` - Original backup (RAG only)
- `oracle_ai_database_adk_fullagent.py` - Alternative implementation (Gemini function calling)
- `run_adk_mcp_agent.sh` - Linux/Mac runner script
- `run_adk_mcp_agent.ps1` - Windows runner script
- `requirements-adk.txt` - Python dependencies
- `ADK_AGENT_README.md` - This file
- `EMBEDDINGS_COMPARISON.md` - HuggingFace vs Vertex AI embeddings guide
