"""Standardized logging configuration for Oracle Mcp."""

import logging
import json
from typing import Dict, Any, Optional
from pathlib import Path


class LoggingConfig:
    """Centralized logging configuration manager."""
    
    def __init__(self, config: Dict[str, Any]):
        """Initialize logging configuration.
        
        Args:
            config: Configuration dictionary containing logging settings
        """
        self.config = config.get("logging", {})
        self._setup_logging()
    
    def _setup_logging(self) -> None:
        """Set up logging configuration based on config settings."""
        # Get logging settings with defaults
        log_level = self.config.get("level", "INFO")
        log_format = self.config.get("format", "%(asctime)s - %(name)s - %(levelname)s - %(message)s")
        
        # Convert string level to logging constant
        numeric_level = getattr(logging, log_level.upper(), logging.INFO)
        
        # Configure root logger
        logging.basicConfig(
            level=numeric_level,
            format=log_format,
            force=True  # Override any existing configuration
        )
        
        # Set specific logger levels
        loggers_to_configure = [
            "llm.oci_provider",
            "llm.openai_provider", 
            "llm.local_provider",
            "core.orchestrator",
            "core.real_database_session",
            "application.cli",
            "mcp_client"
        ]
        
        for logger_name in loggers_to_configure:
            logger = logging.getLogger(logger_name)
            logger.setLevel(numeric_level)
    
    def should_log_llm_requests(self) -> bool:
        """Check if LLM requests should be logged.
        
        Returns:
            True if LLM requests should be logged
        """
        return self.config.get("log_llm_requests", False)
    
    def should_log_llm_responses(self) -> bool:
        """Check if LLM responses should be logged.
        
        Returns:
            True if LLM responses should be logged
        """
        return self.config.get("log_llm_responses", False)
    
    def get_logger(self, name: str) -> logging.Logger:
        """Get a logger with the specified name.
        
        Args:
            name: Logger name
            
        Returns:
            Configured logger instance
        """
        return logging.getLogger(name)


def setup_logging_from_config(config_path: Optional[str] = None) -> LoggingConfig:
    """Set up logging from configuration file.
    
    Args:
        config_path: Path to configuration file. If None, uses default location.
        
    Returns:
        LoggingConfig instance
        
    Raises:
        FileNotFoundError: If config file not found
        json.JSONDecodeError: If config file is invalid JSON
    """
    if config_path is None:
        config_path = Path(__file__).parent.parent / "config" / "config.json"
    
    config_path = Path(config_path)
    if not config_path.exists():
        raise FileNotFoundError(f"Configuration file not found: {config_path}")
    
    with open(config_path, 'r') as f:
        config = json.load(f)
    
    return LoggingConfig(config)


def get_logger(name: str) -> logging.Logger:
    """Get a logger with the specified name.
    
    Args:
        name: Logger name
        
    Returns:
        Logger instance
    """
    return logging.getLogger(name)


# Standard error messages for consistent error handling
class ErrorMessages:
    """Standardized error messages for the application."""
    
    # LLM Provider Errors
    LLM_PROVIDER_NOT_AVAILABLE = "LLM provider is not available - check configuration"
    LLM_API_ERROR = "LLM API error: {error}"
    LLM_INVALID_RESPONSE = "Invalid response from LLM provider"
    LLM_TOOL_CALL_PARSE_ERROR = "Failed to parse tool calls from LLM response"
    
    # Configuration Errors
    CONFIG_MISSING = "Configuration file not found: {path}"
    CONFIG_INVALID = "Invalid configuration file: {error}"
    CONFIG_MISSING_FIELD = "Missing required configuration field: {field}"
    
    # Database Errors
    DATABASE_CONNECTION_FAILED = "Failed to connect to database: {error}"
    DATABASE_NOT_CONNECTED = "No database connection. Please connect to a database first."
    DATABASE_QUERY_FAILED = "Database query failed: {error}"
    
    # MCP Tool Errors
    MCP_TOOL_NOT_FOUND = "MCP tool not found: {tool_name}"
    MCP_TOOL_EXECUTION_FAILED = "MCP tool execution failed: {error}"
    MCP_INVALID_ARGUMENTS = "Invalid arguments for MCP tool: {tool_name}"
    
    # Generic Errors
    UNEXPECTED_ERROR = "An unexpected error occurred: {error}"
    VALIDATION_ERROR = "Validation error: {error}"
    TIMEOUT_ERROR = "Operation timed out: {operation}"
    
    @classmethod
    def format(cls, message: str, **kwargs) -> str:
        """Format an error message with parameters.
        
        Args:
            message: Error message template
            **kwargs: Parameters to format into the message
            
        Returns:
            Formatted error message
        """
        try:
            return message.format(**kwargs)
        except KeyError as e:
            return f"Error formatting message '{message}': missing parameter {e}" 