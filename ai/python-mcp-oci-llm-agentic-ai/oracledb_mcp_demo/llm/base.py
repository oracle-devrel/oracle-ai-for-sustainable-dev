"""Base LLM provider interface."""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional


class LLMResponse:
    """Response from LLM provider."""
    
    def __init__(
        self,
        content: str,
        sql: Optional[str] = None,
        explanation: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
        tool_calls: Optional[List[Dict[str, Any]]] = None,
    ):
        """Initialize LLM response.
        
        Args:
            content: Raw response content
            sql: Generated SQL (if applicable)
            explanation: Explanation of the response
            metadata: Additional metadata
            tool_calls: Tool calls to execute (if any)
        """
        self.content = content
        self.sql = sql
        self.explanation = explanation
        self.metadata = metadata or {}
        self.tool_calls = tool_calls or []


class LLMProvider(ABC):
    """Base class for LLM providers."""
    
    def __init__(self, config: Dict[str, Any]):
        """Initialize LLM provider.
        
        Args:
            config: Provider-specific configuration
        """
        self.config = config
    
    @abstractmethod
    def generate(
        self,
        prompt: str,
        context: Optional[List[str]] = None,
        tools: Optional[List[Dict[str, Any]]] = None,
    ) -> LLMResponse:
        """Generate response from LLM.
        
        Args:
            prompt: User prompt
            context: Optional context information
            tools: Optional tool definitions
            
        Returns:
            LLM response
        """
        pass
    
    @abstractmethod
    def is_available(self) -> bool:
        """Check if provider is available.
        
        Returns:
            True if provider can be used
        """
        pass
    
    def extract_sql(self, content: str) -> Optional[str]:
        """Extract SQL from LLM response.
        
        Args:
            content: LLM response content
            
        Returns:
            Extracted SQL or None
        """
        # Simple SQL extraction - can be overridden by providers
        import re
        
        # Look for SQL code blocks
        sql_pattern = r"```sql\s*(.*?)\s*```"
        match = re.search(sql_pattern, content, re.DOTALL | re.IGNORECASE)
        if match:
            return match.group(1).strip()
        
        # Look for SQL statements
        sql_pattern = r"(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|MERGE|EXECUTE)\s+.*?;"
        match = re.search(sql_pattern, content, re.DOTALL | re.IGNORECASE)
        if match:
            return match.group(0).strip()
        
        return None
    
    def extract_explanation(self, content: str) -> Optional[str]:
        """Extract explanation from LLM response.
        
        Args:
            content: LLM response content
            
        Returns:
            Extracted explanation or None
        """
        # Simple explanation extraction - can be overridden by providers
        lines = content.split('\n')
        explanation_lines = []
        
        for line in lines:
            line = line.strip()
            if line and not line.startswith('```') and not line.startswith('SELECT'):
                explanation_lines.append(line)
        
        if explanation_lines:
            return ' '.join(explanation_lines)
        
        return None 