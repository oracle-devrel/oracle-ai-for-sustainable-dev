# Oracle AI Database RAG API

FastAPI service exposing OpenAPI endpoints for Oracle Database RAG (Retrieval Augmented Generation) using Google Vertex AI. This API can be integrated with GCP Vertex AI Agents and Agent Development Kit (ADK) as an OpenAPI tool.

## Features

- **Vector Search**: Query documents using semantic similarity search with Oracle Database Vector Store
- **Document Upload**: Upload and process PDF documents into vectorized chunks
- **OpenAPI Compatible**: Full OpenAPI 3.0 specification for easy agent integration
- **Production Ready**: Built with FastAPI for high performance and async support
- **Monitoring**: Health check and status endpoints

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | API information |
| `/query` | POST | Query the knowledge base |
| `/upload` | POST | Upload PDF documents |
| `/status` | GET | Service status and document count |
| `/clear` | DELETE | Clear all documents |
| `/health` | GET | Health check |
| `/docs` | GET | Interactive OpenAPI documentation (Swagger UI) |
| `/redoc` | GET | Alternative API documentation (ReDoc) |
| `/openapi.json` | GET | OpenAPI specification JSON |

## Installation

### 1. Install Dependencies

```bash
pip install -r requirements-api.txt
```

### 2. Configure Environment Variables

Ensure your `.env` file contains:

```bash
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=your_password
DB_DSN=YOUR_DB_DSN
DB_WALLET_PASSWORD=your_wallet_password
DB_WALLET_DIR=/path/to/wallet
GCP_PROJECT_ID=adb-pm-prod
GCP_REGION=us-central1
```

### 3. Start the API

```bash
# Make script executable
chmod +x run_api.sh

# Run the API
./run_api.sh
```

Or run directly:

```bash
uvicorn oracle_ai_database_rag:app --host 0.0.0.0 --port 8000 --reload
```

## Usage Examples

### Query the Knowledge Base

```bash
curl -X POST "http://localhost:8000/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the new spatial features in Oracle Database?",
    "top_k": 5
  }'
```

Response:
```json
{
  "answer": "Based on the documents...",
  "context_chunks": ["chunk1 text...", "chunk2 text..."],
  "vector_search_time": 0.123,
  "llm_response_time": 1.456,
  "total_time": 1.579
}
```

### Upload a Document

```bash
curl -X POST "http://localhost:8000/upload" \
  -F "file=@document.pdf"
```

Response:
```json
{
  "message": "Successfully processed document.pdf",
  "chunks_created": 42,
  "processing_time": 3.456
}
```

### Check Status

```bash
curl "http://localhost:8000/status"
```

Response:
```json
{
  "status": "operational",
  "document_count": 428,
  "database_connected": true,
  "models_loaded": true
}
```

## Integration with GCP Vertex AI Agents

### Step 1: Deploy the API

Deploy to a publicly accessible endpoint (Cloud Run, GCE, etc.):

```bash
# Example: Deploy to Cloud Run
gcloud run deploy oracle-rag-api \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

### Step 2: Get OpenAPI Specification

Download the OpenAPI spec:

```bash
curl http://your-api-url/openapi.json > oracle_rag_openapi.json
```

### Step 3: Add as Tool in Vertex AI Agent Builder

1. Go to **Vertex AI** → **Agent Builder** → **Tools**
2. Click **Create Tool** → **OpenAPI**
3. Upload `oracle_rag_openapi.json` or paste the OpenAPI spec
4. Configure authentication if needed
5. Test the tool with sample queries

### Step 4: Create an Agent Using the Tool

1. Go to **Agent Builder** → **Agents** → **Create Agent**
2. Add your data store (if applicable)
3. Under **Tools**, add your Oracle RAG API tool
4. Configure agent instructions:

```
You are a helpful assistant with access to a document knowledge base.
Use the queryKnowledgeBase tool to answer questions about documents.
When a user asks a question, call the tool with their query and provide
the answer from the tool's response.
```

### Step 5: Test the Agent

Test queries like:
- "What are the new features in Oracle Database?"
- "Explain the spatial capabilities mentioned in the documents"

## Integration with Agent Development Kit (ADK)

### Example ADK Configuration

```python
from google.cloud import aiplatform
from google.cloud.aiplatform import gapic

# Initialize ADK
aiplatform.init(project='your-project', location='us-central1')

# Define OpenAPI tool
openapi_tool = {
    "openapi_spec": {
        "url": "http://your-api-url/openapi.json"
    }
}

# Create agent with tool
agent = aiplatform.Agent.create(
    display_name="Oracle RAG Agent",
    description="Agent with Oracle Database RAG capabilities",
    tools=[openapi_tool],
    instructions="Use the queryKnowledgeBase tool to answer questions."
)
```

## Architecture

```
┌─────────────────┐
│  GCP Agent      │
│  or ADK         │
└────────┬────────┘
         │ HTTP/REST
         │ (OpenAPI)
┌────────▼────────┐
│  FastAPI        │
│  oracle_ai_     │
│  database_rag.py│
└────────┬────────┘
         │
    ┌────┴─────┐
    │          │
┌───▼────┐ ┌──▼────────┐
│ Oracle │ │ Vertex AI │
│   DB   │ │ Embeddings│
│ Vector │ │    LLM    │
│ Store  │ │           │
└────────┘ └───────────┘
```

## API Documentation

Once running, access:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## Security Considerations

For production deployment:

1. **Add Authentication**: Implement OAuth2, API keys, or GCP IAM
2. **Use HTTPS**: Deploy behind a load balancer with SSL/TLS
3. **Rate Limiting**: Add request throttling
4. **Input Validation**: Already included via Pydantic models
5. **CORS**: Configure appropriate origins in production

Example with authentication:

```python
from fastapi.security import HTTPBearer

security = HTTPBearer()

@app.post("/query")
async def query_knowledge_base(
    request: QueryRequest,
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    # Validate token
    # ... existing code
```

## Troubleshooting

### API won't start

Check:
- Database connection (`DB_PASSWORD`, wallet files)
- GCP credentials are configured
- All dependencies installed

### Query returns error

Check:
- Documents are uploaded (`/status` endpoint)
- Database table exists: `SELECT * FROM rag_tab`
- Vertex AI is initialized properly

### Agent can't call the tool

Verify:
- API is publicly accessible
- OpenAPI spec is valid
- Agent has correct tool configuration

## Monitoring

Use the `/health` endpoint for monitoring:

```bash
# Add to Cloud Monitoring
gcloud monitoring uptime-checks create http \
  --display-name="Oracle RAG API" \
  --hostname=your-api-url \
  --path=/health
```

## Performance Tuning

- Adjust `top_k` in queries (default: 5)
- Modify chunk size in document processing (default: 1000)
- Configure LLM parameters (temperature, max_tokens)
- Use connection pooling for Oracle DB

## License

Same as parent project.
