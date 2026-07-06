"""RAG Context Predictor - Predicts context needs based on user history."""

import logging
from typing import Dict, Any, List, Optional
from difflib import SequenceMatcher
from collections import defaultdict

from .models import ContextPrediction, UserHistoryEntry

logger = logging.getLogger(__name__)


class RAGContextPredictor:
    """Predicts context needs based on user query history and patterns."""
    
    def __init__(self):
        """Initialize context predictor."""
        self.prediction_patterns = self._initialize_prediction_patterns()
        self.semantic_keywords = self._initialize_semantic_keywords()
    
    def predict_context_needs(self, prompt: str, user_history: List[Dict[str, Any]]) -> ContextPrediction:
        """Predict context needs based on prompt and user history."""
        if not prompt or not user_history:
            return ContextPrediction(
                predicted_contexts=[],
                confidence=0.0,
                reasoning="No history available for prediction"
            )
        
        # Convert history to UserHistoryEntry objects
        history_entries = self._convert_history_to_entries(user_history)
        
        # Find pattern matches
        pattern_matches = self._find_pattern_matches(prompt, history_entries)
        
        # Calculate semantic similarity
        semantic_similarity = self._calculate_semantic_similarity(prompt, history_entries)
        
        # Predict contexts based on patterns and similarity
        predicted_contexts = self._predict_contexts_from_patterns(prompt, pattern_matches)
        
        # Calculate confidence
        confidence = self._calculate_prediction_confidence(
            pattern_matches, semantic_similarity, len(user_history)
        )
        
        # Generate reasoning
        reasoning = self._generate_prediction_reasoning(
            pattern_matches, semantic_similarity, predicted_contexts
        )
        
        # Calculate user history relevance
        user_history_relevance = self._calculate_history_relevance(history_entries)
        
        logger.debug(f"Context prediction: {len(predicted_contexts)} contexts, confidence={confidence:.2f}")
        
        return ContextPrediction(
            predicted_contexts=predicted_contexts,
            confidence=confidence,
            reasoning=reasoning,
            pattern_matches=pattern_matches,
            semantic_similarity=semantic_similarity,
            user_history_relevance=user_history_relevance
        )
    
    def _initialize_prediction_patterns(self) -> Dict[str, List[str]]:
        """Initialize patterns for context prediction."""
        return {
            "performance": [
                "performance", "slow", "fast", "speed", "efficiency", "bottleneck",
                "optimize", "optimization", "tune", "tuning"
            ],
            "indexing": [
                "index", "indexes", "indexing", "create index", "drop index", "rebuild index",
                "index maintenance", "index fragmentation"
            ],
            "troubleshooting": [
                "error", "problem", "issue", "fix", "resolve", "debug", "troubleshoot",
                "broken", "not working", "failed"
            ],
            "monitoring": [
                "monitor", "watch", "track", "observe", "alert", "notification",
                "metrics", "statistics", "reporting"
            ],
            "testing": [
                "test", "validate", "verify", "check", "confirm", "ensure",
                "quality", "validation", "testing"
            ],
            "best_practices": [
                "best practice", "guideline", "recommendation", "tip", "advice",
                "standard", "convention", "pattern"
            ],
            "oracle_specific": [
                "oracle", "plsql", "awr", "ash", "statspack", "v$", "dba_",
                "oracle specific", "oracle database"
            ]
        }
    
    def _initialize_semantic_keywords(self) -> Dict[str, List[str]]:
        """Initialize semantic keywords for similarity calculation."""
        return {
            "sql_optimization": [
                "select", "from", "where", "join", "query", "sql", "database",
                "table", "column", "row", "data"
            ],
            "performance_analysis": [
                "analyze", "analysis", "performance", "speed", "time", "duration",
                "execution", "runtime", "throughput"
            ],
            "database_tuning": [
                "tune", "tuning", "optimize", "optimization", "improve", "enhance",
                "better", "faster", "efficient"
            ],
            "maintenance": [
                "maintain", "maintenance", "clean", "organize", "manage", "update",
                "backup", "restore", "archive"
            ]
        }
    
    def _convert_history_to_entries(self, user_history: List[Dict[str, Any]]) -> List[UserHistoryEntry]:
        """Convert raw history to UserHistoryEntry objects."""
        entries = []
        for history_item in user_history:
            entry = UserHistoryEntry(
                query=history_item.get("query", ""),
                context_used=history_item.get("context_used", []),
                timestamp=history_item.get("timestamp", None),
                success_rate=history_item.get("success_rate", 1.0),
                response_time_ms=history_item.get("response_time_ms", 0.0),
                user_satisfaction=history_item.get("user_satisfaction", None),
                metadata=history_item.get("metadata", {})
            )
            entries.append(entry)
        return entries
    
    def _find_pattern_matches(self, prompt: str, history_entries: List[UserHistoryEntry]) -> List[str]:
        """Find pattern matches between prompt and history."""
        matches = []
        prompt_lower = prompt.lower()
        
        for entry in history_entries:
            entry_lower = entry.query.lower()
            
            # Check for exact matches
            if prompt_lower == entry_lower:
                matches.append("exact_match")
                continue
            
            # Check for partial matches
            if prompt_lower in entry_lower or entry_lower in prompt_lower:
                matches.append("partial_match")
                continue
            
            # Check for keyword matches
            prompt_words = set(prompt_lower.split())
            entry_words = set(entry_lower.split())
            common_words = prompt_words.intersection(entry_words)
            
            if len(common_words) >= 2:  # At least 2 common words
                matches.append("keyword_match")
                continue
            
            # Check for pattern matches using prediction patterns
            for pattern_type, keywords in self.prediction_patterns.items():
                prompt_has_pattern = any(keyword in prompt_lower for keyword in keywords)
                entry_has_pattern = any(keyword in entry_lower for keyword in keywords)
                
                if prompt_has_pattern and entry_has_pattern:
                    matches.append(f"pattern_match_{pattern_type}")
        
        return matches
    
    def _calculate_semantic_similarity(self, prompt: str, history_entries: List[UserHistoryEntry]) -> float:
        """Calculate semantic similarity between prompt and history entries."""
        if not history_entries:
            return 0.0
        
        similarities = []
        prompt_lower = prompt.lower()
        
        for entry in history_entries:
            entry_lower = entry.query.lower()
            
            # Use SequenceMatcher for string similarity
            similarity = SequenceMatcher(None, prompt_lower, entry_lower).ratio()
            similarities.append(similarity)
        
        # Return average similarity
        return sum(similarities) / len(similarities)
    
    def _predict_contexts_from_patterns(self, prompt: str, pattern_matches: List[str]) -> List[str]:
        """Predict contexts based on pattern matches."""
        predicted_contexts = []
        prompt_lower = prompt.lower()
        
        # Extract contexts from pattern matches
        for match in pattern_matches:
            if match.startswith("pattern_match_"):
                context_type = match.replace("pattern_match_", "")
                if context_type not in predicted_contexts:
                    predicted_contexts.append(context_type)
        
        # Additional context prediction based on prompt content
        for context_type, keywords in self.prediction_patterns.items():
            if any(keyword in prompt_lower for keyword in keywords):
                if context_type not in predicted_contexts:
                    predicted_contexts.append(context_type)
        
        # Predict based on semantic keywords
        for semantic_type, keywords in self.semantic_keywords.items():
            if any(keyword in prompt_lower for keyword in keywords):
                # Map semantic types to context types
                context_mapping = {
                    "sql_optimization": "performance",
                    "performance_analysis": "performance",
                    "database_tuning": "performance",
                    "maintenance": "maintenance"
                }
                
                if semantic_type in context_mapping:
                    context_type = context_mapping[semantic_type]
                    if context_type not in predicted_contexts:
                        predicted_contexts.append(context_type)
        
        return predicted_contexts
    
    def _calculate_prediction_confidence(self, pattern_matches: List[str], 
                                       semantic_similarity: float, 
                                       history_size: int) -> float:
        """Calculate confidence in prediction."""
        base_confidence = 0.0
        
        # Pattern match confidence
        if "exact_match" in pattern_matches:
            base_confidence += 0.8
        elif "partial_match" in pattern_matches:
            base_confidence += 0.6
        elif "keyword_match" in pattern_matches:
            base_confidence += 0.4
        
        # Pattern type matches
        pattern_type_matches = [m for m in pattern_matches if m.startswith("pattern_match_")]
        if pattern_type_matches:
            base_confidence += min(len(pattern_type_matches) * 0.2, 0.4)
        
        # Semantic similarity confidence
        semantic_confidence = semantic_similarity * 0.3
        base_confidence += semantic_confidence
        
        # History size confidence (more history = more confidence)
        history_confidence = min(history_size / 10.0, 0.2)
        base_confidence += history_confidence
        
        return min(base_confidence, 1.0)
    
    def _generate_prediction_reasoning(self, pattern_matches: List[str], 
                                     semantic_similarity: float, 
                                     predicted_contexts: List[str]) -> str:
        """Generate human-readable reasoning for prediction."""
        reasoning_parts = []
        
        if "exact_match" in pattern_matches:
            reasoning_parts.append("Exact query match found in history")
        elif "partial_match" in pattern_matches:
            reasoning_parts.append("Partial query match found in history")
        elif "keyword_match" in pattern_matches:
            reasoning_parts.append("Keyword overlap with historical queries")
        
        pattern_type_matches = [m for m in pattern_matches if m.startswith("pattern_match_")]
        if pattern_type_matches:
            reasoning_parts.append(f"Pattern matches: {', '.join(pattern_type_matches)}")
        
        if semantic_similarity > 0.7:
            reasoning_parts.append("High semantic similarity with historical queries")
        elif semantic_similarity > 0.4:
            reasoning_parts.append("Moderate semantic similarity with historical queries")
        
        if predicted_contexts:
            reasoning_parts.append(f"Predicted contexts: {', '.join(predicted_contexts)}")
        
        if not reasoning_parts:
            reasoning_parts.append("Limited pattern matches, low confidence prediction")
        
        return ". ".join(reasoning_parts) + "."
    
    def _calculate_history_relevance(self, history_entries: List[UserHistoryEntry]) -> float:
        """Calculate relevance of user history for prediction."""
        if not history_entries:
            return 0.0
        
        # Calculate average age of history entries
        total_age = sum(entry.get_age_seconds() for entry in history_entries)
        avg_age_hours = total_age / len(history_entries) / 3600
        
        # Recent history is more relevant
        if avg_age_hours < 1:  # Less than 1 hour
            relevance = 1.0
        elif avg_age_hours < 24:  # Less than 1 day
            relevance = 0.8
        elif avg_age_hours < 168:  # Less than 1 week
            relevance = 0.6
        else:  # More than 1 week
            relevance = 0.3
        
        # Adjust based on success rate
        avg_success_rate = sum(entry.success_rate for entry in history_entries) / len(history_entries)
        relevance *= avg_success_rate
        
        return relevance
    
    def predict_contexts_for_user(self, user_id: str, prompt: str, 
                                 user_history: List[Dict[str, Any]]) -> ContextPrediction:
        """Predict contexts for a specific user."""
        # Filter history for specific user
        user_specific_history = [
            item for item in user_history 
            if item.get("user_id") == user_id
        ]
        
        # Get general prediction
        general_prediction = self.predict_context_needs(prompt, user_history)
        
        # Get user-specific prediction
        user_prediction = self.predict_context_needs(prompt, user_specific_history)
        
        # Combine predictions (user-specific gets higher weight)
        combined_contexts = list(set(general_prediction.predicted_contexts + user_prediction.predicted_contexts))
        combined_confidence = (general_prediction.confidence + user_prediction.confidence * 1.5) / 2.5
        
        return ContextPrediction(
            predicted_contexts=combined_contexts,
            confidence=min(combined_confidence, 1.0),
            reasoning=f"Combined prediction: {general_prediction.reasoning} User-specific: {user_prediction.reasoning}",
            pattern_matches=general_prediction.pattern_matches + user_prediction.pattern_matches,
            semantic_similarity=max(general_prediction.semantic_similarity, user_prediction.semantic_similarity),
            user_history_relevance=user_prediction.user_history_relevance
        )
    
    def get_prediction_insights(self, prompt: str, user_history: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Get detailed insights about prediction process."""
        prediction = self.predict_context_needs(prompt, user_history)
        history_entries = self._convert_history_to_entries(user_history)
        
        insights = {
            "prediction": prediction.get_prediction_summary(),
            "history_analysis": {
                "total_entries": len(history_entries),
                "recent_entries": len([e for e in history_entries if e.is_recent(24)]),
                "avg_success_rate": sum(e.success_rate for e in history_entries) / len(history_entries) if history_entries else 0.0,
                "context_usage_patterns": self._analyze_context_usage_patterns(history_entries)
            },
            "pattern_analysis": {
                "pattern_matches": prediction.pattern_matches,
                "semantic_similarity": prediction.semantic_similarity,
                "confidence_factors": self._analyze_confidence_factors(prediction)
            }
        }
        
        return insights
    
    def _analyze_context_usage_patterns(self, history_entries: List[UserHistoryEntry]) -> Dict[str, Any]:
        """Analyze patterns in context usage."""
        context_counts = defaultdict(int)
        context_success_rates = defaultdict(list)
        
        for entry in history_entries:
            for context in entry.context_used:
                context_counts[context] += 1
                context_success_rates[context].append(entry.success_rate)
        
        patterns = {}
        for context, count in context_counts.items():
            avg_success = sum(context_success_rates[context]) / len(context_success_rates[context])
            patterns[context] = {
                "usage_count": count,
                "avg_success_rate": avg_success,
                "frequency": count / len(history_entries) if history_entries else 0.0
            }
        
        return patterns
    
    def _analyze_confidence_factors(self, prediction: ContextPrediction) -> Dict[str, float]:
        """Analyze factors contributing to prediction confidence."""
        factors = {
            "pattern_matches": len(prediction.pattern_matches) * 0.2,
            "semantic_similarity": prediction.semantic_similarity * 0.3,
            "context_count": min(len(prediction.predicted_contexts) * 0.1, 0.2),
            "history_relevance": prediction.user_history_relevance * 0.2
        }
        
        return factors
