# Oracle AI Database ADK RAG Agent

## How ADK is Used

This implementation uses **Google ADK (Agent Development Kit)** to create a production-ready RAG agent with Oracle Database integration.

### ADK Components

1. **`LlmAgent`** - Core ADK agent class that wraps Gemini LLM with tools and instructions
   ```python
   self.agent = LlmAgent(
       model="gemini-2.0-flash-001",
       instruction=instruction,
       tools=[rag_tool]
   )
   ```

2. **`BaseTool`** - ADK's tool interface for extending agent capabilities
   ```python
   class OracleRAGTool(BaseTool):
       def execute(self, query: str, top_k: int = 3):
           # Custom RAG logic using Oracle Vector Store
   ```

3. **`Runner`** - ADK's execution engine that manages agent lifecycle
   ```python
   self.runner = Runner(
       agent=self.agent,
       session_service=self.session_service
   )
   response = await self.runner.run_async(...)
   ```

4. **Session Management** - ADK's `InMemorySessionService` tracks conversation state

5. **Artifact Storage** - ADK's `InMemoryArtifactService` manages generated content

### What ADK Provides

- **Tool orchestration** - Gemini decides when to call `query_oracle_database` tool
- **Multi-turn conversations** - Session management across queries
- **Error handling** - Automatic retries and error reporting
- **Streaming responses** - Event-based async execution

### Custom Components

The RAG logic (Oracle DB + vector search) is custom-built, but ADK handles all the agent framework, LLM interaction, and tool execution.

## LLM Comparison

### This ADK Agent
- **LLM**: `gemini-2.0-flash-001` (stable production version)
- **Framework**: Google ADK
- **Temperature**: 0.2
- **Max Tokens**: 2048
- **Use Case**: Production agentic system with tool calling

### rag_app_ui.py
- **LLM**: `gemini-2.0-flash-exp` (experimental version)
- **Framework**: LangChain VertexAI
- **Temperature**: 0.7
- **Max Tokens**: 8192
- **Use Case**: Streamlit UI with more creative responses

**Key Difference**: This ADK agent uses the **stable `gemini-2.0-flash-001`** model to avoid rate limiting issues, while `rag_app_ui.py` uses the experimental variant which has stricter quotas.

## Architecture

```
User Input
    ↓
ADK Runner
    ↓
LlmAgent (gemini-2.0-flash-001)
    ↓
OracleRAGTool (custom tool)
    ↓
Oracle Vector Store (RAG_TAB)
    ↓
Vertex AI Embeddings (text-embedding-004)
    ↓
Response
```
