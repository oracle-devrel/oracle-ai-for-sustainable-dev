"""Advanced search algorithms for RAG system."""

import logging
import asyncio
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional, Tuple, Union
from dataclasses import dataclass
from enum import Enum
import numpy as np
from concurrent.futures import ThreadPoolExecutor

from vector_db.models import SearchResult
from vector_db.rag_retriever import RAGRetriever, RetrievalResult
from .config import get_config_value

logger = logging.getLogger(__name__)


class SearchStrategy(Enum):
    """Search strategy types."""
    VECTOR_SIMILARITY = "vector_similarity"
    KEYWORD_MATCHING = "keyword_matching"
    SEMANTIC_SEARCH = "semantic_search"
    HYBRID = "hybrid"
    MMR = "mmr"  # Maximal Marginal Relevance


@dataclass
class SearchQuery:
    """Search query with metadata."""
    text: str
    strategy: SearchStrategy = SearchStrategy.HYBRID
    filters: Optional[Dict[str, Any]] = None
    k: int = 4
    alpha: float = 0.5  # Weight for hybrid search
    diversity_threshold: float = 0.3  # For MMR
    semantic_threshold: float = get_config_value('rag.min_relevance_score', 0.5)  # From config


@dataclass
class SearchMetrics:
    """Search performance metrics."""
    query_time: float
    vector_search_time: float
    keyword_search_time: float
    semantic_search_time: float
    total_results: int
    filtered_results: int
    cache_hits: int
    cache_misses: int


class SearchAlgorithm(ABC):
    """Abstract base class for search algorithms."""
    
    @abstractmethod
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute search algorithm.
        
        Args:
            query: Search query
            context: Additional search context
            
        Returns:
            List of search results
        """
        pass
    
    @abstractmethod
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        pass


class VectorSimilaritySearch(SearchAlgorithm):
    """Vector similarity search using cosine distance."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize vector similarity search.
        
        Args:
            rag_retriever: RAG retriever for vector operations
        """
        self.rag_retriever = rag_retriever
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute vector similarity search."""
        try:
            # Use the existing RAG retriever for vector search
            retrieval_result = await self.rag_retriever.retrieve(
                query=query.text,
                k=query.k,
                filters=query.filters
            )
            
            return retrieval_result.results
            
        except Exception as e:
            logger.error(f"Vector similarity search failed: {e}")
            return []
    
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        return "vector_similarity"


class KeywordMatchingSearch(SearchAlgorithm):
    """Keyword-based search using TF-IDF and exact matching."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize keyword matching search.
        
        Args:
            rag_retriever: RAG retriever for text operations
        """
        self.rag_retriever = rag_retriever
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute keyword matching search."""
        try:
            # Extract keywords from query
            keywords = self._extract_keywords(query.text)
            
            # Use RAG retriever with keyword filters
            retrieval_result = await self.rag_retriever.retrieve(
                query=" ".join(keywords),
                k=query.k * 2,  # Get more results for filtering
                filters=query.filters
            )
            
            # Filter by keyword relevance
            keyword_results = self._filter_by_keywords(retrieval_result.results, keywords)
            
            return keyword_results[:query.k]
            
        except Exception as e:
            logger.error(f"Keyword matching search failed: {e}")
            return []
    
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        return "keyword_matching"
    
    def _extract_keywords(self, text: str) -> List[str]:
        """Extract meaningful keywords from text."""
        # Simple keyword extraction - in production, use NLP libraries
        words = text.lower().split()
        # Filter out common stop words and short words
        stop_words = {'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by'}
        keywords = [word for word in words if len(word) > 2 and word not in stop_words]
        return keywords[:10]  # Limit to top 10 keywords
    
    def _filter_by_keywords(self, results: List[SearchResult], keywords: List[str]) -> List[SearchResult]:
        """Filter results by keyword relevance."""
        keyword_scores = []
        
        for result in results:
            if result.prompt:
                # Calculate keyword match score
                text_words = result.prompt.lower().split()
                matches = sum(1 for keyword in keywords if keyword.lower() in text_words)
                score = matches / len(keywords) if keywords else 0
                if score > 0:  # Only include results with some keyword matches
                    keyword_scores.append((result, score))
        
        # Sort by keyword relevance
        keyword_scores.sort(key=lambda x: x[1], reverse=True)
        return [result for result, score in keyword_scores]


class SemanticSearch(SearchAlgorithm):
    """Semantic search using contextual understanding."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize semantic search.
        
        Args:
            rag_retriever: RAG retriever for semantic operations
        """
        self.rag_retriever = rag_retriever
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute semantic search."""
        try:
            # Get initial results
            retrieval_result = await self.rag_retriever.retrieve(
                query=query.text,
                k=query.k * 2,  # Get more results for semantic filtering
                filters=query.filters
            )
            
            # Apply semantic filtering
            semantic_results = await self._apply_semantic_filtering(
                retrieval_result.results, query.text, query.semantic_threshold
            )
            
            return semantic_results[:query.k]
            
        except Exception as e:
            logger.error(f"Semantic search failed: {e}")
            return []
    
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        return "semantic_search"
    
    async def _apply_semantic_filtering(self, results: List[SearchResult], query: str, threshold: float) -> List[SearchResult]:
        """Apply semantic filtering to results."""
        semantic_scores = []
        
        for result in results:
            if result.prompt:
                # Calculate semantic similarity using content analysis
                semantic_score = self._calculate_semantic_similarity(query, result.prompt)
                if semantic_score >= threshold:
                    semantic_scores.append((result, semantic_score))
        
        # Sort by semantic relevance
        semantic_scores.sort(key=lambda x: x[1], reverse=True)
        return [result for result, score in semantic_scores]
    
    def _calculate_semantic_similarity(self, query: str, content: str) -> float:
        """Calculate semantic similarity between query and content."""
        # Simple semantic similarity using word overlap
        # In production, use more sophisticated NLP techniques
        
        query_words = set(query.lower().split())
        content_words = set(content.lower().split())
        
        # Jaccard similarity
        intersection = len(query_words & content_words)
        union = len(query_words | content_words)
        
        if union == 0:
            return 0.0
        
        # If no word overlap, similarity is 0
        if intersection == 0:
            return 0.0
        
        # Boost score for longer content matches
        length_boost = min(len(content_words) / 100, 0.3)  # Max 30% boost
        
        base_similarity = intersection / union
        return min(base_similarity + length_boost, 1.0)


class MaximalMarginalRelevanceSearch(SearchAlgorithm):
    """Maximal Marginal Relevance search for diverse results."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize MMR search.
        
        Args:
            rag_retriever: RAG retriever for base search
        """
        self.rag_retriever = rag_retriever
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute MMR search."""
        try:
            # Get initial results
            initial_results = await self.rag_retriever.retrieve(
                query=query.text,
                k=query.k * 3,  # Get more results for MMR selection
                filters=query.filters
            )
            
            # Apply MMR algorithm
            mmr_results = self._apply_mmr_algorithm(
                initial_results.results, query.text, query.k, query.diversity_threshold
            )
            
            return mmr_results
            
        except Exception as e:
            logger.error(f"MMR search failed: {e}")
            return []
    
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        return "mmr"
    
    def _apply_mmr_algorithm(self, results: List[SearchResult], query: str, k: int, diversity_threshold: float) -> List[SearchResult]:
        """Apply Maximal Marginal Relevance algorithm."""
        if not results:
            return []
        
        # Start with the most relevant result
        selected = [results[0]]
        remaining = results[1:]
        
        while len(selected) < k and remaining:
            mmr_scores = []
            
            for result in remaining:
                # Calculate MMR score: relevance - diversity penalty
                relevance = result.relevance_score
                diversity = self._calculate_diversity_penalty(result, selected, diversity_threshold)
                mmr_score = relevance - diversity
                mmr_scores.append((result, mmr_score))
            
            # Select result with highest MMR score
            if mmr_scores:
                best_result = max(mmr_scores, key=lambda x: x[1])[0]
                selected.append(best_result)
                remaining.remove(best_result)
            else:
                break
        
        return selected[:k]
    
    def _calculate_diversity_penalty(self, candidate: SearchResult, selected: List[SearchResult], threshold: float) -> float:
        """Calculate diversity penalty for MMR."""
        if not selected:
            return 0.0
        
        # Calculate average similarity to already selected results
        similarities = []
        for selected_result in selected:
            if candidate.prompt and selected_result.prompt:
                similarity = self._calculate_content_similarity(candidate.prompt, selected_result.prompt)
                similarities.append(similarity)
        
        if similarities:
            avg_similarity = sum(similarities) / len(similarities)
            # Apply threshold: if similarity is below threshold, reduce penalty
            if avg_similarity < threshold:
                return avg_similarity * 0.5  # Reduced penalty for diverse content
            else:
                return avg_similarity  # Full penalty for similar content
        
        return 0.0
    
    def _calculate_content_similarity(self, text1: str, text2: str) -> float:
        """Calculate content similarity between two texts."""
        # Simple similarity using word overlap
        words1 = set(text1.lower().split())
        words2 = set(text2.lower().split())
        
        intersection = len(words1 & words2)
        union = len(words1 | words2)
        
        return intersection / union if union > 0 else 0.0


class HybridSearch(SearchAlgorithm):
    """Hybrid search combining multiple algorithms."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize hybrid search.
        
        Args:
            rag_retriever: RAG retriever for base operations
        """
        self.rag_retriever = rag_retriever
        self.vector_search = VectorSimilaritySearch(rag_retriever)
        self.keyword_search = KeywordMatchingSearch(rag_retriever)
        self.semantic_search = SemanticSearch(rag_retriever)
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> List[SearchResult]:
        """Execute hybrid search."""
        try:
            # Execute all search algorithms concurrently
            tasks = [
                self.vector_search.search(query, context),
                self.keyword_search.search(query, context),
                self.semantic_search.search(query, context)
            ]
            
            results = await asyncio.gather(*tasks, return_exceptions=True)
            
            # Combine and rank results
            combined_results = self._combine_results(results, query.alpha)
            
            return combined_results[:query.k]
            
        except Exception as e:
            logger.error(f"Hybrid search failed: {e}")
            return []
    
    def get_algorithm_name(self) -> str:
        """Get algorithm name."""
        return "hybrid"
    
    def _combine_results(self, results: List[Union[List[SearchResult], Exception]], alpha: float) -> List[SearchResult]:
        """Combine results from multiple search algorithms."""
        combined = []
        result_scores = {}
        
        # Process results from each algorithm
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.warning(f"Search algorithm {i} failed: {result}")
                continue
            
            for search_result in result:
                if search_result.record_id not in result_scores:
                    result_scores[search_result.record_id] = {
                        'result': search_result,
                        'scores': [],
                        'count': 0
                    }
                
                # Add score from this algorithm
                result_scores[search_result.record_id]['scores'].append(search_result.relevance_score)
                result_scores[search_result.record_id]['count'] += 1
        
        # Calculate combined scores
        for result_id, data in result_scores.items():
            if data['count'] > 0:
                # Weighted average of scores
                avg_score = sum(data['scores']) / len(data['scores'])
                # Boost for results found by multiple algorithms
                diversity_boost = min(data['count'] * 0.1, 0.3)  # Max 30% boost
                combined_score = avg_score + diversity_boost
                
                # Create new result with combined score
                combined_result = SearchResult(
                    record_id=data['result'].record_id,
                    prompt=data['result'].prompt,
                    corrected_answer=data['result'].corrected_answer,
                    context=data['result'].context,
                    tags=data['result'].tags,
                    distance=1.0 - combined_score,  # Convert score to distance
                    metadata=data['result'].metadata
                )
                
                combined.append((combined_result, combined_score))
        
        # Sort by combined score
        combined.sort(key=lambda x: x[1], reverse=True)
        return [result for result, score in combined]


class AdvancedSearchEngine:
    """Advanced search engine orchestrating multiple search algorithms."""
    
    def __init__(self, rag_retriever: RAGRetriever):
        """Initialize advanced search engine.
        
        Args:
            rag_retriever: RAG retriever for base operations
        """
        self.rag_retriever = rag_retriever
        self.algorithms = {
            SearchStrategy.VECTOR_SIMILARITY: VectorSimilaritySearch(rag_retriever),
            SearchStrategy.KEYWORD_MATCHING: KeywordMatchingSearch(rag_retriever),
            SearchStrategy.SEMANTIC_SEARCH: SemanticSearch(rag_retriever),
            SearchStrategy.HYBRID: HybridSearch(rag_retriever),
            SearchStrategy.MMR: MaximalMarginalRelevanceSearch(rag_retriever)
        }
        
        # Performance metrics
        self.metrics: Dict[str, SearchMetrics] = {}
    
    async def search(self, query: SearchQuery, context: Optional[Dict[str, Any]] = None) -> Tuple[List[SearchResult], SearchMetrics]:
        """Execute search using specified strategy.
        
        Args:
            query: Search query with strategy
            context: Additional search context
            
        Returns:
            Tuple of (search results, performance metrics)
        """
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Get algorithm for the specified strategy
            algorithm = self.algorithms.get(query.strategy)
            if not algorithm:
                logger.warning(f"Unknown search strategy: {query.strategy}, falling back to hybrid")
                algorithm = self.algorithms[SearchStrategy.HYBRID]
            
            # Execute search
            results = await algorithm.search(query, context)
            
            # Calculate metrics
            end_time = asyncio.get_event_loop().time()
            metrics = SearchMetrics(
                query_time=end_time - start_time,
                vector_search_time=0.0,  # Will be updated by individual algorithms
                keyword_search_time=0.0,
                semantic_search_time=0.0,
                total_results=len(results),
                filtered_results=len(results),
                cache_hits=0,
                cache_misses=1
            )
            
            # Store metrics
            self.metrics[query.text] = metrics
            
            return results, metrics
            
        except Exception as e:
            logger.error(f"Advanced search failed: {e}")
            end_time = asyncio.get_event_loop().time()
            metrics = SearchMetrics(
                query_time=end_time - start_time,
                vector_search_time=0.0,
                keyword_search_time=0.0,
                semantic_search_time=0.0,
                total_results=0,
                filtered_results=0,
                cache_hits=0,
                cache_misses=1
            )
            return [], metrics
    
    def get_search_strategies(self) -> List[SearchStrategy]:
        """Get available search strategies."""
        return list(self.algorithms.keys())
    
    def get_performance_metrics(self) -> Dict[str, SearchMetrics]:
        """Get performance metrics for all searches."""
        return self.metrics.copy()
    
    def clear_metrics(self) -> None:
        """Clear performance metrics."""
        self.metrics.clear()
