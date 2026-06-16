# Oracle AI Database + GCP Gemini

This directory contains RAG (Retrieval-Augmented Generation) AI agent implementations that integrate Oracle Database 26ai vector storage with Google Cloud Vertex AI for intelligent question-answering.

## Overview

Both implementations provide REST APIs that can be integrated with Google Cloud Dialogflow conversational agents as custom tools/datastores.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Dialogflow Agent                         │
│              (Conversational AI Interface)                  │
└─────────────┬──────────────────────────┬───────────────────┘
              │                          │
              │ HTTP POST                │ HTTP POST
              │ /api/v1/query            │ /api/v1/query
              │                          │
    ┌─────────▼──────────┐    ┌─────────▼──────────┐
    │  Java Spring Boot  │    │   Python Flask     │
    │   RAG Service      │    │   RAG Service      │
    └─────────┬──────────┘    └─────────┬──────────┘
              │                          │
              │ Oracle JDBC              │ oracledb
              │ Vector Search            │ Vector Search
              │                          │
    ┌─────────▼──────────────────────────▼──────────┐
    │         Oracle Database 26ai                   │
    │           Vector Store (RAG_TAB)               │
    │      - Text chunks with embeddings             │
    │      - DOT_PRODUCT similarity search           │
    └────────────────────────────────────────────────┘
              │                          │
              │ Vertex AI API            │ Vertex AI API
              │                          │
    ┌─────────▼──────────────────────────▼──────────┐
    │          Google Cloud Vertex AI                │
    │   - text-embedding-004 (embeddings)            │
    │   - gemini-2.5-flash (LLM)                     │
    └────────────────────────────────────────────────┘
```

## Projects

### 1. Java Spring Boot Implementation
**Directory:** `rag-ai-agent-java/`

Production-ready Spring Boot REST API with:
- Full Spring Boot architecture with dependency injection
- Oracle JDBC for database connectivity
- LangChain4j for RAG orchestration
- OpenAPI/Swagger documentation
- Comprehensive error handling and logging

**Best for:**
- Enterprise deployments
- Microservices architectures
- Strong typing requirements
- JVM-based ecosystems

[→ Java Implementation README](rag-ai-agent-java/README.md)

### 2. Python Flask Implementation
**Directory:** `rag-ai-agent-python/`

Lightweight Flask REST API with:
- Simple, clean Flask architecture
- python-oracledb for database connectivity
- LangChain for RAG orchestration
- OpenAPI specification endpoint
- Environment-based configuration

**Best for:**
- Quick prototyping
- Jupyter/notebook workflows
- Python-heavy data science teams
- Simpler deployment needs

[→ Python Implementation README](rag-ai-agent-python/README.md)

## Common Features

Both implementations provide:

✅ **REST API Endpoints:**
- `POST /api/v1/query` - Submit questions and get AI-generated answers
- `GET /api/v1/health` - Health check
- `GET /api-docs` - OpenAPI specification

✅ **RAG Pipeline:**
1. Question embedding generation (Vertex AI text-embedding-004)
2. Vector similarity search (Oracle Database 26ai)
3. Context retrieval from vector store
4. LLM response generation (Vertex AI Gemini)

✅ **Dialogflow Integration:**
- OpenAPI-compatible endpoints
- Can be added as custom tools in Dialogflow agents
- Enables conversational AI grounded in your documents

## Quick Comparison

| Feature | Java (Spring Boot) | Python (Flask) |
|---------|-------------------|----------------|
| **Startup Time** | ~5-10s | ~2-3s |
| **Memory Usage** | ~300-500MB | ~100-200MB |
| **Deployment** | WAR/JAR | WSGI/gunicorn |
| **Ecosystem** | Maven/JVM | pip/PyPI |
| **Typing** | Strong/Static | Dynamic |
| **Concurrency** | Thread pools | Multi-worker |
| **Best For** | Enterprise | Prototypes/ML |

## Getting Started

### Prerequisites (Both)
- Oracle Database 26ai with vector data populated (RAG_TAB table)
- Oracle wallet files in `~/wallet/`
- Google Cloud project with Vertex AI enabled
- Authenticated GCP credentials: `gcloud auth application-default login`

### Choose Your Implementation

**Java:**
```bash
cd rag-ai-agent-java
cp .env.example .env
# Edit .env with your credentials
mvn clean package
java -jar target/rag-ai-agent-1.0.0.jar
```

**Python:**
```bash
cd rag-ai-agent-python
cp .env.example .env
# Edit .env with your credentials
pip install -r requirements.txt
python app.py
```

Both run on port 8080 by default.

### Test the API

```bash
# Query endpoint
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Tell me about JSON Relational Duality"}'

# Health check
curl http://localhost:8080/api/v1/health

# OpenAPI spec
curl http://localhost:8080/api-docs
```

## Dialogflow Integration

Both implementations can be integrated with Google Cloud Dialogflow agents:

1. **Deploy to GCP VM** (or Cloud Run)
2. **Create Custom Tool** in Dialogflow Agent Builder
   - Type: OpenAPI
   - Tool name: "Oracle Database Knowledge"
   - OpenAPI spec URL: `http://YOUR_VM_IP:8080/api-docs`
3. **Update Agent Instructions**:
   ```
   For questions about Oracle Database 26ai features, use ${TOOL: Oracle Database Knowledge}
   ```

Detailed integration guide: [DIALOGFLOW_INTEGRATION.md](rag-ai-agent-java/DIALOGFLOW_INTEGRATION.md)

## Configuration

Both use similar environment variables:

```bash
# Oracle Database
ORACLE_USERNAME=YOUR_DB_USERNAME
ORACLE_PASSWORD=your_password
ORACLE_DSN=your_connection_string
ORACLE_WALLET_LOCATION=~/wallet
ORACLE_WALLET_PASSWORD=your_wallet_password

# Vertex AI
VERTEX_PROJECT_ID=your-project-id
VERTEX_LOCATION=us-central1
VERTEX_EMBEDDING_MODEL=text-embedding-004
VERTEX_LLM_MODEL=gemini-2.5-flash

# RAG
RAG_TOP_K=10
```

See `.env.example` files in each directory for complete configuration options.

## Deployment Options

### Option 1: GCP VM (Simple)
- Deploy as-is to Compute Engine VM
- Open port 8080 in firewall
- Use external IP in Dialogflow

### Option 2: Cloud Run (Recommended for Production)
- Containerize with Docker
- Deploy to Cloud Run for auto-scaling
- Automatic HTTPS and load balancing

### Option 3: Kubernetes (Enterprise)
- Deploy to GKE for high availability
- Horizontal pod autoscaling
- Service mesh integration

## Performance Considerations

**Java:**
- Warm-up time: First request may be slow (~2-3s)
- Subsequent requests: Fast (~500-1500ms)
- JVM tuning recommended for production

**Python:**
- Consistent response times
- Use gunicorn with multiple workers
- Consider gevent for async I/O

**Both:**
- Vector search time depends on table size and index
- LLM generation typically 1-2 seconds
- Consider caching for frequent queries

## Development Workflow

### Testing Locally
1. Set up Oracle Database and populate vector data (see main repo README)
2. Configure GCP credentials
3. Copy `.env.example` to `.env` and fill in values
4. Run application
5. Test with curl or Postman

### Deploying to GCP
1. Build application (Java: JAR, Python: requirements.txt)
2. Transfer to VM via scp
3. Install dependencies
4. Run with production server (Java: embedded Tomcat, Python: gunicorn)
5. Configure firewall
6. Test endpoint accessibility
7. Integrate with Dialogflow

## Monitoring & Observability

**Java:**
- Spring Boot Actuator for metrics
- Micrometer for monitoring
- Logback for structured logging

**Python:**
- Flask logging
- Prometheus client library
- APM tools (New Relic, Datadog)

## Security Best Practices

1. **Never commit `.env` files** - Use `.env.example` templates
2. **Use service accounts** for GCP authentication in production
3. **Add API authentication** - JWT tokens or API keys
4. **Enable HTTPS** - Use nginx reverse proxy or Cloud Run
5. **Implement rate limiting** - Prevent abuse
6. **Validate inputs** - Sanitize user questions
7. **Keep dependencies updated** - Regular security patches

## Troubleshooting

### Database Connection Issues
- Verify wallet files are in correct location
- Check TNS_ADMIN environment variable
- Test connection with sqlplus

### Vertex AI Authentication
- Run `gcloud auth application-default login`
- Verify project ID is correct
- Check API is enabled

### Dialogflow Integration
- Ensure endpoint is publicly accessible
- Test with curl before configuring in Dialogflow
- Check Dialogflow logs for tool invocation details

## Support & Resources

- **Oracle Database 26ai**: https://docs.oracle.com/en/database/
- **Vertex AI**: https://cloud.google.com/vertex-ai/docs
- **Dialogflow CX**: https://cloud.google.com/dialogflow/cx/docs
- **LangChain**: https://python.langchain.com/docs/

## License

Apache 2.0
