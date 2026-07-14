"""RAG Context Analyzer - Analyzes prompt complexity and context requirements."""

import re
import logging
from typing import Dict, Any, List, Optional
from dataclasses import dataclass

from .models import PromptComplexity, ContextRequirements, UserIntent

logger = logging.getLogger(__name__)


@dataclass
class ComplexityPattern:
    """Pattern for identifying complexity factors."""
    pattern: str
    factor: str
    weight: float
    category: str


class RAGContextAnalyzer:
    """Analyzes prompts for complexity and context requirements."""
    
    def __init__(self):
        """Initialize context analyzer with patterns."""
        self.complexity_patterns = self._initialize_complexity_patterns()
        self.context_patterns = self._initialize_context_patterns()
        self.intent_patterns = self._initialize_intent_patterns()
    
    def analyze_prompt_complexity(self, prompt: str) -> PromptComplexity:
        """Analyze the complexity of a prompt."""
        if not prompt or not prompt.strip():
            return PromptComplexity(
                level="simple",
                score=0.0,
                word_count=0,
                clause_count=0,
                technical_terms=0,
                multi_part_requests=0,
                complexity_factors=[]
            )
        
        # Basic metrics
        word_count = len(prompt.split())
        clause_count = self._count_sql_clauses(prompt)
        technical_terms = self._count_technical_terms(prompt)
        multi_part_requests = self._count_multi_part_requests(prompt)
        
        # Analyze complexity factors
        complexity_factors = self._identify_complexity_factors(prompt)
        
        # Calculate complexity score
        score = self._calculate_complexity_score(
            word_count, clause_count, technical_terms, 
            multi_part_requests, complexity_factors
        )
        
        # Determine complexity level
        level = self._determine_complexity_level(score)
        
        logger.debug(f"Prompt complexity analysis: level={level}, score={score:.2f}")
        
        return PromptComplexity(
            level=level,
            score=score,
            word_count=word_count,
            clause_count=clause_count,
            technical_terms=technical_terms,
            multi_part_requests=multi_part_requests,
            complexity_factors=complexity_factors
        )
    
    def analyze_context_requirements(self, prompt: str, existing_context: Dict[str, Any] = None) -> ContextRequirements:
        """Analyze context requirements for a prompt."""
        if not prompt or not prompt.strip():
            return ContextRequirements(
                required_contexts=[],
                context_priority=0.0,
                context_categories=[],
                estimated_context_size=0,
                context_dependencies={}
            )
        
        existing_context = existing_context or {}
        
        # Identify required contexts
        required_contexts = self._identify_required_contexts(prompt)
        
        # Add Oracle-specific context if existing context suggests Oracle environment
        if existing_context and "database" in existing_context:
            if existing_context["database"] == "oracle" and "oracle_specific" not in required_contexts:
                required_contexts.append("oracle_specific")
        
        # Categorize contexts
        context_categories = self._categorize_contexts(required_contexts)
        
        # Estimate context size
        estimated_context_size = self._estimate_context_size(prompt, required_contexts)
        
        # Calculate context priority
        context_priority = self._calculate_context_priority(
            prompt, required_contexts, existing_context
        )
        
        # Identify context dependencies
        context_dependencies = self._identify_context_dependencies(required_contexts)
        
        logger.debug(f"Context requirements: {len(required_contexts)} contexts, priority={context_priority:.2f}")
        
        return ContextRequirements(
            required_contexts=required_contexts,
            context_priority=context_priority,
            context_categories=context_categories,
            estimated_context_size=estimated_context_size,
            context_dependencies=context_dependencies
        )
    
    def analyze_user_intent(self, prompt: str) -> UserIntent:
        """Analyze user intent from prompt."""
        if not prompt or not prompt.strip():
            return UserIntent(
                primary_intent="unknown",
                urgency="normal",
                keywords=[],
                intent_confidence=0.0
            )
        
        # Identify primary intent
        primary_intent = self._identify_primary_intent(prompt)
        
        # Identify secondary intents
        secondary_intents = self._identify_secondary_intents(prompt)
        
        # Determine urgency
        urgency = self._determine_urgency(prompt)
        
        # Extract keywords
        keywords = self._extract_keywords(prompt)
        
        # Calculate intent confidence
        intent_confidence = self._calculate_intent_confidence(prompt, primary_intent)
        
        # Assess user experience level
        user_experience_level = self._assess_user_experience(prompt)
        
        logger.debug(f"User intent analysis: {primary_intent}, urgency={urgency}, confidence={intent_confidence:.2f}")
        
        return UserIntent(
            primary_intent=primary_intent,
            secondary_intents=secondary_intents,
            urgency=urgency,
            keywords=keywords,
            intent_confidence=intent_confidence,
            user_experience_level=user_experience_level
        )
    
    def _initialize_complexity_patterns(self) -> List[ComplexityPattern]:
        """Initialize patterns for complexity analysis."""
        return [
            # SQL complexity patterns
            ComplexityPattern(r'\bSELECT\b.*\bFROM\b.*\bWHERE\b', 'multi_clause_select', 0.3, 'sql'),
            ComplexityPattern(r'\bJOIN\b', 'joins', 0.2, 'sql'),
            ComplexityPattern(r'\bGROUP\s+BY\b', 'grouping', 0.2, 'sql'),
            ComplexityPattern(r'\bORDER\s+BY\b', 'ordering', 0.1, 'sql'),
            ComplexityPattern(r'\bHAVING\b', 'having_clause', 0.3, 'sql'),
            ComplexityPattern(r'\bUNION\b', 'set_operations', 0.4, 'sql'),
            ComplexityPattern(r'\bSUBQUERY\b|\bEXISTS\b|\bIN\s*\(', 'subqueries', 0.4, 'sql'),
            
            # Performance patterns
            ComplexityPattern(r'\bperformance\b|\boptimize\b|\bslow\b|\bfast\b', 'performance_focus', 0.3, 'performance'),
            ComplexityPattern(r'\bcost\b|\bexecution\s+plan\b|\bexplain\b', 'cost_analysis', 0.3, 'performance'),
            ComplexityPattern(r'\bindex\b|\bindexing\b', 'indexing', 0.2, 'performance'),
            
            # Multi-part request patterns
            ComplexityPattern(r'\d+\.|\balso\b|\badditionally\b|\bfurthermore\b', 'multi_part', 0.4, 'structure'),
            ComplexityPattern(r'\bprovide\b.*\band\b', 'multi_requirements', 0.3, 'structure'),
            
            # Technical complexity patterns
            ComplexityPattern(r'\btroubleshoot\b|\bdebug\b|\bfix\b', 'troubleshooting', 0.4, 'technical'),
            ComplexityPattern(r'\bmonitor\b|\btrack\b|\balert\b', 'monitoring', 0.3, 'technical'),
            ComplexityPattern(r'\btest\b|\bvalidate\b|\bverify\b', 'testing', 0.2, 'technical')
        ]
    
    def _initialize_context_patterns(self) -> Dict[str, List[str]]:
        """Initialize patterns for context identification."""
        return {
            "sql_optimization": [
                "optimize", "performance", "slow", "fast", "efficient", "cost", "execution plan"
            ],
            "performance_analysis": [
                "analyze", "performance", "bottleneck", "slowdown", "resource usage"
            ],
            "indexing": [
                "index", "indexes", "indexing", "create index", "drop index", "rebuild"
            ],
            "troubleshooting": [
                "error", "problem", "issue", "fix", "resolve", "debug", "troubleshoot"
            ],
            "monitoring": [
                "monitor", "watch", "track", "observe", "alert", "notification"
            ],
            "testing": [
                "test", "validate", "verify", "check", "confirm", "ensure"
            ],
            "best_practices": [
                "best practice", "guideline", "recommendation", "tip", "advice"
            ],
            "oracle_specific": [
                "oracle", "plsql", "awr", "ash", "statspack", "v$", "dba_"
            ]
        }
    
    def _initialize_intent_patterns(self) -> Dict[str, List[str]]:
        """Initialize patterns for intent identification."""
        return {
            "optimization": ["optimize", "improve", "better", "enhance", "speed up", "make faster"],
            "analysis": ["analyze", "examine", "investigate", "review", "assess"],
            "troubleshooting": ["fix", "resolve", "debug", "problem", "issue", "error"],
            "learning": ["how", "what", "why", "explain", "understand", "learn"],
            "implementation": ["create", "build", "implement", "set up", "configure"],
            "maintenance": ["maintain", "update", "clean", "organize", "manage"]
        }
    
    def _count_sql_clauses(self, prompt: str) -> int:
        """Count SQL clauses in prompt."""
        sql_keywords = ['SELECT', 'FROM', 'WHERE', 'JOIN', 'GROUP BY', 'ORDER BY', 'HAVING', 'UNION']
        count = 0
        prompt_upper = prompt.upper()
        for keyword in sql_keywords:
            count += prompt_upper.count(keyword)
        return count
    
    def _count_technical_terms(self, prompt: str) -> int:
        """Count technical terms in prompt."""
        technical_terms = [
            'performance', 'optimization', 'index', 'execution plan', 'cost', 'bottleneck',
            'monitoring', 'troubleshooting', 'debugging', 'analysis', 'benchmark'
        ]
        count = 0
        prompt_lower = prompt.lower()
        for term in technical_terms:
            if term in prompt_lower:
                count += 1
        
        # Don't count basic SQL terms as technical for complexity
        basic_sql_terms = ['sql', 'select', 'from', 'where']
        for term in basic_sql_terms:
            if term in prompt_lower:
                count = max(0, count - 1)  # Reduce count for basic terms
        
        return max(0, count)
    
    def _count_multi_part_requests(self, prompt: str) -> int:
        """Count multi-part requests in prompt."""
        patterns = [
            r'\d+\.',  # Numbered lists
            r'\balso\b',  # Also
            r'\badditionally\b',  # Additionally
            r'\bfurthermore\b',  # Furthermore
            r'\bprovide\b.*\band\b',  # Provide X and Y
        ]
        count = 0
        for pattern in patterns:
            count += len(re.findall(pattern, prompt, re.IGNORECASE))
        return count
    
    def _identify_complexity_factors(self, prompt: str) -> List[str]:
        """Identify complexity factors in prompt."""
        factors = []
        prompt_lower = prompt.lower()
        
        for pattern in self.complexity_patterns:
            if re.search(pattern.pattern, prompt_lower):
                factors.append(pattern.factor)
        
        return factors
    
    def _calculate_complexity_score(self, word_count: int, clause_count: int, 
                                  technical_terms: int, multi_part_requests: int,
                                  complexity_factors: List[str]) -> float:
        """Calculate overall complexity score."""
        # Base score from word count
        word_score = min(word_count / 100.0, 0.3)
        
        # SQL clause complexity
        clause_score = min(clause_count / 10.0, 0.3)
        
        # Technical complexity
        technical_score = min(technical_terms / 5.0, 0.2)
        
        # Multi-part complexity
        multi_part_score = min(multi_part_requests / 3.0, 0.2)
        
        # Pattern-based complexity
        pattern_score = min(len(complexity_factors) / 5.0, 0.2)
        
        total_score = word_score + clause_score + technical_score + multi_part_score + pattern_score
        
        # Ensure very simple prompts get low scores
        if word_count < 10 and clause_count == 0 and technical_terms <= 1:
            total_score = min(total_score, 0.15)
        
        return min(total_score, 1.0)
    
    def _determine_complexity_level(self, score: float) -> str:
        """Determine complexity level based on score."""
        if score < 0.25:  # Lowered threshold for simple
            return "simple"
        elif score < 0.6:
            return "moderate"
        else:
            return "complex"
    
    def _identify_required_contexts(self, prompt: str) -> List[str]:
        """Identify required contexts for prompt."""
        required_contexts = []
        prompt_lower = prompt.lower()
        
        for context_type, patterns in self.context_patterns.items():
            for pattern in patterns:
                if pattern in prompt_lower:
                    required_contexts.append(context_type)
                    break
        
        # Special handling for Oracle-specific detection
        oracle_indicators = ["oracle", "plsql", "awr", "ash", "statspack", "v$", "dba_"]
        if any(indicator in prompt_lower for indicator in oracle_indicators):
            if "oracle_specific" not in required_contexts:
                required_contexts.append("oracle_specific")
        
        return required_contexts
    
    def _categorize_contexts(self, contexts: List[str]) -> List[str]:
        """Categorize contexts by type."""
        categories = []
        for context in contexts:
            if context in ["sql_optimization", "performance_analysis"]:
                categories.append("performance")
            elif context in ["indexing", "troubleshooting"]:
                categories.append("technical")
            elif context in ["monitoring", "testing"]:
                categories.append("operational")
            else:
                categories.append("general")
        
        return list(set(categories))
    
    def _estimate_context_size(self, prompt: str, contexts: List[str]) -> int:
        """Estimate the size of required context."""
        base_size = 100  # Base context size in KB
        
        # Adjust based on complexity
        complexity = self.analyze_prompt_complexity(prompt)
        complexity_multiplier = 1.0 + complexity.score
        
        # Adjust based on number of contexts
        context_multiplier = 1.0 + (len(contexts) * 0.2)
        
        estimated_size = int(base_size * complexity_multiplier * context_multiplier)
        return estimated_size
    
    def _calculate_context_priority(self, prompt: str, contexts: List[str], 
                                  existing_context: Dict[str, Any]) -> float:
        """Calculate context retrieval priority."""
        base_priority = 0.5
        
        # Increase priority for complex prompts
        complexity = self.analyze_prompt_complexity(prompt)
        if complexity.is_complex():
            base_priority += 0.2
        
        # Increase priority for urgent intents
        intent = self.analyze_user_intent(prompt)
        if intent.is_urgent():
            base_priority += 0.2
        
        # Increase priority for missing contexts
        missing_contexts = len([c for c in contexts if c not in existing_context])
        if missing_contexts > 0:
            base_priority += min(missing_contexts * 0.1, 0.3)
        
        return min(base_priority, 1.0)
    
    def _identify_context_dependencies(self, contexts: List[str]) -> Dict[str, List[str]]:
        """Identify dependencies between contexts."""
        dependencies = {
            "sql_optimization": ["performance_analysis"],
            "performance_analysis": ["monitoring"],
            "indexing": ["sql_optimization"],
            "troubleshooting": ["performance_analysis", "monitoring"],
            "monitoring": ["performance_analysis"]
        }
        
        result = {}
        for context in contexts:
            if context in dependencies:
                result[context] = dependencies[context]
        
        return result
    
    def _identify_primary_intent(self, prompt: str) -> str:
        """Identify the primary intent of the user."""
        prompt_lower = prompt.lower()
        
        # Find the intent with the most pattern matches
        intent_scores = {}
        for intent, patterns in self.intent_patterns.items():
            score = 0
            for pattern in patterns:
                if pattern in prompt_lower:
                    score += 1
            intent_scores[intent] = score
        
        if not intent_scores:
            return "unknown"
        
        # Return intent with highest score
        return max(intent_scores.items(), key=lambda x: x[1])[0]
    
    def _identify_secondary_intents(self, prompt: str) -> List[str]:
        """Identify secondary intents from prompt."""
        secondary_intents = []
        prompt_lower = prompt.lower()
        
        for intent, patterns in self.intent_patterns.items():
            if intent == self._identify_primary_intent(prompt):
                continue
            
            for pattern in patterns:
                if pattern in prompt_lower:
                    secondary_intents.append(intent)
                    break
        
        return secondary_intents
    
    def _determine_urgency(self, prompt: str) -> str:
        """Determine urgency level from prompt."""
        urgent_keywords = ["critical", "urgent", "emergency", "immediate", "asap", "now"]
        high_keywords = ["important", "priority", "need", "help", "issue", "problem"]
        
        prompt_lower = prompt.lower()
        
        if any(keyword in prompt_lower for keyword in urgent_keywords):
            return "critical"
        elif any(keyword in prompt_lower for keyword in high_keywords):
            return "high"
        else:
            return "normal"
    
    def _extract_keywords(self, prompt: str) -> List[str]:
        """Extract important keywords from prompt."""
        # Simple keyword extraction - in production would use NLP
        stop_words = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "this", "that", "these", "those"}
        
        words = prompt.lower().split()
        keywords = [word for word in words if word not in stop_words and len(word) > 2]
        
        # Add technical terms that might be in the prompt
        technical_indicators = ["performance", "optimize", "slow", "fast", "query", "sql", "database"]
        for indicator in technical_indicators:
            if indicator in prompt.lower() and indicator not in keywords:
                keywords.append(indicator)
        
        # Special handling for performance-related terms
        if "slow" in prompt.lower() and "performance" not in keywords:
            keywords.append("performance")
        if "optimizing" in prompt.lower() and "optimize" not in keywords:
            keywords.append("optimize")
        
        return keywords[:10]  # Limit to top 10 keywords
    
    def _calculate_intent_confidence(self, prompt: str, primary_intent: str) -> float:
        """Calculate confidence in intent identification."""
        if primary_intent == "unknown":
            return 0.0
        
        # Count pattern matches for primary intent
        patterns = self.intent_patterns.get(primary_intent, [])
        matches = sum(1 for pattern in patterns if pattern in prompt.lower())
        
        # Calculate confidence based on matches and prompt length
        if not patterns:
            return 0.0
        
        pattern_confidence = matches / len(patterns)
        length_confidence = min(len(prompt) / 100.0, 1.0)
        
        return (pattern_confidence + length_confidence) / 2
    
    def _assess_user_experience(self, prompt: str) -> str:
        """Assess user experience level from prompt."""
        beginner_indicators = ["what is", "how do i", "explain", "simple", "basic"]
        expert_indicators = ["advanced", "optimization", "performance tuning", "execution plan", "cost analysis"]
        
        prompt_lower = prompt.lower()
        
        if any(indicator in prompt_lower for indicator in expert_indicators):
            return "expert"
        elif any(indicator in prompt_lower for indicator in beginner_indicators):
            return "beginner"
        else:
            return "intermediate"
