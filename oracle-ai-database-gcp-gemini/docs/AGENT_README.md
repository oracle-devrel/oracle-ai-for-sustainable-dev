# Oracle AI Database ADK Agent

Python-based AI agent using Google's Agent Development Kit (ADK) that interfaces with the Oracle Database RAG API.

## Overview

This agent provides a conversational interface to query the Oracle Database knowledge base. It uses:
- **Google Vertex AI** for LLM capabilities (Gemini)
- **Google ADK** (Agent Development Kit) for agent orchestration
- **Oracle RAG API** as a tool for knowledge retrieval

## Features

- ✅ **Conversational Interface**: CLI-based chat with the agent
- ✅ **Knowledge Base Queries**: Ask questions about Oracle Database
- ✅ **Status Checks**: Monitor API and knowledge base health
- ✅ **Detailed Responses**: Shows timing metrics and source information
- ✅ **Error Handling**: Graceful fallbacks for API issues

## Installation

### 1. Install Dependencies

```bash
pip install -r requirements-adk.txt
```

### 2. Configure Environment

Add to your `.env` file:

```bash
# Oracle RAG API URL (defaults to GCP server if not set)
ORACLE_RAG_API_URL=http://YOUR_PUBLIC_AGENT_HOST:8501

# GCP Configuration (already set for other apps)
GCP_PROJECT_ID=adb-pm-prod
GCP_REGION=us-central1
```

### 3. Ensure RAG API is Running

The agent requires the Oracle RAG API to be running:

```bash
# In another terminal or background
./run_api.sh
```

## Usage

### Run the Agent

```bash
# Make script executable (first time)
chmod +x run_agent.sh

# Run the agent
./run_agent.sh
```

Or run directly:

```bash
python oracle_ai_database_adk_agent.py
```

### Example Session

```
======================================================================
Oracle Database AI Agent (powered by Google ADK)
======================================================================
API URL: http://YOUR_PUBLIC_AGENT_HOST:8501
Project: adb-pm-prod
Region: us-central1

✓ Knowledge base: 428 documents
✓ Status: operational

Type your questions about Oracle Database (or 'quit' to exit)
----------------------------------------------------------------------

You: What are the new spatial features in Oracle Database?

🔍 Searching knowledge base...

💡 Answer:
Oracle Database 26ai introduces several new spatial features including...

📊 Retrieved 5 chunks in 2.34s
   - Vector search: 0.12s
   - LLM response: 2.22s

You: status

✓ Status: operational
✓ Documents: 428
✓ Database: Connected
✓ Models: Loaded

You: quit

Goodbye!
```

## Commands

| Command | Description |
|---------|-------------|
| `<question>` | Ask any question about Oracle Database |
| `status` or `health` | Check API and knowledge base status |
| `quit`, `exit`, or `q` | Exit the agent |

## Architecture

```
┌─────────────────────────┐
│  User (CLI)             │
└───────────┬─────────────┘
            │
┌───────────▼─────────────┐
│  ADK Agent              │
│  oracle_ai_database_    │
│  adk_agent.py           │
└───────────┬─────────────┘
            │ HTTP/REST
┌───────────▼─────────────┐
│  FastAPI RAG API        │
│  (port 8501)            │
└───────────┬─────────────┘
            │
    ┌───────┴────────┐
    │                │
┌───▼────┐    ┌─────▼──────┐
│ Oracle │    │ Vertex AI  │
│   DB   │    │ Embeddings │
│ Vector │    │    LLM     │
└────────┘    └────────────┘
```

## Tool Definitions

The agent has access to these tools:

### 1. `query_oracle_database`
- **Purpose**: Search the Oracle Database knowledge base
- **Parameters**:
  - `query` (string): The question to ask
  - `top_k` (integer, optional): Number of chunks to retrieve (default: 5)
- **Returns**: Answer with context and timing metrics

### 2. `check_knowledge_base_status`
- **Purpose**: Check system health and document count
- **Parameters**: None
- **Returns**: Status information

## Agent Instructions

The agent is configured with these instructions:

> You are a helpful assistant specializing in Oracle Database.
> 
> When users ask questions about Oracle Database:
> 1. Use the query_oracle_database tool to search the knowledge base
> 2. Provide clear, accurate answers based on the retrieved information
> 3. If the knowledge base doesn't have relevant information, say so clearly
> 4. You can also use your general knowledge, but prefer the knowledge base

## Advanced: Deploying the Agent

### Option 1: Run on GCP VM

Already set up - just SSH to your VM and run:

```bash
cd /home/YOUR_VM_SSH_USER/vectors/oracle-ai-database-gcp-gemini
./run_agent.sh
```

### Option 2: Create API Endpoint

Modify the agent to expose as a web service:

```python
from fastapi import FastAPI

app = FastAPI()

@app.post("/chat")
async def chat(message: str):
    agent = OracleRAGAgent(...)
    result = agent.query_oracle_rag(message)
    return result
```

### Option 3: Deploy as Reasoning Engine

Use Vertex AI Reasoning Engines to deploy:

```python
agent_instance = agent.create_agent()

# Deploy to Vertex AI
deployed_agent = reasoning_engines.ReasoningEngine.deploy(
    agent_instance,
    display_name="oracle-db-agent"
)
```

## Extending the Agent

### Add More Tools

```python
tools.append({
    "function_declarations": [{
        "name": "search_web",
        "description": "Search the web for additional information",
        "parameters": {...}
    }]
})
```

### Connect to Other APIs

```python
def query_oracle_support(case_id: str) -> str:
    """Query Oracle Support for case information"""
    # Implementation
    pass
```

### Add Memory/Context

```python
from langchain.memory import ConversationBufferMemory

memory = ConversationBufferMemory()
# Use in agent executor
```

## Troubleshooting

### Agent can't connect to API

```
❌ Error: Failed to query Oracle RAG API: Connection refused
```

**Solution**: Ensure RAG API is running:
```bash
./run_api.sh
```

### Authentication errors

```
❌ Error: Could not automatically determine credentials
```

**Solution**: Set up GCP authentication:
```bash
gcloud auth application-default login
```

### No documents in knowledge base

```
✓ Knowledge base: 0 documents
```

**Solution**: Upload documents to the RAG API:
```bash
curl -X POST "http://YOUR_PUBLIC_AGENT_HOST:8501/upload" -F "file=@document.pdf"
```

## Performance Tips

1. **Adjust `top_k`**: Lower values (3-5) for faster responses, higher (10-15) for more context
2. **Cache common queries**: Store frequent Q&A pairs
3. **Batch processing**: Process multiple questions in parallel
4. **Connection pooling**: Reuse HTTP connections to API

## Future Enhancements

- [ ] Web-based chat interface
- [ ] Multi-modal support (images, diagrams)
- [ ] Document upload through agent
- [ ] Conversation history/memory
- [ ] Integration with Oracle Support APIs
- [ ] Voice interface
- [ ] Slack/Teams integration

## Related Files

- `oracle_ai_database_rag.py` - The RAG API server
- `rag_app_ui.py` - Streamlit UI
- `run_api.sh` - Start RAG API
- `run_agent.sh` - Start this agent

## License

Same as parent project.
