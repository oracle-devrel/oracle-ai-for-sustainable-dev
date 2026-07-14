"""
Oracle AI Database ADK Agent - Direct RAG Implementation
Uses Google Agent Development Kit (ADK) with direct Oracle Vector Store integration.

This version:
- Connects directly to Oracle Database with vector storage
- Implements RAG using langchain + Vertex AI embeddings
- Uses ADK LlmAgent with custom RAG tool
- Same functionality as oracle_ai_database_gemini_rag.ipynb notebook

Known Limitations:
- ADK/Gemini 2.0 sometimes shows tool calls as markdown code blocks instead of executing them
- This is intermittent model behavior - if it happens, try rephrasing your question
- For more reliable tool execution, use the Streamlit UI (option 1) or GenerativeModel + MCP (option 4)
"""
import os
import asyncio
from dotenv import load_dotenv
import vertexai
import oracledb
from google.adk.agents import LlmAgent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.adk.artifacts.in_memory_artifact_service import InMemoryArtifactService
from google.adk.tools import BaseTool, ToolContext
from google.genai import types
from google.genai.types import Tool, FunctionDeclaration, Schema, Type
from langchain_google_vertexai import VertexAIEmbeddings
from langchain_community.vectorstores.oraclevs import OracleVS
from langchain_community.vectorstores.utils import DistanceStrategy

# Load environment variables from parent directory
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))


class OracleRAGTool(BaseTool):
    """Custom ADK tool for querying Oracle Vector Store directly"""
    
    def __init__(self, knowledge_base: OracleVS):
        """Initialize the RAG tool
        
        Args:
            knowledge_base: Oracle Vector Store instance
        """
        super().__init__(
            name="query_oracle_database",
            description="Search the Oracle Database knowledge base for information about Oracle Database features, spatial capabilities, vector search, JSON features, SQL enhancements, and other database topics. Use this for technical questions that require specific documentation or feature details."
        )
        self.knowledge_base = knowledge_base
    
    async def execute(self, query: str, top_k: int = 5, context: ToolContext | None = None) -> dict:
        """Execute the RAG query
        
        Args:
            query: The question to ask
            top_k: Number of similar chunks to retrieve
            context: Tool execution context
            
        Returns:
            Dict with result string
        """
        try:
            print(f"  → Searching vector store: {query[:60]}...")
            
            # Perform similarity search
            result_chunks = self.knowledge_base.similarity_search(query, k=top_k)
            
            if not result_chunks:
                return {"result": "No relevant documentation found for this query."}
            
            # Format context from chunks
            context_parts = []
            for i, chunk in enumerate(result_chunks, 1):
                context_parts.append(f"[{i}] {chunk.page_content}")
            
            context = "\n\n".join(context_parts)
            result_text = f"Found {len(result_chunks)} relevant documentation sections:\n\n{context}"
            
            print(f"  ✓ Found {len(result_chunks)} documentation chunks")
            return {"result": result_text}
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return {"result": f"Failed to query the knowledge base: {str(e)}"}


class OracleADKRAGAgent:
    """ADK Agent for Oracle Database documentation search with direct vector store"""
    
    def __init__(self, project_id: str, location: str):
        """
        Initialize the ADK RAG Agent
        
        Args:
            project_id: GCP project ID
            location: GCP region
        """
        self.project_id = project_id
        self.location = location
        self.agent = None
        self.runner = None
        self.session_service = InMemorySessionService()
        self.artifacts_service = InMemoryArtifactService()
        self.session = None
        self.connection = None
        self.knowledge_base = None
        self.auth_script_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "auth.sh"))

    def _is_adc_auth_error(self, error: Exception) -> bool:
        """Return True when the exception chain indicates ADC reauthentication is needed."""
        error_messages = []
        current_error = error

        while current_error is not None:
            error_messages.append(str(current_error).lower())
            current_error = current_error.__cause__ or current_error.__context__

        combined = "\n".join(error_messages)
        auth_markers = [
            "reauthentication is needed",
            "gcloud auth application-default login",
            "failed to retrieve access token",
            "invalid_grant",
            "unable to acquire impersonated credentials",
        ]
        return any(marker in combined for marker in auth_markers)

    def _print_adc_auth_help(self):
        """Print concise remediation for ADC auth failures."""
        print("\n  ⚠️  Google ADC authentication is required.")
        if os.path.exists(self.auth_script_path):
            print(f"  → Run: {self.auth_script_path}")
        print("  → Or run: gcloud auth application-default login --no-launch-browser")
        
    def connect_database(self):
        """Connect to Oracle Database and initialize vector store"""
        print("  → Connecting to Oracle Database...")
        
        # Load credentials from environment
        un = os.getenv("DB_USERNAME")
        pw = os.getenv("DB_PASSWORD")
        dsn = os.getenv("DB_DSN")
        wallet_path = os.getenv("DB_WALLET_DIR")
        wpwd = os.getenv("DB_WALLET_PASSWORD", "")
        
        if not all([un, pw, dsn, wallet_path]):
            raise ValueError("Missing database credentials in .env file")
        
        # Connect to database
        self.connection = oracledb.connect(
            config_dir=wallet_path,
            user=un,
            password=pw,
            dsn=dsn,
            wallet_location=wallet_path,
            wallet_password=wpwd
        )
        
        print(f"  ✓ Connected to {dsn}")
        
        # Check how many documents are in the store
        with self.connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM RAG_TAB")
            count = cursor.fetchone()[0]
            print(f"  ✓ Found {count} document chunks in RAG_TAB")
        
        # Initialize Vertex AI embeddings with retry logic
        print("  → Initializing Vertex AI embeddings...")
        try:
            embeddings = VertexAIEmbeddings(model_name="text-embedding-004")
            
            # Connect to existing vector store
            print("  → Connecting to vector store RAG_TAB...")
            self.knowledge_base = OracleVS(
                client=self.connection,
                embedding_function=embeddings,
                table_name="RAG_TAB",
                distance_strategy=DistanceStrategy.DOT_PRODUCT
            )
            
            print(f"  ✓ Vector store ready")
            
        except Exception as e:
            if self._is_adc_auth_error(e):
                self._print_adc_auth_help()
                raise RuntimeError("Google ADC authentication required") from e

            print(f"  ⚠️  Error initializing vector store: {str(e)}")
            raise
        
        return self.knowledge_base
    
    async def create_agent_async(self):
        """Create ADK agent with Oracle RAG tool"""
        print("  → Initializing ADK session service...")
        
        # Initialize Vertex AI globally
        vertexai.init(project=self.project_id, location=self.location)
        
        # Set environment variables for ADK/GenAI SDK to use Vertex AI
        os.environ['GOOGLE_CLOUD_PROJECT'] = self.project_id
        os.environ['GOOGLE_CLOUD_LOCATION'] = self.location
        os.environ['GOOGLE_GENAI_USE_VERTEXAI'] = 'true'
        
        # Connect to database and initialize vector store
        self.connect_database()
        
        # Create session
        self.session = await self.session_service.create_session(
            app_name="Oracle AI Database RAG Agent",
            user_id="user_123",
            state={}
        )
        
        # Create custom RAG tool with vector store
        rag_tool = OracleRAGTool(self.knowledge_base)
        
        # Define agent instruction
        instruction = """You are an expert Oracle Database AI assistant.

You have access to a tool called query_oracle_database that searches documentation.

IMPORTANT: When you need information, call the query_oracle_database function - DO NOT write code blocks showing the function call. The system will execute it automatically.

After the tool returns results, analyze them and provide a helpful answer to the user."""
        
        print("  → Creating ADK LlmAgent with RAG tool...")
        
        # Create agent with RAG tool
        # Using gemini-2.5-flash (improved tool calling over 2.0)
        from google.genai.types import GenerateContentConfig

        self.agent = LlmAgent(
            model="gemini-2.5-flash",
            name="oracle_rag_assistant",
            instruction=instruction,
            tools=[rag_tool],
            generate_content_config=GenerateContentConfig(
                temperature=0.1,  # Lower temperature for more consistent tool usage
                max_output_tokens=2048,
            )
        )
        
        print("  ✓ Agent created with RAG tool")
        
        # Create runner
        self.runner = Runner(
            app_name="Oracle AI Database RAG Agent",
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
            user_input: User's question
            
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
            
            # Collect response parts and track tool calls
            response_parts = []
            tool_calls_made = 0
            
            async for event in events:
                # Check for tool calls and responses
                if event.content.parts:
                    for part in event.content.parts:
                        # Tool call - model is requesting to execute a tool
                        if hasattr(part, 'function_call') and part.function_call:
                            tool_calls_made += 1
                            print(f"  🔧 Tool call: {part.function_call.name}")
                        # Tool response - result from tool execution
                        elif hasattr(part, 'function_response') and part.function_response:
                            result = part.function_response.response.get('result', '')
                            if result:
                                print(f"  ✓ Tool result received ({len(result)} chars)")
                        # Text response from model - collect ALL text
                        elif part.text and event.content.role == "model":
                            # Check if model is outputting tool syntax instead of executing
                            if '```tool_code' in part.text or 'query_oracle_database(' in part.text:
                                print(f"  ⚠️  Model showing tool call instead of executing - extracting query...")
                                # Extract the query from various formats
                                import re
                                # Try multiple patterns
                                patterns = [
                                    r'query_oracle_database\s*\(\s*query\s*=\s*["\']([^"\']+)["\']',  # query="..."
                                    r'query_oracle_database\s*\(\s*["\']([^"\']+)["\']',  # query_oracle_database("...")
                                    r'query_oracle_database\([^)]*["\']([^"\']+)["\']',  # Any format with quotes
                                ]
                                
                                query = None
                                for pattern in patterns:
                                    match = re.search(pattern, part.text)
                                    if match:
                                        query = match.group(1)
                                        break
                                
                                if query:
                                    print(f"  → Executing manually: {query[:60]}...")
                                    # Manually execute the tool
                                    tool = OracleRAGTool(self.knowledge_base)
                                    tool_result = await tool.execute(query=query)
                                    
                                    # Now feed the results back to the LLM for synthesis
                                    print(f"  → Synthesizing response from {len(tool_result.get('result', ''))} chars...")
                                    synthesis_prompt = f"""Based on these documentation sections, please answer the user's question: "{user_input}"

Documentation sections:
{tool_result.get('result', 'No results found')}

Please provide a clear, concise answer based on this documentation."""
                                    
                                    # Make a follow-up call to synthesize the results
                                    synthesis_content = types.Content(
                                        role="user",
                                        parts=[types.Part(text=synthesis_prompt)]
                                    )
                                    
                                    synthesis_events = self.runner.run_async(
                                        session_id=self.session.id,
                                        user_id="user_123",
                                        new_message=synthesis_content
                                    )
                                    
                                    synthesis_parts = []
                                    async for synth_event in synthesis_events:
                                        if synth_event.content.parts:
                                            for synth_part in synth_event.content.parts:
                                                if synth_part.text and synth_event.content.role == "model":
                                                    synthesis_parts.append(synth_part.text)
                                    
                                    synthesized_response = "\n".join(synthesis_parts) if synthesis_parts else tool_result.get('result', 'No results found')
                                    return synthesized_response
                                else:
                                    # Log what we got for debugging
                                    print(f"  � Debug: Could not extract query from: {part.text[:200]}")
                                    response_parts.append("I attempted to search the documentation but encountered a formatting issue. Please rephrase your question.")
                            else:
                                response_parts.append(part.text)
            
            result = "\n".join(response_parts) if response_parts else "No response generated"
            return result
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return f"Error during agent reasoning: {str(e)}"
    
    async def cleanup_async(self):
        """Cleanup resources"""
        if self.runner:
            await self.runner.close()
        if self.connection:
            self.connection.close()
            print("  ✓ Database connection closed")
    
    async def run_cli_async(self):
        """Run interactive CLI interface with ADK"""
        print("=" * 80)
        print("Oracle Database ADK RAG Agent (Direct Vector Store)")
        print("=" * 80)
        print(f"Project: {self.project_id}")
        print(f"Region: {self.location}")
        print()
        
        print("🔧 Initializing ADK agent with RAG tool...")
        
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
            if self._is_adc_auth_error(e):
                print("\n❌ Failed to initialize agent due to expired or missing ADC credentials.")
                self._print_adc_auth_help()
            else:
                print(f"\n❌ Failed to initialize agent: {str(e)}")
                import traceback
                traceback.print_exc()
            await self.cleanup_async()


async def main():
    """Main entry point"""
    # Configuration from environment
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    
    print("Starting Oracle ADK RAG Agent...\n")
    
    # Create and run agent
    agent = OracleADKRAGAgent(project_id, location)
    await agent.run_cli_async()


if __name__ == "__main__":
    asyncio.run(main())
