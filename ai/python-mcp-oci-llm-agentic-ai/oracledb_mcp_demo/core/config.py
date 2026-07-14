"""Configuration management for Oracle Mcp."""

import json
import os
from pathlib import Path
from typing import Dict, Optional, Any

from pydantic import BaseModel, Field


class LoggingConfig(BaseModel):
    """Logging configuration."""
    
    level: str = Field(default="INFO", description="Log level: DEBUG, INFO, WARNING, ERROR")
    log_llm_requests: bool = Field(default=False, description="Log LLM API requests")
    log_llm_responses: bool = Field(default=False, description="Log LLM API responses")
    format: str = Field(default="%(asctime)s - %(name)s - %(levelname)s - %(message)s", 
                       description="Log message format")


class LLMConfig(BaseModel):
    """LLM provider configuration."""
    
    provider: str = Field(..., description="LLM provider: openai, oci, or local")
    openai: Dict[str, str] = Field(default_factory=dict)
    oci: Dict[str, str] = Field(default_factory=dict)
    local: Dict[str, str] = Field(default_factory=dict)


class VectorDBConfig(BaseModel):
    """Vector database configuration."""
    
    enabled: bool = Field(default=False, description="Whether vector DB is enabled")
    connection_string: str = Field(default="", description="Database connection string")
    table: str = Field(default="VECTOR_KB", description="Vector table name")
    embedding_column: str = Field(default="embedding", description="Embedding column name")
    text_column: str = Field(default="text", description="Text column name")


class RAGDatabaseConfig(BaseModel):
    """RAG database configuration."""
    
    connection_string: str = Field(..., description="Database connection string")
    db_schema: str = Field(..., description="Database schema")
    tables: Dict[str, str] = Field(..., description="Table mappings")
    connection_pool_size: int = Field(default=3, description="Connection pool size")
    connection_timeout: int = Field(default=15, description="Connection timeout in seconds")


class RAGEmbeddingConfig(BaseModel):
    """RAG embedding configuration."""
    
    provider_type: str = Field(..., description="Embedding provider type")
    model_name: str = Field(..., description="Model name")
    model_version: str = Field(..., description="Model version")
    dimension: int = Field(..., description="Embedding dimension")
    batch_size: int = Field(default=5, description="Batch size for embeddings")


class RAGConfig(BaseModel):
    """RAG system configuration - simplified with search settings flattened."""
    
    # Required fields
    database: RAGDatabaseConfig
    embedding: RAGEmbeddingConfig
    
    # Search settings (flattened from search sub-object)
    default_k: int = Field(default=3, description="Default number of results to retrieve")
    max_k: int = Field(default=10, description="Maximum number of results")
    min_relevance_score: float = Field(default=0.5, description="Minimum relevance score threshold")
    search_timeout: int = Field(default=15, description="Search timeout in seconds")
    enable_hybrid_search: bool = Field(default=True, description="Enable hybrid search")
    enable_mmr: bool = Field(default=False, description="Enable Maximal Marginal Relevance")
    
    # Other settings
    enabled: bool = Field(default=True, description="Whether RAG is enabled")
    enable_learning: bool = Field(default=True, description="Enable learning capabilities")
    enable_validation: bool = Field(default=True, description="Enable validation")
    enable_enhancement: bool = Field(default=True, description="Enable enhancement")
    log_level: str = Field(default="DEBUG", description="RAG log level")
    cache_size: int = Field(default=100, description="Cache size")


class PerformanceConfig(BaseModel):
    """Performance monitoring configuration."""
    
    query_duration_warning_ms: int = Field(default=1000, description="Query duration warning threshold in milliseconds")
    query_duration_error_ms: int = Field(default=5000, description="Query duration error threshold in milliseconds")
    cache_hit_rate_minimum: float = Field(default=0.5, description="Minimum acceptable cache hit rate")
    memory_usage_warning: float = Field(default=0.8, description="Memory usage warning threshold (80%)")
    cpu_usage_warning: float = Field(default=0.8, description="CPU usage warning threshold (80%)")


class ParallelConfig(BaseModel):
    """Parallel execution configuration."""
    
    max_concurrent_queries: int = Field(default=5, description="Maximum number of concurrent queries")
    query_timeout_seconds: int = Field(default=30, description="Query timeout in seconds")
    max_retries_per_query: int = Field(default=2, description="Maximum retries per query")
    enable_priority_queuing: bool = Field(default=True, description="Enable priority-based query queuing")
    enable_result_caching: bool = Field(default=True, description="Enable result caching")
    enable_adaptive_concurrency: bool = Field(default=True, description="Enable adaptive concurrency control")
    min_concurrent_queries: int = Field(default=1, description="Minimum number of concurrent queries")


class BatchConfig(BaseModel):
    """Batch processing configuration."""
    
    vector_db_batch_size: int = Field(default=10, description="Vector database batch size")
    mcp_batch_size: int = Field(default=5, description="MCP batch size")


class VectorIndexConfig(BaseModel):
    """Vector index configuration."""
    
    type: str = Field(default="hnsw", description="Index type (hnsw, ivf, etc.)")
    metric: str = Field(default="cosine", description="Distance metric (cosine, euclidean, etc.)")
    m: int = Field(default=16, description="HNSW parameter: number of connections per layer")
    ef_construction: int = Field(default=200, description="HNSW parameter: size of dynamic candidate list during construction")
    ef_search: int = Field(default=64, description="HNSW parameter: size of dynamic candidate list during search")


class AppConfig(BaseModel):
    """Main application configuration."""
    
    logging: LoggingConfig = Field(default_factory=LoggingConfig)
    llm: LLMConfig
    databases: Dict[str, str] = Field(..., description="Database connection strings")
    vector_db: VectorDBConfig = Field(default_factory=VectorDBConfig)
    rag: RAGConfig = Field(default_factory=RAGConfig)
    performance: PerformanceConfig = Field(default_factory=PerformanceConfig)
    parallel: ParallelConfig = Field(default_factory=ParallelConfig)
    batch: BatchConfig = Field(default_factory=BatchConfig)
    vector_index: VectorIndexConfig = Field(default_factory=VectorIndexConfig)


class ConfigManager:
    """Manages application configuration."""
    
    def __init__(self, config_path: Optional[str] = None):
        """Initialize configuration manager.
        
        Args:
            config_path: Path to config file. Defaults to config/config.json
        """
        if config_path is None:
            config_path = Path(__file__).parent.parent / "config" / "config.json"
        
        self.config_path = Path(config_path)
        self._config: Optional[AppConfig] = None
    
    def _resolve_env_vars(self, obj):
        """Recursively resolve environment variable placeholders in configuration data."""
        if isinstance(obj, dict):
            return {key: self._resolve_env_vars(value) for key, value in obj.items()}
        elif isinstance(obj, list):
            return [self._resolve_env_vars(item) for item in obj]
        elif isinstance(obj, str):
            # Simple replacement of ${ENV_VAR} with environment variable value
            result = obj
            while '${' in result and '}' in result:
                start = result.find('${')
                end = result.find('}', start)
                if start == -1 or end == -1:
                    break
                
                env_var_name = result[start + 2:end]
                env_value = os.getenv(env_var_name)
                if env_value is None:
                    raise ValueError(f"Environment variable '{env_var_name}' not found")
                
                result = result.replace(f'${{{env_var_name}}}', env_value)
            
            return result
        else:
            return obj

    def load_config(self) -> AppConfig:
        """Load configuration from file.
        
        Returns:
            Loaded configuration
            
        Raises:
            FileNotFoundError: If config file doesn't exist
            ValueError: If config is invalid
        """
        if not self.config_path.exists():
            raise FileNotFoundError(f"Config file not found: {self.config_path}")
        
        with open(self.config_path, 'r') as f:
            config_data = json.load(f)
        
        # Resolve environment variables
        config_data = self._resolve_env_vars(config_data)
        
        try:
            self._config = AppConfig(**config_data)
            return self._config
        except Exception as e:
            raise ValueError(f"Invalid configuration: {e}")
    
    def get_config(self) -> AppConfig:
        """Get current configuration, loading if necessary.
        
        Returns:
            Current configuration
        """
        if self._config is None:
            self._config = self.load_config()
        return self._config
    
    def save_config(self, config: AppConfig) -> None:
        """Save configuration to file.
        
        Args:
            config: Configuration to save
        """
        self.config_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(self.config_path, 'w') as f:
            json.dump(config.model_dump(), f, indent=2)
        
        self._config = config
    
    def get_database_connection(self, db_name: str) -> str:
        """Get database connection string by name.
        
        Args:
            db_name: Database name/alias
            
        Returns:
            Database connection string
            
        Raises:
            KeyError: If database not found
        """
        config = self.get_config()
        if db_name not in config.databases:
            raise KeyError(f"Database '{db_name}' not found in configuration")
        return config.databases[db_name]
    
    def get_llm_config(self) -> LLMConfig:
        """Get LLM configuration.
        
        Returns:
            LLM configuration
        """
        return self.get_config().llm
    
    def get_rag_config(self) -> RAGConfig:
        """Get RAG configuration.
        
        Returns:
            RAG configuration
        """
        return self.get_config().rag
    
    def get_databases(self) -> Dict[str, str]:
        """Get all database configurations.
        
        Returns:
            Dictionary mapping database names to connection strings
        """
        return self.get_config().databases
    
    def get_logging_config(self) -> LoggingConfig:
        """Get logging configuration.
        
        Returns:
            Logging configuration
        """
        return self.get_config().logging
    
    def get_value(self, path: str, default: Any = None) -> Any:
        """Get a configuration value using dot notation path.
        
        Args:
            path: Dot notation path (e.g., 'rag.min_relevance_score')
            default: Default value if path not found
            
        Returns:
            Configuration value or default
        """
        try:
            config = self.get_config()
            keys = path.split('.')
            value = config
            
            for key in keys:
                if hasattr(value, key):
                    value = getattr(value, key)
                elif isinstance(value, dict) and key in value:
                    value = value[key]
                else:
                    return default
            
            return value
        except Exception:
            return default


# Global configuration instance - accessible to all classes
_global_config: Optional[ConfigManager] = None


def get_global_config() -> ConfigManager:
    """Get the global configuration manager instance.
    
    Returns:
        Global configuration manager
    """
    global _global_config
    if _global_config is None:
        _global_config = ConfigManager()
    return _global_config


def get_config() -> AppConfig:
    """Get the current application configuration.
    
    Returns:
        Application configuration
    """
    return get_global_config().get_config()


def get_rag_config() -> RAGConfig:
    """Get RAG configuration.
    
    Returns:
        RAG configuration
    """
    return get_global_config().get_rag_config()


def get_config_value(path: str, default: Any = None) -> Any:
    """Get a configuration value using dot notation.
    
    Args:
        path: Dot notation path (e.g., 'rag.min_relevance_score')
        default: Default value if path not found
        
    Returns:
        Configuration value or default
    """
    return get_global_config().get_value(path, default) 