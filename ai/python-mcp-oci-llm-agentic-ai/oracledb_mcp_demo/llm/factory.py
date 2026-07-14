"""LLM provider factory."""

from typing import Any, Dict

from .base import LLMProvider
from .openai_provider import OpenAIProvider
from .oci_provider import OCIProvider
from .local_provider import LocalProvider


class LLMFactory:
    """Factory for creating LLM providers."""
    
    _providers = {
        "openai": OpenAIProvider,
        "oci": OCIProvider,
        "local": LocalProvider,
    }
    
    @classmethod
    def create_provider(cls, provider_name: str, config: Dict[str, Any]) -> LLMProvider:
        """Create LLM provider instance.
        
        Args:
            provider_name: Name of the provider (openai, oci, local)
            config: Provider-specific configuration
            
        Returns:
            LLM provider instance
            
        Raises:
            ValueError: If provider is not supported
        """
        if provider_name not in cls._providers:
            supported = ", ".join(cls._providers.keys())
            raise ValueError(f"Unsupported provider '{provider_name}'. Supported: {supported}")
        
        provider_class = cls._providers[provider_name]
        return provider_class(config)
    
    @classmethod
    def get_available_providers(cls) -> list[str]:
        """Get list of available provider names.
        
        Returns:
            List of provider names
        """
        return list(cls._providers.keys())
    
    @classmethod
    def register_provider(cls, name: str, provider_class: type[LLMProvider]) -> None:
        """Register a new provider.
        
        Args:
            name: Provider name
            provider_class: Provider class
        """
        cls._providers[name] = provider_class 