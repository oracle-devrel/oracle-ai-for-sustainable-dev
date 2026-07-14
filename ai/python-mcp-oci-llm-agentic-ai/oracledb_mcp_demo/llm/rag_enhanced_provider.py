"""RAG-Enhanced LLM Provider for Oracle Mcp.

This module provides a wrapper around existing LLM providers that automatically
injects RAG context into prompts, following the RAG-Augmented LLM (RAL) pattern.
"""

import logging
from typing import Dict, Any, Optional, List
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)


class RAGEnhancedLLMProvider(ABC):
    """Abstract base class for RAG-enhanced LLM providers.
    
    This follows the Decorator pattern to enhance existing LLM providers
    with RAG context injection capabilities.
    """
    
    @abstractmethod
    async def generate_with_rag_context(
        self, 
        prompt: str, 
        system_message: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None,
        **kwargs
    ) -> Any:
        """Generate response with RAG context automatically injected.
        
        Args:
            prompt: User prompt
            system_message: Optional system message
            context: Optional additional context
            **kwargs: Additional arguments for the underlying LLM
            
        Returns:
            LLM response with RAG context
        """
        pass


class RAGEnhancedOpenAIProvider(RAGEnhancedLLMProvider):
    """RAG-enhanced OpenAI provider that automatically injects RAG context.
    
    This wrapper enhances the existing OpenAI provider by:
    1. Automatically retrieving relevant RAG context
    2. Injecting context into system messages
    3. Maintaining all existing functionality
    """
    
    def __init__(self, base_provider, rag_context_provider):
        """Initialize with base provider and RAG context provider.
        
        Args:
            base_provider: Base OpenAI provider instance
            rag_context_provider: RAG context provider instance
        """
        self.base_provider = base_provider
        self.rag_context_provider = rag_context_provider
        logger.info("RAG-Enhanced OpenAI Provider initialized")
    
    async def generate_with_rag_context(
        self, 
        prompt: str, 
        system_message: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None,
        **kwargs
    ) -> Any:
        """Generate response with RAG context automatically injected.
        
        Args:
            prompt: User prompt
            system_message: Optional system message
            context: Optional additional context
            **kwargs: Additional arguments for the underlying LLM
            
        Returns:
            LLM response with RAG context
        """
        try:
            # Get RAG context for the prompt
            rag_context = await self.rag_context_provider.get_context_for_prompt(prompt)
            
            # Enhance system message with RAG context
            enhanced_system_message = self._enhance_system_message(
                system_message, rag_context, context
            )
            
            # Log RAG context usage
            if rag_context.get("has_context"):
                logger.info(f"RAG context injected: {rag_context.get('result_count', 0)} results")
                logger.debug(f"RAG context preview: {rag_context.get('formatted_results', '')[:200]}...")
            else:
                logger.debug("No RAG context available for this prompt")
            
            # Call base provider with enhanced system message
            return await self.base_provider.generate(
                prompt=prompt,
                system_message=enhanced_system_message,
                **kwargs
            )
            
        except Exception as e:
            logger.warning(f"Failed to inject RAG context: {e}")
            # Fallback to base provider without RAG context
            return await self.base_provider.generate(
                prompt=prompt,
                system_message=system_message,
                **kwargs
            )
    
    def _enhance_system_message(
        self, 
        system_message: Optional[str], 
        rag_context: Dict[str, Any],
        additional_context: Optional[Dict[str, Any]] = None
    ) -> str:
        """Enhance system message with RAG context.
        
        Args:
            system_message: Original system message
            rag_context: RAG context dictionary
            additional_context: Additional context to include
            
        Returns:
            Enhanced system message with RAG context
        """
        enhanced_parts = []
        
        # Start with original system message
        if system_message:
            enhanced_parts.append(system_message)
        
        # Add RAG context if available
        if rag_context.get("has_context") and rag_context.get("formatted_results"):
            rag_section = f"""
RELEVANT KNOWLEDGE BASE CONTEXT:
{rag_context['formatted_results']}

Use this context to provide more accurate and informed responses. If the context contains relevant information for the user's query, incorporate it into your response.
"""
            enhanced_parts.append(rag_section)
        
        # Add additional context if provided
        if additional_context:
            context_section = f"""
ADDITIONAL CONTEXT:
{self._format_additional_context(additional_context)}
"""
            enhanced_parts.append(context_section)
        
        # Combine all parts
        if enhanced_parts:
            return "\n\n".join(enhanced_parts)
        else:
            return "You are a helpful Oracle database assistant."
    
    def _format_additional_context(self, context: Dict[str, Any]) -> str:
        """Format additional context for system message.
        
        Args:
            context: Additional context dictionary
            
        Returns:
            Formatted context string
        """
        formatted_parts = []
        
        for key, value in context.items():
            if value is not None:
                if isinstance(value, str):
                    formatted_parts.append(f"{key}: {value}")
                elif isinstance(value, (list, tuple)):
                    formatted_parts.append(f"{key}: {', '.join(map(str, value))}")
                else:
                    formatted_parts.append(f"{key}: {value}")
        
        return "\n".join(formatted_parts)
    
    # Delegate other methods to base provider
    def __getattr__(self, name):
        """Delegate unknown attributes to base provider."""
        return getattr(self.base_provider, name)


class RAGEnhancedLocalProvider(RAGEnhancedLLMProvider):
    """RAG-enhanced local provider for local LLM models.
    
    This wrapper enhances local LLM providers with RAG context injection.
    """
    
    def __init__(self, base_provider, rag_context_provider):
        """Initialize with base provider and RAG context provider.
        
        Args:
            base_provider: Base local provider instance
            rag_context_provider: RAG context provider instance
        """
        self.base_provider = base_provider
        self.rag_context_provider = rag_context_provider
        logger.info("RAG-Enhanced Local Provider initialized")
    
    async def generate_with_rag_context(
        self, 
        prompt: str, 
        system_message: Optional[str] = None,
        context: Optional[Dict[str, Any]] = None,
        **kwargs
    ) -> Any:
        """Generate response with RAG context automatically injected.
        
        Args:
            prompt: User prompt
            system_message: Optional system message
            context: Optional additional context
            **kwargs: Additional arguments for the underlying LLM
            
        Returns:
            LLM response with RAG context
        """
        try:
            # Get RAG context for the prompt
            rag_context = await self.rag_context_provider.get_context_for_prompt(prompt)
            
            # Enhance system message with RAG context
            enhanced_system_message = self._enhance_system_message(
                system_message, rag_context, context
            )
            
            # Log RAG context usage
            if rag_context.get("has_context"):
                logger.info(f"RAG context injected: {rag_context.get('result_count', 0)} results")
                logger.debug(f"RAG context preview: {rag_context.get('formatted_results', '')[:200]}...")
            else:
                logger.debug("No RAG context available for this prompt")
            
            # Call base provider with enhanced system message
            return await self.base_provider.generate(
                prompt=prompt,
                system_message=enhanced_system_message,
                **kwargs
            )
            
        except Exception as e:
            logger.warning(f"Failed to inject RAG context: {e}")
            # Fallback to base provider without RAG context
            return await self.base_provider.generate(
                prompt=prompt,
                system_message=system_message,
                **kwargs
            )
    
    def _enhance_system_message(
        self, 
        system_message: Optional[str], 
        rag_context: Dict[str, Any],
        additional_context: Optional[Dict[str, Any]] = None
    ) -> str:
        """Enhance system message with RAG context.
        
        Args:
            system_message: Original system message
            rag_context: RAG context dictionary
            additional_context: Additional context to include
            
        Returns:
            Enhanced system message with RAG context
        """
        enhanced_parts = []
        
        # Start with original system message
        if system_message:
            enhanced_parts.append(system_message)
        
        # Add RAG context if available
        if rag_context.get("has_context") and rag_context.get("formatted_results"):
            rag_section = f"""
RELEVANT KNOWLEDGE BASE CONTEXT:
{rag_context['formatted_results']}

Use this context to provide more accurate and informed responses. If the context contains relevant information for the user's query, incorporate it into your response.
"""
            enhanced_parts.append(rag_section)
        
        # Add additional context if provided
        if additional_context:
            context_section = f"""
ADDITIONAL CONTEXT:
{self._format_additional_context(additional_context)}
"""
            enhanced_parts.append(context_section)
        
        # Combine all parts
        if enhanced_parts:
            return "\n\n".join(enhanced_parts)
        else:
            return "You are a helpful Oracle database assistant."
    
    def _format_additional_context(self, context: Dict[str, Any]) -> str:
        """Format additional context for system message.
        
        Args:
            context: Additional context dictionary
            
        Returns:
            Formatted context string
        """
        formatted_parts = []
        
        for key, value in context.items():
            if value is not None:
                if isinstance(value, str):
                    formatted_parts.append(f"{key}: {value}")
                elif isinstance(value, (list, tuple)):
                    formatted_parts.append(f"{key}: {', '.join(map(str, value))}")
                else:
                    formatted_parts.append(f"{key}: {value}")
        
        return "\n".join(formatted_parts)
    
    # Delegate other methods to base provider
    def __getattr__(self, name):
        """Delegate unknown attributes to base provider."""
        return getattr(self.base_provider, name)
