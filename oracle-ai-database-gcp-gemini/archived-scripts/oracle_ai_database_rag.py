"""
Oracle AI Database RAG API
FastAPI service with OpenAPI endpoints for GCP Vertex AI Agents and ADK
"""
from fastapi import FastAPI, HTTPException, UploadFile, File, Header, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi
from pydantic import BaseModel, Field
from typing import Optional, List, Dict
from dotenv import load_dotenv
from PyPDF2 import PdfReader
from langchain_text_splitters import CharacterTextSplitter
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_community.vectorstores.oraclevs import OracleVS
from langchain_community.vectorstores.utils import DistanceStrategy
from langchain_core.documents import Document
from langchain_google_vertexai import VertexAI, VertexAIEmbeddings
import oracledb
import os
import vertexai
import time
import io
from contextlib import asynccontextmanager

# Load environment variables
load_dotenv()

# Global variables for database connection and models
db_connection = None
knowledge_base = None
llm = None
embeddings = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize resources on startup and cleanup on shutdown"""
    global db_connection, knowledge_base, llm, embeddings
    
    # Startup: Initialize database and models
    un = os.getenv("DB_USERNAME", "ADMIN")
    pw = os.getenv("DB_PASSWORD")
    dsn = os.getenv("DB_DSN", "paulparkdb_tp")
    wpwd = os.getenv("DB_WALLET_PASSWORD")
    wallet_dir = os.getenv("DB_WALLET_DIR", "/home/ssh-key-2025-10-20/wallet")
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    region = os.getenv("GCP_REGION", "us-central1")
    
    # Initialize database connection
    db_connection = oracledb.connect(
        config_dir=wallet_dir,
        user=un,
        password=pw,
        dsn=dsn,
        wallet_location=wallet_dir,
        wallet_password=wpwd
    )
    
    # Initialize Vertex AI
    vertexai.init(project=project_id, location=region)
    
    # Initialize embeddings model
    embeddings = VertexAIEmbeddings(model_name="text-embedding-004")
    
    # Initialize LLM
    llm = VertexAI(
        model_name="gemini-2.0-flash-exp",
        max_output_tokens=8192,
        temperature=0.7,
        top_p=0.8,
        top_k=40,
        verbose=True,
    )
    
    # Load existing knowledge base if available
    try:
        with db_connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM rag_tab")
            count = cursor.fetchone()[0]
            if count > 0:
                knowledge_base = OracleVS(
                    client=db_connection,
                    embedding_function=embeddings,
                    table_name="RAG_TAB",
                    distance_strategy=DistanceStrategy.COSINE
                )
                print(f"✓ Loaded {count} existing document chunks from database")
    except Exception as e:
        print(f"Warning: Could not load existing data: {e}")
    
    yield
    
    # Shutdown: Clean up resources
    if db_connection:
        db_connection.close()

# Initialize FastAPI app with OpenAPI metadata
app = FastAPI(
    title="Oracle AI Database RAG API",
    description="RAG (Retrieval Augmented Generation) API using Oracle Database with Vector Search and Google Vertex AI. Suitable for integration with GCP Vertex AI Agents and Agent Development Kit (ADK).",
    version="1.0.0",
    openapi_version="3.0.3",  # Force OpenAPI 3.0.3 for GCP compatibility
    lifespan=lifespan,
    servers=[
        {"url": "http://10.150.0.8:8501", "description": "GCP internal VPC server"}
    ]
)

# Configure to generate OpenAPI 3.0 compatible schemas (no "null" types)
app.openapi_version = "3.0.3"

# Add CORS middleware for agent access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Security scheme that accepts any bearer token (for GCP compatibility)
security = HTTPBearer(auto_error=False)

async def verify_token(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)):
    """
    Accept any bearer token without validation.
    GCP Vertex AI agents send service tokens - we accept them without verification.
    """
    # Simply return - no validation needed
    return credentials

# Override OpenAPI schema to force version 3.0.3
def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    openapi_schema = get_openapi(
        title=app.title,
        version=app.version,
        openapi_version="3.0.3",  # Force 3.0.3
        description=app.description,
        routes=app.routes,
        servers=app.servers
    )
    
    # Remove security schemes - GCP agents don't allow them
    if "securitySchemes" in openapi_schema.get("components", {}):
        del openapi_schema["components"]["securitySchemes"]
    if "security" in openapi_schema:
        del openapi_schema["security"]
    
    # Clean up schema components to remove invalid types
    def clean_schema(schema_obj):
        """Recursively clean schema objects to remove invalid types"""
        if not isinstance(schema_obj, dict):
            return schema_obj
        
        # Remove null types and anyOf/oneOf with null
        if "type" in schema_obj:
            if isinstance(schema_obj["type"], list):
                # Remove 'null' from type arrays
                schema_obj["type"] = [t for t in schema_obj["type"] if t != "null"]
                if len(schema_obj["type"]) == 1:
                    schema_obj["type"] = schema_obj["type"][0]
            elif schema_obj["type"] == "null":
                # Remove null-only types
                del schema_obj["type"]
        
        # Handle anyOf/oneOf with null
        for key in ["anyOf", "oneOf"]:
            if key in schema_obj:
                schema_obj[key] = [s for s in schema_obj[key] if s.get("type") != "null"]
                if len(schema_obj[key]) == 1:
                    # Flatten single-item anyOf/oneOf
                    only_schema = schema_obj[key][0]
                    del schema_obj[key]
                    schema_obj.update(only_schema)
                elif len(schema_obj[key]) == 0:
                    del schema_obj[key]
        
        # Recursively clean nested objects
        for key, value in list(schema_obj.items()):
            if isinstance(value, dict):
                clean_schema(value)
            elif isinstance(value, list):
                for item in value:
                    if isinstance(item, dict):
                        clean_schema(item)
        
        return schema_obj
    
    # Clean all schema components
    if "components" in openapi_schema and "schemas" in openapi_schema["components"]:
        for schema_name, schema_def in openapi_schema["components"]["schemas"].items():
            clean_schema(schema_def)
    
    # Ensure all responses have explicit application/json content type
    for path in openapi_schema.get("paths", {}).values():
        for operation in path.values():
            if isinstance(operation, dict):
                # Remove security from individual operations
                if "security" in operation:
                    del operation["security"]
                
                # Clean request body schemas
                if "requestBody" in operation:
                    clean_schema(operation["requestBody"])
                
                # Clean response schemas
                if "responses" in operation:
                    for response in operation["responses"].values():
                        if isinstance(response, dict):
                            clean_schema(response)
                            if "content" in response:
                                # Ensure only application/json is present
                                if "application/json" in response["content"]:
                                    response["content"] = {
                                        "application/json": response["content"]["application/json"]
                                    }
    
    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi

# Pydantic models for request/response
class QueryRequest(BaseModel):
    """Request model for querying the knowledge base"""
    query: str = Field(
        ..., 
        description="The question to ask about the documents in the knowledge base",
        example="What are the new spatial features in Oracle Database?"
    )
    top_k: int = Field(
        default=5, 
        description="Number of similar document chunks to retrieve",
        ge=1,
        le=20
    )

class QueryResponse(BaseModel):
    """Response model for query results"""
    answer: str = Field(..., description="Generated answer based on retrieved context")
    context_chunks: List[str] = Field(..., description="Retrieved document chunks used for the answer")
    vector_search_time: float = Field(..., description="Time taken for vector search in seconds")
    llm_response_time: float = Field(..., description="Time taken for LLM response in seconds")
    total_time: float = Field(..., description="Total query processing time in seconds")

class UploadResponse(BaseModel):
    """Response model for document upload"""
    message: str = Field(..., description="Status message")
    chunks_created: int = Field(..., description="Number of text chunks created")
    processing_time: float = Field(..., description="Time taken to process and store document")

class StatusResponse(BaseModel):
    """Response model for service status"""
    status: str = Field(..., description="Service status")
    document_count: int = Field(..., description="Number of document chunks in the knowledge base")
    database_connected: bool = Field(..., description="Database connection status")
    models_loaded: bool = Field(..., description="AI models initialization status")

def chunks_to_docs_wrapper(row: dict) -> Document:
    """Converts a row into a Document object for Oracle Vector Store"""
    metadata = {'id': str(row['id']), 'link': row['link']}
    return Document(page_content=row['text'], metadata=metadata)

@app.get("/", 
         summary="Root endpoint",
         description="Returns basic API information")
async def root():
    """Root endpoint providing API information"""
    return {
        "name": "Oracle AI Database RAG API",
        "version": "1.0.0",
        "description": "RAG API for GCP Vertex AI Agents",
        "endpoints": {
            "query": "/query - Ask questions about uploaded documents",
            "upload": "/upload - Upload PDF documents",
            "status": "/status - Check service status",
            "docs": "/docs - OpenAPI documentation"
        }
    }

@app.get("/status",
         response_model=StatusResponse,
         summary="Get service status",
         description="Returns the current status of the RAG service including document count and connection status")
async def get_status():
    """Get current service status"""
    global db_connection, knowledge_base, llm, embeddings
    
    document_count = 0
    try:
        if db_connection:
            with db_connection.cursor() as cursor:
                cursor.execute("SELECT COUNT(*) FROM rag_tab")
                document_count = cursor.fetchone()[0]
    except Exception as e:
        pass
    
    return StatusResponse(
        status="operational" if db_connection and llm and embeddings else "degraded",
        document_count=document_count,
        database_connected=db_connection is not None,
        models_loaded=(llm is not None and embeddings is not None)
    )

@app.post("/query",
          response_model=QueryResponse,
          summary="Query the knowledge base",
          description="Submit a question to search the document knowledge base and generate an answer using RAG. This endpoint performs vector similarity search on stored documents and uses Google Vertex AI to generate a contextual answer.",
          operation_id="query")
async def query_knowledge_base(
    request: QueryRequest,
    token: Optional[HTTPAuthorizationCredentials] = Depends(verify_token)
):
    """
    Query the knowledge base with a question.
    
    This endpoint:
    1. Performs vector similarity search to find relevant document chunks
    2. Uses retrieved context to generate an answer via Vertex AI LLM
    3. Returns the answer along with context and timing metrics
    
    Accepts bearer tokens from GCP service agents without validation.
    """
    global knowledge_base, llm, embeddings
    
    if knowledge_base is None:
        raise HTTPException(
            status_code=400,
            detail="Knowledge base is empty. Please upload documents first using the /upload endpoint."
        )
    
    try:
        # Vector search
        s3time = time.time()
        result_chunks = knowledge_base.similarity_search(request.query, request.top_k)
        s4time = time.time()
        
        # Prepare context
        context_texts = [chunk.page_content for chunk in result_chunks]
        
        # Define prompt template
        template = """Answer the question based only on the following context:
{context}

Question: {question}

Answer:"""
        prompt = PromptTemplate.from_template(template)
        retriever = knowledge_base.as_retriever(search_kwargs={"k": request.top_k})
        
        # Create RAG chain
        chain = (
            {"context": retriever, "question": RunnablePassthrough()}
            | prompt
            | llm
            | StrOutputParser()
        )
        
        # Generate response
        s4_5time = time.time()
        response = chain.invoke(request.query)
        s5time = time.time()
        
        return QueryResponse(
            answer=response,
            context_chunks=context_texts,
            vector_search_time=round(s4time - s3time, 3),
            llm_response_time=round(s5time - s4_5time, 3),
            total_time=round(s5time - s3time, 3)
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing query: {str(e)}")

@app.post("/upload",
          response_model=UploadResponse,
          summary="Upload PDF document",
          description="Upload a PDF document to be processed, chunked, vectorized and stored in the Oracle Database knowledge base. The document will be split into chunks and embedded using Vertex AI embeddings.",
          operation_id="uploadDocument",
          include_in_schema=False)  # Exclude from OpenAPI for GCP compatibility
async def upload_document(file: UploadFile = File(..., description="PDF file to upload and process")):
    """
    Upload and process a PDF document.
    
    This endpoint:
    1. Extracts text from the uploaded PDF
    2. Splits text into chunks for better retrieval
    3. Creates vector embeddings using Vertex AI
    4. Stores chunks and embeddings in Oracle Database
    """
    global db_connection, knowledge_base, embeddings
    
    if not file.filename.endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Only PDF files are supported")
    
    try:
        s1time = time.time()
        
        # Read PDF content
        pdf_content = await file.read()
        pdf_reader = PdfReader(io.BytesIO(pdf_content))
        
        # Extract text
        text = ""
        for page in pdf_reader.pages:
            text += page.extract_text()
        
        if not text.strip():
            raise HTTPException(status_code=400, detail="PDF contains no extractable text")
        
        # Split into chunks
        text_splitter = CharacterTextSplitter(
            separator="\n",
            chunk_size=1000,
            chunk_overlap=200,
            length_function=len
        )
        chunks = text_splitter.split_text(text)
        
        # Create documents
        docs = [
            chunks_to_docs_wrapper({
                'id': page_num, 
                'link': f'{file.filename}#Page{page_num}', 
                'text': text
            }) 
            for page_num, text in enumerate(chunks)
        ]
        
        # Store in vector database
        knowledge_base = OracleVS.from_documents(
            docs, 
            embeddings, 
            client=db_connection, 
            table_name="RAG_TAB", 
            distance_strategy=DistanceStrategy.COSINE
        )
        
        s2time = time.time()
        
        return UploadResponse(
            message=f"Successfully processed {file.filename}",
            chunks_created=len(chunks),
            processing_time=round(s2time - s1time, 3)
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing file: {str(e)}")

@app.delete("/clear",
            summary="Clear knowledge base",
            description="Delete all documents from the knowledge base (truncates RAG_TAB table)")
async def clear_knowledge_base():
    """Clear all documents from the knowledge base"""
    global db_connection, knowledge_base
    
    try:
        with db_connection.cursor() as cursor:
            cursor.execute("TRUNCATE TABLE rag_tab")
            db_connection.commit()
        
        knowledge_base = None
        return {"message": "Knowledge base cleared successfully"}
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error clearing knowledge base: {str(e)}")

# Health check endpoint for monitoring
@app.get("/health",
         summary="Health check",
         description="Simple health check endpoint for monitoring and load balancers")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy"}

# Suppress Streamlit health check noise
@app.get("/_stcore/{path:path}", include_in_schema=False)
async def ignore_streamlit_checks(path: str):
    """Ignore Streamlit-specific health check requests"""
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8501)
