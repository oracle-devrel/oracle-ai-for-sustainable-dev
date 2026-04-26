"""
Oracle MCP Wrapper - Filters complex schemas to work around ADK schema converter bug

The google-adk schema converter (v1.22.1) has a bug where it calls .items() on list objects.
This wrapper simplifies Oracle SQLcl MCP tool schemas before passing them to ADK.

Bug location: google/adk/tools/_gemini_schema_util.py line 160
Error: AttributeError: 'list' object has no attribute 'items'
"""
import copy
from typing import Any, Dict


def simplify_mcp_schema(schema: Dict[str, Any]) -> Dict[str, Any]:
    """
    Recursively simplify an MCP tool schema to avoid ADK schema converter bugs.
    
    Removes or simplifies problematic constructs:
    - Arrays with complex items -> convert to simple string description
    - Nested list properties -> flatten to object descriptions
    - anyOf/oneOf with lists -> simplify to most common type
    
    Args:
        schema: Original MCP tool inputSchema
        
    Returns:
        Simplified schema that ADK can handle
    """
    if not isinstance(schema, dict):
        return schema
    
    simplified = copy.deepcopy(schema)
    
    # Handle properties (most common case)
    if 'properties' in simplified and isinstance(simplified['properties'], dict):
        for prop_name, prop_value in list(simplified['properties'].items()):
            if not isinstance(prop_value, dict):
                # Skip non-dict properties
                continue
                
            prop_type = prop_value.get('type')
            
            # Simplify array types - the main culprit
            if prop_type == 'array':
                # Replace complex array with simple string description
                simplified['properties'][prop_name] = {
                    'type': 'string',
                    'description': prop_value.get('description', '') + ' (comma-separated values)'
                }
            
            # Recursively simplify nested objects
            elif prop_type == 'object':
                simplified['properties'][prop_name] = simplify_mcp_schema(prop_value)
            
            # Handle anyOf/oneOf - take the first non-null type
            elif 'anyOf' in prop_value or 'oneOf' in prop_value:
                variants = prop_value.get('anyOf') or prop_value.get('oneOf')
                if isinstance(variants, list) and variants:
                    # Find first non-null type
                    for variant in variants:
                        if isinstance(variant, dict) and variant.get('type') != 'null':
                            simplified['properties'][prop_name] = {
                                'type': variant.get('type', 'string'),
                                'description': prop_value.get('description', '')
                            }
                            break
    
    # Handle items (for array-type schemas)
    if 'items' in simplified:
        items = simplified['items']
        if isinstance(items, list):
            # Multiple items types - simplify to object
            simplified['items'] = {
                'type': 'object',
                'description': 'Item object'
            }
        elif isinstance(items, dict):
            simplified['items'] = simplify_mcp_schema(items)
    
    # Remove unsupported fields that might confuse ADK
    unsupported = ['examples', 'default', '$schema', '$ref', 'definitions']
    for field in unsupported:
        simplified.pop(field, None)
    
    return simplified


def filter_oracle_mcp_tools(tools_list: list) -> list:
    """
    Filter and simplify Oracle MCP tools list for ADK compatibility.
    
    Args:
        tools_list: List of MCP Tool objects from SQLcl server
        
    Returns:
        List with simplified schemas
    """
    filtered = []
    
    for tool in tools_list:
        # Copy tool object
        filtered_tool = copy.deepcopy(tool)
        
        # Simplify the inputSchema
        if hasattr(filtered_tool, 'inputSchema') and filtered_tool.inputSchema:
            filtered_tool.inputSchema = simplify_mcp_schema(filtered_tool.inputSchema)
        
        filtered.append(filtered_tool)
    
    return filtered


# Monkey-patch approach - intercept McpToolset schema processing
def patch_mcp_toolset():
    """
    Monkey-patch google-adk's McpToolset to filter schemas before processing.
    
    Call this before creating any McpToolset instances.
    """
    try:
        from google.adk.tools.mcp_tool.mcp_toolset import McpToolset
        
        # Save original from_server method
        original_from_server = McpToolset.from_server
        
        @classmethod
        async def patched_from_server(cls, connection_params, **kwargs):
            """Patched version that filters schemas"""
            # Call original to get tools
            tools, exit_stack = await original_from_server(connection_params, **kwargs)
            
            # Filter/simplify the tools (if they're in a list)
            if isinstance(tools, list):
                # Each tool in the list needs its schema simplified
                for tool in tools:
                    if hasattr(tool, '_tool') and hasattr(tool._tool, 'inputSchema'):
                        # Simplify the internal MCP tool schema
                        tool._tool.inputSchema = simplify_mcp_schema(tool._tool.inputSchema)
            
            return tools, exit_stack
        
        # Replace with patched version
        McpToolset.from_server = patched_from_server
        return True
        
    except Exception as e:
        print(f"Warning: Could not patch McpToolset: {e}")
        return False
