"""Data models for Smart RAG Context Retrieval system."""

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional
from enum import Enum


class ContextPriority(Enum):
    """Context retrieval priority levels."""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class RetrievalStrategy(Enum):
    """Context retrieval strategies."""
    FULL = "full"           # Retrieve all required context
    PARTIAL = "partial"     # Retrieve only missing context
    CACHED = "cached"       # Use existing context only
    PREDICTIVE = "predictive"  # Prefetch predicted context


@dataclass
class ContextRequest:
    """Request for context analysis and optimization."""
    prompt: str
    required_contexts: List[str] = field(default_factory=list)
    predicted_contexts: List[str] = field(default_factory=list)
    existing_context: Dict[str, Any] = field(default_factory=dict)
    priority: str = ContextPriority.MEDIUM.value
    estimated_cost: float = 1.0
    user_id: Optional[str] = None
    session_id: Optional[str] = None
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    
    def get_total_contexts(self) -> List[str]:
        """Get all required and predicted contexts."""
        all_contexts = set(self.required_contexts + self.predicted_contexts)
        return list(all_contexts)
    
    def get_missing_contexts(self) -> List[str]:
        """Get contexts that are not in existing context."""
        existing_keys = set(self.existing_context.keys())
        required_keys = set(self.required_contexts)
        return list(required_keys - existing_keys)
    
    def is_high_priority(self) -> bool:
        """Check if request is high priority."""
        return self.priority in [ContextPriority.HIGH.value, ContextPriority.CRITICAL.value]


@dataclass
class ContextResponse:
    """Response from context analysis and optimization."""
    request: ContextRequest
    should_retrieve: bool
    retrieval_strategy: RetrievalStrategy
    optimized_contexts: List[str] = field(default_factory=list)
    existing_context_usage: Dict[str, Any] = field(default_factory=dict)
    efficiency_gain: float = 0.0
    cost_benefit_ratio: float = 1.0
    priority_boost: bool = False
    reasoning: str = ""
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def get_retrieval_summary(self) -> Dict[str, Any]:
        """Get summary of retrieval decision."""
        return {
            "should_retrieve": self.should_retrieve,
            "strategy": self.retrieval_strategy.value,
            "contexts_to_retrieve": self.optimized_contexts,
            "efficiency_gain": self.efficiency_gain,
            "cost_benefit_ratio": self.cost_benefit_ratio,
            "priority_boost": self.priority_boost
        }


@dataclass
class ContextPrediction:
    """Prediction of context needs based on user history."""
    predicted_contexts: List[str] = field(default_factory=list)
    confidence: float = 0.0
    reasoning: str = ""
    pattern_matches: List[str] = field(default_factory=list)
    semantic_similarity: float = 0.0
    user_history_relevance: float = 0.0
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def is_high_confidence(self) -> bool:
        """Check if prediction has high confidence."""
        return self.confidence > 0.7
    
    def get_prediction_summary(self) -> Dict[str, Any]:
        """Get summary of prediction results."""
        return {
            "predicted_contexts": self.predicted_contexts,
            "confidence": self.confidence,
            "reasoning": self.reasoning,
            "high_confidence": self.is_high_confidence()
        }


@dataclass
class ContextOptimization:
    """Optimization result for context retrieval."""
    should_retrieve: bool
    retrieval_strategy: RetrievalStrategy
    optimized_contexts: List[str] = field(default_factory=list)
    efficiency_gain: float = 0.0
    cost_benefit_ratio: float = 1.0
    priority_boost: bool = False
    reasoning: str = ""
    optimization_metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_response(self, request: ContextRequest) -> ContextResponse:
        """Convert to ContextResponse."""
        return ContextResponse(
            request=request,
            should_retrieve=self.should_retrieve,
            retrieval_strategy=self.retrieval_strategy,
            optimized_contexts=self.optimized_contexts,
            efficiency_gain=self.efficiency_gain,
            cost_benefit_ratio=self.cost_benefit_ratio,
            priority_boost=self.priority_boost,
            reasoning=self.reasoning,
            metadata=self.optimization_metadata
        )


@dataclass
class PromptComplexity:
    """Analysis of prompt complexity."""
    level: str  # "simple", "moderate", "complex"
    score: float  # 0.0 to 1.0
    word_count: int = 0
    clause_count: int = 0
    technical_terms: int = 0
    multi_part_requests: int = 0
    complexity_factors: List[str] = field(default_factory=list)
    
    def is_complex(self) -> bool:
        """Check if prompt is complex."""
        return self.level == "complex" or self.score > 0.7
    
    def get_complexity_summary(self) -> Dict[str, Any]:
        """Get summary of complexity analysis."""
        return {
            "level": self.level,
            "score": self.score,
            "is_complex": self.is_complex(),
            "factors": self.complexity_factors
        }


@dataclass
class ContextRequirements:
    """Analysis of context requirements."""
    required_contexts: List[str] = field(default_factory=list)
    context_priority: float = 0.0  # 0.0 to 1.0
    context_categories: List[str] = field(default_factory=list)
    estimated_context_size: int = 0
    context_dependencies: Dict[str, List[str]] = field(default_factory=dict)
    
    def get_requirements_summary(self) -> Dict[str, Any]:
        """Get summary of context requirements."""
        return {
            "required_contexts": self.required_contexts,
            "context_priority": self.context_priority,
            "context_categories": self.context_categories,
            "estimated_context_size": self.estimated_context_size
        }


@dataclass
class UserIntent:
    """Analysis of user intent."""
    primary_intent: str
    secondary_intents: List[str] = field(default_factory=list)
    urgency: str = "normal"  # "low", "normal", "high", "critical"
    keywords: List[str] = field(default_factory=list)
    intent_confidence: float = 0.0
    user_experience_level: str = "intermediate"  # "beginner", "intermediate", "expert"
    
    def is_urgent(self) -> bool:
        """Check if request is urgent."""
        return self.urgency in ["high", "critical"]
    
    def get_intent_summary(self) -> Dict[str, Any]:
        """Get summary of user intent."""
        return {
            "primary_intent": self.primary_intent,
            "urgency": self.urgency,
            "is_urgent": self.is_urgent(),
            "keywords": self.keywords,
            "confidence": self.intent_confidence
        }


@dataclass
class UserHistoryEntry:
    """Entry in user query history."""
    query: str
    context_used: List[str] = field(default_factory=list)
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    success_rate: float = 1.0
    response_time_ms: float = 0.0
    user_satisfaction: Optional[float] = None
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def get_age_seconds(self) -> float:
        """Get age of history entry in seconds."""
        if self.timestamp is None:
            return 0.0  # Default to 0 age if no timestamp
        return (datetime.now(timezone.utc) - self.timestamp).total_seconds()
    
    def is_recent(self, max_age_hours: int = 24) -> bool:
        """Check if entry is recent."""
        return self.get_age_seconds() < (max_age_hours * 3600)


@dataclass
class SmartContextResult:
    """Complete result from smart context system."""
    request: ContextRequest
    complexity_analysis: PromptComplexity
    requirements_analysis: ContextRequirements
    intent_analysis: UserIntent
    prediction: ContextPrediction
    optimization: ContextOptimization
    final_response: ContextResponse
    execution_metadata: Dict[str, Any] = field(default_factory=dict)
    
    def get_summary(self) -> Dict[str, Any]:
        """Get comprehensive summary of smart context analysis."""
        return {
            "prompt_complexity": self.complexity_analysis.get_complexity_summary(),
            "context_requirements": self.requirements_analysis.get_requirements_summary(),
            "user_intent": self.intent_analysis.get_intent_summary(),
            "context_prediction": self.prediction.get_prediction_summary(),
            "retrieval_decision": self.final_response.get_retrieval_summary(),
            "efficiency_gain": self.final_response.efficiency_gain,
            "cost_benefit_ratio": self.final_response.cost_benefit_ratio
        }
