#!/usr/bin/env python3
"""
Agentic AI LangGraph Backend for Oracle Database 23ai MCP Integration

This module provides a FastAPI backend that receives workflow requests from the
AgenticAIBuilder.js frontend and executes them using LangGraph with MCP calls
to Oracle Database 23ai for vector search and data operations.
"""

import os
import json
import asyncio
from typing import List, Dict, Any, Optional
from datetime import datetime
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv
import logging
import uuid

# LangGraph and LangChain imports
from langgraph.graph import StateGraph, END
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langchain_core.tools import BaseTool, tool
from typing_extensions import TypedDict

# MCP and Oracle DB imports
try:
    from OraDBVectorStore import OraDBVectorStore
    ORACLE_DB_AVAILABLE = True
except ImportError:
    ORACLE_DB_AVAILABLE = False
    print("Oracle DB MCP support not available. Install with: pip install oracledb sentence-transformers")

# Import modular workflow handlers
try:
    from workflows.base_workflow import BaseWorkflowHandler, WorkflowRegistry
    from workflows.investment_advisor_handler import InvestmentAdvisorHandler
    from workflows.banking_concierge_handler import BankingConciergeHandler  
    from workflows.spatial_digital_twins_handler import SpatialDigitalTwinsHandler
    MODULAR_WORKFLOWS_AVAILABLE = True
except ImportError as e:
    MODULAR_WORKFLOWS_AVAILABLE = False
    print(f"Modular workflow system not available: {e}")

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Agentic AI LangGraph API",
    description="Backend API for executing AI agent workflows with Oracle Database 23ai MCP integration",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic models for API requests/responses
class AgentComponent(BaseModel):
    id: int
    type: str
    source: str
    componentType: str
    position: Dict[str, float]
    config1: Optional[str] = None
    config2: Optional[str] = None

class WorkflowRequest(BaseModel):
    workflow_name: str
    components: List[AgentComponent]
    input_data: Optional[Dict[str, Any]] = None
    user_query: Optional[str] = None

class WorkflowResponse(BaseModel):
    workflow_id: str
    status: str
    result: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    execution_time: Optional[float] = None

class WorkflowStatus(BaseModel):
    workflow_id: str
    status: str
    current_step: Optional[str] = None
    progress: Optional[float] = None
    result: Optional[Dict[str, Any]] = None
    error: Optional[str] = None

# Global workflow storage
active_workflows: Dict[str, Dict[str, Any]] = {}

class OracleMCPTools:
    """Oracle Database 23ai MCP Tools for AI Agents"""
    
    def __init__(self):
        """Initialize Oracle MCP connection"""
        self.vector_store = None
        self.connection_available = False
        
        if ORACLE_DB_AVAILABLE:
            try:
                self.vector_store = OraDBVectorStore()
                self.connection_available = True
                logger.info("Oracle DB 23ai MCP connection established")
            except Exception as e:
                logger.error(f"Failed to initialize Oracle DB MCP: {str(e)}")
                self.connection_available = False
        else:
            logger.warning("Oracle DB MCP not available - using mock responses")
    
    def vector_search(self, query: str, collection: str = "pdf_documents", k: int = 5) -> List[Dict[str, Any]]:
        """Perform vector search in Oracle Database 23ai"""
        try:
            if self.connection_available and self.vector_store:
                if collection == "pdf_documents":
                    results = self.vector_store.query_pdf_collection(query, n_results=k)
                elif collection == "web_documents":
                    results = self.vector_store.query_web_collection(query, n_results=k)
                elif collection == "repository_documents":
                    results = self.vector_store.query_repo_collection(query, n_results=k)
                elif collection == "general_knowledge":
                    results = self.vector_store.query_general_collection(query, n_results=k)
                else:
                    results = self.vector_store.query_pdf_collection(query, n_results=k)
                
                # Results from existing OraDBVectorStore already have content and metadata
                # Add a default score since the existing implementation doesn't provide it
                formatted_results = []
                for i, result in enumerate(results):
                    formatted_results.append({
                        "content": result.get("content", ""),
                        "metadata": result.get("metadata", {}),
                        "score": 0.9 - (i * 0.1)  # Decreasing score based on order
                    })
                
                logger.info(f"Vector search returned {len(formatted_results)} results")
                return formatted_results
            else:
                # Mock response when Oracle DB is not available
                logger.info("Using mock vector search response")
                return [
                    {
                        "content": f"Mock search result for query: {query}",
                        "metadata": {"source": "mock_document", "collection": collection},
                        "score": 0.95
                    }
                ]
        except Exception as e:
            logger.error(f"Vector search error: {str(e)}")
            return []
    
    def store_vector_data(self, content: str, metadata: Dict[str, Any], collection: str = "pdf_documents") -> bool:
        """Store vector data in Oracle Database 23ai"""
        try:
            if self.connection_available and self.vector_store:
                # Convert content to chunks for storage
                chunks = [{"text": content, "metadata": metadata}]
                
                if collection == "pdf_documents":
                    self.vector_store.add_pdf_chunks(chunks, document_id=metadata.get("document_id", str(uuid.uuid4())))
                elif collection == "web_documents":
                    self.vector_store.add_web_chunks(chunks, source_id=metadata.get("source_id", str(uuid.uuid4())))
                elif collection == "repository_documents":
                    self.vector_store.add_repo_chunks(chunks, document_id=metadata.get("document_id", str(uuid.uuid4())))
                elif collection == "general_knowledge":
                    self.vector_store.add_general_knowledge(chunks, source_id=metadata.get("source_id", str(uuid.uuid4())))
                
                logger.info(f"Stored vector data in collection: {collection}")
                return True
            else:
                logger.info("Mock vector data storage (Oracle DB not available)")
                return True
        except Exception as e:
            logger.error(f"Vector storage error: {str(e)}")
            return False
    
    def execute_sql_query(self, query: str) -> List[Dict[str, Any]]:
        """Execute SQL query on Oracle Database 23ai"""
        try:
            if self.connection_available and self.vector_store and hasattr(self.vector_store, 'cursor'):
                cursor = self.vector_store.cursor
                cursor.execute(query)
                
                # Get column names
                columns = [desc[0] for desc in cursor.description] if cursor.description else []
                
                # Fetch results and convert to dict
                rows = cursor.fetchall()
                results = []
                for row in rows:
                    if columns:
                        results.append(dict(zip(columns, row)))
                    else:
                        results.append({"result": str(row)})
                
                logger.info(f"SQL query executed, returned {len(results)} rows")
                return results
            else:
                logger.info("Mock SQL query execution (Oracle DB not available)")
                return [{"mock_column": "mock_value", "query": query}]
        except Exception as e:
            logger.error(f"SQL query error: {str(e)}")
            return []

# Initialize Oracle MCP tools
oracle_tools = OracleMCPTools()

class AgenticWorkflowExecutor:
    """LangGraph-based workflow executor for Agentic AI components with modular workflow support"""
    
    def __init__(self):
        """Initialize the workflow executor"""
        self.llm = None
        self.workflow_registry = None
        self.initialize_llm()
        self.initialize_workflow_handlers()
    
    def initialize_llm(self):
        """Initialize the language model"""
        openai_api_key = os.getenv("OPENAI_API_KEY")
        if openai_api_key:
            self.llm = ChatOpenAI(
                temperature=0.1,
                model="gpt-4",
                openai_api_key=openai_api_key
            )
            logger.info("Initialized OpenAI GPT-4 for workflow execution")
        else:
            logger.warning("OpenAI API key not found - using mock LLM responses")
    
    def initialize_workflow_handlers(self):
        """Initialize modular workflow handlers"""
        if MODULAR_WORKFLOWS_AVAILABLE:
            try:
                self.workflow_registry = WorkflowRegistry()
                
                # Register available workflow handlers
                self.workflow_registry.register_handler("investment_advisor", InvestmentAdvisorHandler())
                self.workflow_registry.register_handler("banking_concierge", BankingConciergeHandler())
                self.workflow_registry.register_handler("spatial_digital_twins", SpatialDigitalTwinsHandler())
                
                logger.info("Initialized modular workflow handlers")
            except Exception as e:
                logger.error(f"Failed to initialize workflow handlers: {e}")
                self.workflow_registry = None
        else:
            logger.warning("Modular workflow system not available - using legacy handlers")
    
    def detect_workflow_type(self, components: List[AgentComponent]) -> str:
        """Detect workflow type based on components"""
        component_types = [comp.type.lower() for comp in components]
        component_sources = [comp.source.lower() for comp in components]
        
        # Check for investment/financial keywords
        investment_keywords = ["investment", "portfolio", "financial", "stock", "bond", "risk", "advisor"]
        if any(keyword in " ".join(component_types + component_sources) for keyword in investment_keywords):
            return "investment_advisor"
        
        # Check for banking/customer service keywords  
        banking_keywords = ["banking", "customer", "fraud", "account", "transaction", "concierge", "authentication"]
        if any(keyword in " ".join(component_types + component_sources) for keyword in banking_keywords):
            return "banking_concierge"
        
        # Check for spatial/logistics keywords
        spatial_keywords = ["spatial", "digital", "twin", "iot", "route", "logistics", "gps", "location"]
        if any(keyword in " ".join(component_types + component_sources) for keyword in spatial_keywords):
            return "spatial_digital_twins"
        
        # Default to investment advisor for unknown workflows
        return "investment_advisor"
    
    def create_langchain_tools(self) -> List[BaseTool]:
        """Create LangChain tools for MCP operations"""
        
        @tool
        def vector_search(query: str) -> List[Dict[str, Any]]:
            """Search Oracle Database 23ai vector store for relevant information"""
            return oracle_tools.vector_search(query)
        
        @tool  
        def store_data(content: str, metadata: Dict[str, Any] = None, collection: str = "pdf_documents") -> bool:
            """Store data in Oracle Database 23ai vector store"""
            return oracle_tools.store_vector_data(content, metadata or {}, collection)
        
        @tool
        def sql_query(query: str) -> List[Dict[str, Any]]:
            """Execute SQL query on Oracle Database 23ai"""
            return oracle_tools.execute_sql_query(query)
        
        return [vector_search, store_data, sql_query]
    
    def build_workflow_graph(self, components: List[AgentComponent]) -> StateGraph:
        """Build LangGraph workflow from components"""
        
        # Define the state structure using TypedDict
        class WorkflowState(TypedDict):
            messages: List[BaseMessage]
            current_data: Dict[str, Any]
            results: Dict[str, Any]
            error: Optional[str]
        
        # Create the graph with the state schema
        workflow = StateGraph(WorkflowState)
        
        # Sort components by workflow order (simple left-to-right for now)
        sorted_components = sorted(components, key=lambda x: x.position["left"])
        
        # Add nodes for each component
        for i, component in enumerate(sorted_components):
            node_name = f"step_{i}_{component.componentType.lower().replace(' ', '_')}"
            workflow.add_node(node_name, self.create_component_handler(component))
        
        # Add edges between components (sequential for now)
        for i in range(len(sorted_components) - 1):
            from_node = f"step_{i}_{sorted_components[i].componentType.lower().replace(' ', '_')}"
            to_node = f"step_{i+1}_{sorted_components[i+1].componentType.lower().replace(' ', '_')}"
            workflow.add_edge(from_node, to_node)
        
        # Set entry and exit points
        if sorted_components:
            start_node = f"step_0_{sorted_components[0].componentType.lower().replace(' ', '_')}"
            workflow.set_entry_point(start_node)
            
            end_node = f"step_{len(sorted_components)-1}_{sorted_components[-1].componentType.lower().replace(' ', '_')}"
            workflow.add_edge(end_node, END)
        
        return workflow
    
    def create_component_handler(self, component: AgentComponent):
        """Create a handler function for a specific component using modular system"""
        
        def component_handler(state: Dict[str, Any]) -> Dict[str, Any]:
            """Handle execution of a single component with modular workflow support"""
            try:
                logger.info(f"Executing component: {component.type} ({component.componentType})")
                
                result = None
                
                # Try to use modular workflow handlers first
                if self.workflow_registry:
                    workflow_type = self.detect_workflow_type([component])
                    handler = self.workflow_registry.get_handler(workflow_type)
                    
                    if handler:
                        # Create a simple state object for the handler
                        class SimpleState:
                            def __init__(self, data):
                                self.current_data = data
                        
                        handler_state = SimpleState(state.get("current_data", {}))
                        # Process component using modular handler
                        result = handler.process_component(component.componentType, component, handler_state)
                        
                        # Update the main state with handler results
                        if result.get("status") == "success":
                            state["current_data"].update(result.get("data", {}))
                        else:
                            state["error"] = result.get("error", "Component processing failed")
                
                # Fallback to legacy handlers if modular system not available or failed
                if not result or result.get("status") != "success":
                    if component.componentType == "Graph Input":
                        result = self.handle_input_component(component, state)
                    elif component.componentType == "Node":
                        result = self.handle_processing_node(component, state)
                    elif component.componentType == "Decision Node":
                        result = self.handle_decision_node(component, state)
                    elif component.componentType == "Graph Output":
                        result = self.handle_output_component(component, state)
                    elif component.componentType == "Edge":
                        result = self.handle_edge_component(component, state)
                    else:
                        result = {"status": "unknown_component", "data": state.get("current_data", {})}
                
                # Update state
                if result.get("status") == "success":
                    state["current_data"].update(result.get("data", {}))
                    if "results" not in state:
                        state["results"] = {}
                    state["results"][f"component_{component.id}"] = result
                else:
                    state["error"] = result.get("error", "Component processing failed")
                
                return state
            except Exception as e:
                logger.error(f"Error in component {component.type}: {str(e)}")
                state["error"] = str(e)
                return state
        
        return component_handler
    
    def handle_input_component(self, component: AgentComponent, state: Any) -> Dict[str, Any]:
        """Handle Graph Input components"""
        logger.info(f"Processing input: {component.source}")
        
        # Simulate different input types
        if "Market Data" in component.type:
            return {
                "status": "success",
                "data": {
                    "market_data": {
                        "timestamp": datetime.now().isoformat(),
                        "symbols": ["AAPL", "GOOGL", "MSFT"],
                        "prices": [150.25, 2800.50, 350.75]
                    }
                }
            }
        elif "Customer Request" in component.type:
            return {
                "status": "success",
                "data": {
                    "customer_query": state.current_data.get("user_query", "Default customer request"),
                    "customer_id": "CUST_001",
                    "timestamp": datetime.now().isoformat()
                }
            }
        else:
            return {
                "status": "success",
                "data": {
                    "input_type": component.source,
                    "raw_data": state.current_data.get("input_data", {}),
                    "timestamp": datetime.now().isoformat()
                }
            }
    
    def handle_processing_node(self, component: AgentComponent, state: Any) -> Dict[str, Any]:
        """Handle Node components (processing)"""
        logger.info(f"Processing node: {component.source}")
        
        # Perform vector search based on component type
        if "Risk Analysis" in component.type:
            search_results = oracle_tools.vector_search("risk assessment portfolio analysis", "pdf_documents")
            return {
                "status": "success",
                "data": {
                    "risk_analysis": {
                        "risk_score": 0.65,
                        "factors": ["market_volatility", "sector_concentration"],
                        "search_results": search_results[:3]  # Top 3 results
                    }
                }
            }
        elif "Intent Recognition" in component.type:
            query = state.current_data.get("customer_query", "")
            search_results = oracle_tools.vector_search(f"customer service intent {query}", "pdf_documents")
            return {
                "status": "success",
                "data": {
                    "intent": "account_inquiry",
                    "confidence": 0.85,
                    "search_results": search_results[:2]
                }
            }
        else:
            # Generic processing
            search_query = f"{component.type} {component.source}"
            search_results = oracle_tools.vector_search(search_query, "pdf_documents")
            return {
                "status": "success",
                "data": {
                    "processed_data": state.current_data,
                    "processing_type": component.source,
                    "search_results": search_results[:3]
                }
            }
    
    def handle_decision_node(self, component: AgentComponent, state: Any) -> Dict[str, Any]:
        """Handle Decision Node components"""
        logger.info(f"Processing decision: {component.source}")
        
        # Make decisions based on current state
        if "Portfolio Strategy" in component.type:
            risk_score = state.current_data.get("risk_analysis", {}).get("risk_score", 0.5)
            decision = "conservative" if risk_score > 0.7 else "aggressive" if risk_score < 0.3 else "balanced"
            return {
                "status": "success",
                "data": {
                    "strategy_decision": decision,
                    "confidence": 0.9,
                    "reasoning": f"Based on risk score of {risk_score}"
                }
            }
        elif "Fraud Check" in component.type:
            # Simulate fraud detection
            fraud_score = 0.15  # Low fraud probability
            is_fraud = fraud_score > 0.8
            return {
                "status": "success",
                "data": {
                    "fraud_detected": is_fraud,
                    "fraud_score": fraud_score,
                    "action": "approve" if not is_fraud else "block"
                }
            }
        else:
            return {
                "status": "success",
                "data": {
                    "decision": "approved",
                    "decision_type": component.source,
                    "confidence": 0.8
                }
            }
    
    def handle_output_component(self, component: AgentComponent, state: Any) -> Dict[str, Any]:
        """Handle Graph Output components"""
        logger.info(f"Generating output: {component.source}")
        
        # Generate appropriate output based on component type
        if "Investment Recommendation" in component.type:
            strategy = state.current_data.get("strategy_decision", "balanced")
            return {
                "status": "success",
                "data": {
                    "recommendation": f"Recommended {strategy} investment strategy",
                    "portfolio_allocation": {
                        "stocks": 60 if strategy == "aggressive" else 40,
                        "bonds": 20 if strategy == "aggressive" else 50,
                        "cash": 20 if strategy == "aggressive" else 10
                    },
                    "confidence": 0.9
                }
            }
        elif "Response Output" in component.type:
            intent = state.current_data.get("intent", "general_inquiry")
            fraud_status = state.current_data.get("action", "approve")
            return {
                "status": "success",
                "data": {
                    "response": f"Your {intent} has been processed and {fraud_status}d",
                    "next_steps": ["Account updated", "Confirmation sent"],
                    "timestamp": datetime.now().isoformat()
                }
            }
        else:
            return {
                "status": "success",
                "data": {
                    "output": state.current_data,
                    "output_type": component.source,
                    "final_result": True
                }
            }
    
    def handle_edge_component(self, component: AgentComponent, state: Any) -> Dict[str, Any]:
        """Handle Edge components (connections)"""
        logger.info(f"Processing edge: {component.source}")
        
        # Edges typically just pass data through with optional transformations
        return {
            "status": "success",
            "data": {
                "connection_type": component.source,
                "data_flow": "passed_through"
            }
        }
    
    async def execute_workflow(self, workflow_id: str, components: List[AgentComponent], input_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """Execute a workflow using LangGraph with modular workflow support"""
        try:
            logger.info(f"Starting workflow execution: {workflow_id}")
            
            # Detect workflow type for modular handling
            workflow_type = self.detect_workflow_type(components)
            logger.info(f"Detected workflow type: {workflow_type}")
            
            # Initialize workflow handler if available
            workflow_handler = None
            if self.workflow_registry:
                workflow_handler = self.workflow_registry.get_handler(workflow_type)
                if workflow_handler:
                    # Pass the Oracle tools reference to the handler
                    workflow_handler.oracle_tools = oracle_tools
                    logger.info(f"Using modular handler for {workflow_type}")
            
            # Build the workflow graph
            workflow_graph = self.build_workflow_graph(components)
            
            # Compile the graph
            compiled_workflow = workflow_graph.compile()
            
            # Initialize state
            initial_state = {
                "messages": [SystemMessage(content=f"Starting {workflow_type} workflow execution")],
                "current_data": input_data or {},
                "results": {},
                "error": None
            }
            
            # Add workflow type to state for handlers
            initial_state["current_data"]["_workflow_type"] = workflow_type
            initial_state["current_data"]["_workflow_id"] = workflow_id
            
            # Execute the workflow
            start_time = datetime.now()
            final_state = await compiled_workflow.ainvoke(initial_state)
            execution_time = (datetime.now() - start_time).total_seconds()
            
            # Prepare result with workflow type information
            result = {
                "workflow_id": workflow_id,
                "workflow_type": workflow_type,
                "status": "completed" if not final_state.get("error") else "failed",
                "execution_time": execution_time,
                "final_data": final_state.get("current_data", {}),
                "component_results": final_state.get("results", {}),
                "error": final_state.get("error"),
                "handler_used": "modular" if workflow_handler else "legacy"
            }
            
            logger.info(f"Workflow {workflow_id} ({workflow_type}) completed in {execution_time:.2f} seconds")
            return result
            
        except Exception as e:
            logger.error(f"Workflow execution error: {str(e)}")
            return {
                "workflow_id": workflow_id,
                "status": "failed",
                "error": str(e),
                "execution_time": 0
            }

# Initialize workflow executor
workflow_executor = AgenticWorkflowExecutor()

@app.post("/api/workflow/execute", response_model=WorkflowResponse)
async def execute_workflow(request: WorkflowRequest, background_tasks: BackgroundTasks):
    """Execute an agentic AI workflow"""
    try:
        workflow_id = str(uuid.uuid4())
        
        # Store workflow in active workflows
        active_workflows[workflow_id] = {
            "status": "running",
            "request": request.dict(),
            "start_time": datetime.now(),
            "current_step": "initializing"
        }
        
        # Execute workflow in background
        async def run_workflow():
            result = await workflow_executor.execute_workflow(
                workflow_id=workflow_id,
                components=request.components,
                input_data=request.input_data or {"user_query": request.user_query}
            )
            active_workflows[workflow_id].update({
                "status": result["status"],
                "result": result,
                "end_time": datetime.now()
            })
        
        background_tasks.add_task(run_workflow)
        
        return WorkflowResponse(
            workflow_id=workflow_id,
            status="started",
            result=None
        )
        
    except Exception as e:
        logger.error(f"Error starting workflow: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/workflow/status/{workflow_id}", response_model=WorkflowStatus)
async def get_workflow_status(workflow_id: str):
    """Get the status of a running workflow"""
    if workflow_id not in active_workflows:
        raise HTTPException(status_code=404, detail="Workflow not found")
    
    workflow = active_workflows[workflow_id]
    
    return WorkflowStatus(
        workflow_id=workflow_id,
        status=workflow["status"],
        current_step=workflow.get("current_step"),
        result=workflow.get("result"),
        error=workflow.get("result", {}).get("error") if workflow.get("result") else None
    )

@app.get("/api/workflow/result/{workflow_id}")
async def get_workflow_result(workflow_id: str):
    """Get the final result of a completed workflow"""
    if workflow_id not in active_workflows:
        raise HTTPException(status_code=404, detail="Workflow not found")
    
    workflow = active_workflows[workflow_id]
    
    if workflow["status"] == "running":
        raise HTTPException(status_code=202, detail="Workflow still running")
    
    return workflow.get("result", {})

@app.get("/api/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "oracle_db_available": oracle_tools.connection_available,
        "llm_available": workflow_executor.llm is not None,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/api/workflows")
async def list_workflows():
    """List all workflows"""
    workflows = []
    for workflow_id, workflow in active_workflows.items():
        workflows.append({
            "workflow_id": workflow_id,
            "status": workflow["status"],
            "start_time": workflow["start_time"].isoformat(),
            "workflow_name": workflow["request"].get("workflow_name", "Unknown")
        })
    
    return {"workflows": workflows}

if __name__ == "__main__":
    import uvicorn
    
    logger.info("Starting Agentic AI LangGraph API server...")
    logger.info(f"Oracle DB 23ai MCP Available: {oracle_tools.connection_available}")
    logger.info(f"LLM Available: {workflow_executor.llm is not None}")
    
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8001,
        log_level="info"
    )
