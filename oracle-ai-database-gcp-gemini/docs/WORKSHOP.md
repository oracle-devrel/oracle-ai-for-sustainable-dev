# Oracle AI Database RAG with Google Vertex AI Agents Workshop

## Workshop Overview

This workshop demonstrates building a production-ready RAG (Retrieval Augmented Generation) system using Oracle Autonomous Database, Google Vertex AI, and multiple agent interfaces. You'll learn how to create a vector search knowledge base, expose it via FastAPI, and integrate it with Google's Conversational Agents and Agent Development Kit (ADK).

**What You'll Build:**
- Oracle Database vector store with 768-dimensional embeddings
- FastAPI service with OpenAPI specification
- Streamlit UI for document management
- GCP Vertex AI Conversational Agent integration
- Full ADK agent with multi-step reasoning

**Time Required:** 2-3 hours

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Document Ingestion                          │
│  Streamlit UI (port 8502) → Upload PDFs → Oracle RAG_TAB       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Oracle Autonomous Database                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ RAG_TAB: 428 document chunks (768d vectors)              │  │
│  │ - text-embedding-004 embeddings                          │  │
│  │ - COSINE similarity search                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    FastAPI RAG Service                           │
│  oracle_ai_database_rag.py (port 8501)                          │
│  - OpenAPI 3.0.3 specification                                  │
│  - Vertex AI embeddings + Gemini LLM                            │
│  - Internal VPC: http://YOUR_INTERNAL_API_HOST:8501                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┴─────────────────────┐
        ↓                                           ↓
┌──────────────────────┐              ┌──────────────────────────┐
│  GCP Conversational  │              │   ADK Full Agent         │
│       Agents         │              │ oracle_ai_adk_fullagent  │
│                      │              │                          │
│ - OpenAPI Tool Import│              │ - Gemini Function Calling│
│ - Service Agent Auth │              │ - Multi-step Reasoning   │
│ - Web UI Interface   │              │ - Conversation History   │
└──────────────────────┘              └──────────────────────────┘
```

---

## Prerequisites

### Required Accounts & Access
- **Oracle Cloud Account** with Autonomous Database (26ai)
- **Google Cloud Project** with billing enabled
- **APIs Enabled:**
  - Vertex AI API
  - Generative AI API
  - Cloud Resource Manager API

### Local Tools
- Python 3.12+
- Git
- GCP SDK (`gcloud`)
- SSH client

### Knowledge
- Basic Python programming
- REST API concepts
- Vector embeddings fundamentals
- Oracle SQL basics

---

## Part 1: Environment Setup

### 1.1 Clone Repository

```bash
git clone https://github.com/paulparkinson/interactive-ai-holograms.git
cd interactive-ai-holograms/oracle-ai-database-gcp-gemini
```

### 1.2 Configure Oracle Database

1. **Create Autonomous Database** (if not already created):
   - Database Name: `PAULPARKDB_TP`
   - Workload Type: Transaction Processing
   - Version: 26ai
   - Storage: 1TB minimum

2. **Download Wallet**:
   ```bash
   # Place wallet files in ./Wallet_PAULPARKDB directory
   mkdir -p Wallet_PAULPARKDB
   # Download from OCI Console → Autonomous Database → DB Connection
   ```

3. **Create RAG Table**:
   ```sql
   CREATE TABLE rag_tab (
       id NUMBER GENERATED ALWAYS AS IDENTITY,
       text VARCHAR2(4000),
       link VARCHAR2(500),
       embedding VECTOR(768, FLOAT32)
   );
   
   CREATE VECTOR INDEX rag_idx ON rag_tab(embedding)
   ORGANIZATION INMEMORY NEIGHBOR GRAPH
   DISTANCE COSINE;
   ```

### 1.3 Configure GCP

1. **Set Project**:
   ```bash
   gcloud config set project adb-pm-prod
   gcloud config set compute/region us-central1
   ```

2. **Authenticate**:
   ```bash
   gcloud auth login
   gcloud auth application-default login
   ```

3. **Create VM (if needed)**:
   ```bash
   gcloud compute instances create paulpark-instance \
     --zone=us-east4-a \
     --machine-type=e2-medium \
     --image-family=debian-12 \
     --boot-disk-size=50GB
   ```

### 1.4 Configure Environment Variables

Create `.env` file:

```bash
# Oracle Database
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=your_password
DB_DSN=YOUR_DB_DSN_HIGH
DB_WALLET_PASSWORD=your_wallet_password
DB_WALLET_DIR=./Wallet_PAULPARKDB

# Google Cloud
GCP_PROJECT_ID=adb-pm-prod
GCP_REGION=us-central1

# API Configuration
ORACLE_RAG_API_URL=http://YOUR_INTERNAL_API_HOST:8501
API_PORT=8501
STREAMLIT_PORT=8502
```

### 1.5 Install Dependencies

```bash
pip install -r requirements.txt
```

**Key Dependencies:**
- `langchain>=1.0.0` - LangChain framework
- `langchain-google-vertexai>=3.2.0` - Vertex AI integration
- `oracledb` - Oracle database driver
- `fastapi` - REST API framework
- `streamlit` - UI framework
- `vertexai` - Google Vertex AI SDK

---

## Part 2: Document Ingestion with Streamlit

### 2.1 Understanding the Streamlit UI

**File**: `rag_app_ui.py`

**Key Components:**
- PDF upload and parsing (PyPDF2)
- Text chunking (CharacterTextSplitter)
- Vertex AI embeddings generation
- Oracle Vector Store insertion

**Embedding Model**: `text-embedding-004` (768 dimensions)

### 2.2 Run Streamlit UI

```bash
./run.sh
```

Access: `http://your-vm-ip:8502`

### 2.3 Upload Documents

1. **Click "Upload PDF Document"**
2. **Select PDF** (e.g., Oracle Database documentation)
3. **Monitor Processing**:
   - Text extraction
   - Chunking (1000 chars, 200 overlap)
   - Embedding generation
   - Database insertion

4. **Verify Storage**:
   ```sql
   SELECT COUNT(*) FROM rag_tab;
   -- Should show number of chunks
   ```

### 2.4 Test Search

Use the Streamlit UI to query your documents:
- Enter question: "What are new spatial features?"
- View retrieved chunks and generated answer
- Observe timing metrics (vector search vs. LLM)

---

## Part 3: FastAPI Service with OpenAPI

### 3.1 Understanding the FastAPI Service

**File**: `oracle_ai_database_rag.py`

**Architecture**:
```python
@app.post("/query")  # Main RAG endpoint
async def query_knowledge_base(request: QueryRequest):
    # 1. Generate query embedding
    # 2. Vector similarity search (COSINE)
    # 3. Retrieve top_k chunks
    # 4. Create context-aware prompt
    # 5. Call Gemini LLM
    # 6. Return answer + metadata
```

**OpenAPI Compatibility**:
- Version: 3.0.3 (required by GCP)
- No security schemes (managed externally)
- JSON-only content type
- Single server URL (internal VPC)

### 3.2 Start FastAPI Service

```bash
./run_api.sh
```

**Endpoints**:
- `POST /query` - RAG query with answer generation
- `GET /status` - Service health check
- `DELETE /clear` - Clear knowledge base
- `GET /health` - Simple health ping
- `GET /openapi.json` - OpenAPI specification

### 3.3 Test FastAPI Locally

```bash
curl -X POST "http://localhost:8501/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are new JSON features in Oracle Database?",
    "top_k": 5
  }'
```

**Response**:
```json
{
  "answer": "Oracle Database 26ai introduces...",
  "context_chunks": ["chunk1", "chunk2", ...],
  "vector_search_time": 0.15,
  "llm_response_time": 2.3,
  "total_time": 2.45
}
```

### 3.4 OpenAPI Specification

View at: `http://localhost:8501/docs`

**Key Features**:
- Interactive API testing
- Schema validation
- Request/response examples
- Model definitions (QueryRequest, QueryResponse)

---

## Part 4: GCP Conversational Agents Integration

### 4.1 Understanding GCP Agents

**GCP Conversational Agents** provide:
- Natural language interface
- Multi-turn conversation
- Tool/function calling
- Built-in authentication
- Web UI for testing

**Architecture**:
```
User → GCP Agent → OpenAPI Tool → FastAPI → Oracle DB
```

### 4.2 Create OpenAPI Tool

1. **Navigate to Vertex AI Console**:
   - Conversational Agents → Tools → Create Tool

2. **Import OpenAPI Spec**:
   ```
   Method: OpenAPI URL
   URL: http://YOUR_INTERNAL_API_HOST:8501/openapi.json
   ```
   
   **Important**: Use internal VPC IP (YOUR_INTERNAL_API_HOST), not external IP

3. **Configure Authentication**:
   - Type: No auth (API accepts tokens without validation)
   - Alternative: Service agent token (for production)

4. **Verify Tool**:
   - Tool name: `Oracle AI Database (Vector RAG)`
   - Action: `query`
   - Input: `query` (string), `top_k` (integer)
   - Output: JSON response

### 4.3 Create Conversational Agent

1. **Create New Agent**:
   - Name: "Oracle Database Expert"
   - Model: Gemini 2.0 Flash

2. **Add Instructions**:
   ```
   You are an expert assistant for Oracle Database questions.
   
   Use the "query" tool to search the Oracle Database knowledge base
   when users ask about:
   - Database features
   - SQL syntax
   - Configuration
   - Performance optimization
   
   Provide clear, accurate answers based on the retrieved information.
   ```

3. **Attach Tool**:
   - Add previously created OpenAPI tool
   - Set as required for database questions

4. **Configure Settings**:
   - Temperature: 0.7
   - Max tokens: 2048
   - Top-p: 0.95

### 4.4 Test GCP Agent

1. **Open Agent Playground**
2. **Test Queries**:
   - "What are new spatial features in Oracle Database?"
   - "Explain JSON Relational Duality"
   - "How do I optimize vector search performance?"

3. **Monitor Tool Calls**:
   - View tool execution in conversation
   - Check API logs for incoming requests
   - Verify response integration

### 4.5 GCP Agent Limitations

**Known Issues**:
- Security schemes not supported in OpenAPI
- Only JSON content types allowed
- External IPs unreachable (use internal VPC)
- Limited multipart/form-data support

**Solutions Applied**:
- Removed security definitions from OpenAPI
- Filtered content types to `application/json`
- Used internal VPC address (YOUR_INTERNAL_API_HOST)
- Excluded `/upload` endpoint from schema

---

## Part 5: ADK Full Agent Implementation

### 5.1 Understanding Google ADK

**Agent Development Kit (ADK)** provides:
- **Multi-step reasoning**: Agent makes multiple tool calls
- **Conversation context**: Maintains history across turns
- **Function calling**: Native Gemini function calling
- **Extensibility**: Easy to add new tools/capabilities

**vs. GCP Conversational Agents**:
| Feature | GCP Agents | ADK |
|---------|-----------|-----|
| Deployment | Managed service | Custom code |
| UI | Built-in web UI | Build your own |
| Reasoning | Single-step | Multi-step |
| Customization | Limited | Full control |
| Cost | Per-use | Compute + LLM calls |

### 5.2 ADK Architecture

**File**: `oracle_ai_database_adk_fullagent.py`

```python
# 1. Initialize Vertex AI and Gemini
vertexai.init(project=project_id, location=location)

# 2. Define function declarations
query_function = FunctionDeclaration(
    name="query_oracle_database",
    description="Search Oracle knowledge base...",
    parameters={...}
)

# 3. Create tool with functions
oracle_tool = Tool(function_declarations=[query_function, ...])

# 4. Create Gemini model with tools
model = GenerativeModel(
    "gemini-2.0-flash-exp",
    tools=[oracle_tool],
    system_instruction=instructions
)

# 5. Query with function calling
chat = model.start_chat()
response = chat.send_message(user_input)

# 6. Handle function calls iteratively
while response.has_function_call:
    result = execute_function(...)
    response = chat.send_message(function_response)
```

### 5.3 Key Components

**Function Declarations**:
```python
FunctionDeclaration(
    name="query_oracle_database",
    description="Search the Oracle Database knowledge base...",
    parameters={
        "type": "object",
        "properties": {
            "query": {"type": "string", ...},
            "top_k": {"type": "integer", "default": 5}
        },
        "required": ["query"]
    }
)
```

**System Instructions**:
```python
instructions = """You are an expert Oracle Database assistant.

Use query_oracle_database when users ask about:
- Specific features or functionality
- SQL syntax and best practices
- Configuration and administration

For complex questions:
- Break into sub-queries
- Make multiple tool calls
- Synthesize information

Maintain conversation context and reference previous answers."""
```

**Multi-Step Execution**:
```python
max_iterations = 5
while has_function_call and iteration < max_iterations:
    # Execute function
    result = execute_function_call(function_name, args)
    
    # Send result back to model
    response = chat.send_message(
        Part.from_function_response(name=function_name, response=result)
    )
```

### 5.4 Run ADK Agent

```bash
./run_fullagent.sh
```

**Interactive Commands**:
- Type questions naturally
- `history` - View conversation
- `clear` - Reset context
- `quit` - Exit

### 5.5 Test Multi-Step Reasoning

**Example 1: Complex Query**
```
You: Compare spatial features between Oracle 19c and 26ai

Agent reasoning:
  🔧 query_oracle_database(query="Oracle 19c spatial features", top_k=5)
  🔧 query_oracle_database(query="Oracle 26ai spatial features", top_k=5)
  
Agent: Oracle 26ai introduces several enhancements over 19c:
1. Spatial Web Services...
2. Enhanced GeoJSON support...
[Synthesized from 2 tool calls]
```

**Example 2: Follow-up Questions**
```
You: What are new JSON features?
Agent: [Uses context from previous conversation]

You: How do I use those with spatial data?
Agent: [References both previous answers, makes new query]
```

### 5.6 Conversation History

```python
conversation_history = [
    {"user": "What are new features?", "agent": {...}},
    {"user": "How do I use them?", "agent": {...}}
]
```

View history:
```bash
> history

[1] User: What are new spatial features in the oracle database
    Agent: Oracle Database 26ai introduces enhanced spatial capabilities...

[2] User: How do I enable these features?
    Agent: To enable spatial features, you need to...
```

---

## Part 6: Advanced Topics

### 6.1 Embedding Model Details

**Model**: `text-embedding-004`
- Dimensions: 768
- Max input: 20,000 tokens
- Multilingual support
- Cost: $0.00025 per 1K tokens

**Alternative Models**:
- `text-embedding-005`: 256/768/1024 dimensions (configurable)
- `textembedding-gecko@003`: Legacy model
- Custom fine-tuned models

### 6.2 Vector Search Optimization

**Distance Strategies**:
```python
# COSINE (default) - best for normalized embeddings
DistanceStrategy.COSINE

# EUCLIDEAN - faster but requires normalization
DistanceStrategy.EUCLIDEAN_DISTANCE

# MAX_INNER_PRODUCT - for non-normalized vectors
DistanceStrategy.MAX_INNER_PRODUCT
```

**Index Types**:
```sql
-- IVF (Inverted File) - fast for large datasets
CREATE VECTOR INDEX rag_idx ON rag_tab(embedding)
ORGANIZATION INMEMORY NEIGHBOR GRAPH;

-- HNSW (Hierarchical Navigable Small World) - highest accuracy
CREATE VECTOR INDEX rag_idx_hnsw ON rag_tab(embedding)
DISTANCE COSINE WITH TARGET ACCURACY 95;
```

### 6.3 Prompt Engineering

**RAG Prompt Template**:
```python
template = """Use the following context to answer the question.
If you cannot answer based on the context, say so clearly.

Context:
{context}

Question: {question}

Answer:"""
```

**Advanced Techniques**:
- Few-shot examples
- Chain-of-thought reasoning
- Self-consistency
- Retrieval augmentation strategies

### 6.4 Future Enhancements

**MCP Server Integration**:
```python
# Model Context Protocol for extended capabilities
from mcp import MCPServer

mcp_tool = FunctionDeclaration(
    name="call_mcp_server",
    description="Access additional data sources via MCP"
)
```

**Multi-Modal Support**:
```python
# Process images, PDFs with layout
from vertexai.vision_models import MultiModalEmbeddingModel

vision_embeddings = MultiModalEmbeddingModel.from_pretrained(
    "multimodalembedding@001"
)
```

---

## Part 7: Deployment & Production

### 7.1 Cloud Run Deployment

```bash
# Build container
gcloud builds submit --tag gcr.io/adb-pm-prod/oracle-rag-api

# Deploy to Cloud Run
gcloud run deploy oracle-rag-api \
  --image gcr.io/adb-pm-prod/oracle-rag-api \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GCP_PROJECT_ID=adb-pm-prod
```

### 7.2 Security Hardening

**Add API Key Authentication**:
```python
from fastapi.security import APIKeyHeader

api_key_header = APIKeyHeader(name="X-API-Key")

@app.post("/query")
async def query(request: QueryRequest, api_key: str = Depends(api_key_header)):
    if api_key != os.getenv("API_KEY"):
        raise HTTPException(401, "Invalid API key")
```

**Bearer Token Validation**:
```python
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token

def verify_token(token: str):
    idinfo = id_token.verify_oauth2_token(
        token, google_requests.Request()
    )
    return idinfo
```

### 7.3 Monitoring & Logging

```python
import logging
from google.cloud import logging as cloud_logging

# Cloud Logging
client = cloud_logging.Client()
client.setup_logging()

# Log queries
logging.info("Query received", extra={
    "query": query,
    "top_k": top_k,
    "response_time": response_time
})
```

### 7.4 Performance Optimization

**Caching**:
```python
from functools import lru_cache

@lru_cache(maxsize=1000)
def get_embedding(text: str):
    return embeddings.embed_query(text)
```

**Connection Pooling**:
```python
import oracledb

pool = oracledb.create_pool(
    user=username,
    password=password,
    dsn=dsn,
    min=2,
    max=10,
    increment=1
)
```

---

## Troubleshooting

### Common Issues

**Issue**: `langchain.load` module not found
```bash
# Solution: Use Gemini function calling instead of LangchainAgent
# Already implemented in oracle_ai_database_adk_fullagent.py
```

**Issue**: GCP Agent returns authentication error
```bash
# Solution: Use internal VPC IP, not external
URL: http://YOUR_INTERNAL_API_HOST:8501 (not YOUR_PUBLIC_AGENT_HOST)
```

**Issue**: Port 8501 already in use
```bash
# Solution: Streamlit moved to 8502
pkill -f uvicorn  # Stop FastAPI
./run.sh          # Streamlit on 8502
./run_api.sh      # FastAPI on 8501
```

**Issue**: Vector search returns no results
```sql
-- Check embeddings exist
SELECT COUNT(*) FROM rag_tab WHERE embedding IS NOT NULL;

-- Verify index
SELECT * FROM USER_INDEXES WHERE TABLE_NAME = 'RAG_TAB';
```

---

## Resources

### Documentation
- [Oracle AI Vector Search](https://docs.oracle.com/en/database/oracle/oracle-database/23/vecse/)
- [Vertex AI Gemini API](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini)
- [LangChain Documentation](https://python.langchain.com/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

### Code Repository
- GitHub: `paulparkinson/interactive-ai-holograms`
- Directory: `oracle-ai-database-gcp-gemini/`

### Support
- Oracle Cloud Support: https://support.oracle.com
- Google Cloud Support: https://cloud.google.com/support
- Community: Oracle AI Forum

---

## Workshop Summary

You've learned to:
1. ✅ Set up Oracle Database vector store
2. ✅ Ingest documents with Streamlit
3. ✅ Build FastAPI RAG service with OpenAPI
4. ✅ Integrate with GCP Conversational Agents
5. ✅ Implement full ADK agent with multi-step reasoning
6. ✅ Deploy and optimize for production

**Next Steps**:
- Add custom documents to your knowledge base
- Experiment with different embedding models
- Implement MCP server integration
- Deploy to Cloud Run for production
- Build custom UI for your agent

**Workshop Complete!** 🎉
