# Oracle ADK Agent Files - Comparison

## Three Different Agent Implementations

This directory contains **three different** Oracle AI Database agent implementations, each with a different architecture and purpose.

---

## 1. oracle_ai_database_adk_agent.py (MCP-Enhanced Version) ⭐

**Size:** 12KB | **Lines:** ~312

### Architecture
- **Framework:** Google Agent Development Kit (ADK)
- **Import:** `from google.adk.agents import LlmAgent`
- **MCP Integration:** ✅ Yes (Oracle SQLcl MCP Server)
- **RAG Integration:** ✅ Yes (FastAPI endpoint)

### Key Features
- Dual tool approach: RAG + MCP database access
- Async implementation with `asyncio`
- Uses `MCPToolset` with `StdioServerParameters`
- Connects to Oracle via `paulparkdb_mcp` connection
- Intelligent tool selection based on query type

### Tools Available
1. **RAG Tool** - Documentation search via vector similarity
2. **MCP Tools** (6+):
   - `list-connections`
   - `connect`
   - `disconnect`
   - `run-sql`
   - `run-sqlcl`
   - `schema-information`

### When to Use
- ✅ Production use with both documentation search and database queries
- ✅ When you need direct SQL execution
- ✅ Schema inspection and database metadata
- ✅ Full agent capabilities with MCP tools

### Example Query
```python
# Can handle both types of questions
"What are vector indexes?" → Uses RAG
"Show me tables created today" → Uses MCP
```

---

## 2. oracle_ai_database_adk_agent_ragonlynomcp.py (RAG-Only Backup) 📦

**Size:** 10KB | **Lines:** ~251

### Architecture
- **Framework:** Vertex AI Reasoning Engines
- **Import:** `from vertexai.preview import reasoning_engines`
- **MCP Integration:** ❌ No
- **RAG Integration:** ✅ Yes (FastAPI endpoint)

### Key Features
- Original version before MCP integration
- Uses `LangchainAgent` from reasoning engines
- Simpler architecture
- Function declarations for tool definitions
- Sync implementation (no async)

### Tools Available
1. **query_oracle_database** - Search RAG knowledge base
2. **check_knowledge_base_status** - Health check

### When to Use
- ✅ Simple RAG-only scenarios
- ✅ When MCP/database access not needed
- ✅ Reference for pre-MCP implementation
- ✅ Learning/understanding RAG basics

### Example Query
```python
# Only documentation questions
"What are vector indexes?" → Uses RAG ✅
"Show me tables created today" → Cannot do ❌
```

---

## 3. oracle_ai_database_adk_fullagent.py (Gemini Function Calling) 🔧

**Size:** 14KB | **Lines:** ~362

### Architecture
- **Framework:** Direct Vertex AI Gemini
- **Import:** `from vertexai.generative_models import GenerativeModel`
- **MCP Integration:** ⚠️ Placeholder (not implemented)
- **RAG Integration:** ✅ Yes (FastAPI endpoint)

### Key Features
- Uses Gemini's native function calling
- Conversation history management
- Multi-turn chat support
- Tool definitions via `FunctionDeclaration`
- Detailed logging and debugging

### Tools Available
1. **query_oracle_database** - Search RAG knowledge base with detailed parameters
2. **check_knowledge_base_status** - API health check

### When to Use
- ✅ When you want direct Gemini API control
- ✅ Multi-turn conversational flows
- ✅ Custom function calling logic
- ✅ Detailed debugging of tool calls

### Example Query
```python
# Multi-turn conversation support
"What are vector indexes?"
  → Follow-up: "Tell me more about the HNSW algorithm"
```

---

## Detailed Comparison Table

| Feature | adk_agent.py (MCP) | adk_agent_ragonlynomcp.py | adk_fullagent.py |
|---------|-------------------|---------------------------|------------------|
| **Framework** | ADK | Reasoning Engines | Direct Gemini |
| **MCP Support** | ✅ Full | ❌ None | ⚠️ Placeholder |
| **RAG Support** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Database Queries** | ✅ SQL via MCP | ❌ No | ❌ No |
| **Async** | ✅ Yes | ❌ No | ❌ No |
| **Tool Count** | 7+ | 2 | 2 |
| **Conversation History** | ✅ Yes | ⚠️ Limited | ✅ Yes |
| **Multi-turn Chat** | ✅ Yes | ⚠️ Limited | ✅ Yes |
| **Complexity** | High | Low | Medium |
| **Size** | 12KB | 10KB | 14KB |
| **Best For** | Production+DB | Learning/Simple | Conversations |

---

## Import Differences

### adk_agent.py (MCP Version)
```python
from google.adk.agents import LlmAgent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.adk.tools.mcp_tool.mcp_toolset import MCPToolset, StdioServerParameters
```

### adk_agent_ragonlynomcp.py (RAG Only)
```python
from google.cloud import aiplatform
from vertexai.preview import reasoning_engines
# No MCP imports
```

### adk_fullagent.py (Gemini Direct)
```python
from vertexai.generative_models import GenerativeModel, Tool, FunctionDeclaration
# No ADK framework imports
```

---

## Agent Initialization Differences

### 1. MCP Version (adk_agent.py)
```python
# Create MCP server parameters
oracle_mcp_params = StdioServerParameters(
    command="/opt/sqlcl/bin/sql",
    args=["-mcp"],
    env={"TNS_ADMIN": wallet_path}
)

# Create agent with MCPToolset
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

### 2. RAG Only Version (adk_agent_ragonlynomcp.py)
```python
# Define function declarations
tools = [{
    "function_declarations": [
        {
            "name": "query_oracle_database",
            "description": "Search the Oracle Database knowledge base...",
            "parameters": {...}
        }
    ]
}]

# Create reasoning engine agent
agent = reasoning_engines.LangchainAgent(
    model="gemini-2.0-flash-exp",
    tools=tools,
    agent_executor_kwargs={"return_intermediate_steps": True}
)
```

### 3. Full Agent Version (adk_fullagent.py)
```python
# Define tools using FunctionDeclaration
query_db_func = FunctionDeclaration(
    name="query_oracle_database",
    description="Search the Oracle Database knowledge base...",
    parameters={...}
)

# Create Gemini model with tools
model = GenerativeModel(
    "gemini-2.0-flash-exp",
    tools=[Tool(function_declarations=[query_db_func, status_func])]
)
```

---

## Execution Flow Differences

### MCP Version (adk_agent.py)
```
User Input
    ↓
ADK Runner
    ↓
Agent decides tool
    ↓
    ┌─────────┴────────┐
    ↓                  ↓
RAG Tool          MCP Toolset
    ↓                  ↓
FastAPI          Oracle DB (via SQLcl)
    ↓                  ↓
Response         Query Results
    ↓                  ↓
    └─────────┬────────┘
              ↓
      Combined Response
```

### RAG Only (adk_agent_ragonlynomcp.py)
```
User Input
    ↓
Reasoning Engine
    ↓
query_oracle_database()
    ↓
FastAPI RAG
    ↓
Response
```

### Full Agent (adk_fullagent.py)
```
User Input
    ↓
Gemini GenerativeModel
    ↓
Function Calling
    ↓
Tool Execution
    ↓
Response with History
```

---

## Dependencies Comparison

### Common to All
```txt
requests
python-dotenv
google-cloud-aiplatform
vertexai
```

### MCP Version Only
```txt
google-adk>=1.4.2
google-genai>=1.0.0
mcp>=0.9.0
```

---

## Use Case Recommendations

### Choose `oracle_ai_database_adk_agent.py` (MCP) when:
- ✅ Need both documentation search AND database queries
- ✅ Want to execute SQL directly
- ✅ Need schema inspection
- ✅ Building production application with full capabilities
- ✅ Have Oracle SQLcl MCP server configured

### Choose `oracle_ai_database_adk_agent_ragonlynomcp.py` when:
- ✅ Only need documentation search
- ✅ Learning RAG concepts
- ✅ Don't have MCP server setup
- ✅ Want simplest possible implementation
- ✅ Don't need database access

### Choose `oracle_ai_database_adk_fullagent.py` when:
- ✅ Need conversational multi-turn chat
- ✅ Want direct control over Gemini API
- ✅ Need detailed tool call debugging
- ✅ Building chatbot with history
- ✅ Don't need MCP (yet)

---

## Migration Path

If you want to migrate between versions:

### From RAG-Only → MCP Version
1. Install MCP dependencies
2. Configure SQLcl MCP server
3. Create saved connection (`paulparkdb_mcp`)
4. Use `oracle_ai_database_adk_agent.py`

### From Full Agent → MCP Version
1. Refactor from `GenerativeModel` to `LlmAgent`
2. Add MCP tools configuration
3. Update tool definitions
4. Convert sync to async

---

## Running Each Version

### MCP Version
```bash
./run_adk_mcp_agent.sh
# or
python oracle_ai_database_adk_agent.py
```

### RAG Only
```bash
python oracle_ai_database_adk_agent_ragonlynomcp.py
```

### Full Agent
```bash
python oracle_ai_database_adk_fullagent.py
```

---

## Which Version is Active?

**Currently Active for Production:**
- `oracle_ai_database_adk_agent.py` (MCP version) ⭐

**Backup/Reference:**
- `oracle_ai_database_adk_agent_ragonlynomcp.py`

**Alternative/Experimental:**
- `oracle_ai_database_adk_fullagent.py`

---

## Summary

| File | Purpose | Production Ready |
|------|---------|------------------|
| `oracle_ai_database_adk_agent.py` | MCP + RAG agent | ✅ Yes |
| `oracle_ai_database_adk_agent_ragonlynomcp.py` | RAG only (backup) | ✅ Yes (limited) |
| `oracle_ai_database_adk_fullagent.py` | Conversation-focused | ⚠️ Experimental |

Choose based on your needs: **MCP version for full capabilities**, **RAG-only for simplicity**, **Full agent for conversations**.
