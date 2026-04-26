# ADK McpToolset Schema Converter Bug: oneOf/allOf Arrays

## Bug Summary
**Component:** google-adk (Agent Development Kit)
**Version:** 1.25.1 (latest as of 2026-02-24)
**File:** `google/adk/tools/_gemini_schema_util.py`
**Function:** `_sanitize_schema_formats_for_gemini`
**Error:** `pydantic_core._pydantic_core.ValidationError: Input should be a valid dictionary or object to extract fields from [...] input_type=list`

## Description

When using `McpToolset` with Oracle SQLcl MCP server, ADK's schema converter fails because it does not handle `oneOf` (or `allOf`) JSON Schema keywords as list-type fields. The converter correctly handles `anyOf` (it is in `list_schema_field_names`), but `oneOf` and `allOf` are omitted. They fall through to the generic `supported_fields` branch, which passes the raw Python list directly into `_ExtendedJSONSchema.model_validate()`. Pydantic rejects the list because it expects a dict.

### Previous Bug (Fixed in 1.25.1)

The original bug (`AttributeError: 'list' object has no attribute 'items'` at line 160) was fixed — `_sanitize_schema_formats_for_gemini` now handles `isinstance(schema, list)` at the top of the function. However, the `oneOf`/`allOf` issue is a separate code path.

## Root Cause

In `_sanitize_schema_formats_for_gemini`, line ~136:

```python
list_schema_field_names: set[str] = {"any_of"}
```

This only includes `any_of`. When a schema contains `oneOf` (converted to `one_of` by `_to_snake_case`), it is NOT matched by `list_schema_field_names`. It falls through to:

```python
elif field_name in supported_fields and field_value is not None:
    snake_case_schema[field_name] = field_value  # raw list passed through!
```

This raw list is then passed to `_ExtendedJSONSchema.model_validate(sanitized_schema)` which fails.

## Reproduction

### Environment
- Python: 3.12+
- google-adk: 1.25.1
- mcp: 0.9.0+
- Oracle SQLcl: Latest (with MCP support, `-mcp` flag)

### Steps
```bash
pip install google-adk>=1.25.1 mcp
python oracle_ai_database_adk_sqlcl_mcp_agent.py
# Ask any question — the error occurs when ADK processes SQLcl MCP tool schemas
```

### Oracle SQLcl MCP Tool Schema (Triggering)

SQLcl exposes tools with `oneOf` in their `inputSchema`. Example structure:

```json
{
  "name": "some-tool",
  "inputSchema": {
    "properties": {
      "oneOf": [
        {
          "description": "Execute...",
          "required": ["job_id", "command"]
        }
      ]
    }
  }
}
```

## Stack Trace
```
File ".../google/adk/tools/mcp_tool/mcp_tool.py", line 201, in _get_declaration
    parameters = _to_gemini_schema(input_schema)
File ".../google/adk/tools/_gemini_schema_util.py", line 218, in _to_gemini_schema
    json_schema=_ExtendedJSONSchema.model_validate(sanitized_schema),
pydantic_core._pydantic_core.ValidationError: 1 validation error for _ExtendedJSONSchema
properties.oneOf
  Input should be a valid dictionary or object to extract fields from
  [type=model_attributes_type, input_value=[{'description': 'Execute... ['job_id', 'command']}], input_type=list]
```

## Suggested Fix

Two issues need to be addressed:

### Issue 1: Add `one_of` and `all_of` to `list_schema_field_names`

```python
# Current (broken):
list_schema_field_names: set[str] = {"any_of"}

# Fixed:
list_schema_field_names: set[str] = {"any_of", "one_of", "all_of"}
```

This handles `oneOf`/`allOf` at the top level of a schema. The `should_preserve`
variable already references `one_of`, confirming it was intended to be handled.

### Issue 2: Handle `oneOf`/`allOf`/`anyOf` appearing inside `properties`

Some MCP servers (Oracle SQLcl) emit schemas where `oneOf` appears as a key
inside the `properties` dict rather than at the schema level. For example:

```json
{
  "properties": {
    "sql": {"type": "string"},
    "oneOf": [{"required": ["job_id", "command"]}]
  }
}
```

The `properties` handler iterates values and recurses, but since `oneOf`'s value
is a list (not a dict), `_ExtendedJSONSchema` rejects it. The fix: when
processing `properties`, detect JSON Schema keywords that are lists and either
hoist them to the parent schema level or skip them, and skip any remaining
non-dict property values.

## Workaround

Monkey-patch `_sanitize_schema_formats_for_gemini` at import time. See `oracle_ai_database_adk_sqlcl_mcp_agent.py` for a working implementation.

## Impact
- Prevents use of ADK `McpToolset` with any MCP server whose tool schemas contain `oneOf` or `allOf`
- Oracle SQLcl MCP server is affected
- Any MCP server with complex JSON Schema tool definitions may be affected

## Related
- Previous bug report: `BUG_REPORT_ADK_MCP.md` (array `.items()` bug, fixed in 1.25.1)
- GitHub: https://github.com/google/adk-python
- File: https://github.com/google/adk-python/blob/main/src/google/adk/tools/_gemini_schema_util.py

## Where to Report
**Repository:** https://github.com/google/adk-python
**Issue Title:** `McpToolset schema converter fails with oneOf/allOf arrays in MCP tool schemas`
**Labels:** bug, mcp
