"""
Oracle AI Database Agent - ADK + SQLcl MCP (McpToolset)

Uses Google Agent Development Kit (ADK) with native McpToolset integration
to connect to Oracle Database via SQLcl's MCP server.

This version:
- Uses ADK's built-in McpToolset (no manual JSON-RPC plumbing)
- Connects to Oracle SQLcl MCP server via stdio
- Requires SQLcl with MCP support and Java runtime
- Requires google-adk>=1.25.1
- Includes monkey-patch for ADK schema converter bug with oneOf arrays
  (see docs/BUG_REPORT_ADK_MCP_ONEOF.md)
"""
import os
import re
import asyncio
from typing import Any, Optional
from contextlib import AsyncExitStack
from dotenv import load_dotenv
import vertexai
from google.adk.agents import LlmAgent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.adk.artifacts.in_memory_artifact_service import InMemoryArtifactService
from google.adk.tools.mcp_tool.mcp_toolset import McpToolset, StdioServerParameters
from google.adk.tools.mcp_tool import StdioConnectionParams
from google.genai import types
from google.genai.types import GenerateContentConfig

# Load environment variables from parent directory
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))


# ---------------------------------------------------------------------------
# Monkey-patch: ADK _gemini_schema_util.py oneOf / allOf handling
#
# ADK 1.25.1 _sanitize_schema_formats_for_gemini only lists "any_of" in
# list_schema_field_names. "one_of" and "all_of" (snake_case of JSON Schema
# oneOf / allOf) are arrays of schemas but are NOT routed through the list
# handler. They fall through to the generic "supported_fields" branch, which
# passes the raw list straight to _ExtendedJSONSchema.model_validate() and
# Pydantic rejects it:
#   ValidationError: properties.oneOf - Input should be a valid dictionary
#
# Fix: add "one_of" and "all_of" to list_schema_field_names so they get the
# same recursive-list treatment as "any_of".
# ---------------------------------------------------------------------------
import google.adk.tools._gemini_schema_util as _schema_mod

_original_sanitize = _schema_mod._sanitize_schema_formats_for_gemini


def _merge_schema_variants(variants: list) -> dict:
    """Merge oneOf/anyOf/allOf schema variants into a single flat schema.

    Combines all properties from all variants into one object schema.
    Only marks a field as required if it appears in ALL variants.
    This avoids using any_of which Gemini doesn't support alongside other fields.
    """
    if not variants or not isinstance(variants, list):
        return {}

    merged_props = {}
    merged_description = []
    all_required_sets = []

    for variant in variants:
        if not isinstance(variant, dict):
            continue
        props = variant.get("properties", {})
        for prop_name, prop_schema in props.items():
            if prop_name not in merged_props:
                merged_props[prop_name] = prop_schema
        req = variant.get("required", [])
        if req:
            all_required_sets.append(set(req))
        desc = variant.get("description", "")
        if desc:
            merged_description.append(desc)

    if not merged_props:
        return {}

    # Only require fields present in ALL variants
    if all_required_sets:
        common_required = list(set.intersection(*all_required_sets))
    else:
        common_required = []

    result = {"type": "object", "properties": merged_props}
    if common_required:
        result["required"] = common_required
    if merged_description:
        result["description"] = " | ".join(merged_description)
    return result


def _patched_sanitize_schema_formats_for_gemini(
    schema: Any, preserve_null_type: bool = False
) -> Any:
    """Patched version that handles oneOf/allOf as list schema fields."""
    if isinstance(schema, list):
        return [
            _patched_sanitize_schema_formats_for_gemini(
                item, preserve_null_type=preserve_null_type
            )
            for item in schema
        ]
    if not isinstance(schema, dict):
        return schema

    from google.adk.tools._gemini_schema_util import (
        _ExtendedJSONSchema,
        _sanitize_schema_type,
        _to_snake_case,
    )

    supported_fields: set[str] = set(_ExtendedJSONSchema.model_fields.keys())
    supported_fields.discard("additional_properties")
    schema_field_names: set[str] = {"items"}
    # FIX: include one_of and all_of alongside any_of
    # _ExtendedJSONSchema only has "any_of", so we remap one_of/all_of → any_of
    list_schema_field_names: set[str] = {"any_of", "one_of", "all_of"}
    dict_schema_field_names: tuple[str, ...] = ("properties", "defs")
    snake_case_schema: dict[str, Any] = {}

    for field_name, field_value in schema.items():
        field_name = _to_snake_case(field_name)
        if field_name in schema_field_names:
            snake_case_schema[field_name] = (
                _patched_sanitize_schema_formats_for_gemini(field_value)
            )
        elif field_name in list_schema_field_names:
            # Gemini doesn't support any_of alongside other fields, so
            # flatten oneOf/allOf/anyOf variants into a single merged schema
            # with all properties combined (union of all variants).
            merged = _merge_schema_variants(field_value)
            if merged:
                for mk, mv in _patched_sanitize_schema_formats_for_gemini(merged).items():
                    # Merge into current schema (properties, required, etc.)
                    if mk == "properties" and "properties" in snake_case_schema:
                        snake_case_schema["properties"].update(mv)
                    elif mk == "required" and "required" in snake_case_schema:
                        # Only keep required fields common to ALL variants
                        pass
                    else:
                        snake_case_schema[mk] = mv
        elif field_name in dict_schema_field_names and field_value is not None:
            # JSON Schema keywords (oneOf, allOf, anyOf) sometimes appear as
            # keys inside "properties" in malformed MCP schemas (e.g. Oracle
            # SQLcl puts oneOf inside properties dict). Flatten the variants
            # into merged properties instead of hoisting.
            cleaned_props = {}
            for key, value in field_value.items():
                snake_key = _to_snake_case(key)
                if snake_key in list_schema_field_names and isinstance(value, list):
                    # Flatten oneOf variants into merged properties
                    merged = _merge_schema_variants(value)
                    if merged:
                        sanitized = _patched_sanitize_schema_formats_for_gemini(merged)
                        for mk, mv in sanitized.items():
                            if mk == "properties":
                                cleaned_props.update(mv)
                            elif mk == "required":
                                snake_case_schema.setdefault("required", [])
                            elif mk not in snake_case_schema:
                                snake_case_schema[mk] = mv
                elif not isinstance(value, dict):
                    # Skip non-dict property values (malformed)
                    continue
                else:
                    cleaned_props[key] = (
                        _patched_sanitize_schema_formats_for_gemini(value)
                    )
            snake_case_schema[field_name] = cleaned_props
        elif field_name == "format" and field_value:
            current_type = schema.get("type")
            if (
                (current_type in ("integer", "number")
                 and field_value in ("int32", "int64"))
                or
                (current_type == "string"
                 and field_value in ("date-time", "enum"))
            ):
                snake_case_schema[field_name] = field_value
        elif field_name in supported_fields and field_value is not None:
            snake_case_schema[field_name] = field_value

    # Ensure top-level schemas have a "type" field — Gemini API rejects
    # function declarations whose parameters schema lacks "type".
    if "type" not in snake_case_schema and "properties" in snake_case_schema:
        snake_case_schema["type"] = "object"

    return _sanitize_schema_type(snake_case_schema, preserve_null_type)


# Apply the patch
_schema_mod._sanitize_schema_formats_for_gemini = (
    _patched_sanitize_schema_formats_for_gemini
)
print("  ✓ Applied ADK schema converter patch (oneOf/allOf list handling)")


class OracleADKSqlclMCPAgent:
    """ADK Agent using McpToolset with Oracle SQLcl MCP server"""

    def __init__(self, project_id: str, location: str,
                 sqlcl_path: str, wallet_path: str):
        self.project_id = project_id
        self.location = location
        self.sqlcl_path = sqlcl_path
        self.wallet_path = wallet_path
        self.agent = None
        self.runner = None
        self.session_service = InMemorySessionService()
        self.artifacts_service = InMemoryArtifactService()
        self.session = None
        self.exit_stack = AsyncExitStack()
        self.auth_script_path = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "..", "auth.sh")
        )

    def _is_adc_auth_error(self, error: Exception) -> bool:
        """Return True when the exception chain indicates ADC reauthentication is needed."""
        error_messages = []
        current_error = error
        while current_error is not None:
            error_messages.append(str(current_error).lower())
            current_error = current_error.__cause__ or current_error.__context__
        combined = "\n".join(error_messages)
        auth_markers = [
            "reauthentication is needed",
            "gcloud auth application-default login",
            "failed to retrieve access token",
            "invalid_grant",
            "unable to acquire impersonated credentials",
        ]
        return any(marker in combined for marker in auth_markers)

    def _print_adc_auth_help(self):
        """Print concise remediation for ADC auth failures."""
        print("\n  ⚠️  Google ADC authentication is required.")
        if os.path.exists(self.auth_script_path):
            print(f"  → Run: {self.auth_script_path}")
        print("  → Or run: gcloud auth application-default login --no-launch-browser")

    async def create_agent(self):
        """Create ADK agent with McpToolset connected to SQLcl"""
        print("  → Initializing ADK session service...")

        # Initialize Vertex AI
        vertexai.init(project=self.project_id, location=self.location)
        os.environ['GOOGLE_CLOUD_PROJECT'] = self.project_id
        os.environ['GOOGLE_CLOUD_LOCATION'] = self.location
        os.environ['GOOGLE_GENAI_USE_VERTEXAI'] = 'true'

        # Create session
        self.session = await self.session_service.create_session(
            app_name="Oracle ADK SQLcl MCP Agent",
            user_id="user_123",
            state={}
        )

        # Configure SQLcl MCP server connection
        print(f"  → Configuring SQLcl MCP: {self.sqlcl_path} -mcp")
        print(f"  → Wallet/TNS_ADMIN: {self.wallet_path}")

        mcp_connection_params = StdioConnectionParams(
            server_params=StdioServerParameters(
                command=self.sqlcl_path,
                args=["-mcp"],
                env={"TNS_ADMIN": self.wallet_path}
            ),
            timeout=30.0,
        )

        # Create McpToolset - ADK handles MCP protocol, schema conversion, tool dispatch
        mcp_toolset = McpToolset(connection_params=mcp_connection_params)

        print("  → Starting SQLcl MCP server and discovering tools...")

        instruction = """You are an expert Oracle Database AI assistant with direct database access via MCP.

**CRITICAL: Call only ONE tool at a time.** Wait for each tool's response before calling the next tool. NEVER call multiple tools in the same turn. The database server cannot handle concurrent requests and will crash.

**Available Database Tools (via SQLcl MCP):**
- List available database connections with list-connections
- Connect to databases with connect
- Run SQL queries with run-sql
- Get schema information with schema-information
- Disconnect with disconnect

**AUTONOMOUS Multi-Step Workflow (one tool call per step):**
When a user asks for database information, be proactive and autonomous:
1. First, call list-connections to see available connections. Wait for the result.
2. Then, call connect with the first/most relevant connection. Wait for the result.
3. Then, call the appropriate tool (run-sql or schema-information). Wait for the result.
4. Present the results to the user in a clear, formatted way.

**DO NOT ask the user for connection names or approval - be autonomous:**
- "Show me tables" → list-connections, then connect, then run SQL
- "What's in my database" → list-connections, then connect, then schema-information
- "Query my data" → list-connections, then connect, then run-sql

**Connection Strategy:**
- When multiple connections exist, prefer connections with "prod", "main", or similar names
- If connection fails, try the next available connection
- Only ask the user for input if NO connections are available

Be helpful, technically accurate, and AUTONOMOUS - don't make users specify connection details."""

        print("  → Creating ADK LlmAgent with McpToolset...")

        self.agent = LlmAgent(
            model="gemini-2.5-flash",
            name="oracle_sqlcl_mcp_agent",
            instruction=instruction,
            tools=[mcp_toolset],
            generate_content_config=GenerateContentConfig(
                temperature=0.1,
                max_output_tokens=2048,
            )
        )

        print("  ✓ Agent created with SQLcl MCP tools")

        # Create runner
        self.runner = Runner(
            app_name="Oracle ADK SQLcl MCP Agent",
            agent=self.agent,
            artifact_service=self.artifacts_service,
            session_service=self.session_service
        )

        print("  ✓ Runner initialized")
        return self.agent

    async def query(self, user_input: str) -> str:
        """Query the agent"""
        if not self.runner or not self.session:
            raise RuntimeError("Agent not initialized. Call create_agent() first.")

        try:
            content = types.Content(
                role="user",
                parts=[types.Part(text=user_input)]
            )

            events = self.runner.run_async(
                session_id=self.session.id,
                user_id="user_123",
                new_message=content
            )

            response_parts = []
            async for event in events:
                if event.content.parts:
                    for part in event.content.parts:
                        if hasattr(part, 'function_call') and part.function_call:
                            print(f"  🔧 Tool call: {part.function_call.name}")
                        elif hasattr(part, 'function_response') and part.function_response:
                            result = part.function_response.response.get('result', '')
                            if result:
                                print(f"  ✓ Tool result received ({len(str(result))} chars)")
                        elif part.text and event.content.role == "model":
                            response_parts.append(part.text)

            return "\n".join(response_parts) if response_parts else "No response generated"

        except Exception as e:
            import traceback
            traceback.print_exc()
            return f"Error during agent reasoning: {str(e)}"

    async def cleanup(self):
        """Cleanup resources"""
        if self.runner:
            await self.runner.close()
        await self.exit_stack.aclose()
        print("  ✓ MCP server and resources cleaned up")

    async def run_cli(self):
        """Run interactive CLI"""
        print("=" * 80)
        print("Oracle Database ADK Agent (SQLcl MCP via McpToolset)")
        print("=" * 80)
        print(f"Project: {self.project_id}")
        print(f"Region: {self.location}")
        print(f"SQLcl: {self.sqlcl_path}")
        print()

        print("🔧 Initializing ADK agent with SQLcl MCP tools...")

        try:
            await self.create_agent()
            print()
            print("Type your questions about Oracle Database (or 'quit' to exit)")
            print("-" * 80)
            print()

            while True:
                try:
                    user_input = input("You: ").strip()

                    if not user_input:
                        continue

                    if user_input.lower() in ['quit', 'exit', 'q']:
                        print("\nGoodbye!")
                        break

                    response = await self.query(user_input)
                    print(f"\nAgent: {response}\n")

                except KeyboardInterrupt:
                    print("\n\nInterrupted. Goodbye!")
                    break
                except Exception as e:
                    print(f"\nError: {str(e)}\n")
                    import traceback
                    traceback.print_exc()
                    continue

            await self.cleanup()

        except Exception as e:
            if self._is_adc_auth_error(e):
                print("\n❌ Failed to initialize agent due to expired or missing ADC credentials.")
                self._print_adc_auth_help()
            else:
                print(f"\n❌ Failed to initialize agent: {str(e)}")
                import traceback
                traceback.print_exc()
            await self.cleanup()


async def main():
    """Main entry point"""
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    sqlcl_path = os.getenv("SQLCL_PATH", "/opt/sqlcl/bin/sql")
    wallet_path = os.getenv("TNS_ADMIN", os.path.expanduser("~/wallet"))

    print("Starting ADK + SQLcl MCP Agent...\n")

    agent = OracleADKSqlclMCPAgent(project_id, location, sqlcl_path, wallet_path)
    await agent.run_cli()


if __name__ == "__main__":
    asyncio.run(main())
