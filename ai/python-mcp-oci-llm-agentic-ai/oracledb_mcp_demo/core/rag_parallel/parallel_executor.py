"""Parallel RAG query execution following SOLID principles."""

import asyncio
import logging
import time
from typing import List, Dict, Any, Optional
from abc import ABC, abstractmethod

from .models import RAGQuery, RAGResult, ParallelExecutionResult, ExecutionMetrics, ParallelExecutionConfig
from ..config import get_config_value

logger = logging.getLogger(__name__)


class RAGSystem(ABC):
    """Abstract interface for RAG system."""
    
    @abstractmethod
    async def query(self, query: str, k: int = 5, filters: Dict[str, Any] = None) -> RAGResult:
        """Execute a RAG query."""
        pass


class ParallelRAGExecutor:
    """Executes multiple RAG queries in parallel with priority queuing."""
    
    def __init__(self, config: ParallelExecutionConfig = None):
        """Initialize parallel executor."""
        self.config = config or ParallelExecutionConfig()
        self.config.validate()
        
        # Concurrency control
        self.semaphore = asyncio.Semaphore(self.config.max_concurrent_queries)
        
        # Metrics tracking
        self.metrics = ExecutionMetrics()
        
        # RAG system reference (will be set later)
        self.rag_system: Optional[RAGSystem] = None
    
    def set_rag_system(self, rag_system: RAGSystem) -> None:
        """Set the RAG system for query execution."""
        self.rag_system = rag_system
    
    async def execute_parallel_queries(self, queries: List[RAGQuery]) -> List[RAGResult]:
        """Execute multiple RAG queries in parallel."""
        if not queries:
            return []
        
        if not self.rag_system:
            raise RuntimeError("RAG system not set. Call set_rag_system() first.")
        
        start_time = time.time()
        self.metrics.total_queries = len(queries)
        
        try:
            logger.info(f"Executing {len(queries)} queries in parallel (max concurrent: {self.config.max_concurrent_queries})")
            
            # Sort queries by priority (lower number = higher priority)
            sorted_queries = sorted(queries, key=lambda q: q.priority)
            
            # Execute queries in parallel with concurrency control
            tasks = []
            for query in sorted_queries:
                task = asyncio.create_task(
                    self._execute_single_query_with_semaphore(query)
                )
                tasks.append(task)
            
            # Wait for all tasks to complete
            results = await asyncio.gather(*tasks, return_exceptions=True)
            
            # Process results and handle exceptions
            processed_results = []
            for i, result in enumerate(results):
                if isinstance(result, Exception):
                    logger.error(f"Query {i} failed: {result}")
                    self.metrics.failed_queries += 1
                    processed_results.append(None)
                else:
                    self.metrics.successful_queries += 1
                    processed_results.append(result)
            
            # Update metrics
            execution_time = (time.time() - start_time) * 1000
            self.metrics.total_execution_time_ms = execution_time
            self.metrics.parallel_execution_time_ms = execution_time
            
            logger.info(f"Parallel execution completed in {execution_time:.2f}ms. "
                       f"Success: {self.metrics.successful_queries}/{self.metrics.total_queries}")
            
            return processed_results
            
        except Exception as e:
            logger.error(f"Error in parallel execution: {e}")
            raise
    
    async def _execute_single_query_with_semaphore(self, query: RAGQuery) -> RAGResult:
        """Execute a single query with semaphore control."""
        async with self.semaphore:
            return await self._execute_single_query(query)
    
    async def _execute_single_query(self, query: RAGQuery) -> RAGResult:
        """Execute a single RAG query."""
        start_time = time.time()
        
        try:
            logger.debug(f"Executing query: {query.query[:100]}... (priority: {query.priority})")
            
            # Execute query with timeout and minimum relevance threshold
            if query.timeout_seconds:
                result = await asyncio.wait_for(
                    self.rag_system.query(
                        query=query.query,
                        k=query.k,
                        filters=query.filters,
                        min_relevance=get_config_value('rag.min_relevance_score', 0.5)  # From config
                    ),
                    timeout=query.timeout_seconds
                )
            else:
                result = await self.rag_system.query(
                    query=query.query,
                    k=query.k,
                    filters=query.filters,
                    min_relevance=get_config_value('rag.min_relevance_score', 0.5)  # From config
                )
            
            # Update result metadata
            execution_time = (time.time() - start_time) * 1000
            result.execution_time_ms = execution_time
            result.parallel_query_id = query.get_execution_key()
            
            logger.debug(f"Query completed in {execution_time:.2f}ms: {result.total_found} results")
            
            return result
            
        except asyncio.TimeoutError:
            execution_time = (time.time() - start_time) * 1000
            logger.warning(f"Query timed out after {execution_time:.2f}ms: {query.query[:100]}...")
            
            # Return empty result for timeout
            return RAGResult(
                query=query.query,
                results=[],
                total_found=0,
                metadata={"error": "timeout", "execution_time_ms": execution_time},
                execution_time_ms=execution_time,
                parallel_query_id=query.get_execution_key()
            )
            
        except Exception as e:
            execution_time = (time.time() - start_time) * 1000
            logger.error(f"Query failed after {execution_time:.2f}ms: {e}")
            
            # Return empty result for failure
            return RAGResult(
                query=query.query,
                results=[],
                total_found=0,
                metadata={"error": str(e), "execution_time_ms": execution_time},
                execution_time_ms=execution_time,
                parallel_query_id=query.get_execution_key()
            )
    
    async def execute_with_retry(self, query: RAGQuery) -> RAGResult:
        """Execute a query with retry logic."""
        max_attempts = query.max_retries + 1
        
        for attempt in range(max_attempts):
            try:
                result = await self._execute_single_query(query)
                
                # Check if result is valid
                if result.total_found > 0 or not query.is_retryable():
                    return result
                
                # If no results and retryable, try again
                if attempt < max_attempts - 1:
                    logger.info(f"Query returned no results, retrying... (attempt {attempt + 1}/{max_attempts})")
                    query.increment_retry()
                    await asyncio.sleep(0.1 * (attempt + 1))  # Exponential backoff
                
            except Exception as e:
                if attempt < max_attempts - 1:
                    logger.warning(f"Query failed, retrying... (attempt {attempt + 1}/{max_attempts}): {e}")
                    query.increment_retry()
                    await asyncio.sleep(0.1 * (attempt + 1))
                else:
                    logger.error(f"Query failed after {max_attempts} attempts: {e}")
                    raise
        
        # If we get here, all attempts failed
        return RAGResult(
            query=query.query,
            results=[],
            total_found=0,
            metadata={"error": "all_retries_failed"},
            parallel_query_id=query.get_execution_key()
        )
    
    async def execute_sequential_queries(self, queries: List[RAGQuery]) -> List[RAGResult]:
        """Execute queries sequentially for comparison."""
        if not queries:
            return []
        
        start_time = time.time()
        results = []
        
        for query in queries:
            result = await self._execute_single_query(query)
            results.append(result)
        
        execution_time = (time.time() - start_time) * 1000
        self.metrics.sequential_execution_time_ms = execution_time
        
        logger.info(f"Sequential execution completed in {execution_time:.2f}ms")
        
        return results
    
    def get_execution_metrics(self) -> Dict[str, Any]:
        """Get current execution metrics."""
        return self.metrics.to_dict()
    
    def reset_metrics(self) -> None:
        """Reset execution metrics."""
        self.metrics = ExecutionMetrics()
    
    async def close(self) -> None:
        """Close executor and cleanup resources."""
        # Cancel any pending tasks
        tasks = [task for task in asyncio.all_tasks() if not task.done()]
        for task in tasks:
            task.cancel()
        
        # Wait for cancellation to complete
        if tasks:
            await asyncio.gather(*tasks, return_exceptions=True)
        
        logger.info("Parallel executor closed")


class AdaptiveParallelExecutor(ParallelRAGExecutor):
    """Adaptive parallel executor that adjusts concurrency based on performance."""
    
    def __init__(self, config: ParallelExecutionConfig = None):
        """Initialize adaptive executor."""
        super().__init__(config)
        self.performance_history = []
        self.adaptation_interval = 10  # Adapt every 10 queries
        self.query_count = 0
    
    async def _execute_single_query_with_semaphore(self, query: RAGQuery) -> RAGResult:
        """Execute query with adaptive concurrency control."""
        # Update performance tracking
        self.query_count += 1
        
        # Execute query
        result = await super()._execute_single_query_with_semaphore(query)
        
        # Track performance
        self.performance_history.append({
            "execution_time": result.execution_time_ms,
            "concurrency": self.semaphore._value,
            "success": result.total_found > 0
        })
        
        # Adapt concurrency if needed
        if self.query_count % self.adaptation_interval == 0:
            await self._adapt_concurrency()
        
        return result
    
    async def _adapt_concurrency(self) -> None:
        """Adapt concurrency based on performance history."""
        if len(self.performance_history) < 5:
            return
        
        # Calculate performance metrics
        recent_performance = self.performance_history[-self.adaptation_interval:]
        avg_execution_time = sum(p["execution_time"] for p in recent_performance) / len(recent_performance)
        success_rate = sum(1 for p in recent_performance if p["success"]) / len(recent_performance)
        
        # Determine if we should adjust concurrency
        current_concurrency = self.semaphore._value
        
        if avg_execution_time < 100 and success_rate > 0.8:  # Good performance
            if current_concurrency < self.config.max_concurrent_queries:
                new_concurrency = min(current_concurrency + 1, self.config.max_concurrent_queries)
                self._adjust_concurrency(new_concurrency)
                logger.info(f"Performance good, increasing concurrency to {new_concurrency}")
        
        elif avg_execution_time > 500 or success_rate < 0.6:  # Poor performance
            if current_concurrency > self.config.min_concurrent_queries:
                new_concurrency = max(current_concurrency - 1, self.config.min_concurrent_queries)
                self._adjust_concurrency(new_concurrency)
                logger.info(f"Performance poor, decreasing concurrency to {new_concurrency}")
    
    def _adjust_concurrency(self, new_concurrency: int) -> None:
        """Adjust the concurrency level."""
        # Create new semaphore with adjusted value
        old_semaphore = self.semaphore
        self.semaphore = asyncio.Semaphore(new_concurrency)
        
        # Transfer any available permits
        available_permits = old_semaphore._value
        if available_permits > 0:
            for _ in range(available_permits):
                self.semaphore.release()
    
    def get_adaptation_stats(self) -> Dict[str, Any]:
        """Get adaptation statistics."""
        if not self.performance_history:
            return {}
        
        return {
            "total_queries": self.query_count,
            "performance_history_length": len(self.performance_history),
            "current_concurrency": self.semaphore._value,
            "recent_avg_execution_time": sum(
                p["execution_time"] for p in self.performance_history[-10:]
            ) / min(10, len(self.performance_history)),
            "recent_success_rate": sum(
                1 for p in self.performance_history[-10:] if p["success"]
            ) / min(10, len(self.performance_history))
        }
