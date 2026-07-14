"""Advanced RAG system with advanced search, caching, and monitoring."""

import logging
import asyncio
import uuid
from typing import Dict, List, Any, Optional, Tuple, Union
from dataclasses import dataclass
from collections import defaultdict

from .rag_system import RAGSystem, OracleRAGSystem
from .advanced_search import AdvancedSearchEngine, SearchQuery, SearchStrategy
from .semantic_cache import SemanticCache, CacheConfig, CacheStrategy, CacheManager
from .performance_monitor import PerformanceMonitor, monitor_performance
from vector_db.rag_retriever import RAGRetriever
from vector_db.models import SearchResult, RetrievalResult
from .config import get_config_value

logger = logging.getLogger(__name__)


@dataclass
class AdvancedRAGConfig:
    """Configuration for advanced RAG system."""
    # Search configuration
    default_search_strategy: SearchStrategy = SearchStrategy.HYBRID
    enable_hybrid_search: bool = True
    enable_mmr: bool = True
    enable_semantic_search: bool = True
    
    # Caching configuration
    cache_enabled: bool = True
    cache_strategy: CacheStrategy = CacheStrategy.SEMANTIC
    cache_max_size: int = 1000
    cache_ttl_seconds: int = 3600
    cache_semantic_threshold: float = 0.8
    
    # Performance monitoring
    monitoring_enabled: bool = True
    performance_thresholds: Dict[str, float] = None
    
    # Advanced features
    enable_query_optimization: bool = True
    enable_result_reranking: bool = True
    enable_adaptive_search: bool = True
    
    def __post_init__(self):
        """Set default performance thresholds."""
        # Set default performance thresholds.
        if self.performance_thresholds is None:
            self.performance_thresholds = {
                'query_duration_warning': get_config_value('performance.query_duration_warning_ms', 1000),  # From config
                'query_duration_error': get_config_value('performance.query_duration_error_ms', 5000),      # From config
                'cache_hit_rate_minimum': get_config_value('performance.cache_hit_rate_minimum', 0.5),      # From config
                'relevance_threshold': get_config_value('rag.min_relevance_score', 0.5)  # From config
            }


class AdvancedRAGSystem(RAGSystem):
    """Advanced RAG system with advanced search, caching, and monitoring."""
    
    def __init__(self, rag_retriever: RAGRetriever, config: AdvancedRAGConfig = None):
        """Initialize advanced RAG system.
        
        Args:
            rag_retriever: Base RAG retriever
            config: Advanced RAG configuration
        """
        super().__init__(rag_retriever)
        self.config = config or AdvancedRAGConfig()
        
        # Initialize components
        self.search_engine = AdvancedSearchEngine(rag_retriever)
        self.cache_manager = CacheManager()
        self.performance_monitor = PerformanceMonitor() if self.config.monitoring_enabled else None
        
        # Initialize caches
        if self.config.cache_enabled:
            self._initialize_caches()
        
        # Initialize performance monitoring
        if self.performance_monitor:
            self._initialize_monitoring()
        
        logger.info("Advanced RAG system initialized")
    
    def _initialize_caches(self) -> None:
        """Initialize cache instances."""
        # Main search cache
        search_cache_config = CacheConfig(
            max_size=self.config.cache_max_size,
            strategy=self.config.cache_strategy,
            ttl_seconds=self.config.cache_ttl_seconds,
            semantic_threshold=self.config.cache_semantic_threshold
        )
        
        # Get embedding provider from retriever if available
        embedding_provider = getattr(self.rag_retriever, 'embedding_provider', None)
        
        self.search_cache = self.cache_manager.create_cache(
            "search_results", search_cache_config, embedding_provider
        )
        
        # Embedding cache for performance
        embedding_cache_config = CacheConfig(
            max_size=500,
            strategy=CacheStrategy.LRU,
            ttl_seconds=7200  # 2 hours
        )
        
        self.embedding_cache = self.cache_manager.create_cache(
            "embeddings", embedding_cache_config, embedding_provider
        )
        
        logger.info("Caches initialized")
    
    def _initialize_monitoring(self) -> None:
        """Initialize performance monitoring."""
        if not self.performance_monitor:
            return
        
        # Add alert handlers
        self.performance_monitor.add_alert_handler(self._handle_performance_alert)
        
        # Set custom thresholds
        for key, value in self.config.performance_thresholds.items():
            if hasattr(self.performance_monitor.thresholds, key):
                self.performance_monitor.thresholds[key] = value
        
        logger.info("Performance monitoring initialized")
    
    async def query(self, query_text: str, k: int = 4, 
                   strategy: Optional[SearchStrategy] = None,
                   filters: Optional[Dict[str, Any]] = None,
                   use_cache: bool = True) -> RetrievalResult:
        """Execute advanced RAG query.
        
        Args:
            query_text: Query text
            k: Number of results to return
            strategy: Search strategy to use
            filters: Search filters
            use_cache: Whether to use caching
            
        Returns:
            Retrieval result with search results
        """
        query_id = str(uuid.uuid4())
        search_strategy = strategy or self.config.default_search_strategy
        
        # Start performance monitoring
        if self.performance_monitor:
            self.performance_monitor.start_query_timer(query_id, query_text, search_strategy.value)
        
        try:
            # Check cache first
            if use_cache and self.config.cache_enabled:
                cached_result = await self.search_cache.get(query_text)
                if cached_result:
                    logger.info(f"Cache hit for query: {query_text[:50]}...")
                    return cached_result
            
            # Execute search
            search_query = SearchQuery(
                text=query_text,
                strategy=search_strategy,
                filters=filters,
                k=k
            )
            
            results, metrics = await self.search_engine.search(search_query)
            
            # Apply result optimization
            if self.config.enable_result_reranking:
                results = await self._rerank_results(results, query_text)
            
            # Create retrieval result
            retrieval_result = RetrievalResult(
                query=query_text,
                results=results,
                metadata={
                    'strategy': search_strategy.value,
                    'query_id': query_id,
                    'search_metrics': metrics,
                    'cache_used': False
                }
            )
            
            # Cache results
            if use_cache and self.config.cache_enabled:
                await self.search_cache.set(query_text, retrieval_result)
            
            # Update metadata
            retrieval_result.metadata['cache_used'] = use_cache and self.config.cache_enabled
            
            return retrieval_result
            
        except Exception as e:
            logger.error(f"Advanced RAG query failed: {e}")
            # Return empty result on error
            return RetrievalResult(
                query=query_text,
                results=[],
                metadata={
                    'error': str(e),
                    'strategy': search_strategy.value,
                    'query_id': query_id
                }
            )
        
        finally:
            # Stop performance monitoring
            if self.performance_monitor:
                results_count = len(results) if 'results' in locals() else 0
                relevance_scores = [r.relevance_score for r in results] if 'results' in locals() else []
                duration = self.performance_monitor.stop_query_timer(
                    query_id, results_count, relevance_scores
                )
                logger.debug(f"Query completed in {duration:.2f}ms")
    
    async def _rerank_results(self, results: List[SearchResult], query: str) -> List[SearchResult]:
        """Rerank search results for better relevance."""
        if not results:
            return results
        
        try:
            # Calculate additional relevance scores
            reranked_results = []
            
            for result in results:
                # Combine multiple relevance factors
                base_score = result.relevance_score
                
                # Content length bonus (longer content often more informative)
                length_bonus = min(len(result.prompt or "") / 1000, 0.2) if result.prompt else 0
                
                # Recency bonus (if available in metadata)
                recency_bonus = 0
                if result.metadata and 'timestamp' in result.metadata:
                    # Simple recency calculation
                    recency_bonus = min(0.1, 0.1)  # Max 10% bonus
                
                # Combined score
                combined_score = min(1.0, base_score + length_bonus + recency_bonus)
                
                # Create reranked result
                reranked_result = SearchResult(
                    id=result.id,
                    prompt=result.prompt,
                    corrected_answer=result.corrected_answer,
                    distance=1.0 - combined_score,  # Convert score to distance
                    metadata={
                        **(result.metadata or {}),
                        'original_score': base_score,
                        'reranked_score': combined_score,
                        'length_bonus': length_bonus,
                        'recency_bonus': recency_bonus
                    }
                )
                
                reranked_results.append(reranked_result)
            
            # Sort by reranked score
            reranked_results.sort(key=lambda x: x.relevance_score, reverse=True)
            
            return reranked_results
            
        except Exception as e:
            logger.warning(f"Result reranking failed: {e}")
            return results
    
    async def validate(self, content: str, context: RetrievalResult) -> Dict[str, Any]:
        """Validate content using advanced validation."""
        if not self.performance_monitor:
            return await super().validate(content, context)
        
        # Use performance monitoring decorator
        @monitor_performance(self.performance_monitor, "rag_validation_duration")
        async def _validate_with_monitoring():
            return await super().validate(content, context)
        
        return await _validate_with_monitoring()
    
    async def enhance(self, content: str, patterns: Optional[List[str]] = None) -> str:
        """Enhance content using advanced enhancement."""
        if not self.performance_monitor:
            return await super().enhance(content, patterns)
        
        # Use performance monitoring decorator
        @monitor_performance(self.performance_monitor, "rag_enhancement_duration")
        async def _enhance_with_monitoring():
            return await super().enhance(content, patterns)
        
        return await _enhance_with_monitoring()
    
    async def learn(self, feedback: Any) -> bool:
        """Learn from feedback using advanced learning."""
        if not self.performance_monitor:
            return await super().learn(feedback)
        
        # Use performance monitoring decorator
        @monitor_performance(self.performance_monitor, "rag_learning_duration")
        async def _learn_with_monitoring():
            return await super().learn(feedback)
        
        return await _learn_with_monitoring()
    
    async def get_system_info(self) -> Dict[str, Any]:
        """Get advanced system information."""
        base_info = await super().get_system_info()
        
        advanced_info = {
            'phase': 'Phase 4 - Advanced Features & Performance',
            'advanced_features': {
                'hybrid_search': self.config.enable_hybrid_search,
                'mmr_search': self.config.enable_mmr,
                'semantic_search': self.config.enable_semantic_search,
                'query_optimization': self.config.enable_query_optimization,
                'result_reranking': self.config.enable_result_reranking,
                'adaptive_search': self.config.enable_adaptive_search
            },
            'caching': {
                'enabled': self.config.cache_enabled,
                'strategy': self.config.cache_strategy.value,
                'max_size': self.config.cache_max_size,
                'ttl_seconds': self.config.cache_ttl_seconds
            },
            'monitoring': {
                'enabled': self.config.monitoring_enabled,
                'thresholds': self.config.performance_thresholds
            }
        }
        
        # Add cache statistics
        if self.config.cache_enabled:
            advanced_info['cache_stats'] = {
                'search_cache': self.search_cache.get_stats() if hasattr(self, 'search_cache') else {},
                'embedding_cache': self.embedding_cache.get_stats() if hasattr(self, 'embedding_cache') else {}
            }
        
        # Add performance metrics
        if self.performance_monitor:
            advanced_info['performance'] = self.performance_monitor.get_performance_summary()
        
        return {**base_info, **advanced_info}
    
    def _handle_performance_alert(self, alert_type: str, alert_data: Dict[str, Any]) -> None:
        """Handle performance alerts."""
        logger.warning(f"Performance alert: {alert_type} - {alert_data}")
        
        # Implement alert handling logic here
        # This could include:
        # - Sending notifications
        # - Scaling resources
        # - Switching to fallback strategies
        # - Logging to external monitoring systems
        
        if alert_type == "query_duration_error":
            # Consider switching to faster search strategy
            logger.warning("Query duration exceeded error threshold, consider optimization")
        
        elif alert_type == "cache_hit_rate_low":
            # Consider cache warming or optimization
            logger.warning("Cache hit rate below threshold, consider cache optimization")
    
    async def optimize_query(self, query: str) -> str:
        """Optimize query for better search performance."""
        if not self.config.enable_query_optimization:
            return query
        
        try:
            # Simple query optimization
            # In production, this could use NLP techniques
            
            # Remove unnecessary words
            stop_words = {'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for', 'of', 'with', 'by'}
            words = query.lower().split()
            optimized_words = [word for word in words if word not in stop_words and len(word) > 2]
            
            # Limit query length
            if len(optimized_words) > 10:
                optimized_words = optimized_words[:10]
            
            optimized_query = " ".join(optimized_words)
            
            if optimized_query != query:
                logger.debug(f"Query optimized: '{query}' -> '{optimized_query}'")
            
            return optimized_query
            
        except Exception as e:
            logger.warning(f"Query optimization failed: {e}")
            return query
    
    async def get_search_strategies(self) -> List[SearchStrategy]:
        """Get available search strategies."""
        return self.search_engine.get_search_strategies()
    
    async def get_cache_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        if not self.config.cache_enabled:
            return {'caching_disabled': True}
        
        return {
            'search_cache': self.search_cache.get_stats() if hasattr(self, 'search_cache') else {},
            'embedding_cache': self.embedding_cache.get_stats() if hasattr(self, 'embedding_cache') else {},
            'all_caches': self.cache_manager.get_all_stats()
        }
    
    async def clear_caches(self) -> None:
        """Clear all caches."""
        if not self.config.cache_enabled:
            return
        
        await self.cache_manager.clear_all_caches()
        logger.info("All caches cleared")
    
    async def get_performance_metrics(self, format: str = "json") -> str:
        """Get performance metrics in specified format."""
        if not self.performance_monitor:
            return "Performance monitoring not enabled"
        
        return self.performance_monitor.export_metrics(format)
    
    async def clear_performance_metrics(self) -> None:
        """Clear performance metrics."""
        if not self.performance_monitor:
            return
        
        self.performance_monitor.clear_metrics()
        logger.info("Performance metrics cleared")


class AdaptiveRAGSystem(AdvancedRAGSystem):
    """Adaptive RAG system that automatically adjusts strategies based on performance."""
    
    def __init__(self, rag_retriever: RAGRetriever, config: AdvancedRAGConfig = None):
        """Initialize adaptive RAG system."""
        super().__init__(rag_retriever, config)
        self.performance_history: List[Dict[str, Any]] = []
        self.strategy_performance: Dict[str, List[float]] = defaultdict(list)
        
        # Start adaptation task
        if self.config.enable_adaptive_search:
            asyncio.create_task(self._adaptation_task())
    
    async def query(self, query_text: str, k: int = 4, 
                   strategy: Optional[SearchStrategy] = None,
                   filters: Optional[Dict[str, Any]] = None,
                   use_cache: bool = True) -> RetrievalResult:
        """Execute adaptive RAG query."""
        # Auto-select strategy if none specified
        if not strategy and self.config.enable_adaptive_search:
            strategy = await self._select_optimal_strategy(query_text)
        
        # Execute query
        result = await super().query(query_text, k, strategy, filters, use_cache)
        
        # Record performance for adaptation
        if self.performance_monitor and strategy:
            await self._record_strategy_performance(strategy.value, result)
        
        return result
    
    async def _select_optimal_strategy(self, query: str) -> SearchStrategy:
        """Select optimal search strategy based on query and performance history."""
        try:
            # Simple strategy selection based on query characteristics
            # In production, this could use ML models
            
            query_lower = query.lower()
            
            # Keyword-heavy queries -> keyword matching
            if len(query.split()) <= 3:
                return SearchStrategy.KEYWORD_MATCHING
            
            # Technical queries -> semantic search
            technical_terms = ['sql', 'database', 'performance', 'optimization', 'query']
            if any(term in query_lower for term in technical_terms):
                return SearchStrategy.SEMANTIC_SEARCH
            
            # General queries -> hybrid search
            return SearchStrategy.HYBRID
            
        except Exception as e:
            logger.warning(f"Strategy selection failed: {e}")
            return SearchStrategy.HYBRID
    
    async def _record_strategy_performance(self, strategy: str, result: RetrievalResult) -> None:
        """Record performance metrics for strategy adaptation."""
        try:
            # Extract performance metrics
            metadata = result.metadata or {}
            search_metrics = metadata.get('search_metrics', {})
            
            # Record query duration
            if 'query_time' in search_metrics:
                self.strategy_performance[strategy].append(search_metrics['query_time'])
                
                # Keep only recent performance data
                if len(self.strategy_performance[strategy]) > 100:
                    self.strategy_performance[strategy] = self.strategy_performance[strategy][-100:]
            
            # Record result quality
            if result.results:
                avg_relevance = sum(r.relevance_score for r in result.results) / len(result.results)
                self.strategy_performance[f"{strategy}_quality"].append(avg_relevance)
                
                if len(self.strategy_performance[f"{strategy}_quality"]) > 100:
                    self.strategy_performance[f"{strategy}_quality"] = self.strategy_performance[f"{strategy}_quality"][-100:]
        
        except Exception as e:
            logger.warning(f"Failed to record strategy performance: {e}")
    
    async def _adaptation_task(self) -> None:
        """Background task for strategy adaptation."""
        while True:
            try:
                await asyncio.sleep(300)  # Adapt every 5 minutes
                await self._adapt_strategies()
            except Exception as e:
                logger.error(f"Strategy adaptation failed: {e}")
                await asyncio.sleep(60)
    
    async def _adapt_strategies(self) -> None:
        """Adapt search strategies based on performance."""
        try:
            # Analyze strategy performance
            strategy_analysis = {}
            
            for strategy in self.strategy_performance:
                if strategy.endswith('_quality'):
                    continue
                
                if strategy in self.strategy_performance:
                    durations = self.strategy_performance[strategy]
                    quality_key = f"{strategy}_quality"
                    qualities = self.strategy_performance.get(quality_key, [])
                    
                    if durations and qualities:
                        avg_duration = sum(durations) / len(durations)
                        avg_quality = sum(qualities) / len(qualities)
                        
                        # Calculate performance score (lower duration, higher quality = better)
                        performance_score = avg_quality / (avg_duration + 1)  # Avoid division by zero
                        
                        strategy_analysis[strategy] = {
                            'avg_duration': avg_duration,
                            'avg_quality': avg_quality,
                            'performance_score': performance_score
                        }
            
            # Log adaptation insights
            if strategy_analysis:
                best_strategy = max(strategy_analysis.items(), key=lambda x: x[1]['performance_score'])
                logger.info(f"Strategy adaptation: {best_strategy[0]} performs best "
                          f"(score: {best_strategy[1]['performance_score']:.4f})")
        
        except Exception as e:
            logger.error(f"Strategy adaptation analysis failed: {e}")
    
    async def get_adaptation_insights(self) -> Dict[str, Any]:
        """Get insights from strategy adaptation."""
        insights = {
            'strategy_performance': {},
            'recommendations': []
        }
        
        for strategy, metrics in self.strategy_performance.items():
            if not strategy.endswith('_quality') and metrics:
                avg_duration = sum(metrics) / len(metrics)
                insights['strategy_performance'][strategy] = {
                    'avg_duration_ms': avg_duration,
                    'sample_count': len(metrics)
                }
        
        # Generate recommendations
        if insights['strategy_performance']:
            fastest_strategy = min(insights['strategy_performance'].items(), 
                                 key=lambda x: x[1]['avg_duration_ms'])
            insights['recommendations'].append(
                f"Consider using {fastest_strategy[0]} for fastest response times "
                f"({fastest_strategy[1]['avg_duration_ms']:.2f}ms average)"
            )
        
        return insights
