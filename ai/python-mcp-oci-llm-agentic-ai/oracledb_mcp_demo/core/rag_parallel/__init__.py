"""Parallel RAG Execution System - Concurrent query execution with smart aggregation."""

from .models import (
    QueryPriority, QueryAspect, RAGQuery, RAGResult,
    ParallelExecutionResult, ExecutionMetrics, ParallelExecutionConfig
)
from .query_decomposer import RAGQueryDecomposer, QueryPattern
from .parallel_executor import (
    RAGSystem, ParallelRAGExecutor, AdaptiveParallelExecutor
)
from .result_aggregator import (
    DeduplicationStrategy, ContentBasedDeduplication, RelevanceBasedDeduplication,
    RAGResultAggregator, SmartResultAggregator
)

__all__ = [
    # Models
    "QueryPriority",
    "QueryAspect", 
    "RAGQuery",
    "RAGResult",
    "ParallelExecutionResult",
    "ExecutionMetrics",
    "ParallelExecutionConfig",
    
    # Query Decomposition
    "RAGQueryDecomposer",
    "QueryPattern",
    
    # Parallel Execution
    "RAGSystem",
    "ParallelRAGExecutor",
    "AdaptiveParallelExecutor",
    
    # Result Aggregation
    "DeduplicationStrategy",
    "ContentBasedDeduplication",
    "RelevanceBasedDeduplication",
    "RAGResultAggregator",
    "SmartResultAggregator"
]
