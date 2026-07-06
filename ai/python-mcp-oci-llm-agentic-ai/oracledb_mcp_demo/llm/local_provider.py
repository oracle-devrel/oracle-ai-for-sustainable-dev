"""Local LLM provider."""

import logging
from typing import Any, Dict, List, Optional

from .base import LLMProvider, LLMResponse


class LocalProvider(LLMProvider):
    """Local LLM provider implementation."""
    
    def __init__(self, config: Dict[str, Any]):
        """Initialize local provider.
        
        Args:
            config: Provider configuration
        """
        super().__init__(config)
        self.model_path = config.get("model_path")
        self.model = config.get("model", "local")
        
        if not self.model_path:
            logger.warning("No model path provided for local provider")
    
    def is_available(self) -> bool:
        """Check if provider is available.
        
        Returns:
            True if available
        """
        return bool(self.model_path)
    
    def generate(self, prompt: str, context: Optional[List[str]] = None, tools: Optional[List[Dict[str, Any]]] = None) -> LLMResponse:
        """Generate response from local LLM.
        
        Args:
            prompt: User prompt
            context: Optional context information
            tools: Optional tools to use
            
        Returns:
            LLM response
        """
        # Placeholder implementation
        logger.info("Local provider not fully implemented")
        
        return LLMResponse(
            content="Local provider not fully implemented",
            sql=None,
            explanation="This is a placeholder response from local provider",
            metadata={"provider": "local"},
            tool_calls=[]
        ) 