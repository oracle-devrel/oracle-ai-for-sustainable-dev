"""
Minimal Reproducer for ADK McpToolset Schema Converter Bug
Bug: AttributeError: 'list' object has no attribute 'items'
Location: google/adk/tools/_gemini_schema_util.py line 160

This bug occurs when ADK tries to convert Oracle SQLcl MCP tool schemas to Gemini format.
Oracle MCP tools have array-type properties in their schemas, which ADK's schema converter
cannot handle properly.

To reproduce:
1. Install: pip install google-adk mcp
2. Ensure Oracle SQLcl is installed and accessible
3. Run: python adk_mcp_bug_reproducer.py

Expected: Agent initializes successfully
Actual: AttributeError: 'list' object has no attribute 'items'
"""
import os
import asyncio
from google.adk.agents import LlmAgent
from google.adk.tools.mcp_tool.mcp_toolset import McpToolset, StdioServerParameters
from google.adk.tools.mcp_tool import StdioConnectionParams

async def reproduce_bug():
    """Minimal code to reproduce the ADK McpToolset schema bug"""
    
    print("=" * 80)
    print("ADK McpToolset Schema Converter Bug Reproducer")
    print("=" * 80)
    print()
    print("Environment:")
    print(f"  - Python: {os.sys.version}")
    print(f"  - Oracle SQLcl MCP Server: /opt/sqlcl/bin/sql -mcp")
    print()
    print("Bug Details:")
    print("  - File: google/adk/tools/_gemini_schema_util.py")
    print("  - Line: 160")
    print("  - Error: AttributeError: 'list' object has no attribute 'items'")
    print()
    print("Reproducing bug...")
    print("-" * 80)
    print()
    
    try:
        # Configure Oracle SQLcl MCP server
        # This is the standard way to use MCP with ADK according to official examples
        mcp_server_params = StdioServerParameters(
            command="/opt/sqlcl/bin/sql",
            args=["-mcp"],
            env={"TNS_ADMIN": os.path.expanduser("~/wallet")}
        )
        
        # Wrap in StdioConnectionParams
        mcp_connection_params = StdioConnectionParams(
            server_params=mcp_server_params,
            protocol_version="2024-11-05"  # Match Oracle SQLcl server version
        )
        
        print("Step 1: Creating LlmAgent with McpToolset...")
        print(f"  - MCP Server: {mcp_server_params.command} {' '.join(mcp_server_params.args)}")
        print(f"  - Protocol: {mcp_connection_params.protocol_version}")
        print()
        
        # This is where the bug occurs
        # ADK tries to convert Oracle MCP tool schemas to Gemini format
        # but fails because some Oracle MCP tools have array properties
        # in their schemas that ADK's converter can't handle
        agent = LlmAgent(
            model="gemini-2.0-flash-exp",
            name="test_agent",
            instruction="Test agent",
            tools=[McpToolset(connection_params=mcp_connection_params)]
        )
        
        print("✓ SUCCESS: Agent created without errors!")
        print()
        print("If you see this message, the bug has been fixed!")
        
    except AttributeError as e:
        print("❌ BUG REPRODUCED!")
        print()
        print(f"Error: {e}")
        print()
        print("Stack trace will show:")
        print("  File \".../google/adk/tools/_gemini_schema_util.py\", line 160")
        print("    for field_name, field_value in schema.items():")
        print("  AttributeError: 'list' object has no attribute 'items'")
        print()
        print("Root Cause:")
        print("  - Oracle SQLcl MCP tools have schemas with array-type properties")
        print("  - ADK's schema converter expects all properties to be dicts")
        print("  - When it encounters an array, it tries to call .items() and fails")
        print()
        print("Example Oracle MCP Tool Schema (run-sql):")
        print("  {")
        print("    'name': 'run-sql',")
        print("    'inputSchema': {")
        print("      'properties': {")
        print("        'sql': {'type': 'string'},")
        print("        'model': {'type': 'string'}")
        print("      },")
        print("      'required': ['sql']  # <-- This is an array, not a dict!")
        print("    }")
        print("  }")
        print()
        print("Suggested Fix:")
        print("  In _gemini_schema_util.py line 160, check if schema is a dict before")
        print("  calling .items():")
        print()
        print("  # Current (broken):")
        print("  for field_name, field_value in schema.items():")
        print()
        print("  # Fixed:")
        print("  if isinstance(schema, dict):")
        print("      for field_name, field_value in schema.items():")
        print("  elif isinstance(schema, list):")
        print("      # Handle array schemas appropriately")
        print("      pass")
        print()
        raise
    
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        print()
        print("This may indicate a different issue or environment problem.")
        raise

async def main():
    """Main entry point"""
    try:
        await reproduce_bug()
    except AttributeError:
        print()
        print("=" * 80)
        print("Bug successfully reproduced!")
        print("=" * 80)
        print()
        print("To Report:")
        print("  1. Repository: https://github.com/googleapis/python-genai")
        print("  2. Component: google-adk (Agent Development Kit)")
        print("  3. Title: 'McpToolset schema converter fails with array properties'")
        print("  4. Attach this reproducer script")
        print("  5. Mention: Occurs with Oracle SQLcl MCP server")
        print()
        print("Workaround:")
        print("  - Use google-genai SDK's MCP support directly (not ADK)")
        print("  - Or manually create FunctionDeclarations instead of McpToolset")
        return 1
    except Exception as e:
        print()
        print("=" * 80)
        print("Error during reproduction")
        print("=" * 80)
        print(f"Error: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit_code = asyncio.run(main())
    exit(exit_code)
