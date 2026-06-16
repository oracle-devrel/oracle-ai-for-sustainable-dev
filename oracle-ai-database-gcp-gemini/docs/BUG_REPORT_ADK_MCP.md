# ADK McpToolset Schema Converter Bug Report

## Status Update (2026-02-23)
- This issue was fixed upstream in `google/adk-python` after it was reported.
- Original reproduction was against `google-adk==1.22.1`.
- Local project baseline has been moved to `google-adk>=1.25.1` for MCP retesting.
- Keep this document as historical context and for root-cause details.

## Bug Summary
**Component:** google-adk (Agent Development Kit)  
**Version:** 1.22.1  
**File:** `google/adk/tools/_gemini_schema_util.py`  
**Line:** 160  
**Error:** `AttributeError: 'list' object has no attribute 'items'`

## Description
When using `McpToolset` with Oracle SQLcl MCP server, ADK's schema converter fails because it cannot handle array-type properties in MCP tool schemas. The converter assumes all schema values are dictionaries and calls `.items()` on them, but Oracle MCP tools have arrays (like `required: ['sql']`) which causes the crash.

## Reproduction
Run the included `adk_mcp_bug_reproducer.py` script with:
- Python 3.12+
- `google-adk>=1.22.1`
- `mcp>=0.9.0`
- Oracle SQLcl with MCP support (`/opt/sqlcl/bin/sql -mcp`)

```bash
python adk_mcp_bug_reproducer.py
```

## Stack Trace
```
File ".../google/adk/tools/_gemini_schema_util.py", line 206, in _to_gemini_schema
  sanitized_schema = _sanitize_schema_formats_for_gemini(dereferenced_schema)
File ".../google/adk/tools/_gemini_schema_util.py", line 176, in _sanitize_schema_formats_for_gemini
  key: _sanitize_schema_formats_for_gemini(value)
File ".../google/adk/tools/_gemini_schema_util.py", line 160, in _sanitize_schema_formats_for_gemini
  for field_name, field_value in schema.items():
AttributeError: 'list' object has no attribute 'items'
```

## Root Cause
Oracle SQLcl MCP tool schemas include array properties:

```json
{
  "name": "run-sql",
  "inputSchema": {
    "properties": {
      "sql": {"type": "string"},
      "model": {"type": "string"}
    },
    "required": ["sql"]  ← This is an array, not a dict!
  }
}
```

The schema converter in `_gemini_schema_util.py` line 160 does:
```python
for field_name, field_value in schema.items():
```

This fails when `schema` is a list (like the `required` array).

## Suggested Fix
In `_gemini_schema_util.py`, line 160, add type checking:

```python
# Current (broken):
for field_name, field_value in schema.items():
    # ...

# Fixed:
if isinstance(schema, dict):
    for field_name, field_value in schema.items():
        # ... existing logic
elif isinstance(schema, list):
    # Handle array schemas - likely just pass through
    return schema
else:
    return schema
```

## Impact
- Prevents use of ADK with Oracle SQLcl MCP server
- Likely affects other MCP servers with array-type schema properties
- Forces users to implement manual MCP integration instead of using ADK's built-in `McpToolset`

## Workaround
1. Use `google-genai` SDK's MCP support directly (not ADK)
2. Manually create `FunctionDeclaration` objects instead of using `McpToolset`
3. Create schema wrapper to simplify complex MCP schemas before ADK processes them (attempted but failed due to internal API limitations)

## Environment
- Python: 3.12.12
- google-adk: 1.22.1
- mcp: 0.9.0
- Oracle SQLcl: Latest (with MCP support)
- OS: Linux (GCP Compute Engine)

## Where to Report
**Repository:** https://github.com/google/adk-python  
**File:** https://github.com/google/adk-python/blob/main/src/google/adk/tools/_gemini_schema_util.py  
**Issue Title:** "McpToolset schema converter fails with array properties in Oracle SQLcl MCP"

Attach:
1. This bug report (`BUG_REPORT_ADK_MCP.md`)
2. Reproducer script (`adk_mcp_bug_reproducer.py`)
3. Stack trace from reproduction

## Additional Context
This bug was discovered while building a tutorial that integrates Oracle Database with Google ADK using the Model Context Protocol (MCP). The official ADK MCP examples work with simpler MCP servers but fail with Oracle SQLcl's more complex schemas.

Related: Google's own `google-genai` SDK handles MCP correctly (it has working tests for array handling), but ADK's implementation is broken.
