"""RAG result aggregation and deduplication following SOLID principles."""

import logging
from typing import List, Dict, Any, Optional
from abc import ABC, abstractmethod
from collections import defaultdict

from .models import RAGResult, ParallelExecutionResult

logger = logging.getLogger(__name__)


class DeduplicationStrategy(ABC):
    """Abstract base class for deduplication strategies."""
    
    @abstractmethod
    def should_deduplicate(self, result1: Dict[str, Any], result2: Dict[str, Any]) -> bool:
        """Determine if two results should be deduplicated."""
        pass


class ContentBasedDeduplication(DeduplicationStrategy):
    """Deduplication based on content similarity."""
    
    def __init__(self, similarity_threshold: float = 0.8):
        """Initialize content-based deduplication."""
        self.similarity_threshold = similarity_threshold
    
    def should_deduplicate(self, result1: Dict[str, Any], result2: Dict[str, Any]) -> bool:
        """Check if results have similar content."""
        content1 = result1.get("content", "")
        content2 = result2.get("content", "")
        
        if not content1 or not content2:
            return False
        
        # Simple content similarity check
        # In production, this would use more sophisticated NLP techniques
        similarity = self._calculate_content_similarity(content1, content2)
        return similarity >= self.similarity_threshold
    
    def _calculate_content_similarity(self, content1: str, content2: str) -> float:
        """Calculate similarity between two content strings."""
        # Simple word overlap similarity
        words1 = set(content1.lower().split())
        words2 = set(content2.lower().split())
        
        if not words1 or not words2:
            return 0.0
        
        intersection = len(words1.intersection(words2))
        union = len(words1.union(words2))
        
        return intersection / union if union > 0 else 0.0


class RelevanceBasedDeduplication(DeduplicationStrategy):
    """Deduplication based on relevance scores."""
    
    def __init__(self, relevance_threshold: float = 0.1):
        """Initialize relevance-based deduplication."""
        self.relevance_threshold = relevance_threshold
    
    def should_deduplicate(self, result1: Dict[str, Any], result2: Dict[str, Any]) -> bool:
        """Check if results have similar relevance scores."""
        score1 = result1.get("relevance_score", 0.0)
        score2 = result2.get("relevance_score", 0.0)
        
        # Check if scores are close enough to consider duplicates
        score_diff = abs(score1 - score2)
        return score_diff <= self.relevance_threshold


class RAGResultAggregator:
    """Aggregates and deduplicates RAG results from parallel execution."""
    
    def __init__(self, deduplication_strategies: List[DeduplicationStrategy] = None):
        """Initialize result aggregator."""
        self.deduplication_strategies = deduplication_strategies or [
            ContentBasedDeduplication(),
            RelevanceBasedDeduplication()
        ]
    
    def aggregate_results(self, results: List[RAGResult]) -> RAGResult:
        """Aggregate multiple RAG results into a single result."""
        if not results:
            return RAGResult(
                query="",
                results=[],
                total_found=0,
                metadata={"aggregation_method": "empty"}
            )
        
        if len(results) == 1:
            return results[0]
        
        logger.info(f"Aggregating {len(results)} RAG results")
        
        # Collect all results
        all_results = []
        total_found = 0
        execution_times = []
        
        for result in results:
            if result and result.results:
                all_results.extend(result.results)
                total_found += result.total_found
                if result.execution_time_ms:
                    execution_times.append(result.execution_time_ms)
        
        # Deduplicate results
        deduplicated_results = self._deduplicate_results(all_results)
        
        # Rerank results by relevance
        reranked_results = self._rerank_results(deduplicated_results)
        
        # Calculate aggregation metadata
        metadata = self._calculate_aggregation_metadata(
            results, total_found, len(deduplicated_results), execution_times
        )
        
        logger.info(f"Aggregation complete: {len(deduplicated_results)} unique results from {total_found} total")
        
        return RAGResult(
            query="[Aggregated Results]",
            results=reranked_results,
            total_found=len(deduplicated_results),
            metadata=metadata
        )
    
    def _deduplicate_results(self, results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Remove duplicate results using multiple strategies."""
        if not results:
            return []
        
        deduplicated = []
        seen_indices = set()
        
        for i, result in enumerate(results):
            if i in seen_indices:
                continue
            
            # Check against all other results
            duplicates = []
            for j, other_result in enumerate(results[i+1:], i+1):
                if j in seen_indices:
                    continue
                
                if self._is_duplicate(result, other_result):
                    duplicates.append(j)
            
            # Mark duplicates as seen
            seen_indices.update(duplicates)
            
            # Add the representative result
            deduplicated.append(result)
        
        return deduplicated
    
    def _is_duplicate(self, result1: Dict[str, Any], result2: Dict[str, Any]) -> bool:
        """Check if two results are duplicates using all strategies."""
        # Only deduplicate if ALL strategies agree they are duplicates
        # This makes deduplication less aggressive
        duplicate_votes = 0
        total_strategies = len(self.deduplication_strategies)
        
        for strategy in self.deduplication_strategies:
            if strategy.should_deduplicate(result1, result2):
                duplicate_votes += 1
        
        # Require majority vote for deduplication
        return duplicate_votes > total_strategies / 2
    
    def _rerank_results(self, results: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Rerank results by relevance score."""
        if not results:
            return []
        
        # Sort by relevance score (highest first)
        # Results without relevance scores get 0.0
        sorted_results = sorted(
            results,
            key=lambda x: x.get("relevance_score", 0.0),
            reverse=True
        )
        
        return sorted_results
    
    def _calculate_aggregation_metadata(self, original_results: List[RAGResult], 
                                      total_found: int, unique_count: int,
                                      execution_times: List[float]) -> Dict[str, Any]:
        """Calculate metadata about the aggregation process."""
        metadata = {
            "aggregation_method": "parallel_aggregation",
            "original_result_count": len(original_results),
            "total_results_found": total_found,
            "unique_results_after_deduplication": unique_count,
            "deduplication_rate": (total_found - unique_count) / total_found if total_found > 0 else 0.0,
            "deduplication_strategies": [strategy.__class__.__name__ for strategy in self.deduplication_strategies]
        }
        
        # Add execution time statistics
        if execution_times:
            metadata.update({
                "avg_execution_time_ms": sum(execution_times) / len(execution_times),
                "min_execution_time_ms": min(execution_times),
                "max_execution_time_ms": max(execution_times),
                "total_execution_time_ms": sum(execution_times)
            })
        
        # Add result quality metrics
        if original_results:
            cache_hits = sum(1 for r in original_results if r.cache_hit)
            metadata["cache_hit_rate"] = cache_hits / len(original_results)
        
        return metadata
    
    def create_parallel_execution_result(self, original_prompt: str, 
                                       decomposed_queries: List, 
                                       results: List[RAGResult],
                                       aggregated_result: RAGResult) -> ParallelExecutionResult:
        """Create a parallel execution result from aggregated data."""
        # Calculate execution efficiency
        total_execution_time = sum(
            r.execution_time_ms for r in results if r and r.execution_time_ms
        )
        
        # Calculate cache hit rate
        cache_hits = sum(1 for r in results if r and r.cache_hit)
        cache_hit_rate = cache_hits / len(results) if results else 0.0
        
        # Calculate parallelization efficiency
        # This is a simplified calculation - in production you'd compare with sequential execution
        parallelization_efficiency = 0.8  # Placeholder
        
        return ParallelExecutionResult(
            original_prompt=original_prompt,
            decomposed_queries=decomposed_queries,
            results=results,
            aggregated_result=aggregated_result,
            execution_metadata=aggregated_result.metadata,
            total_execution_time_ms=total_execution_time,
            cache_hit_rate=cache_hit_rate,
            parallelization_efficiency=parallelization_efficiency
        )


class SmartResultAggregator(RAGResultAggregator):
    """Smart aggregator with advanced deduplication and ranking."""
    
    def __init__(self, deduplication_strategies: List[DeduplicationStrategy] = None):
        """Initialize smart aggregator."""
        super().__init__(deduplication_strategies)
        self.result_clusters = defaultdict(list)
    
    def aggregate_results(self, results: List[RAGResult]) -> RAGResult:
        """Smart aggregation with clustering and advanced ranking."""
        if not results:
            return super().aggregate_results(results)
        
        # Cluster similar results
        self._cluster_results(results)
        
        # Select best representative from each cluster
        representative_results = self._select_cluster_representatives()
        
        # Advanced ranking
        ranked_results = self._advanced_ranking(representative_results)
        
        # Create aggregated result
        return self._create_smart_aggregated_result(results, ranked_results)
    
    def _cluster_results(self, results: List[RAGResult]) -> None:
        """Cluster results by similarity."""
        self.result_clusters.clear()
        
        for result in results:
            if not result or not result.results:
                continue
            
            # Find best cluster for this result
            best_cluster = None
            best_similarity = 0.0
            
            for cluster_id, cluster_results in self.result_clusters.items():
                for cluster_result in cluster_results:
                    similarity = self._calculate_result_similarity(result, cluster_result)
                    if similarity > best_similarity:
                        best_similarity = similarity
                        best_cluster = cluster_id
            
            # Create new cluster or add to existing
            if best_similarity > 0.7 and best_cluster is not None:
                self.result_clusters[best_cluster].append(result)
            else:
                # Create new cluster
                new_cluster_id = len(self.result_clusters)
                self.result_clusters[new_cluster_id] = [result]
    
    def _calculate_result_similarity(self, result1: RAGResult, result2: RAGResult) -> float:
        """Calculate similarity between two RAG results."""
        # Simple similarity based on query and result count
        # In production, this would use more sophisticated NLP
        query_similarity = 0.0
        if result1.query and result2.query:
            query_similarity = self._calculate_text_similarity(result1.query, result2.query)
        
        result_count_similarity = 1.0 - abs(result1.total_found - result2.total_found) / max(result1.total_found, result2.total_found, 1)
        
        return (query_similarity + result_count_similarity) / 2
    
    def _calculate_text_similarity(self, text1: str, text2: str) -> float:
        """Calculate text similarity using word overlap."""
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())
        
        if not words1 or not words2:
            return 0.0
        
        intersection = len(words1.intersection(words2))
        union = len(words1.union(words2))
        
        return intersection / union if union > 0 else 0.0
    
    def _select_cluster_representatives(self) -> List[RAGResult]:
        """Select the best representative from each cluster."""
        representatives = []
        
        for cluster_results in self.result_clusters.values():
            if not cluster_results:
                continue
            
            # Select the result with the highest relevance score
            best_result = max(
                cluster_results,
                key=lambda r: r.get_relevance_score() if r else 0.0
            )
            
            if best_result:
                representatives.append(best_result)
        
        return representatives
    
    def _advanced_ranking(self, results: List[RAGResult]) -> List[RAGResult]:
        """Advanced ranking considering multiple factors."""
        if not results:
            return []
        
        # Calculate composite scores
        scored_results = []
        for result in results:
            score = self._calculate_composite_score(result)
            scored_results.append((result, score))
        
        # Sort by composite score
        scored_results.sort(key=lambda x: x[1], reverse=True)
        
        # Return sorted results
        return [result for result, score in scored_results]
    
    def _calculate_composite_score(self, result: RAGResult) -> float:
        """Calculate composite score for ranking."""
        # Relevance score (40% weight)
        relevance_score = result.get_relevance_score() * 0.4
        
        # Result count score (20% weight)
        result_count_score = min(result.total_found / 10.0, 1.0) * 0.2
        
        # Execution time score (20% weight) - faster is better
        execution_time_score = 0.0
        if result.execution_time_ms:
            execution_time_score = max(0, 1.0 - (result.execution_time_ms / 1000.0)) * 0.2
        
        # Cache hit bonus (20% weight)
        cache_bonus = 0.2 if result.cache_hit else 0.0
        
        return relevance_score + result_count_score + execution_time_score + cache_bonus
    
    def _create_smart_aggregated_result(self, original_results: List[RAGResult], 
                                      ranked_results: List[RAGResult]) -> RAGResult:
        """Create the final aggregated result."""
        # Flatten results from ranked RAG results
        all_results = []
        for rag_result in ranked_results:
            if rag_result and rag_result.results:
                all_results.extend(rag_result.results)
        
        # Calculate metadata
        metadata = self._calculate_aggregation_metadata(
            original_results, 
            sum(r.total_found for r in original_results if r), 
            len(all_results),
            [r.execution_time_ms for r in original_results if r and r.execution_time_ms]
        )
        
        metadata.update({
            "aggregation_method": "smart_clustering",
            "clusters_created": len(self.result_clusters),
            "representatives_selected": len(ranked_results)
        })
        
        return RAGResult(
            query="[Smart Aggregated Results]",
            results=all_results,
            total_found=len(all_results),
            metadata=metadata
        )
