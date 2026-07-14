"""OCI LLM provider implementation with DBA assistant functionality using Generative AI Inference client."""

import logging
import re
import json
import os
import importlib.metadata
from typing import Any, Dict, List, Optional

import oci
from oci.auth.signers import SecurityTokenSigner
from oci.generative_ai_inference import GenerativeAiInferenceClient
from oci.generative_ai_inference.models import (
    ChatDetails,
    GenericChatRequest,
    OnDemandServingMode,
    TextContent,
    Message,
    BaseChatRequest,
)

from .base import LLMProvider, LLMResponse
from core.logging_config import get_logger, ErrorMessages

logger = get_logger(__name__)

# Log OCI SDK version for debugging
try:
    oci_version = importlib.metadata.version("oci")
    logger.info(f"Using OCI SDK version: {oci_version}")
except importlib.metadata.PackageNotFoundError:
    logger.warning("OCI SDK version could not be determined")

DB_LIST_PHRASES = [
    "list all database connections",
    "show me saved database connections",
    "which databases are configured or connected",
    "what databases can i work with",
    "list the mcp databases available"
]

def make_security_token_signer(oci_config: Dict[str, Any]) -> SecurityTokenSigner:
    config = oci_config.copy()
    try:
        token_file = config.get("security_token_file")
        if not token_file or not os.path.exists(token_file):
            raise ValueError(f"Security token file not found: {token_file}")
        with open(token_file, 'r') as f:
            token = f.read().strip()
        private_key_file = config.get("key_file")
        if not private_key_file or not os.path.exists(private_key_file):
            raise ValueError(f"Private key file not found: {private_key_file}")
        private_key = oci.signer.load_private_key_from_file(private_key_file)
        signer = oci.auth.signers.SecurityTokenSigner(token, private_key)
        return signer
    except Exception as e:
        logger.error(f"Failed to create security token signer: {e}", exc_info=True)
        raise

def get_endpoint(region: str, stage: str = "prod") -> str:
    if stage == "prod":
        return f"https://inference.generativeai.{region}.oci.oraclecloud.com"
    elif stage == "dev":
        return f"https://dev.inference.generativeai.{region}.oci.oraclecloud.com"
    elif stage == "ppe":
        return f"https://ppe.inference.generativeai.{region}.oci.oraclecloud.com"
    else:
        raise ValueError("Invalid stage: must be one of 'dev', 'ppe', or 'prod'")

def get_generative_ai_client(endpoint: str, profile: str, use_session_token: bool) -> GenerativeAiInferenceClient:
    config = oci.config.from_file("~/.oci/config", profile)
    retry_strategy = None
    try:
        # Attempt to configure custom retry strategy
        if hasattr(oci.retry, "RetryStrategyBuilder"):
            builder = oci.retry.RetryStrategyBuilder()
            builder.add_max_attempts(3).add_total_elapsed_time(600)
            # Check if build method exists
            if hasattr(builder, "get_retry_strategy"):
                retry_strategy = builder.get_retry_strategy()
            elif hasattr(builder, "build"):
                retry_strategy = builder.build()
            else:
                retry_strategy = builder  # Use builder directly if no build method
            logger.info("Custom retry strategy configured")
        else:
            # Fallback to default retry strategy
            retry_strategy = oci.retry.DEFAULT_RETRY_STRATEGY
            logger.info("Using default retry strategy")
    except Exception as e:
        logger.warning(f"Failed to configure custom retry strategy: {e}. Falling back to no retry.")
        retry_strategy = None

    try:
        if use_session_token:
            signer = make_security_token_signer(config)
            return GenerativeAiInferenceClient(
                config=config,
                signer=signer,
                service_endpoint=endpoint,
                timeout=(30, 600),
                retry_strategy=retry_strategy
            )
        else:
            return GenerativeAiInferenceClient(
                config=config,
                service_endpoint=endpoint,
                timeout=(30, 600),
                retry_strategy=retry_strategy
            )
    except Exception as e:
        logger.error(f"Failed to create GenerativeAiInferenceClient: {e}", exc_info=True)
        raise

class OCIProvider(LLMProvider):
    """OCI LLM provider using Generative AI Inference Client with DBA assistant prompt."""

    def __init__(self, config: Dict[str, Any]):
        """Initialize OCI provider.
        
        Args:
            config: OCI configuration with region, stage, profile, etc.
        """
        super().__init__(config)
        self.region = config.get("region", "us-chicago-1")
        self.stage = config.get("stage", "prod")
        self.endpoint = get_endpoint(self.region, self.stage)
        self.profile = config.get("profile", "DEFAULT")
        self.use_session_token = config.get("use_session_token", True)
        self.client = None
        self.compartment_id = config.get(
            "compartment_id",
            "ocid1.compartment.oc1..aaaaaaaajdyhd7dqnix2avhlckbhhkkcl3cujzyuz6jzyzonadca3i66pqjq"
        )
        
        # Get logging configuration
        self.log_requests = config.get("log_llm_requests", False)
        self.log_responses = config.get("log_llm_responses", False)

        try:
            self.client = get_generative_ai_client(
                endpoint=self.endpoint,
                profile=self.profile,
                use_session_token=self.use_session_token
            )
            logger.info(f"OCI Generative AI client initialized for region={self.region} stage={self.stage}")
        except Exception as e:
            logger.error(f"Failed to initialize OCI Generative AI client: {e}", exc_info=True)
            self.client = None

    def is_available(self) -> bool:
        """Check if OCI provider is available.
        
        Returns:
            True if client is configured and available
        """
        return self.client is not None

    def generate(
        self,
        prompt: str,
        context: Optional[List[str]] = None,
        tools: Optional[List[Dict[str, Any]]] = None,
    ) -> LLMResponse:
        """Generate response using OCI Generative AI.
        
        Args:
            prompt: User prompt
            context: Optional context information
            tools: Optional tool definitions
            
        Returns:
            LLM response
            
        Raises:
            RuntimeError: If provider is not available
        """
        if not self.is_available():
            raise RuntimeError(ErrorMessages.LLM_PROVIDER_NOT_AVAILABLE)

        logger.info(f"Generating response for prompt: {prompt[:100]}...")

        # Build system message
        system_prompt = self._build_system_message(context)
        user_content = TextContent()
        user_content.text = f"{system_prompt}\n\nUser: {prompt}"
        user_message = Message(role="USER", content=[user_content])

        messages = [user_message]

        if tools:
            logger.debug(f"Injecting {len(tools)} tool definitions into user message.")
            tools_content = TextContent()
            tools_text = "\n\nAvailable Tools:\n" + "\n".join(
                [f"- {tool['function']['name']}: {tool['function'].get('description', '')}" for tool in tools]
            )
            tools_content.text = tools_text
            messages.append(Message(role="USER", content=[tools_content]))

        chat_request = GenericChatRequest(
            api_format=BaseChatRequest.API_FORMAT_GENERIC,
            messages=messages,
            max_tokens=8000,  # Increased to 8000 for complex queries with long system prompts
            temperature=0.1,
            top_p=1,
            top_k=0
        )

        chat_detail = ChatDetails(
            serving_mode=OnDemandServingMode(
                model_id="ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceya3bsfz4ogiuv3yc7gcnlry7gi3zzx6tnikg6jltqszm2q"  # Replace with actual Grok 3 OCID
            ),
            chat_request=chat_request,
            compartment_id=self.compartment_id
        )

        # Log API request details
        logger.debug("=== OCI API REQUEST ===")
        logger.debug(f"Endpoint: {self.endpoint}")
        logger.debug(f"Model: {chat_detail.serving_mode.model_id}")
        logger.debug(f"Messages: {len(messages)} message(s)")
        logger.debug("=== END REQUEST ===")

        try:
            logger.info("Making API call to OCI Generative AI...")
            response = self.client.chat(chat_detail)
            logger.info(f"OCI API response received with status: {response.status}")
            
            # Log API response if log_responses is enabled
            if self.log_responses:
                logger.info("=== OCI API RESPONSE ===")
                logger.info(f"Status: {response.status}")
                logger.info(f"Model: {response.data.model_id if hasattr(response.data, 'model_id') else 'N/A'}")
                logger.info(f"Response Data: {response.data}")
                logger.info("=== END RESPONSE ===")
            
            if response.status // 100 != 2:
                error_msg = ErrorMessages.format(ErrorMessages.LLM_API_ERROR, error=f"HTTP {response.status}")
                raise RuntimeError(error_msg)

            content = ""
            
            # Try different ways to extract content from OCI response
            if hasattr(response.data, "choices") and response.data.choices:
                choice = response.data.choices[0]
                
                if hasattr(choice, "message") and choice.message:
                    message = choice.message
                    
                    if hasattr(message, "content") and message.content:
                        if isinstance(message.content, list):
                            # Handle list of content items
                            for content_item in message.content:
                                if hasattr(content_item, "text"):
                                    content += content_item.text
                        elif isinstance(message.content, str):
                            content = message.content
                        else:
                            content = str(message.content)
                elif hasattr(choice, "content") and choice.content:
                    content = choice.content
            elif hasattr(response.data, "content"):
                content = response.data.content
            elif hasattr(response.data, "text"):
                content = response.data.text
            else:
                # Fallback: try to get any text content from the response
                logger.debug("Trying fallback content extraction...")
                response_text = str(response.data)
                if response_text and response_text != "None":
                    content = response_text

            # If we still have the full JSON response, try to extract just the text content
            if content and content.startswith('{') and '"text":' in content:
                logger.debug("Extracting text from JSON response...")
                try:
                    import json
                    response_json = json.loads(content)
                    if "chat_response" in response_json and "choices" in response_json["chat_response"]:
                        choices = response_json["chat_response"]["choices"]
                        if choices and "message" in choices[0] and "content" in choices[0]["message"]:
                            message_content = choices[0]["message"]["content"]
                            if isinstance(message_content, list):
                                extracted_content = ""
                                for item in message_content:
                                    if isinstance(item, dict) and "text" in item:
                                        extracted_content += item["text"]
                                content = extracted_content
                                logger.debug(f"Extracted text content: {content}")
                except json.JSONDecodeError:
                    logger.debug("Failed to parse JSON response, using raw content")

            logger.info(f"Extracted content from OCI response: {len(content)} characters")

            # Log response format variations
            self._log_response_format_variation(content)

            # Extract tool calls from OCI-specific format
            tool_calls = self.parse_oci_tool_calls(content)
            if tool_calls:
                logger.info(f"Parsed {len(tool_calls)} tool calls from response")
            else:
                logger.warning("No tool calls parsed from response")

            # Extract SQL and explanation
            sql = self.extract_sql(content)
            explanation = self.extract_explanation(content)

            return LLMResponse(
                content=content,
                sql=sql,
                explanation=explanation,
                tool_calls=tool_calls,
                metadata={
                    "model": 'ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceya3bsfz4ogiuv3yc7gcnlry7gi3zzx6tnikg6jltqszm2q',
                    "provider": "oci",
                    "region": self.region,
                    "stage": self.stage
                }
            )
        except Exception as e:
            logger.error(f"OCI generate failed: {e}", exc_info=True)
            error_msg = ErrorMessages.format(ErrorMessages.LLM_API_ERROR, error=str(e))
            raise RuntimeError(error_msg)

    def _repair_json(self, json_str: str) -> str:
        """Repair common JSON issues in LLM responses.
        
        Args:
            json_str: Potentially malformed JSON string
            
        Returns:
            Repaired JSON string
        """
        # Remove any leading/trailing whitespace
        json_str = json_str.strip()
        
        # Count opening and closing braces/brackets
        open_braces = json_str.count('{')
        close_braces = json_str.count('}')
        open_brackets = json_str.count('[')
        close_brackets = json_str.count(']')
        
        # Add missing closing braces
        while close_braces < open_braces:
            json_str += '}'
            close_braces += 1
        
        # Add missing closing brackets
        while close_brackets < open_brackets:
            json_str += ']'
            close_brackets += 1
        
        # Handle common LLM JSON issues
        # Fix missing quotes around property names
        json_str = re.sub(r'(\w+):', r'"\1":', json_str)
        
        # Fix single quotes to double quotes
        json_str = json_str.replace("'", '"')
        
        # Remove trailing commas
        json_str = re.sub(r',(\s*[}\]])', r'\1', json_str)
        
        return json_str

    def _safe_json_loads(self, json_str: str) -> Optional[Dict[str, Any]]:
        """Safely parse JSON with repair attempts.
        
        Args:
            json_str: JSON string to parse
            
        Returns:
            Parsed JSON dictionary or None if parsing fails
        """
        # First try direct parsing
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            logger.debug(f"Direct JSON parsing failed, attempting repair: {json_str}")
        
        # Try with repair
        try:
            repaired_json = self._repair_json(json_str)
            result = json.loads(repaired_json)
            logger.info(f"Successfully repaired and parsed JSON: {json_str[:100]}...")
            return result
        except json.JSONDecodeError as e:
            logger.warning(f"JSON repair failed: {e}, original: {json_str}")
            return None

    def parse_oci_tool_calls(self, content: str) -> List[Dict[str, Any]]:
        """Parse tool calls from OCI LLM response using multiple format patterns.
        
        Args:
            content: Response content from OCI LLM
            
        Returns:
            List of parsed tool calls
        """
        tool_calls = []
        logger.debug(f"Parsing tool calls from {len(content)} character response")

        # Try -tool format: "-tool tool_name\n{arguments}"
        tool_pattern = r"-tool\s+([^\n]+)\s*\n(\{.*?\})"
        matches = re.findall(tool_pattern, content, re.DOTALL)
        for tool_name, arguments_str in matches:
            arguments = self._safe_json_loads(arguments_str)
            if arguments is not None:
                tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
            else:
                logger.warning(f"Failed to parse JSON arguments for tool {tool_name}")

        # Try -tool_call format: -tool_call{...} (handle newlines properly)
        if not tool_calls:
            tool_call_pattern = r"-tool_call\s*\n?\s*(\{.*?\})"
            tool_call_matches = re.findall(tool_call_pattern, content, re.DOTALL)
            for json_str in tool_call_matches:
                json_data = self._safe_json_loads(json_str)
                if json_data is not None:
                    tool_name = json_data.get("tool_name")
                    if tool_name:
                        arguments = json_data.get("parameters", {})
                        tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON in tool_call format")

        # Try /tool format: /tool tool_name\n{arguments}
        if not tool_calls:
            slash_tool_pattern = r"/tool\s+([^\n]+)\s*\n(\{.*?\})"
            slash_tool_matches = re.findall(slash_tool_pattern, content, re.DOTALL)
            for tool_name, arguments_str in slash_tool_matches:
                arguments = self._safe_json_loads(arguments_str)
                if arguments is not None:
                    tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON arguments for /tool {tool_name}")

        # Try {"name": "tool", "args": {...}} format
        if not tool_calls:
            name_args_pattern = r'\{[^{}]*"name"[^{}]*"args"[^{}]*\}'
            name_args_matches = re.findall(name_args_pattern, content, re.DOTALL)
            for json_str in name_args_matches:
                json_data = self._safe_json_loads(json_str)
                if json_data is not None:
                    tool_name = json_data.get("name")
                    if tool_name:
                        arguments = json_data.get("args", {})
                        tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON in name/args format")

        # Try JSON format: ```json\n{"tool": "tool_name", "parameters": {...}}\n```
        if not tool_calls:
            json_pattern = r"```json\s*\n(\{.*?\})\s*\n```"
            json_matches = re.findall(json_pattern, content, re.DOTALL)
            for json_str in json_matches:
                json_data = self._safe_json_loads(json_str)
                if json_data is not None:
                    tool_name = json_data.get("tool") or json_data.get("toolName")
                    if tool_name:
                        arguments = json_data.get("parameters", {}) or json_data.get("args", {})
                        tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON in code block format")

        # Try Python code block format: ```python\ntool_call_id = tool_call(tool_name="...", tool_args={})\n```
        if not tool_calls:
            python_pattern = r"tool_call\(tool_name=[\"']([^\"']+)[\"'],\s*tool_args=(\{[^}]*\})\)"
            python_matches = re.findall(python_pattern, content, re.DOTALL)
            for tool_name, arguments_str in python_matches:
                arguments = self._safe_json_loads(arguments_str)
                if arguments is not None:
                    tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON arguments for Python tool_call {tool_name}")

        # Try Python tool_call format: tool_call("tool_name", {"param": "value"})
        if not tool_calls:
            python_tool_call_pattern = r'tool_call\(["\']([^"\']+)["\'],\s*(\{[^}]*\})\)'
            python_tool_call_matches = re.findall(python_tool_call_pattern, content, re.DOTALL)
            for tool_name, arguments_str in python_tool_call_matches:
                arguments = self._safe_json_loads(arguments_str)
                if arguments is not None:
                    tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON arguments for Python tool_call {tool_name}")

        # Try Python tool_call with parameters format: tool_call(tool_name="...", parameters={...})
        if not tool_calls:
            python_params_pattern = r'tool_call\(tool_name=["\']([^"\']+)["\'],\s*parameters=(\{[^}]*\})\)'
            python_params_matches = re.findall(python_params_pattern, content, re.DOTALL)
            for tool_name, arguments_str in python_params_matches:
                arguments = self._safe_json_loads(arguments_str)
                if arguments is not None:
                    tool_calls.append({"function": {"name": tool_name, "description": f"Execute {tool_name} tool", "arguments": arguments}})
                else:
                    logger.warning(f"Failed to parse JSON arguments for Python tool_call {tool_name}")

        # If no OCI format found, try the original parsing as fallback
        if not tool_calls:
            tool_calls = self.parse_tool_calls(content)
        
        # Validate and normalize tool calls
        validated_tool_calls = []
        for tool_call in tool_calls:
            validated_call = self._validate_tool_call(tool_call)
            if validated_call:
                validated_tool_calls.append(validated_call)
        
        return validated_tool_calls

    def _validate_tool_call(self, tool_call: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Validate and normalize a tool call.
        
        Args:
            tool_call: Raw tool call dictionary
            
        Returns:
            Validated and normalized tool call, or None if invalid
        """
        try:
            # Ensure required structure
            if "function" not in tool_call:
                logger.warning(f"Invalid tool call structure: {tool_call}")
                return None
            
            function = tool_call["function"]
            if "name" not in function:
                logger.warning(f"Tool call missing function name: {tool_call}")
                return None
            
            tool_name = function["name"]
            # Arguments are now inside the function object
            arguments = function.get("arguments", {})
            
            # Normalize parameter names for specific tools
            if tool_name == "mcp_oracle-sqlcl-mcp_run-sql":
                # Convert sql_query to sql if present
                if "sql_query" in arguments and "sql" not in arguments:
                    arguments["sql"] = arguments.pop("sql_query")
                    logger.info(f"Normalized sql_query to sql for {tool_name}")
            
            # Validate required parameters
            if tool_name == "mcp_oracle-sqlcl-mcp_run-sql" and "sql" not in arguments:
                logger.warning(f"Tool {tool_name} missing required 'sql' parameter")
                return None
            
            if tool_name == "mcp_oracle-sqlcl-mcp_connect" and "connection_name" not in arguments:
                logger.warning(f"Tool {tool_name} missing required 'connection_name' parameter")
                return None
            
            return tool_call
            
        except Exception as e:
            logger.error(f"Error validating tool call: {e}")
            return None

    def _log_response_format_variation(self, content: str) -> None:
        """Log response format variations for monitoring and debugging.
        
        Args:
            content: Response content from OCI LLM
        """
        # Detect format variations
        format_indicators = {
            "-tool_call": "tool_call format",
            "-tool": "tool format", 
            "/tool": "slash tool format",
            "```json": "JSON code block format",
            "tool_call(": "Python function format",
            '"tool"': "JSON tool field format",
            '"name"': "JSON name field format"
        }
        
        detected_formats = []
        for indicator, format_name in format_indicators.items():
            if indicator in content:
                detected_formats.append(format_name)
        
        if detected_formats:
            logger.info(f"Detected format(s): {', '.join(detected_formats)}")
        else:
            logger.warning("No recognized tool call format detected")
        
        # Log brief content preview for debugging
        if logger.isEnabledFor(logging.DEBUG):
            preview = content[:100].replace('\n', ' ').strip()
            logger.debug(f"Content preview: {preview}...")

    def parse_tool_calls(self, content: str) -> List[Dict[str, Any]]:
        """Parse tool calls from OCI response content.
        
        Args:
            content: Response content from OCI
            
        Returns:
            List of tool call dictionaries
        """
        tool_calls = []
        try:
            # Try parsing explicit tool call JSON
            tool_call_json_match = re.search(r"<tool_call>(.*?)</tool_call>", content, re.DOTALL)
            if tool_call_json_match:
                tool_call_json = tool_call_json_match.group(1).strip()
                tool_calls_data = json.loads(tool_call_json)
                if isinstance(tool_calls_data, dict):
                    tool_calls.append(tool_calls_data)
                elif isinstance(tool_calls_data, list):
                    tool_calls.extend(tool_calls_data)
            else:
                # Fallback: Check for JSON-like structures
                json_match = re.search(r"(\{.*?\})", content, re.DOTALL)
                if json_match:
                    try:
                        tool_calls_data = json.loads(json_match.group(1))
                        if isinstance(tool_calls_data, dict) and "function" in tool_calls_data:
                            tool_calls.append(tool_calls_data)
                        elif isinstance(tool_calls_data, list):
                            tool_calls.extend([tc for tc in tool_calls_data if "function" in tc])
                    except json.JSONDecodeError:
                        logger.debug("No valid JSON tool call found in fallback parsing")
        except Exception as e:
            logger.debug(f"Failed to parse tool_call: {e}")

        # Fallback for DB list phrases
        prompt_lower = content.lower()
        if not tool_calls and any(phrase in prompt_lower for phrase in DB_LIST_PHRASES):
            tool_calls.append({
                "function": {
                    "name": "list-connections",
                    "description": "List all database connections",
                    "arguments": {}
                }
            })

        # Ensure tool calls have required fields and proper structure
        for call in tool_calls:
            if "function" not in call:
                call["function"] = {"name": "unknown", "description": "", "arguments": {}}
            elif "arguments" not in call["function"]:
                # Move arguments from top level to function level if needed
                if "arguments" in call:
                    call["function"]["arguments"] = call.pop("arguments")
                else:
                    call["function"]["arguments"] = {}
        return tool_calls

    def execute_mcp_tool(self, tool_call: Dict[str, Any]) -> Dict[str, Any]:
        function_name = tool_call["function"]["name"]
        arguments = tool_call["function"].get("arguments", {})
        try:
            # Simulate MCP server interaction (replace with actual MCP client logic)
            if function_name == "list-connections":
                return {"result": "Available connections: HR, SCOTT"}
            elif function_name == "connect":
                connection_name = arguments.get("connection_name", "")
                return {"result": f"Connected to {connection_name}"}
            elif function_name == "run-sql":
                query = arguments.get("query", "")
                return {"result": f"Executed query: {query}"}
            elif function_name == "run-sqlcl":
                command = arguments.get("command", "")
                return {"result": f"Executed SQLcl command: {command}"}
            elif function_name == "disconnect":
                return {"result": "Disconnected from database"}
            else:
                return {"error": f"Unknown tool: {function_name}"}
        except Exception as e:
            logger.error(f"MCP tool execution failed: {e}", exc_info=True)
            return {"error": str(e)}

    def _build_system_message(self, context: Optional[List[str]] = None) -> str:
        """Build system message for Oracle DBA assistant.
        
        Args:
            context: Optional context information
            
        Returns:
            System message string
        """
        system_parts = [
            "You are an Oracle DBA assistant with MCP tools. ALWAYS execute tools - never just show SQL.",
            "",
            "WORKFLOW:",
            "1. Analyze user request",
            "2. Choose appropriate tool(s)", 
            "3. Execute tool with correct parameters",
            "4. For complex tasks: connect to database first, then run SQL",
            "",
            "TOOL CALL FORMAT (CRITICAL):",
            "-tool_call",
            "{\"tool_name\": \"tool_name\", \"parameters\": {\"param\": \"value\"}}",
            "",
            "TOOLS:",
            "- mcp_oracle-sqlcl-mcp_list-connections: List databases",
            "- mcp_oracle-sqlcl-mcp_connect: Connect to database {\"connection_name\": \"db_name\"}",
            "- mcp_oracle-sqlcl-mcp_run-sql: Execute SQL {\"sql\": \"SELECT * FROM table\"}",
            "- mcp_oracle-sqlcl-mcp_run-sqlcl: SQLcl commands",
            "- mcp_oracle-sqlcl-mcp_disconnect: Disconnect",
            "",
            "EXECUTION RULES:",
            "- List tables: Connect + run SQL 'SELECT table_name FROM user_tables'",
            "- AWR report: Connect + run AWR SQL",
            "- Complex requests: Break into steps (connect, then execute SQL)",
            "- ALWAYS execute - never just show SQL",
            "",
            "ORACLE BASICS:",
            "- user_tables: User tables view",
            "- v$session, v$sqlarea: Performance views", 
            "- CREATE TABLE syntax: CREATE TABLE name (col1 TYPE, col2 TYPE)",
            "- AWR: Use DBMS_WORKLOAD_REPOSITORY or v$sqlarea for performance analysis"
        ]
        
        if context:
            system_parts.extend([
                "",
                "Additional context:",
                *context
            ])
        
        return "\n".join(system_parts)

    def extract_sql(self, content: str) -> Optional[str]:
        match = re.search(r"```sql\n(.*?)```", content, re.DOTALL)
        return match.group(1).strip() if match else None

    def extract_explanation(self, content: str) -> Optional[str]:
        return re.sub(r"```sql\n.*?```", "", content, flags=re.DOTALL).strip()


# Example usage
if __name__ == "__main__":
    config = {
        "region": "us-chicago-1",
        "stage": "prod",
        "profile": "DEFAULT",
        "use_session_token": True,
        "model_id": "ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceya3bsfz4ogiuv3yc7gcnlry7gi3zzx6tnikg6jltqszm2q",
        "compartment_id": "ocid1.compartment.oc1..aaaaaaaajdyhd7dqnix2avhlckbhhkkcl3cujzyuz6jzyzonadca3i66pqjq"
    }
    provider = OCIProvider(config)
    try:
        response = provider.generate("List all database connections")
        print("Content:", response.content)
        print("SQL:", response.sql)
        print("Tool Calls:", response.tool_calls)
    except Exception as e:
        print(f"Error: {e}")
