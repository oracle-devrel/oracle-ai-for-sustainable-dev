"""
Oracle AI Database Agent with GenerativeModel + Manual MCP Integration

Direct database access via Oracle SQLcl MCP protocol.
Uses GenerativeModel with manual MCP subprocess integration.
"""
import os
import json
import asyncio
from dotenv import load_dotenv
from google.cloud import aiplatform
import vertexai
from vertexai.generative_models import GenerativeModel, Tool, FunctionDeclaration, Part
from typing import Dict, Any, Optional

# Load environment variables from parent directory
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))

class MCPClient:
    """Manual MCP client using JSON-RPC over stdio"""
    
    def __init__(self, sqlcl_path: str, wallet_path: str):
        self.sqlcl_path = sqlcl_path
        self.wallet_path = wallet_path
        self.process = None
        self.message_id = 0
        self.initialized = False
        
    async def start(self):
        """Start the MCP server process"""
        try:
            print(f"  → Starting SQLcl MCP server: {self.sqlcl_path}")
            
            # Set up environment
            env = os.environ.copy()
            env['TNS_ADMIN'] = self.wallet_path
            
            # Start SQLcl in MCP mode
            self.process = await asyncio.create_subprocess_exec(
                self.sqlcl_path,
                "-mcp",
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                env=env
            )
            
            # Initialize MCP session
            await self._initialize()
            
            print("  ✓ MCP server started and initialized")
            
        except Exception as e:
            raise RuntimeError(f"Failed to start MCP server: {e}")
    
    async def _initialize(self):
        """Initialize MCP protocol"""
        try:
            # Send initialize request
            init_request = {
                "jsonrpc": "2.0",
                "id": self._next_id(),
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {
                        "name": "oracle-genai-mcp-client",
                        "version": "1.0.0"
                    }
                }
            }
            
            response = await self._send_request(init_request)
            
            if "error" in response:
                raise RuntimeError(f"Initialize failed: {response['error']}")
            
            # Send initialized notification
            initialized_notification = {
                "jsonrpc": "2.0",
                "method": "notifications/initialized"
            }
            
            await self._send_notification(initialized_notification)
            
            self.initialized = True
            
        except Exception as e:
            raise RuntimeError(f"MCP initialization failed: {e}")
    
    def _next_id(self) -> int:
        """Get next message ID"""
        self.message_id += 1
        return self.message_id
    
    async def _send_request(self, request: dict, timeout: float = 30.0) -> dict:
        """Send JSON-RPC request and wait for response"""
        if not self.process or not self.process.stdin:
            raise RuntimeError("MCP server not started")
        
        try:
            # Send request
            request_line = json.dumps(request) + "\n"
            self.process.stdin.write(request_line.encode())
            await self.process.stdin.drain()
            
            # Read response with timeout
            response_line = await asyncio.wait_for(
                self.process.stdout.readline(),
                timeout=timeout
            )
            
            if not response_line:
                raise RuntimeError("MCP server closed connection")
            
            response = json.loads(response_line.decode())
            return response
            
        except asyncio.TimeoutError:
            raise RuntimeError(f"MCP request timed out after {timeout}s")
        except Exception as e:
            raise RuntimeError(f"MCP request failed: {e}")
    
    async def _send_notification(self, notification: dict):
        """Send JSON-RPC notification (no response expected)"""
        if not self.process or not self.process.stdin:
            raise RuntimeError("MCP server not started")
        
        notification_line = json.dumps(notification) + "\n"
        self.process.stdin.write(notification_line.encode())
        await self.process.stdin.drain()
    
    async def list_tools(self) -> list:
        """List available MCP tools"""
        request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "tools/list"
        }
        
        response = await self._send_request(request)
        
        if "error" in response:
            raise RuntimeError(f"Failed to list tools: {response['error']}")
        
        return response.get("result", {}).get("tools", [])
    
    async def call_tool(self, tool_name: str, arguments: dict) -> Any:
        """Call an MCP tool"""
        request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            }
        }
        
        response = await self._send_request(request, timeout=60.0)
        
        if "error" in response:
            raise RuntimeError(f"Tool call failed: {response['error']}")
        
        result = response.get("result", {})
        
        # Extract content from MCP response
        if "content" in result:
            content_items = result["content"]
            if isinstance(content_items, list) and len(content_items) > 0:
                return content_items[0].get("text", "")
        
        return str(result)
    
    async def stop(self):
        """Stop the MCP server"""
        if self.process:
            self.process.terminate()
            await self.process.wait()
            self.process = None
            self.initialized = False


class OracleGenAIMCPAgent:
    """Agent using GenerativeModel with manual MCP integration for Oracle Database"""
    
    def __init__(self, project_id: str, location: str,
                 sqlcl_path: str = "/opt/sqlcl/bin/sql",
                 wallet_path: str = None):
        """
        Initialize the agent
        
        Args:
            project_id: GCP project ID
            location: GCP region
            sqlcl_path: Path to SQLcl executable
            wallet_path: Path to Oracle wallet directory
        """
        self.project_id = project_id
        self.location = location
        self.sqlcl_path = sqlcl_path
        self.wallet_path = wallet_path or os.path.expanduser("~/wallet")
        self.mcp_client = None
        self.agent = None
        self.conversation_history = []
        
        # Initialize Vertex AI
        vertexai.init(project=project_id, location=location)
        
    async def initialize(self):
        """Initialize the agent and MCP client"""
        print("🤖 Initializing GenAI + MCP Agent...\n")
        
        # Start MCP client
        self.mcp_client = MCPClient(self.sqlcl_path, self.wallet_path)
        await self.mcp_client.start()
        
        # Create agent with function declarations
        self.agent = await self.create_agent()
        
        print("✓ Agent ready!\n")
    
    async def create_agent(self):
        """Create Gemini agent with MCP function declarations"""
        
        # MCP functions - list available connections
        list_connections_function = FunctionDeclaration(
            name="mcp_list_connections",
            description="List all available Oracle database connections. Use this to see what database connections are configured and available.",
            parameters={
                "type": "object",
                "properties": {
                    "filter": {
                        "type": "string",
                        "description": "Optional filter to refine the list of connections",
                        "default": ""
                    }
                }
            }
        )
        
        # MCP function - connect to database
        connect_function = FunctionDeclaration(
            name="mcp_connect",
            description="Connect to an Oracle database using a saved connection name. The connection name is case sensitive. Use mcp_list_connections first to see available connections.",
            parameters={
                "type": "object",
                "properties": {
                    "connection_name": {
                        "type": "string",
                        "description": "The name of the saved connection to connect to"
                    }
                },
                "required": ["connection_name"]
            }
        )
        
        # MCP function - run SQL query
        run_sql_function = FunctionDeclaration(
            name="mcp_run_sql",
            description="Execute a SQL query on the connected Oracle database. Returns results in CSV format. Requires an active database connection (use mcp_connect first).",
            parameters={
                "type": "object",
                "properties": {
                    "sql": {
                        "type": "string",
                        "description": "The SQL query to execute"
                    }
                },
                "required": ["sql"]
            }
        )
        
        # MCP function - get schema information
        schema_info_function = FunctionDeclaration(
            name="mcp_schema_information",
            description="Get detailed information about the currently connected database schema, including tables, columns, indexes, and relationships. Requires an active database connection.",
            parameters={
                "type": "object",
                "properties": {}
            }
        )
        
        # MCP function - disconnect
        disconnect_function = FunctionDeclaration(
            name="mcp_disconnect",
            description="Disconnect from the current Oracle database session.",
            parameters={
                "type": "object",
                "properties": {}
            }
        )
        
        # Create tool with all function declarations
        oracle_tool = Tool(
            function_declarations=[
                list_connections_function,
                connect_function,
                run_sql_function,
                schema_info_function,
                disconnect_function
            ]
        )
        
        # System instructions
        instructions = """You are an expert Oracle Database AI assistant with direct database access via MCP.

**Available Database Tools (via MCP):**
- List available database connections with mcp_list_connections
- Connect to databases with mcp_connect
- Run SQL queries with mcp_run_sql
- Get schema information with mcp_schema_information
- Disconnect with mcp_disconnect

**AUTONOMOUS Multi-Step Workflow:**
When a user asks for database information, be proactive and autonomous:
1. First, automatically call mcp_list_connections to see available connections
2. If connections are available, automatically pick the first/most relevant one and call mcp_connect
3. Once connected, automatically call the appropriate tool (mcp_run_sql or mcp_schema_information)
4. Present the results to the user in a clear, formatted way

**DO NOT ask the user for connection names or approval - be autonomous:**
- "Show me tables" → List connections, connect to first one, run "SELECT table_name FROM user_tables"
- "What's in my database" → List connections, connect, get schema information
- "Query my data" → List connections, connect, run the appropriate SQL

**Connection Strategy:**
- When multiple connections exist, prefer connections with "prod", "main", or similar names
- If connection fails, try the next available connection
- Only ask the user for input if NO connections are available

Be helpful, technically accurate, and AUTONOMOUS - don't make users specify connection details."""
        
        # Create Gemini model with tools
        model = GenerativeModel(
            "gemini-2.5-flash",
            tools=[oracle_tool],
            system_instruction=instructions
        )
        
        return model
    
    async def execute_function_call(self, function_name: str, args: dict) -> str:
        """Execute a function call from the agent"""
        print(f"  🔧 Tool called: {function_name}({args})")
        
        try:
            # MCP functions
            if function_name == "mcp_list_connections":
                filter_str = args.get("filter", "")
                mcp_args = {}
                if filter_str:
                    mcp_args["filter"] = filter_str
                result = await self.mcp_client.call_tool("list-connections", mcp_args)
                return f"Available Connections:\n{result}"
            
            elif function_name == "mcp_connect":
                connection_name = args.get("connection_name", "")
                if not connection_name:
                    return "Error: connection_name is required"
                result = await self.mcp_client.call_tool("connect", {"connection_name": connection_name, "model": "gemini-2.5-flash"})
                return f"Connection Result:\n{result}"
            
            elif function_name == "mcp_run_sql":
                sql = args.get("sql", "")
                if not sql:
                    return "Error: sql query is required"
                result = await self.mcp_client.call_tool("run-sql", {"sql": sql, "model": "gemini-2.5-flash"})
                return f"Query Results (CSV):\n{result}"
            
            elif function_name == "mcp_schema_information":
                result = await self.mcp_client.call_tool("schema-information", {"model": "gemini-2.5-flash"})
                return f"Schema Information:\n{result}"
            
            elif function_name == "mcp_disconnect":
                result = await self.mcp_client.call_tool("disconnect", {"model": "gemini-2.5-flash"})
                return f"Disconnect Result:\n{result}"
            
            else:
                return f"Unknown function: {function_name}"
                
        except Exception as e:
            return f"Error executing {function_name}: {str(e)}"
    
    async def query(self, user_input: str) -> Dict[str, Any]:
        """
        Query the agent with full reasoning capabilities
        
        Args:
            user_input: User's question or request
            
        Returns:
            Agent response
        """
        try:
            print("🤔 Agent reasoning...\n")
            
            # Start chat
            chat = self.agent.start_chat()
            
            # Send message
            response = chat.send_message(user_input)
            
            # Handle function calls (scan all parts for function_call,
            # since Gemini 2.5 may return text + function_call in one response)
            max_iterations = 10  # More iterations for multi-step MCP workflows
            iteration = 0

            def _find_function_call(resp):
                """Find the first function_call across all parts."""
                for part in resp.candidates[0].content.parts:
                    if part.function_call and part.function_call.name:
                        return part.function_call
                return None

            fc = _find_function_call(response)
            while fc and iteration < max_iterations:
                iteration += 1
                function_name = fc.name
                function_args = dict(fc.args)

                # Execute the function (async for MCP calls)
                function_response = await self.execute_function_call(function_name, function_args)

                # Send function response back to model
                response = chat.send_message(
                    Part.from_function_response(
                        name=function_name,
                        response={"result": function_response}
                    )
                )
                fc = _find_function_call(response)

            # Extract text from potentially multi-part response
            # (Gemini 2.5 may return text + function_call in one response)
            text_parts = []
            for part in response.candidates[0].content.parts:
                if part.function_call:
                    continue
                try:
                    if part.text:
                        text_parts.append(part.text)
                except (AttributeError, ValueError):
                    continue
            final_answer = "\n".join(text_parts) if text_parts else "No response generated"
            
            # Store in conversation history
            self.conversation_history.append({
                "user": user_input,
                "agent": {"output": final_answer}
            })
            
            return {"output": final_answer}
            
        except Exception as e:
            return {"output": f"Error during agent reasoning: {str(e)}"}
    
    async def run_cli(self):
        """Run interactive CLI"""
        print("=" * 80)
        print("Oracle Database GenAI + MCP Agent (Direct Database Access)")
        print("=" * 80)
        print(f"Project: {self.project_id}")
        print(f"SQLcl: {self.sqlcl_path}")
        print()
        print("Query your Oracle databases directly via MCP.")
        print("Commands: 'quit' to exit, 'history' to see conversation")
        print("-" * 80)
        print()
        
        while True:
            try:
                user_input = input("\n🧑 You: ").strip()
                
                if not user_input:
                    continue
                
                if user_input.lower() in ['quit', 'exit', 'q']:
                    print("\nShutting down...")
                    await self.mcp_client.stop()
                    print("Goodbye!")
                    break
                
                if user_input.lower() == 'history':
                    print("\n📜 Conversation History:")
                    for i, entry in enumerate(self.conversation_history, 1):
                        print(f"\n[{i}] User: {entry['user'][:80]}")
                        print(f"    Agent: {entry['agent'].get('output', '')[:80]}...")
                    continue
                
                # Query the agent
                response = await self.query(user_input)
                
                # Display answer
                print(f"\n🤖 Agent: {response.get('output', 'No response generated')}\n")
                
            except KeyboardInterrupt:
                print("\n\nShutting down...")
                await self.mcp_client.stop()
                print("Goodbye!")
                break
            except Exception as e:
                print(f"\n❌ Error: {str(e)}\n")

async def main():
    """Main entry point"""
    # Configuration
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    sqlcl_path = os.getenv("SQLCL_PATH", "/opt/sqlcl/bin/sql")
    wallet_path = os.getenv("TNS_ADMIN", os.path.expanduser("~/wallet"))
    
    print("Starting GenAI + MCP Agent...\n")
    
    # Create agent
    agent = OracleGenAIMCPAgent(project_id, location, sqlcl_path, wallet_path)
    
    # Initialize (starts MCP)
    await agent.initialize()
    
    # Run CLI
    await agent.run_cli()

if __name__ == "__main__":
    asyncio.run(main())
