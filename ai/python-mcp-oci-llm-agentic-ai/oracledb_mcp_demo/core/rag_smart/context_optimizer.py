"""RAG Context Optimizer - Optimizes context retrieval strategies."""

import logging
from typing import Dict, Any, List
from .models import ContextRequest, ContextOptimization, RetrievalStrategy

logger = logging.getLogger(__name__)


class RAGContextOptimizer:
    """Optimizes context retrieval based on analysis and predictions."""
    
    def __init__(self):
        """Initialize context optimizer."""
        self.retrieval_thresholds = self._initialize_thresholds()
    
    def optimize_context_retrieval(self, context_request: ContextRequest) -> ContextOptimization:
        """Optimize context retrieval strategy."""
        # Check if we should retrieve at all
        should_retrieve = self._should_retrieve_context(context_request)
        
        if not should_retrieve:
            return ContextOptimization(
                should_retrieve=False,
                retrieval_strategy=RetrievalStrategy.CACHED,
                optimized_contexts=[],
                efficiency_gain=1.0,
                cost_benefit_ratio=float('inf'),
                reasoning="Using existing context, no retrieval needed"
            )
        
        # Determine retrieval strategy
        strategy = self._determine_retrieval_strategy(context_request)
        
        # Optimize context list
        optimized_contexts = self._optimize_context_list(context_request)
        
        # Calculate efficiency metrics
        efficiency_gain = self._calculate_efficiency_gain(context_request, optimized_contexts)
        cost_benefit_ratio = self._calculate_cost_benefit_ratio(context_request, optimized_contexts)
        
        # Check for priority boost
        priority_boost = self._should_priority_boost(context_request)
        
        # Generate reasoning
        reasoning = self._generate_optimization_reasoning(
            context_request, strategy, optimized_contexts, efficiency_gain
        )
        
        logger.debug(f"Context optimization: strategy={strategy.value}, contexts={len(optimized_contexts)}")
        
        return ContextOptimization(
            should_retrieve=should_retrieve,
            retrieval_strategy=strategy,
            optimized_contexts=optimized_contexts,
            efficiency_gain=efficiency_gain,
            cost_benefit_ratio=cost_benefit_ratio,
            priority_boost=priority_boost,
            reasoning=reasoning
        )
    
    def _initialize_thresholds(self) -> Dict[str, float]:
        """Initialize optimization thresholds."""
        return {
            "min_context_priority": 0.3,
            "min_prediction_confidence": 0.5,
            "efficiency_threshold": 0.2,
            "cost_benefit_threshold": 1.5,
            "priority_boost_threshold": 0.7
        }
    
    def _should_retrieve_context(self, request: ContextRequest) -> bool:
        """Determine if context should be retrieved."""
        # High priority requests should always retrieve
        if request.is_high_priority():
            return True
        
        # Check if we have required contexts
        if not request.required_contexts:
            return False
        
        # Check if existing context covers requirements
        missing_contexts = request.get_missing_contexts()
        if not missing_contexts:
            return False
        
        # Check context priority threshold
        if hasattr(request, 'context_priority'):
            if request.context_priority < self.retrieval_thresholds["min_context_priority"]:
                return False
        
        return True
    
    def _determine_retrieval_strategy(self, request: ContextRequest) -> RetrievalStrategy:
        """Determine the best retrieval strategy."""
        missing_contexts = request.get_missing_contexts()
        existing_coverage = len(request.existing_context) / max(len(request.required_contexts), 1)
        
        if existing_coverage >= 0.8:
            return RetrievalStrategy.PARTIAL
        elif request.is_high_priority():
            return RetrievalStrategy.FULL
        elif len(missing_contexts) <= 2:
            return RetrievalStrategy.PARTIAL
        else:
            return RetrievalStrategy.FULL
    
    def _optimize_context_list(self, request: ContextRequest) -> List[str]:
        """Optimize the list of contexts to retrieve."""
        missing_contexts = request.get_missing_contexts()
        
        if not missing_contexts:
            return []
        
        # Prioritize contexts based on importance
        prioritized_contexts = self._prioritize_contexts(missing_contexts, request)
        
        # For partial retrieval, limit to top contexts
        if len(missing_contexts) <= 2:
            max_contexts = min(len(prioritized_contexts), 3)
            return prioritized_contexts[:max_contexts]
        
        return prioritized_contexts
    
    def _prioritize_contexts(self, contexts: List[str], request: ContextRequest) -> List[str]:
        """Prioritize contexts by importance."""
        # Define context priorities
        context_priorities = {
            "troubleshooting": 1,
            "performance_analysis": 2,
            "sql_optimization": 3,
            "indexing": 4,
            "monitoring": 5,
            "testing": 6,
            "best_practices": 7,
            "oracle_specific": 8
        }
        
        # Sort by priority (lower number = higher priority)
        sorted_contexts = sorted(
            contexts,
            key=lambda x: context_priorities.get(x, 999)
        )
        
        return sorted_contexts
    
    def _calculate_efficiency_gain(self, request: ContextRequest, optimized_contexts: List[str]) -> float:
        """Calculate efficiency gain from optimization."""
        if not request.required_contexts:
            return 0.0
        
        # Calculate what we're saving
        total_required = len(request.required_contexts)
        total_retrieving = len(optimized_contexts)
        
        if total_required == 0:
            return 0.0
        
        # Efficiency gain = (total - retrieving) / total
        efficiency_gain = (total_required - total_retrieving) / total_required
        
        return max(efficiency_gain, 0.0)
    
    def _calculate_cost_benefit_ratio(self, request: ContextRequest, optimized_contexts: List[str]) -> float:
        """Calculate cost-benefit ratio of context retrieval."""
        if not optimized_contexts:
            return float('inf')
        
        # Estimate benefit (context coverage)
        benefit = len(optimized_contexts) / max(len(request.required_contexts), 1)
        
        # Estimate cost (context size and complexity)
        cost = request.estimated_cost * len(optimized_contexts)
        
        if cost == 0:
            return float('inf')
        
        return benefit / cost
    
    def _should_priority_boost(self, request: ContextRequest) -> bool:
        """Determine if request should get priority boost."""
        if request.is_high_priority():
            return True
        
        # Check if context priority is high
        if hasattr(request, 'context_priority'):
            if request.context_priority >= self.retrieval_thresholds["priority_boost_threshold"]:
                return True
        
        return False
    
    def _generate_optimization_reasoning(self, request: ContextRequest, strategy: RetrievalStrategy,
                                       optimized_contexts: List[str], efficiency_gain: float) -> str:
        """Generate human-readable reasoning for optimization."""
        reasoning_parts = []
        
        if strategy == RetrievalStrategy.CACHED:
            reasoning_parts.append("Using existing context, no retrieval needed")
        elif strategy == RetrievalStrategy.PARTIAL:
            reasoning_parts.append(f"Partial retrieval: {len(optimized_contexts)} contexts")
        else:
            reasoning_parts.append(f"Full retrieval: {len(optimized_contexts)} contexts")
        
        if efficiency_gain > 0:
            reasoning_parts.append(f"Efficiency gain: {efficiency_gain:.1%}")
        
        if request.is_high_priority():
            reasoning_parts.append("High priority request")
        
        return ". ".join(reasoning_parts) + "."
