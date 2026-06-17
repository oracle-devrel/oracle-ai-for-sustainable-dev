"""
Oracle AI Database RAG-Only Agent
Simple, reliable agent that only queries the Oracle Database documentation knowledge base.
No MCP, no database connections - just documentation search.

Based on working oracle_ai_database_adk_fullagent.py pattern.
"""
import os
import requests
from dotenv import load_dotenv
import vertexai
from vertexai.generative_models import GenerativeModel, Tool, FunctionDeclaration, Part
from typing import Dict, Any

# Load environment variables
load_dotenv()

class OracleRAGOnlyAgent:
    """Simple RAG-only agent for Oracle Database documentation"""
    
    def __init__(self, api_url: str, project_id: str, location: str):
        """
        Initialize the RAG-only agent
        
        Args:
            api_url: Base URL of the Oracle RAG API
            project_id: GCP project ID
            location: GCP region
        """
        self.api_url = api_url.rstrip('/')
        self.project_id = project_id
        self.location = location
        self.agent = None
        self.conversation_history = []
        
        # Initialize Vertex AI
        vertexai.init(project=project_id, location=location)
        
        print("🤖 Initializing Gemini Agent with RAG...")
        self.agent = self.create_agent()
        print("✓ Agent ready!\n")
        
    def query_oracle_rag(self, query: str, top_k: int = 5) -> dict:
        """Query the Oracle RAG knowledge base"""
        try:
            payload = {"query": query, "top_k": top_k}
            print(f"  → Querying knowledge base: {query[:60]}...")
            
            response = requests.post(
                f"{self.api_url}/query",
                json=payload,
                timeout=120
            )
            
            print(f"  ← API response status: {response.status_code}")
            
            if response.status_code != 200:
                error_detail = response.text
                print(f"  ← API error: {error_detail}")
                return {
                    "error": f"API returned {response.status_code}",
                    "answer": f"API error ({response.status_code}): {error_detail[:200]}"
                }
            
            response.raise_for_status()
            return response.json()
            
        except requests.exceptions.Timeout:
            return {
                "error": "Request timed out",
                "answer": "Knowledge base query timed out. Please try again."
            }
        except requests.exceptions.ConnectionError as e:
            return {
                "error": f"Cannot connect to API at {self.api_url}",
                "answer": "Cannot connect to the knowledge base API."
            }
        except requests.exceptions.RequestException as e:
            return {
                "error": f"API error: {str(e)}",
                "answer": "Failed to query the knowledge base."
            }
    
    def get_api_status(self) -> dict:
        """Get the status of the Oracle RAG API"""
        try:
            response = requests.get(f"{self.api_url}/status", timeout=30)
            response.raise_for_status()
            return response.json()
        except Exception as e:
            return {"error": str(e), "status": "unavailable"}
    
    def create_agent(self):
        """Create a Gemini agent with RAG function calling"""
        
        # Define function declarations
        query_oracle_function = FunctionDeclaration(
            name="query_oracle_database",
            description="Search the Oracle Database knowledge base for information about Oracle Database features, spatial capabilities, vector search, JSON features, SQL enhancements, and other database topics. Use this for technical questions that require specific documentation or feature details.",
            parameters={
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The detailed question or search query about Oracle Database"
                    },
                    "top_k": {
                        "type": "integer",
                        "description": "Number of relevant document chunks to retrieve (1-20)",
                        "default": 5
                    }
                },
                "required": ["query"]
            }
        )
        
        status_function = FunctionDeclaration(
            name="check_knowledge_base_status",
            description="Check the status and availability of the Oracle Database knowledge base system",
            parameters={
                "type": "object",
                "properties": {}
            }
        )
        
        # Create tool
        oracle_tool = Tool(
            function_declarations=[query_oracle_function, status_function]
        )
        
        # System instructions
        instructions = """You are an expert Oracle Database AI assistant with access to a comprehensive knowledge base.

**Your Capabilities:**
- Access to Oracle Database documentation through the knowledge base
- Answer questions about Oracle Database features, SQL, performance, configuration, etc.

**When to Use Tools:**
1. Use `query_oracle_database` when users ask about:
   - Specific Oracle Database features or functionality
   - SQL syntax, commands, or best practices
   - Database configuration or administration
   - Performance optimization
   - New features in recent Oracle versions
   - Vector search, spatial data, JSON capabilities
   - Technical implementation details

2. Use `check_knowledge_base_status` when users ask about:
   - System availability
   - How many documents are available
   - Whether the knowledge base is working

**Response Style:**
- Be concise but thorough
- Cite specific features or capabilities when possible
- If knowledge base doesn't have the answer, say so clearly
- Format technical content clearly with examples when helpful

Be helpful and technically accurate."""
        
        # Create Gemini model with tools
        model = GenerativeModel(
            "gemini-2.0-flash-exp",
            tools=[oracle_tool],
            system_instruction=instructions
        )
        
        return model
    
    def execute_function_call(self, function_name: str, args: dict) -> str:
        """Execute a function call from the agent"""
        print(f"  🔧 Tool called: {function_name}({args})")
        
        if function_name == "query_oracle_database":
            query = args.get("query", "")
            top_k = args.get("top_k", 5)
            result = self.query_oracle_rag(query, top_k)
            
            if "error" in result:
                return f"Error: {result['answer']}"
            
            response = f"{result['answer']}\n\n"
            response += f"[Source: {len(result.get('context_chunks', []))} document chunks, "
            response += f"processed in {result.get('total_time', 0):.2f}s]"
            return response
            
        elif function_name == "check_knowledge_base_status":
            status = self.get_api_status()
            if "error" in status:
                return f"Knowledge base unavailable: {status['error']}"
            
            return (
                f"Knowledge Base Status:\n"
                f"- Status: {status['status']}\n"
                f"- Documents: {status['document_count']} chunks\n"
                f"- Database: {'Connected' if status['database_connected'] else 'Disconnected'}\n"
                f"- Models: {'Loaded' if status['models_loaded'] else 'Not loaded'}"
            )
        
        return f"Unknown function: {function_name}"
    
    def query(self, user_input: str) -> Dict[str, Any]:
        """
        Query the agent
        
        Args:
            user_input: User's question
            
        Returns:
            Agent response
        """
        try:
            print("🤔 Agent reasoning...\n")
            
            # Start chat
            chat = self.agent.start_chat()
            
            # Send message
            response = chat.send_message(user_input)
            
            # Handle function calls
            max_iterations = 5
            iteration = 0
            
            while response.candidates[0].content.parts[0].function_call and iteration < max_iterations:
                iteration += 1
                function_call = response.candidates[0].content.parts[0].function_call
                function_name = function_call.name
                function_args = dict(function_call.args)
                
                # Execute the function
                function_response = self.execute_function_call(function_name, function_args)
                
                # Send function response back to model
                response = chat.send_message(
                    Part.from_function_response(
                        name=function_name,
                        response={"result": function_response}
                    )
                )
            
            # Get final text response
            final_answer = response.text
            
            # Store in conversation history
            self.conversation_history.append({
                "user": user_input,
                "agent": {"output": final_answer}
            })
            
            return {"output": final_answer}
            
        except Exception as e:
            return {
                "output": f"Error during agent reasoning: {str(e)}"
            }
    
    def run_cli(self):
        """Run interactive CLI"""
        print("=" * 80)
        print("Oracle Database RAG-Only Agent (Documentation Search)")
        print("=" * 80)
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
        print("Ask questions about Oracle Database features and documentation.")
        print("Commands: 'quit' to exit, 'history' to see conversation")
        print("-" * 80)
        print()
        
        while True:
            try:
                user_input = input("\n🧑 You: ").strip()
                
                if not user_input:
                    continue
                    
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("\nGoodbye!")
                    break
                
                if user_input.lower() == 'history':
                    print("\n📜 Conversation History:")
                    for i, entry in enumerate(self.conversation_history, 1):
                        print(f"\n[{i}] User: {entry['user'][:80]}")
                        print(f"    Agent: {entry['agent'].get('output', '')[:80]}...")
                    print()
                    continue
                
                # Query the agent
                response = self.query(user_input)
                
                # Display answer
                print(f"\n🤖 Agent: {response.get('output', 'No response generated')}\n")
                
            except KeyboardInterrupt:
                print("\n\nGoodbye!")
                break
            except Exception as e:
                print(f"\n❌ Error: {str(e)}\n")

def main():
    """Main entry point"""
    # Configuration
    api_url = os.getenv("ORACLE_RAG_API_URL", "http://localhost:8501")
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    
    print("Starting Oracle RAG-Only Agent...\n")
    
    # Create and run agent
    agent = OracleRAGOnlyAgent(api_url, project_id, location)
    agent.run_cli()

if __name__ == "__main__":
    main()
