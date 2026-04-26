"""
Oracle AI Database ADK Agent with MCP Integration
Google Agent Development Kit (ADK) agent that uses:
- Oracle RAG API for vector search via FunctionDeclaration
- Oracle MCP Server for direct database queries via McpToolset

Note: Uses oracle_mcp_wrapper to work around ADK v1.22.1 schema converter bug
Bug: AttributeError: 'list' object has no attribute 'items'
Location: google/adk/tools/_gemini_schema_util.py line 160
"""
import os
import asyncio
import requests
from dotenv import load_dotenv
from google.adk.agents import LlmAgent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.adk.artifacts.in_memory_artifact_service import InMemoryArtifactService
from google.adk.tools.mcp_tool.mcp_toolset import McpToolset, StdioServerParameters
from google.adk.tools.mcp_tool import StdioConnectionParams
from google.genai import types

# Import schema wrapper to fix ADK MCP bug
from oracle_mcp_wrapper import patch_mcp_toolset

# Load environment variables
load_dotenv()

# Patch McpToolset to handle complex Oracle schemas
print("  → Applying MCP schema wrapper patch for ADK v1.22.1...")
patch_mcp_toolset()
print("  ✓ MCP schema wrapper applied")

class OracleRAGMCPAgent:
    """Agent that uses Oracle Database RAG API and MCP Server for answering questions"""
    
    def __init__(self, api_url: str, project_id: str, location: str, 
                 sqlcl_path: str = "/opt/sqlcl/bin/sql",
                 wallet_path: str = None):
        """
        Initialize the Oracle RAG Agent with MCP integration
        
        Args:
            api_url: Base URL of the Oracle RAG API
            project_id: GCP project ID
            location: GCP region
            sqlcl_path: Path to SQLcl executable for MCP server
            wallet_path: Path to Oracle wallet directory
        """
        self.api_url = api_url.rstrip('/')
        self.project_id = project_id
        self.location = location
        self.sqlcl_path = sqlcl_path
        self.wallet_path = wallet_path or os.path.expanduser("~/wallet")
        self.agent = None
        self.runner = None
        self.session_service = InMemorySessionService()
        self.artifacts_service = InMemoryArtifactService()
        self.session = None
        
    def query_oracle_database_docs(self, query: str, top_k: int = 5) -> str:
        """
        Query the Oracle RAG knowledge base for Oracle Database documentation
        This is implemented as a regular function for use with ADK
        
        Args:
            query: The question to ask about Oracle Database
            top_k: Number of similar chunks to retrieve
            
        Returns:
            String with the answer from documentation
        """
        try:
            response = requests.post(
                f"{self.api_url}/query",
                json={"query": query, "top_k": top_k},
                timeout=30
            )
            response.raise_for_status()
            result = response.json()
            return result.get("answer", "No answer found in documentation")
        except Exception as e:
            return f"Error querying documentation: {str(e)}"
    
    def get_api_status(self) -> dict:
        """Get the status of the Oracle RAG API"""
        try:
            response = requests.get(f"{self.api_url}/status", timeout=10)
            response.raise_for_status()
            return response.json()
        except Exception as e:
            return {"error": f"Failed to get API status: {str(e)}", "status": "unavailable"}
            
            return "No response from MCP tool"
            
        except Exception as e:
            return f"Error calling MCP tool: {str(e)}"
    
    async def create_agent_async(self):
        """
        Create ADK agent with Oracle RAG function and MCP database tools
        Following official ADK MCP pattern from:
        https://github.com/GoogleCloudPlatform/generative-ai/blob/main/gemini/mcp/adk_mcp_app/main.py
        """
        print("  → Initializing ADK session service...")
        
        # Create session
        self.session = await self.session_service.create_session(
            app_name="Oracle AI Database Agent",
            user_id="user_123",
            state={}
        )
        
        # Create StdioServerParameters for SQLcl MCP server
        # Set protocol version to match server (2024-11-05) to avoid negotiation issues
        mcp_server_params = StdioServerParameters(
            command=self.sqlcl_path,
            args=["-mcp"],
            env={"TNS_ADMIN": self.wallet_path}
        )
        
        # Wrap in StdioConnectionParams with explicit protocol version
        # This fixes the protocol version mismatch error:
        # Client was requesting 2025-11-25, server offers 2024-11-05
        mcp_connection_params = StdioConnectionParams(
            server_params=mcp_server_params,
            protocol_version="2024-11-05"  # Match Oracle SQLcl MCP server version
        )
        
        # Define agent instruction
        instruction = """You are an expert Oracle Database AI assistant with access to:

1. **query_oracle_database_docs**: Search Oracle Database documentation (features, spatial, vector search, JSON, SQL)
2. **MCP Database Tools**: Direct database operations via Oracle SQLcl MCP server

**Usage Strategy:**
- Data queries ("show", "list tables", "run SQL", "get schema") → use MCP tools
- For database operations: list-connections → connect → run-sql or schema-information
- Documentation questions can be answered from your knowledge or by querying the database

Always be helpful, concise, and technically accurate."""
        
        print("  → Creating ADK LlmAgent with MCP integration...")
        
        # Create agent with MCP tools directly via McpToolset
        # Following official ADK MCP pattern
        # Note: Increased timeouts to handle MCP server initialization
        try:
            self.agent = LlmAgent(
                model="gemini-2.0-flash-exp",
                name="oracle_assistant",
                instruction=instruction,
                tools=[McpToolset(
                    connection_params=mcp_connection_params,
                    # Add timeout configuration if supported
                    timeout=120.0  # 2 minute timeout for MCP operations
                )]
            )
        except TypeError:
            # If timeout parameter not supported, fallback to default
            self.agent = LlmAgent(
                model="gemini-2.0-flash-exp",
                name="oracle_assistant",
                instruction=instruction,
                tools=[McpToolset(connection_params=mcp_connection_params)]
            )
        
        print("  ✓ Agent created with MCP toolset")
        
        # Create runner
        self.runner = Runner(
            app_name="Oracle AI Database Agent",
            agent=self.agent,
            artifact_service=self.artifacts_service,
            session_service=self.session_service
        )
        
        print("  ✓ Runner initialized")
        return self.agent
    
    async def query_async(self, user_input: str) -> str:
        """
        Query the agent asynchronously using ADK Runner
        
        Args:
            user_input: User's question or request
            
        Returns:
            Agent response as string
        """
        if not self.runner or not self.session:
            raise RuntimeError("Agent not initialized. Call create_agent_async() first.")
        
        try:
            # Create message content
            content = types.Content(
                role="user",
                parts=[types.Part(text=user_input)]
            )
            
            # Run agent with message
            events = self.runner.run_async(
                session_id=self.session.id,
                user_id="user_123",
                new_message=content
            )
            
            # Collect response parts
            response_parts = []
            async for event in events:
                if event.content.role == "model" and event.content.parts:
                    for part in event.content.parts:
                        if part.text:
                            response_parts.append(part.text)
            
            return "\n".join(response_parts) if response_parts else "No response generated"
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return f"Error during agent reasoning: {str(e)}"
    
    async def cleanup_async(self):
        """Cleanup resources"""
        if self.runner:
            await self.runner.close()
    
    
    async def run_cli_async(self):
        """Run interactive CLI interface with ADK"""
        print("=" * 80)
        print("Oracle Database AI Agent with RAG + MCP (powered by Google ADK)")
        print("=" * 80)
        print(f"RAG API URL: {self.api_url}")
        print(f"SQLcl Path: {self.sqlcl_path}")
        print(f"Wallet Path: {self.wallet_path}")
        print(f"Project: {self.project_id}")
        print(f"Region: {self.location}")
        print()
        
        # Check API status
        status = self.get_api_status()
        if "error" not in status:
            print(f"✓ Knowledge base: {status['document_count']} documents")
            print(f"✓ RAG API Status: {status['status']}")
        else:
            print(f"⚠️  Warning: {status.get('error', 'RAG API unavailable')}")
        
        print()
        print("🔧 Initializing ADK agent with MCP tools...")
        
        try:
            await self.create_agent_async()
            print()
            print("Type your questions about Oracle Database (or 'quit' to exit)")
            print("-" * 80)
            print()
            
            # Interactive loop
            while True:
                try:
                    user_input = input("You: ").strip()
                    
                    if not user_input:
                        continue
                        
                    if user_input.lower() in ['quit', 'exit', 'q']:
                        print("\nGoodbye!")
                        break
                    
                    # Process query using ADK runner
                    response = await self.query_async(user_input)
                    print(f"\nAgent: {response}\n")
                    
                except KeyboardInterrupt:
                    print("\n\nInterrupted. Goodbye!")
                    break
                except Exception as e:
                    print(f"\nError: {str(e)}\n")
                    import traceback
                    traceback.print_exc()
                    continue
            
            # Cleanup
            await self.cleanup_async()
                    
        except Exception as e:
            print(f"\n❌ Failed to initialize agent: {str(e)}")
            import traceback
            traceback.print_exc()
            await self.cleanup_async()

async def main():
    """Main entry point"""
    # Configuration
    api_url = os.getenv("ORACLE_RAG_API_URL", "http://localhost:8501")
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    sqlcl_path = os.getenv("SQLCL_PATH", "/opt/sqlcl/bin/sql")
    wallet_path = os.getenv("TNS_ADMIN", os.path.expanduser("~/wallet"))
    
    # Create and run agent
    agent = OracleRAGMCPAgent(
        api_url=api_url,
        project_id=project_id,
        location=location,
        sqlcl_path=sqlcl_path,
        wallet_path=wallet_path
    )
    await agent.run_cli_async()

if __name__ == "__main__":
    asyncio.run(main())
