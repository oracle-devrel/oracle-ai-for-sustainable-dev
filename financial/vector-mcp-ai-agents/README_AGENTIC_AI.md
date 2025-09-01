# Agentic AI Builder with Oracle Database 23ai Integration

This system provides a complete agentic AI workflow builder with a React frontend and a Python backend powered by LangGraph, integrated with Oracle Database 23ai for vector search and MCP operations.

## Architecture Overview

```
React Frontend (AgenticAIBuilder.js) 
    ↓ HTTP API calls
Python Backend (agenticai_langgraph.py)
    ↓ MCP calls
Oracle Database 23ai (Vector Search + Data)
```

## Components

### Frontend (React)
- **AgenticAIBuilder.js**: Interactive workflow builder with drag-and-drop components
- **agenticAIAPI.js**: API client for backend communication
- Visual workflow creation with different component types:
  - Graph Input/Output (circles)
  - Processing Nodes (rectangles)
  - Decision Nodes (diamonds)
  - Edges (arrows)

### Backend (Python)
- **agenticai_langgraph.py**: FastAPI server with LangGraph workflow execution
- **OraDBVectorStore.py**: Oracle Database 23ai vector store integration
- **start_backend.py**: Server startup script
- Supports multiple AI models (OpenAI, Ollama, local models)

## Setup Instructions

### 1. Backend Setup

#### Prerequisites
- Python 3.8+
- Oracle Database 23ai instance
- Oracle client libraries

#### Install Dependencies
```bash
cd financial/vector-mcp-ai-agents
pip install -r requirements_backend.txt
```

#### Configure Database Connection
```bash
# Copy the template configuration
cp config.yaml.template config.yaml

# Edit config.yaml with your Oracle DB credentials
nano config.yaml
```

Required configuration:
```yaml
ORACLE_DB_USERNAME: "your_username"
ORACLE_DB_PASSWORD: "your_password"  
ORACLE_DB_DSN: "your_host:1521/your_service_name"
OPENAI_API_KEY: "your_openai_key"  # Optional
```

#### Start the Backend Server
```bash
# Option 1: Direct startup
python agenticai_langgraph.py

# Option 2: Using startup script
python start_backend.py

# Option 3: Manual uvicorn
uvicorn agenticai_langgraph:app --host 0.0.0.0 --port 8001 --reload
```

The backend will be available at:
- API: http://localhost:8001
- API Docs: http://localhost:8001/docs
- Health Check: http://localhost:8001/api/health

### 2. Frontend Setup

#### Prerequisites
- Node.js 16+
- React 19.1.0

#### Install Dependencies
```bash
cd financial/react-frontend
npm install
```

#### Configure Environment Variables
```bash
# Create or update .env file
echo "REACT_APP_BACKEND_API_URL=http://localhost:8001/api" >> .env
```

#### Start the React Development Server
```bash
npm start
```

The frontend will be available at: http://localhost:3000

### 3. Oracle Database 23ai Setup

#### Create Vector Tables
The backend automatically creates the required tables:
- `PDFCollection` - PDF document vectors
- `WebCollection` - Web content vectors  
- `RepoCollection` - Repository document vectors
- `GeneralCollection` - General knowledge vectors

#### Sample Data Loading
```python
# Example: Load sample data
from OraDBVectorStore import OraDBVectorStore

vector_store = OraDBVectorStore()

# Add sample financial documents
sample_chunks = [
    {
        "text": "Risk assessment is crucial for portfolio management.",
        "metadata": {"type": "financial", "category": "risk"}
    }
]

vector_store.add_pdf_chunks(sample_chunks, "financial_guide_001")
```

## Usage Guide

### 1. Building Workflows

1. **Access the Builder**: Navigate to "Agentic AI Builder" in the React app
2. **Add Components**: Click component buttons to add to workflow
3. **Configure Components**: Click the edit button (✎) on any component
4. **Arrange Layout**: Drag components to position them
5. **Load Examples**: Use predefined workflow templates

### 2. Executing Workflows

1. **Check Backend Status**: Green status indicator shows backend health
2. **Execute Workflow**: Click "Execute Workflow" button
3. **Monitor Progress**: Watch status indicator for execution progress
4. **View Results**: Results appear in the results panel below

### 3. Example Workflows

#### ✅ Investment Advisor Agent (Fully Implemented)
```javascript
// Automatic execution example
import agenticAIAPI from './api/agenticAIAPI';

const result = await agenticAIAPI.executeInvestmentAdvisorWorkflow(
  "Analyze tech stock portfolio"
);
console.log('Investment advice:', result);
```

#### ✅ Banking Concierge (Fully Implemented)
```javascript
const result = await agenticAIAPI.executeBankingConciergeWorkflow(
  "Check account balance and recent transactions"
);
console.log('Banking response:', result);
```

#### ⚠️ Spatial Digital Twins (Frontend Template Only)
- **Frontend**: Complete UI workflow template available
- **Backend**: Uses generic processing, no spatial-specific Oracle DB integration yet
- **Future**: Would need spatial vector queries and IoT data processing logic

#### ⚠️ Disaster Response (Frontend Template Only)  
- **Frontend**: Emergency management workflow template
- **Backend**: Uses generic processing, no emergency-specific logic yet

#### ⚠️ Content Management (Frontend Template Only)
- **Frontend**: Document processing and approval workflow template  
- **Backend**: Uses generic processing, no CMS-specific logic yet

## API Reference

### Backend Endpoints (5 Total)

#### 1. POST `/api/workflow/execute`
Execute a workflow
```json
{
  "workflow_name": "Investment Advisor",
  "components": [...],
  "input_data": {...},
  "user_query": "Analyze portfolio"
}
```

#### 2. GET `/api/workflow/status/{workflow_id}`
Get workflow execution status

#### 3. GET `/api/workflow/result/{workflow_id}`
Get workflow final result

#### 4. GET `/api/health`
Backend health check

#### 5. GET `/api/workflows`
List all workflows (running and completed)

**Note**: These endpoints work with **dynamically created workflows** from the frontend. There are no pre-stored workflow templates in the backend - workflows are created on-demand when you execute them from the React interface.

### Actually Implemented Workflow Logic

The backend contains specific business logic for these workflow types:

#### ✅ **Financial/Investment Workflows**
- **Market Data Input**: Simulates real-time stock data (AAPL, GOOGL, MSFT)
- **Risk Analysis**: Vector search for "risk assessment portfolio analysis" 
- **Portfolio Strategy**: Decision logic based on risk scores (conservative/balanced/aggressive)
- **Investment Recommendation**: Portfolio allocation recommendations

#### ✅ **Banking/Fraud Detection Workflows**  
- **Customer Request**: Customer service input processing
- **Intent Recognition**: NLP processing with vector search for customer intents
- **Fraud Check**: Fraud detection simulation (score-based decisions)
- **Response Output**: Formatted customer service responses

#### ❌ **Spatial/Logistics Workflows** (UI Only)
- **Frontend**: Has example workflow template with IoT sensors, digital twins, route optimization
- **Backend**: Falls back to **generic processing** - no specific spatial logic implemented
- **MCP Integration**: Uses general vector search, no spatial-specific Oracle DB queries

#### ❌ **Disaster Response Workflows** (UI Only)
- **Frontend**: Has emergency alert, threat assessment, resource allocation templates
- **Backend**: Falls back to **generic processing** - no disaster-specific logic

#### ❌ **Content Management Workflows** (UI Only)  
- **Frontend**: Has content input, analysis, approval workflow templates
- **Backend**: Falls back to **generic processing** - no CMS-specific logic

### Frontend API Client
```javascript
import agenticAIAPI from './api/agenticAIAPI';

// Execute workflow and wait for completion
const result = await agenticAIAPI.executeWorkflowAndWait(
  'My Workflow',
  components,
  inputData,
  userQuery
);

// Check backend health
const health = await agenticAIAPI.checkBackendHealth();
```

## Workflow Component Types

### Graph Input (Circles)
- **Purpose**: Data entry points
- **Examples**: Market Data, Customer Requests, IoT Sensors
- **Backend Processing**: Initializes workflow with input data

### Processing Nodes (Rectangles) 
- **Purpose**: Data transformation and analysis
- **Examples**: Risk Analysis, NLP Processing, AI Models
- **Backend Processing**: Performs vector search and data processing

### Decision Nodes (Diamonds)
- **Purpose**: Conditional logic and routing
- **Examples**: Fraud Detection, Portfolio Strategy, Risk Assessment
- **Backend Processing**: Makes decisions based on processed data

### Graph Output (Circles)
- **Purpose**: Final results and responses
- **Examples**: Recommendations, Reports, Alerts
- **Backend Processing**: Formats and returns final results

### Edges (Arrows)
- **Purpose**: Data flow connections
- **Automatic**: Created between sequential components
- **Backend Processing**: Passes data between components

## Oracle Database 23ai Integration

### Vector Search Operations
```python
# Search PDF documents
results = oracle_tools.vector_search(
    "risk assessment portfolio", 
    collection="pdf_documents", 
    k=5
)

# Store new data
success = oracle_tools.store_vector_data(
    content="Financial analysis report...",
    metadata={"type": "report", "date": "2024-01-01"},
    collection="pdf_documents"
)
```

### SQL Query Execution
```python
# Direct SQL queries
results = oracle_tools.execute_sql_query(
    "SELECT COUNT(*) FROM PDFCollection WHERE metadata LIKE '%financial%'"
)
```

## Troubleshooting

### Backend Issues
1. **Database Connection**: Check Oracle DB credentials in config.yaml
2. **Dependencies**: Ensure all Python packages are installed
3. **Port Conflicts**: Backend runs on port 8001 by default

### Frontend Issues  
1. **API Connection**: Verify REACT_APP_BACKEND_API_URL environment variable
2. **CORS**: Backend allows all origins by default
3. **Build Issues**: Check Node.js version and dependencies

### Common Error Messages
- `"Oracle DB MCP support not available"`: Install oracledb and sentence-transformers
- `"Backend is not healthy"`: Check backend server status and configuration
- `"Workflow execution timeout"`: Increase polling timeout in frontend

## Development

### Adding New Component Types
1. **Frontend**: Add to `componentTypes` array and `componentOptions` object
2. **Backend**: Add handler in `create_component_handler()` method
3. **Styling**: Add styled component for visual representation

### Extending Vector Collections
1. **Database**: Add new table creation in `OraDBVectorStore.py`
2. **Backend**: Add new collection type in `vector_search()` method
3. **Frontend**: Add new collection option in API calls

### Custom Workflow Templates
1. **Frontend**: Add new case in `loadExampleWorkflow()` function
2. **Backend**: Optionally add specialized handlers for new workflow types

## Performance Considerations

- **Vector Search**: Oracle DB 23ai provides optimized vector operations
- **Concurrent Workflows**: Backend supports multiple simultaneous executions
- **Large Datasets**: Consider chunking and batch processing for large documents
- **Memory Usage**: Monitor memory usage with large embedding models

## Security Notes

- Store sensitive credentials in config.yaml (not in source control)
- Use environment variables for production deployments
- Consider Oracle DB security features for production use
- Implement authentication for production API endpoints

## License

This project follows the same license as the parent repository.
