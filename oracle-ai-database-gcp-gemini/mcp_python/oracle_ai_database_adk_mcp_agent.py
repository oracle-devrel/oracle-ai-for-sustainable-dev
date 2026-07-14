"""
Oracle AI Database Agent with ADK + MCP Toolbox for Databases

This implementation combines:
- Google Agent Development Kit (ADK) for agent orchestration
- MCP Toolbox for Databases for Oracle database operations
- Oracle AI Vector Search for RAG capabilities

MCP Toolbox provides connection pooling, authentication, and observability
for database operations without requiring custom SQLcl MCP server setup.
"""
import os
import asyncio
from dotenv import load_dotenv
from google.adk import Agent, Runner
from google.adk.sessions import InMemorySessionService
from toolbox_core import ToolboxClient

# Load environment variables from parent directory
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))

# Configuration
GCP_PROJECT_ID = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
GCP_REGION = os.getenv("GCP_REGION", "us-central1")
TOOLBOX_URL = os.getenv("TOOLBOX_URL", "http://127.0.0.1:5000")
MODEL_NAME = "gemini-2.5-flash"  # Improved tool calling over 2.0


class OracleADKMCPAgent:
    """ADK Agent with MCP Toolbox for Oracle Database operations"""
    
    def __init__(self):
        self.agent = None
        self.toolbox_client = None
        
    async def initialize(self):
        """Initialize the agent with MCP Toolbox tools"""
        print("\n🚀 Initializing Oracle AI Database Agent with MCP Toolbox...")
        
        try:
            # Connect to MCP Toolbox server
            print(f"  → Connecting to MCP Toolbox at {TOOLBOX_URL}")
            self.toolbox_client = ToolboxClient(TOOLBOX_URL)
            
            # Load Oracle RAG toolset from Toolbox
            print("  → Loading oracle-rag-toolset from Toolbox")
            tools = await self.toolbox_client.load_toolset("oracle-rag-toolset")
            
            print(f"  ✓ Loaded {len(tools)} tools from MCP Toolbox")
            
            # Create ADK agent with tools
            print(f"  → Creating ADK agent with model: {MODEL_NAME}")
            self.agent = Agent(
                name='oracle_ai_agent',
                model=MODEL_NAME,
                instruction=self._get_system_instruction(),
                tools=tools
            )
            
            print("  ✓ Agent initialized successfully\n")
            
        except Exception as e:
            raise RuntimeError(f"Failed to initialize agent: {e}")
    
    def _get_system_instruction(self) -> str:
        """Get the system instruction for the agent"""
        return """You are an expert Oracle Database assistant with access to a RAG knowledge base and database tools.

Your capabilities:
1. **Search Knowledge Base**: Use search-rag-documents to find relevant information about Oracle Database features, concepts, and best practices.
2. **Database Analysis**: Use get-rag-stats, list-tables, and get-table-schema to understand the database structure.
3. **SQL Execution**: Use execute-sql for custom queries and data analysis.

When answering questions:
- First, use search-rag-documents to find relevant information from the knowledge base
- Provide accurate answers based on the retrieved context
- If the knowledge base doesn't have the answer, use your general knowledge but clearly indicate this
- For database-specific questions, use the appropriate tools to inspect schema and data
- Always cite your sources when using the knowledge base

For complex questions:
- Break them into sub-questions
- Use multiple tool calls if needed
- Synthesize information from multiple sources

Be helpful, accurate, and concise in your responses."""
    
    async def run_interactive(self):
        """Run the agent in interactive CLI mode"""
        print("=" * 70)
        print("🤖 Oracle AI Database Agent - MCP Toolbox Edition")
        print("=" * 70)
        print("\nAsk questions about Oracle Database features, or query the database.")
        print("Type 'quit' or 'exit' to stop.\n")
        
        runner = Runner(
            agent=self.agent,
            session_service=InMemorySessionService()
        )
        
        while True:
            try:
                # Get user input
                user_input = input("\n💬 You: ").strip()
                
                if not user_input:
                    continue
                    
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("\n👋 Goodbye!")
                    break
                
                # Run the agent
                print(f"\n🤔 Agent: ", end='', flush=True)
                result = await runner.run(user_input, session_id='default')
                print(result.text)
                
            except KeyboardInterrupt:
                print("\n\n👋 Goodbye!")
                break
            except Exception as e:
                print(f"\n❌ Error: {e}")
                print("Please try again.")
    
    async def cleanup(self):
        """Cleanup resources"""
        if self.toolbox_client:
            await self.toolbox_client.close()


async def main():
    """Main entry point"""
    agent = OracleADKMCPAgent()
    
    try:
        # Initialize agent
        await agent.initialize()
        
        # Run interactive loop
        await agent.run_interactive()
        
    except Exception as e:
        print(f"\n❌ Fatal Error: {e}")
        return 1
    finally:
        # Cleanup
        await agent.cleanup()
    
    return 0


if __name__ == "__main__":
    # Run the async main function
    exit_code = asyncio.run(main())
    exit(exit_code)
