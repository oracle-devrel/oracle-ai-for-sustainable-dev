"""
Oracle AI Database ADK RAG Agent - Simplified Version
Follows the pattern from: https://github.com/arjunprabhulal/adk-vertex-ai-rag-engine

This version uses:
- Plain Python functions (not BaseTool)
- FunctionTool wrapper
- Clean separation of tools and agent
- No manual vertexai.init() in agent code
"""
import os
import requests
from dotenv import load_dotenv
from google.adk.agents import Agent
from google.adk.tools import FunctionTool
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part
from typing import Dict, Any

# Load environment variables
load_dotenv()


# =============================================================================
# RAG TOOL - Plain Python function following ADK best practices
# =============================================================================

def query_oracle_database(query: str, top_k: int = 5) -> Dict[str, Any]:
    """
    Search the Oracle Database knowledge base for information about Oracle Database 
    features, spatial capabilities, vector search, JSON features, SQL enhancements, 
    and other database topics.
    
    Args:
        query: The question to ask about Oracle Database
        top_k: Number of similar chunks to retrieve (default: 5)
        
    Returns:
        A dictionary containing the search results with answer and sources
    """
    api_url = os.getenv('RAG_API_URL', 'http://localhost:8501')
    
    try:
        response = requests.post(
            f"{api_url}/query",
            json={"query": query, "top_k": top_k},
            timeout=30
        )
        response.raise_for_status()
        result = response.json()
        
        # Format the response nicely
        answer = result.get('answer', 'No answer found')
        sources = result.get('sources', [])
        
        formatted_response = f"📚 Answer:\n{answer}\n"
        
        if sources:
            formatted_response += f"\n🔗 Sources:\n"
            for i, source in enumerate(sources, 1):
                formatted_response += f"{i}. {source}\n"
        
        return {
            "status": "success",
            "answer": answer,
            "sources": sources,
            "formatted_response": formatted_response
        }
        
    except requests.exceptions.RequestException as e:
        return {
            "status": "error",
            "error_message": str(e),
            "formatted_response": f"❌ Error querying database: {str(e)}"
        }


# =============================================================================
# CREATE FUNCTION TOOL - ADK wrapper
# =============================================================================

query_oracle_tool = FunctionTool(query_oracle_database)


# =============================================================================
# CREATE THE AGENT
# =============================================================================

agent = Agent(
    name="rag",
    model="gemini-2.0-flash-exp",
    description="Agent for searching Oracle Database documentation and knowledge base",
    instruction="""
    You are a helpful assistant that answers questions about Oracle Database using 
    a comprehensive knowledge base.
    
    Your primary goal is to help users find information about:
    - Oracle Database features (Vector Search, Spatial, JSON, etc.)
    - SQL enhancements and new capabilities
    - SELECT AI and other AI features
    - Database configuration and best practices
    
    When the user asks a question:
    1. Use the query_oracle_database tool to search the knowledge base
    2. Present the information clearly with proper formatting
    3. Include source citations when available
    
    Use emojis to make responses friendly:
    - 📚 for answers
    - 🔗 for sources
    - ✅ for success
    - ❌ for errors
    - ℹ️ for additional information
    """,
    tools=[query_oracle_tool]
)


# =============================================================================
# MAIN EXECUTION
# =============================================================================

def main():
    """Main execution function"""
    print("\n" + "="*70)
    print("Oracle Database RAG Agent (ADK Simplified)")
    print("="*70)
    print("\nType your questions about Oracle Database. Type 'exit' to quit.\n")
    
    # Create session service and runner
    session_service = InMemorySessionService()
    runner = Runner(
        app_name="oracle_rag",
        agent=agent,
        session_service=session_service
    )
    
    # Session will be auto-created on first run
    user_id = "oracle_user"
    session_id = "default"
    
    while True:
        try:
            # Get user input
            user_input = input("\nYou: ").strip()
            
            if not user_input:
                continue
                
            if user_input.lower() in ['exit', 'quit', 'q']:
                print("\nGoodbye! 👋")
                break
            
            print("\nAgent: ", end="", flush=True)
            
            # Create proper Content message
            message = Content(
                role="user",
                parts=[Part(text=user_input)]
            )
            
            # Run the agent
            for event in runner.run(
                session_id=session_id,
                user_id=user_id,
                new_message=message
            ):
                if hasattr(event, 'content') and event.content:
                    if hasattr(event.content, 'parts'):
                        for part in event.content.parts:
                            if hasattr(part, 'text') and part.text:
                                print(part.text, end="", flush=True)
            
            print()  # New line after response
            
        except KeyboardInterrupt:
            print("\n\nGoodbye! 👋")
            break
        except Exception as e:
            print(f"\n❌ Error: {str(e)}")
            import traceback
            traceback.print_exc()


if __name__ == "__main__":
    main()
