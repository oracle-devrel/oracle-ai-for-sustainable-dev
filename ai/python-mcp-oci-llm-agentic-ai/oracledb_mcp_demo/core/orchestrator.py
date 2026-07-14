"""Core orchestrator for Oracle Mcp."""

import asyncio
import logging
import subprocess
from typing import Any, Dict, List, Optional

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

from .config import ConfigManager
from .real_database_session import RealDatabaseSession
from .logging_config import setup_logging_from_config, get_logger, ErrorMessages
from .feedback_loop import IterativeFeedbackLoop, FeedbackLoopConfig
from .task_orchestrator import TaskOrchestrator
from llm.factory import LLMFactory
from core.planning.llm_planner import LLMPlanner

logger = get_logger(__name__)


class Orchestrator:
    """Main orchestrator for Oracle Mcp."""
    
    def __init__(self, config_path: Optional[str] = None, enable_feedback_loop: bool = True):
        """Initialize orchestrator.
        
        Args:
            config_path: Path to configuration file
            enable_feedback_loop: Whether to enable iterative feedback loop
        """
        # Set up logging first
        try:
            self.logging_config = setup_logging_from_config(config_path)
        except Exception as e:
            logger.warning(f"Failed to set up logging from config: {e}. Using default logging.")
        
        self.config_manager = ConfigManager(config_path)
        self.llm_provider = None
        self.session: Optional[ClientSession] = None
        self._mcp_client = None
        self._current_database: Optional[str] = None
        self.enable_feedback_loop = enable_feedback_loop
        self.feedback_loop = None
        self.task_orchestrator = None
        
        self._initialize_components()
    
    def _initialize_components(self) -> None:
        """Initialize all components."""
        try:
            # Initialize LLM provider
            llm_config = self.config_manager.get_llm_config()
            provider_config = llm_config.model_dump()[llm_config.provider]
            
            # Add logging configuration to provider config
            logging_config = self.config_manager.get_logging_config()
            provider_config.update({
                "log_llm_requests": logging_config.log_llm_requests,
                "log_llm_responses": logging_config.log_llm_responses
            })
            
            self.llm_provider = LLMFactory.create_provider(llm_config.provider, provider_config)
            
            # Initialize feedback loop if enabled
            if self.enable_feedback_loop:
                feedback_config = FeedbackLoopConfig(
                    max_iterations=3,
                    target_quality_score=0.8,
                    min_improvement_threshold=0.1
                )
                self.feedback_loop = IterativeFeedbackLoop(feedback_config, self.config_manager, orchestrator=self)
                logger.info("Feedback loop initialized")
            
            # Initialize task orchestrator for complex prompts
            # Note: Will be fully initialized when MCP session is available
            self.task_orchestrator = None
            logger.info("Task orchestrator will be initialized when MCP session is available")
            
            logger.info("All components initialized successfully")
            
        except Exception as e:
            logger.error(f"Failed to initialize components: {e}")
            raise
    
    async def start_mcp_server(self) -> None:
        """Start MCP server using SQLcl."""
        try:
            # Check if SQLcl is available
            try:
                subprocess.run(["sql", "-V"], capture_output=True, check=True, timeout=5)
                logger.info("SQLcl found, starting MCP server")
            except (subprocess.TimeoutExpired, subprocess.CalledProcessError, FileNotFoundError):
                logger.warning("SQLcl not found or not working, trying alternative check")
                try:
                    subprocess.run(["which", "sql"], capture_output=True, check=True)
                    logger.info("SQLcl found via which command")
                except subprocess.CalledProcessError:
                    raise RuntimeError("SQLcl not found. Please install Oracle SQLcl.")
            
            # Use direct database connection for now (avoids MCP async context issues)
            logger.info("Using direct database connection")
            self.session = RealDatabaseSession(self.config_manager)
            
        except Exception as e:
            logger.warning(f"Failed to start MCP server: {e} - continuing without MCP")
            self.session = None
    
    async def stop_mcp_server(self) -> None:
        """Stop database session."""
        try:
            if self.session:
                await self.session.disconnect()
                self.session = None
            
            logger.info("Database session stopped")
        except Exception as e:
            logger.warning(f"Error stopping database session: {e}")
    
    async def process_prompt(self, prompt: str, use_feedback_loop: Optional[bool] = None) -> dict:
        """Process user prompt using database tools.
        
        Args:
            prompt: User prompt
            use_feedback_loop: Whether to use feedback loop (overrides default)
            
        Returns:
            Response dictionary with results and explanation
        """
        if not self.llm_provider:
            raise RuntimeError("LLM provider not available")
        
        # Initialize task orchestrator if not already done and session is available
        if self.task_orchestrator is None and self.session:
            # Initialize RAG system
            try:
                # Use the new unified config system
                from core.rag_factory import create_rag_system
                rag_system = create_rag_system()
                logger.debug("RAG system initialized successfully with unified config")
            except Exception as e:
                logger.warning(f"Failed to initialize RAG system: {e}")
                rag_system = None
            
            # Initialize LLM planner with RAG context
            try:
                llm_planner = LLMPlanner(self.llm_provider, rag_system)
                logger.debug("LLM planner initialized successfully")
            except Exception as e:
                logger.warning(f"Failed to initialize LLM planner: {e}")
                llm_planner = None
            
            # Create task orchestrator with all components
            self.task_orchestrator = TaskOrchestrator(self.llm_provider, self.session)
            
            # Set up the LLM planner and RAG system
            if llm_planner:
                self.task_orchestrator.llm_planner = llm_planner
                logger.debug("LLM planner connected to task orchestrator")
            
            # Store RAG system for later use
            if rag_system:
                self.task_orchestrator.rag_system = rag_system
                logger.debug("RAG system connected to task orchestrator")
            
            # Inject the single prompt processor for simple prompts
            self.task_orchestrator._process_prompt_single = self._process_prompt_single
            logger.info("Task orchestrator initialized with MCP session, LLM planner, and RAG system")
        
        # Check if this is a complex prompt that needs task decomposition
        if self.task_orchestrator and self.task_orchestrator._is_complex_prompt(prompt):
            logger.info(f"Complex prompt detected, using task-centric execution: {prompt[:50]}...")
            return await self.task_orchestrator.execute_complex_prompt(prompt)
        
        # Determine if we should use feedback loop for simple prompts
        should_use_feedback = use_feedback_loop if use_feedback_loop is not None else self.enable_feedback_loop
        
        if should_use_feedback and self.feedback_loop:
            logger.info("Using iterative feedback loop for prompt processing")
            return await self.feedback_loop.execute_with_feedback(self, prompt)
        
        # Original processing logic for simple prompts
        logger.info("Using single prompt processing for simple prompt")
        return await self._process_prompt_single(prompt)
     
    async def _process_prompt_single(self, prompt: str) -> dict:
        """Process prompt without feedback loop (original logic).
        
        Args:
            prompt: User prompt
            
        Returns:
            Response dictionary with results and explanation
        """
        try:
            # Get MCP tools if available
            mcp_tools = []
            if self.session:
                try:
                    tools_response = await self.session.list_tools()
                    mcp_tools = []
                    for tool in tools_response.tools:
                        mcp_tools.append({
                            "type": "function",
                            "function": {
                                "name": tool.name,
                                "description": tool.description,
                                "parameters": tool.inputSchema
                            }
                        })
                    logger.info(f"Found {len(mcp_tools)} MCP tools")
                except Exception as e:
                    logger.warning(f"Failed to get MCP tools: {e}")
            
            # Build context with MCP information
            if mcp_tools:
                context = self._build_mcp_context(mcp_tools)
            else:
                context = [
                    "You are an Oracle DBA assistant. Generate SQL queries and explanations based on user requests.",
                    "Available database operations: connect, query, monitor, analyze performance",
                    "Use Oracle-specific syntax and best practices.",
                    "No MCP tools are currently available - generate SQL queries instead."
                ]
            
            # Let the LLM decide what to do with the prompt
            llm_response = self.llm_provider.generate(prompt, context, tools=mcp_tools if mcp_tools else None)
            
            # Execute any tool calls from the LLM
            results = None
            if llm_response.tool_calls:
                logger.info(f"LLM generated {len(llm_response.tool_calls)} tool calls")
                if self.session:
                    logger.info("MCP session available, executing tool calls")
                    try:
                        results = await self._execute_tool_calls_relay(llm_response.tool_calls)
                    except Exception as e:
                        results = f"Error executing tools: {e}"
                        logger.error(f"Tool execution error: {e}")
                else:
                    logger.warning("MCP session not available, cannot execute tool calls")
                    results = f"Tool calls generated but MCP server not available: {llm_response.tool_calls}"
            elif llm_response.sql:
                results = f"SQL generated: {llm_response.sql}"
            
            return {
                "prompt": prompt,
                "sql": llm_response.sql,
                "results": results,
                "explanation": llm_response.explanation,
                "metadata": llm_response.metadata
            }
            
        except Exception as e:
            logger.error(f"Failed to process prompt: {e}")
            raise
    
    def _build_mcp_context(self, tools: List[Dict[str, Any]]) -> List[str]:
        """Build context with MCP tools information.
        
        Args:
            tools: Available MCP tools
            
        Returns:
            Context strings
        """
        context = []
        
        # Add MCP tools information with emphasis on usage
        context.append("MCP TOOLS ARE AVAILABLE - YOU MUST USE THEM:")
        context.append("These tools allow you to execute SQL queries directly on the database.")
        context.append("DO NOT just generate SQL - EXECUTE it using these tools!")
        
        for tool in tools:
            func = tool["function"]
            context.append(f"- {func['name']}: {func['description']}")
        
        context.append("")
        context.append("CRITICAL: When user asks for database information, you MUST:")
        context.append("1. Generate the appropriate SQL query")
        context.append("2. IMMEDIATELY execute it using the appropriate MCP tool")
        context.append("3. Provide the actual results, not just the SQL")
        
        # Add current database context if connected
        if self._current_database:
            context.append(f"")
            context.append(f"CURRENT CONTEXT: Connected to database '{self._current_database}'")
            context.append(f"For analysis requests, use mcp_oracle-sqlcl-mcp_run-sql to query this database")
            context.append(f"Only use mcp_oracle-sqlcl-mcp_list-connections if user explicitly asks to list databases")
        
        return context
    
    async def _execute_tool_calls_relay(self, tool_calls: List[Dict[str, Any]]) -> str:
        """Execute tool calls by relaying them to the MCP server.
        
        Args:
            tool_calls: List of tool calls to execute
            
        Returns:
            Combined results from tool executions
        """
        results = []
        
        for tool_call in tool_calls:
            try:
                tool_name = tool_call["function"]["name"]
                # Handle both old and new argument formats for robustness
                if "arguments" in tool_call["function"]:
                    arguments = tool_call["function"]["arguments"]
               
                logger.info(f"Executing tool call: {tool_name} with arguments: {arguments}")
                
                # Call the MCP server using session.call_tool()
                try:
                    result = await self.session.call_tool(tool_name, arguments)
                    
                    # Extract the result content properly
                    result_text = ""
                    if hasattr(result, 'content') and result.content:
                        for content in result.content:
                            if hasattr(content, 'text'):
                                result_text += str(content.text) + "\n"
                    else:
                        result_text = str(result)
                    
                    results.append(f"Tool: {tool_name}")
                    results.append(f"Result: {result_text.strip()}")
                    
                    # Update current database if this was a connect
                    if tool_name == "mcp_oracle-sqlcl-mcp_connect" and arguments.get("connection_name"):
                        self._current_database = arguments["connection_name"]
                    
                except Exception as mcp_error:
                    error_msg = f"Error executing {tool_name}: {mcp_error}"
                    logger.error(error_msg)
                    results.append(error_msg)
                
            except Exception as e:
                error_msg = f"Error processing tool call: {e}"
                logger.error(error_msg)
                results.append(error_msg)
        
        return "\n".join(results)
    
    def get_current_database(self) -> Optional[str]:
        """Get current database name.
        
        Returns:
            Current database name or None
        """
        return self._current_database
    
    async def disconnect_database(self) -> bool:
        """Disconnect from current database.
        
        Returns:
            True if disconnection successful
        """
        try:
            if self.session:
                await self.session.call_tool("mcp_oracle-sqlcl-mcp_disconnect", {})
                self._current_database = None
                return True
            return False
        except Exception as e:
            logger.error(f"Error disconnecting: {e}")
            return False
     
    def toggle_feedback_loop(self, enabled: bool) -> None:
        """Toggle feedback loop on/off.
        
        Args:
            enabled: Whether to enable feedback loop
        """
        self.enable_feedback_loop = enabled
        if enabled and not self.feedback_loop:
            feedback_config = FeedbackLoopConfig(
                max_iterations=3,
                target_quality_score=0.8,
                min_improvement_threshold=0.1
            )
            self.feedback_loop = IterativeFeedbackLoop(feedback_config, self.config_manager, orchestrator=self)
            logger.info("Feedback loop enabled")
        elif not enabled:
            self.feedback_loop = None
            logger.info("Feedback loop disabled")
     
    def configure_feedback_loop(self, max_iterations: int = None, 
                              target_score: float = None,
                              min_improvement: float = None) -> None:
        """Configure feedback loop parameters.
        
        Args:
            max_iterations: Maximum number of iterations
            target_score: Target quality score to reach
            min_improvement: Minimum improvement threshold
        """
        if self.feedback_loop:
            if max_iterations is not None:
                self.feedback_loop.config.max_iterations = max_iterations
            if target_score is not None:
                self.feedback_loop.config.target_quality_score = target_score
            if min_improvement is not None:
                self.feedback_loop.config.min_improvement_threshold = min_improvement
            logger.info(f"Feedback loop configured: max_iterations={self.feedback_loop.config.max_iterations}, "
                       f"target_score={self.feedback_loop.config.target_quality_score}, "
                       f"min_improvement={self.feedback_loop.config.min_improvement_threshold}")
     
    def get_feedback_loop_status(self) -> Dict[str, Any]:
        """Get current feedback loop status and configuration.
        
        Returns:
            Dictionary with feedback loop status
        """
        if not self.enable_feedback_loop or not self.feedback_loop:
            return {"enabled": False}
            
        return {
            "enabled": True,
            "max_iterations": self.feedback_loop.config.max_iterations,
            "target_quality_score": self.feedback_loop.config.target_quality_score,
            "min_improvement_threshold": self.feedback_loop.config.min_improvement_threshold,
            "total_iterations_run": len(self.feedback_loop.iterations) if self.feedback_loop.iterations else 0
        } 