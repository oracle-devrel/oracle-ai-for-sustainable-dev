"""Data models for parallel RAG execution system."""

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional
from enum import Enum


class QueryPriority(Enum):
    """Query execution priority levels."""
    CRITICAL = 1
    HIGH = 2
    MEDIUM = 3
    LOW = 4
    BACKGROUND = 5


class QueryAspect(Enum):
    """Different aspects of a complex query."""
    PERFORMANCE_ANALYSIS = "performance_analysis"
    OPTIMIZATION = "optimization"
    INDEX_RECOMMENDATIONS = "index_recommendations"
    TESTING_STEPS = "testing_steps"
    COST_ANALYSIS = "cost_analysis"
    TROUBLESHOOTING = "troubleshooting"
    BEST_PRACTICES = "best_practices"
    MONITORING = "monitoring"


@dataclass
class RAGQuery:
    """Individual RAG query for parallel execution."""
    query: str
    k: int = 5
    filters: Dict[str, Any] = field(default_factory=dict)
    priority: int = QueryPriority.MEDIUM.value
    aspect: Optional[QueryAspect] = None
    timeout_seconds: Optional[int] = None
    retry_count: int = 0
    max_retries: int = 2
    
    def __post_init__(self):
        """Validate query parameters."""
        if self.k <= 0:
            raise ValueError("k must be positive")
        if not (1 <= self.priority <= 10):
            raise ValueError("priority must be between 1 and 10")
        if self.timeout_seconds is not None and self.timeout_seconds <= 0:
            raise ValueError("timeout_seconds must be positive")
    
    def is_retryable(self) -> bool:
        """Check if query can be retried."""
        return self.retry_count < self.max_retries
    
    def increment_retry(self) -> None:
        """Increment retry count."""
        self.retry_count += 1
    
    def get_execution_key(self) -> str:
        """Get unique execution key for this query."""
        return f"{self.query}_{self.priority}_{self.aspect}_{self.k}"


@dataclass
class RAGResult:
    """Result from RAG system query."""
    query: str
    results: List[Dict[str, Any]]
    total_found: int
    metadata: Dict[str, Any] = field(default_factory=dict)
    execution_time_ms: float = 0.0
    cache_hit: bool = False
    parallel_query_id: Optional[str] = None
    
    def is_empty(self) -> bool:
        """Check if result contains no data."""
        return len(self.results) == 0 or self.total_found == 0
    
    def get_relevance_score(self) -> float:
        """Get average relevance score from results."""
        if not self.results:
            return 0.0
        
        scores = [
            result.get("relevance_score", 0.0) 
            for result in self.results
        ]
        return sum(scores) / len(scores) if scores else 0.0
    
    def get_top_results(self, n: int = None) -> List[Dict[str, Any]]:
        """Get top N results by relevance."""
        if n is None:
            n = len(self.results)
        
        # Sort by relevance score (highest first)
        sorted_results = sorted(
            self.results,
            key=lambda x: x.get("relevance_score", 0.0),
            reverse=True
        )
        
        return sorted_results[:n]


@dataclass
class ParallelExecutionResult:
    """Result of parallel RAG query execution."""
    original_prompt: str
    decomposed_queries: List[RAGQuery]
    results: List[RAGResult]
    aggregated_result: RAGResult
    execution_metadata: Dict[str, Any] = field(default_factory=dict)
    total_execution_time_ms: float = 0.0
    cache_hit_rate: float = 0.0
    parallelization_efficiency: float = 0.0
    
    def get_successful_queries(self) -> List[RAGQuery]:
        """Get queries that executed successfully."""
        return [
            query for query, result in zip(self.decomposed_queries, self.results)
            if result is not None
        ]
    
    def get_failed_queries(self) -> List[RAGQuery]:
        """Get queries that failed to execute."""
        return [
            query for query, result in zip(self.decomposed_queries, self.results)
            if result is None
        ]
    
    def get_execution_summary(self) -> Dict[str, Any]:
        """Get summary of execution results."""
        return {
            "total_queries": len(self.decomposed_queries),
            "successful_queries": len(self.get_successful_queries()),
            "failed_queries": len(self.get_failed_queries()),
            "total_execution_time_ms": self.total_execution_time_ms,
            "cache_hit_rate": self.cache_hit_rate,
            "parallelization_efficiency": self.parallelization_efficiency,
            "total_results_found": self.aggregated_result.total_found
        }


@dataclass
class ExecutionMetrics:
    """Metrics for parallel execution performance."""
    start_time: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    end_time: Optional[datetime] = None
    total_queries: int = 0
    successful_queries: int = 0
    failed_queries: int = 0
    cache_hits: int = 0
    cache_misses: int = 0
    total_execution_time_ms: float = 0.0
    parallel_execution_time_ms: float = 0.0
    sequential_execution_time_ms: float = 0.0
    
    def complete(self) -> None:
        """Mark execution as complete."""
        self.end_time = datetime.now(timezone.utc)
    
    @property
    def success_rate(self) -> float:
        """Calculate success rate."""
        if self.total_queries == 0:
            return 0.0
        return self.successful_queries / self.total_queries
    
    @property
    def cache_hit_rate(self) -> float:
        """Calculate cache hit rate."""
        total_requests = self.cache_hits + self.cache_misses
        if total_requests == 0:
            return 0.0
        return self.cache_hits / total_requests
    
    @property
    def parallelization_speedup(self) -> float:
        """Calculate parallelization speedup factor."""
        if self.sequential_execution_time_ms == 0:
            return 1.0
        return self.sequential_execution_time_ms / self.parallel_execution_time_ms
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary."""
        return {
            "total_queries": self.total_queries,
            "successful_queries": self.successful_queries,
            "failed_queries": self.failed_queries,
            "success_rate": self.success_rate,
            "cache_hits": self.cache_hits,
            "cache_misses": self.cache_misses,
            "cache_hit_rate": self.cache_hit_rate,
            "total_execution_time_ms": self.total_execution_time_ms,
            "parallel_execution_time_ms": self.parallel_execution_time_ms,
            "sequential_execution_time_ms": self.sequential_execution_time_ms,
            "parallelization_speedup": self.parallelization_speedup,
            "start_time": self.start_time.isoformat(),
            "end_time": self.end_time.isoformat() if self.end_time else None
        }


@dataclass
class ParallelExecutionConfig:
    """Configuration for parallel RAG execution."""
    max_concurrent_queries: int = 5
    query_timeout_seconds: int = 30
    max_retries_per_query: int = 2
    enable_priority_queuing: bool = True
    enable_result_caching: bool = True
    enable_adaptive_concurrency: bool = True
    min_concurrent_queries: int = 1
    
    def __post_init__(self):
        """Load configuration values from config system."""
        from core.config import get_config_value
        
        # Override defaults with config values if not explicitly set
        if self.max_concurrent_queries == 5:  # Default value
            self.max_concurrent_queries = get_config_value('parallel.max_concurrent_queries', 5)
        if self.query_timeout_seconds == 30:  # Default value
            self.query_timeout_seconds = get_config_value('parallel.query_timeout_seconds', 30)
        if self.max_retries_per_query == 2:  # Default value
            self.max_retries_per_query = get_config_value('parallel.max_retries_per_query', 2)
        if self.enable_priority_queuing is True:  # Default value
            self.enable_priority_queuing = get_config_value('parallel.enable_priority_queuing', True)
        if self.enable_result_caching is True:  # Default value
            self.enable_result_caching = get_config_value('parallel.enable_result_caching', True)
        if self.enable_adaptive_concurrency is True:  # Default value
            self.enable_adaptive_concurrency = get_config_value('parallel.enable_adaptive_concurrency', True)
        if self.min_concurrent_queries == 1:  # Default value
            self.min_concurrent_queries = get_config_value('parallel.min_concurrent_queries', 1)
    
    def validate(self) -> None:
        """Validate configuration parameters."""
        if self.max_concurrent_queries <= 0:
            raise ValueError("max_concurrent_queries must be positive")
        if self.query_timeout_seconds <= 0:
            raise ValueError("query_timeout_seconds must be positive")
        if self.max_retries_per_query < 0:
            raise ValueError("max_retries_per_query must be non-negative")
        if self.min_concurrent_queries > self.max_concurrent_queries:
            raise ValueError("min_concurrent_queries cannot exceed max_concurrent_queries")
