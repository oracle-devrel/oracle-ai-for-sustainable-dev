"""
Oracle AI Database ADK Agent (RAG Only - No MCP)
=================================================
This is the original version without MCP integration.
Uses only the Oracle RAG API (vector search) for answering questions.

For the MCP-enabled version with direct database access, see:
oracle_ai_database_adk_agent.py

Google Agent Development Kit (ADK) agent that uses Oracle RAG API as a tool
"""
import os
import requests
from dotenv import load_dotenv
from google.cloud import aiplatform
from vertexai.preview import reasoning_engines

# Load environment variables
load_dotenv()

class OracleRAGAgent:
    """Agent that uses Oracle Database RAG API for answering questions"""
    
    def __init__(self, api_url: str, project_id: str, location: str):
        """
        Initialize the Oracle RAG Agent
        
        Args:
            api_url: Base URL of the Oracle RAG API
            project_id: GCP project ID
            location: GCP region
        """
        self.api_url = api_url.rstrip('/')
        self.project_id = project_id
        self.location = location
        
        # Initialize Vertex AI
        aiplatform.init(project=project_id, location=location)
        
    def query_oracle_rag(self, query: str, top_k: int = 5) -> dict:
        """
        Query the Oracle RAG knowledge base
        
        Args:
            query: The question to ask
            top_k: Number of similar chunks to retrieve
            
        Returns:
            Dictionary with answer and metadata
        """
        try:
            response = requests.post(
                f"{self.api_url}/query",
                json={"query": query, "top_k": top_k},
                timeout=120  # Increased timeout for LLM processing
            )
            response.raise_for_status()
            return response.json()
        except requests.exceptions.Timeout:
            return {
                "error": "Request timed out. The API might be processing a large query.",
                "answer": "I couldn't retrieve information from the knowledge base (timeout). Please try again."
            }
        except requests.exceptions.ConnectionError as e:
            return {
                "error": f"Cannot connect to API at {self.api_url}. Is it running? {str(e)}",
                "answer": "I couldn't connect to the knowledge base API. Please verify it's running."
            }
        except requests.exceptions.RequestException as e:
            return {
                "error": f"Failed to query Oracle RAG API: {str(e)}",
                "answer": "I couldn't retrieve information from the knowledge base. Please try again."
            }
    
    def get_api_status(self) -> dict:
        """Get the status of the Oracle RAG API"""
        try:
            response = requests.get(f"{self.api_url}/status", timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.ConnectionError as e:
            return {"error": f"Cannot connect to {self.api_url}", "status": "unavailable"}
        except requests.exceptions.RequestException as e:
            return {"error": str(e), "status": "unavailable"}
    
    def create_agent(self):
        """
        Create a reasoning engine agent with Oracle RAG tool
        
        Returns:
            Reasoning engine instance
        """
        # Define the agent's tools
        tools = [
            {
                "function_declarations": [
                    {
                        "name": "query_oracle_database",
                        "description": "Search the Oracle Database knowledge base to find information about Oracle Database features, spatial capabilities, vector search, and other database topics. Use this tool when users ask questions about database functionality, features, or documentation.",
                        "parameters": {
                            "type": "object",
                            "properties": {
                                "query": {
                                    "type": "string",
                                    "description": "The question or search query about Oracle Database"
                                },
                                "top_k": {
                                    "type": "integer",
                                    "description": "Number of relevant document chunks to retrieve (1-20)",
                                    "default": 5
                                }
                            },
                            "required": ["query"]
                        }
                    },
                    {
                        "name": "check_knowledge_base_status",
                        "description": "Check the status and availability of the Oracle Database knowledge base, including document count and system health.",
                        "parameters": {
                            "type": "object",
                            "properties": {}
                        }
                    }
                ]
            }
        ]
        
        # Define agent instructions
        instructions = """You are a helpful assistant specializing in Oracle Database.
        
You have access to a knowledge base containing Oracle Database documentation and information.

When users ask questions about Oracle Database:
1. Use the query_oracle_database tool to search the knowledge base
2. Provide clear, accurate answers based on the retrieved information
3. If the knowledge base doesn't have relevant information, say so clearly
4. You can also use your general knowledge, but prefer the knowledge base for specific technical questions

When users ask about the system status:
- Use check_knowledge_base_status to get information about the knowledge base

Be concise, helpful, and technically accurate."""
        
        # Tool function implementations
        def query_oracle_database(query: str, top_k: int = 5) -> str:
            """Query the Oracle RAG knowledge base"""
            result = self.query_oracle_rag(query, top_k)
            if "error" in result:
                return result["answer"]
            return f"{result['answer']}\n\n[Retrieved from {len(result.get('context_chunks', []))} document chunks in {result.get('total_time', 0)}s]"
        
        def check_knowledge_base_status() -> str:
            """Check knowledge base status"""
            status = self.get_api_status()
            if "error" in status:
                return f"Knowledge base status: unavailable - {status['error']}"
            return f"Knowledge base status: {status['status']}\nDocuments: {status['document_count']}\nDatabase connected: {status['database_connected']}"
        
        # Create reasoning engine
        agent = reasoning_engines.LangchainAgent(
            model="gemini-2.0-flash-exp",
            tools=tools,
            agent_executor_kwargs={"return_intermediate_steps": True},
            model_kwargs={
                "temperature": 0.7,
                "max_output_tokens": 2048
            }
        )
        
        # Set up tool routing
        agent.set_up(
            {
                "query_oracle_database": query_oracle_database,
                "check_knowledge_base_status": check_knowledge_base_status
            },
            instructions=instructions
        )
        
        return agent
    
    def run_cli(self):
        """Run interactive CLI interface"""
        print("=" * 70)
        print("Oracle Database AI Agent (powered by Google ADK)")
        print("=" * 70)
        print(f"API URL: {self.api_url}")
        print(f"Project: {self.project_id}")
        print(f"Region: {self.location}")
        print()
        
        # Check API status
        status = self.get_api_status()
        if "error" not in status:
            print(f"✓ Knowledge base: {status['document_count']} documents")
            print(f"✓ Status: {status['status']}")
        else:
            print(f"⚠️  Warning: {status.get('error', 'API unavailable')}")
        
        print()
        print("Type your questions about Oracle Database (or 'quit' to exit)")
        print("-" * 70)
        print()
        
        # Simple direct query mode (without full ADK agent for now)
        while True:
            try:
                user_input = input("You: ").strip()
                
                if not user_input:
                    continue
                    
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("\nGoodbye!")
                    break
                
                if user_input.lower() in ['status', 'health']:
                    status = self.get_api_status()
                    if "error" in status:
                        print(f"\n❌ Error: {status['error']}\n")
                    else:
                        print(f"\n✓ Status: {status['status']}")
                        print(f"✓ Documents: {status['document_count']}")
                        print(f"✓ Database: {'Connected' if status['database_connected'] else 'Disconnected'}")
                        print(f"✓ Models: {'Loaded' if status['models_loaded'] else 'Not loaded'}\n")
                    continue
                
                # Query the RAG API
                print("\n🔍 Searching knowledge base...")
                result = self.query_oracle_rag(user_input)
                
                if "error" in result:
                    print(f"\n❌ Error: {result['error']}\n")
                else:
                    print(f"\n💡 Answer:\n{result['answer']}")
                    print(f"\n📊 Retrieved {len(result.get('context_chunks', []))} chunks in {result.get('total_time', 0):.2f}s")
                    print(f"   - Vector search: {result.get('vector_search_time', 0):.2f}s")
                    print(f"   - LLM response: {result.get('llm_response_time', 0):.2f}s\n")
                
            except KeyboardInterrupt:
                print("\n\nGoodbye!")
                break
            except Exception as e:
                print(f"\n❌ Error: {str(e)}\n")

def main():
    """Main entry point"""
    # Configuration
    api_url = os.getenv("ORACLE_RAG_API_URL", "http://34.48.146.146:8501")
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    
    # Create and run agent
    agent = OracleRAGAgent(api_url, project_id, location)
    agent.run_cli()

if __name__ == "__main__":
    main()
