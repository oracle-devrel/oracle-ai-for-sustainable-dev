"""Real database session that uses the actual database connections from config."""

import asyncio
import logging
import os
import oracledb
from typing import Any, Dict, List, Optional
from dataclasses import dataclass

logger = logging.getLogger(__name__)

@dataclass
class RealTool:
    """Real tool definition."""
    name: str
    description: str
    inputSchema: Dict[str, Any]

@dataclass
class RealContent:
    """Real content wrapper."""
    text: str
    
    def __str__(self):
        return self.text

@dataclass
class RealToolCallResult:
    """Real tool call result."""
    content: List[RealContent]
    isError: bool = False

class RealDatabaseSession:
    """Real database session that uses the actual database connections from config."""
    
    def __init__(self, config_manager):
        """Initialize real database session."""
        self.config_manager = config_manager
        self.connection = None
        self.current_connection_name = None
        
        # Define the available tools
        self.tools = [
            RealTool(
                name="mcp_oracle-sqlcl-mcp_list-connections",
                description="List all available oracle named/saved connections in the connections storage",
                inputSchema={"type": "object", "properties": {}}
            ),
            RealTool(
                name="mcp_oracle-sqlcl-mcp_connect",
                description="Provides an interface to connect to a specified database",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "connection_name": {"type": "string", "description": "The name of the saved connection you want to connect to"}
                    },
                    "required": ["connection_name"]
                }
            ),
            RealTool(
                name="mcp_oracle-sqlcl-mcp_disconnect",
                description="This tool performs a disconnection from the current session in an Oracle database",
                inputSchema={"type": "object", "properties": {}}
            ),
            RealTool(
                name="mcp_oracle-sqlcl-mcp_run-sqlcl",
                description="This tool executes SQLcl commands in the SQLcl CLI",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "sqlcl": {"type": "string", "description": "The SQLcl command to execute"}
                    },
                    "required": ["sqlcl"]
                }
            ),
            RealTool(
                name="mcp_oracle-sqlcl-mcp_run-sql",
                description="This tool executes SQL queries in an Oracle database",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "sql": {"type": "string", "description": "The SQL query to execute"}
                    },
                    "required": ["sql"]
                }
            )
        ]
    
    async def list_tools(self):
        """List available tools."""
        class RealToolsResponse:
            def __init__(self, tools):
                self.tools = tools
        
        return RealToolsResponse(self.tools)
    
    async def call_tool(self, tool_name: str, arguments: Dict[str, Any]):
        """Call a tool using the real database connection."""
        try:
            logger.info(f"Calling real database tool: {tool_name} with arguments: {arguments}")
            
            if tool_name == "mcp_oracle-sqlcl-mcp_list-connections":
                # List available connections from config
                databases = self.config_manager.get_databases()
                result = "Available connections:\n" + "\n".join(databases.keys())
            
            elif tool_name == "mcp_oracle-sqlcl-mcp_connect":
                connection_name = arguments.get("connection_name")
                if connection_name:
                    result = await self._connect_to_database(connection_name)
                else:
                    result = "Error: connection_name required"
            
            elif tool_name == "mcp_oracle-sqlcl-mcp_disconnect":
                result = await self._disconnect_from_database()
            
            elif tool_name == "mcp_oracle-sqlcl-mcp_run-sql":
                sql = arguments.get("sql")
                if sql:
                    result = await self._execute_sql(sql)
                else:
                    result = "Error: sql required"
            
            elif tool_name == "mcp_oracle-sqlcl-mcp_run-sqlcl":
                sqlcl = arguments.get("sqlcl")
                if sqlcl:
                    result = f"SQLcl command: {sqlcl}\n(Would execute against {self.current_connection_name or 'no database'})"
                else:
                    result = "Error: sqlcl required"
            
            else:
                result = f"Tool {tool_name} not implemented"
            
            # Create result object
            return RealToolCallResult(content=[RealContent(str(result))])
                
        except Exception as e:
            logger.error(f"Error calling tool {tool_name}: {e}")
            return RealToolCallResult(content=[RealContent(f"Error: {e}")], isError=True)
    
    async def _connect_to_database(self, connection_name: str):
        """Connect to a database using the connection string from config."""
        try:
            logger.info(f"Starting connection to: {connection_name}")
            databases = self.config_manager.get_databases()
            
            # Case-insensitive connection lookup
            connection_string = None
            actual_connection_name = None
            
            # First try exact match
            if connection_name in databases:
                connection_string = databases[connection_name]
                actual_connection_name = connection_name
            else:
                # Try case-insensitive match
                for db_name, conn_str in databases.items():
                    if db_name.lower() == connection_name.lower():
                        connection_string = conn_str
                        actual_connection_name = db_name
                        logger.info(f"Found case-insensitive match: '{connection_name}' -> '{db_name}'")
                        break
            
            if connection_string is None:
                available_connections = list(databases.keys())
                return f"Error: Connection '{connection_name}' not found in config. Available connections: {available_connections}"
            
            logger.info(f"Connecting to {actual_connection_name}")
            
            # Parse connection string components
            username, password, dsn, wallet_dir = self._parse_connection_string(connection_string, actual_connection_name)
            
            # Connect to the database
            if wallet_dir:
                # Wallet-based connection (Oracle Cloud, etc.)
                self.connection = oracledb.connect(
                    config_dir=wallet_dir,
                    user=username,
                    password=password,
                    dsn=dsn,
                    wallet_location=wallet_dir,
                    wallet_password=password
                )
            else:
                # Direct connection (no wallet)
                self.connection = oracledb.connect(
                    user=username,
                    password=password,
                    dsn=dsn
                )
            self.current_connection_name = actual_connection_name
            
            # Test the connection
            cursor = self.connection.cursor()
            cursor.execute("SELECT 1 FROM DUAL")
            cursor.fetchone()
            cursor.close()
            
            return f"Successfully connected to database: {actual_connection_name}"
            
        except Exception as e:
            logger.error(f"Error connecting to database {connection_name}: {e}")
            return f"Error connecting to database {connection_name}: {e}"
    
    async def _disconnect_from_database(self):
        """Disconnect from the current database."""
        try:
            if self.connection:
                self.connection.close()
                self.connection = None
                self.current_connection_name = None
                return "Successfully disconnected from database"
            else:
                return "No active database connection"
        except Exception as e:
            logger.error(f"Error disconnecting from database: {e}")
            return f"Error disconnecting from database: {e}"
    
    async def _execute_sql(self, sql: str):
        """Execute SQL query against the connected database."""
        try:
            if not self.connection:
                return "Error: No database connection. Please connect to a database first."
            
            cursor = self.connection.cursor()
            cursor.execute(sql)
            
            # Fetch results
            if cursor.description:
                # Query with results
                columns = [desc[0] for desc in cursor.description]
                rows = cursor.fetchall()
                
                # Format results
                result = " | ".join(columns) + "\n"
                result += "-" * len(result) + "\n"
                
                for row in rows:
                    result += " | ".join(str(col) for col in row) + "\n"
                
                result += f"\n({len(rows)} rows selected)"
            else:
                # DML statement
                result = f"SQL executed successfully. {cursor.rowcount} rows affected."
            
            cursor.close()
            return result
            
        except Exception as e:
            logger.error(f"Error executing SQL: {e}")
            return f"Error executing SQL: {e}"
    
    def _parse_connection_string(self, connection_string: str, connection_name: str) -> tuple:
        """Parse connection string and extract components.
        
        Returns:
            tuple: (username, password, dsn, wallet_dir)
        """

        
        # Extract credentials and DSN
        username = ""
        password = ""
        dsn = ""
        wallet_dir = None
        
        if '@' in connection_string:
            credentials, rest = connection_string.split('@', 1)
            if '/' in credentials:
                username, password = credentials.split('/', 1)
            else:
                username = credentials
                password = ""
            
            # Handle TNS_ADMIN if present
            if '?' in rest:
                dsn, tns_admin = rest.split('?', 1)
                if 'TNS_ADMIN=' in tns_admin:
                    wallet_dir = tns_admin.split('TNS_ADMIN=')[1]
                    # Set TNS_ADMIN environment variable (crucial for Oracle)
                    os.environ['TNS_ADMIN'] = wallet_dir
            else:
                dsn = rest
        else:
            dsn = connection_string
        
        return username, password, dsn, wallet_dir
    
    async def disconnect(self):
        """Disconnect from the database."""
        if self.connection:
            self.connection.close()
            self.connection = None
            self.current_connection_name = None
    
    async def __aenter__(self):
        """Async context manager entry."""
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit."""
        await self.disconnect() 