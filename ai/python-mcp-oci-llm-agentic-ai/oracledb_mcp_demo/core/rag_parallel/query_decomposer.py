"""Query decomposition for parallel RAG execution."""

import re
import logging
from typing import List, Dict, Any, Optional
from dataclasses import dataclass

from .models import RAGQuery, QueryAspect, QueryPriority

logger = logging.getLogger(__name__)


@dataclass
class QueryPattern:
    """Pattern for identifying query aspects."""
    keywords: List[str]
    aspect: QueryAspect
    priority: int
    k_value: int
    filters: Dict[str, Any]


class RAGQueryDecomposer:
    """Decomposes complex prompts into parallelizable sub-queries."""
    
    def __init__(self):
        """Initialize query decomposer with patterns."""
        self.patterns = self._initialize_patterns()
        self.aspect_keywords = self._initialize_aspect_keywords()
    
    def decompose_query(self, prompt: str, context: Dict[str, Any] = None) -> List[RAGQuery]:
        """Decompose a complex prompt into multiple RAG queries."""
        if not prompt or not prompt.strip():
            return []
        
        context = context or {}
        prompt_lower = prompt.lower()
        
        # Identify query aspects
        identified_aspects = self._identify_aspects(prompt_lower)
        
        # Generate queries for each aspect
        queries = []
        
        # Always include the main query
        main_query = RAGQuery(
            query=prompt.strip(),
            k=5,
            filters=context,
            priority=QueryPriority.HIGH.value,
            aspect=None
        )
        queries.append(main_query)
        
        # Add aspect-specific queries
        for aspect, priority in identified_aspects:
            aspect_query = self._create_aspect_query(prompt, aspect, priority, context)
            if aspect_query:
                queries.append(aspect_query)
        
        # Sort by priority (lower number = higher priority)
        queries.sort(key=lambda q: q.priority)
        
        logger.debug(f"Decomposed prompt into {len(queries)} queries: {[q.query[:50] for q in queries]}")
        
        return queries
    
    def _initialize_patterns(self) -> List[QueryPattern]:
        """Initialize query patterns for aspect identification."""
        return [
            QueryPattern(
                keywords=["performance", "slow", "fast", "speed", "efficiency"],
                aspect=QueryAspect.PERFORMANCE_ANALYSIS,
                priority=QueryPriority.HIGH.value,
                k_value=5,
                filters={}
            ),
            QueryPattern(
                keywords=["optimize", "optimization", "improve", "better", "enhance"],
                aspect=QueryAspect.OPTIMIZATION,
                priority=QueryPriority.HIGH.value,
                k_value=5,
                filters={}
            ),
            QueryPattern(
                keywords=["index", "indexes", "indexing", "create index"],
                aspect=QueryAspect.INDEX_RECOMMENDATIONS,
                priority=QueryPriority.MEDIUM.value,
                k_value=3,
                filters={}
            ),
            QueryPattern(
                keywords=["test", "testing", "validate", "verify", "check"],
                aspect=QueryAspect.TESTING_STEPS,
                priority=QueryPriority.MEDIUM.value,
                k_value=3,
                filters={}
            ),
            QueryPattern(
                keywords=["cost", "expensive", "cheap", "execution plan", "explain plan"],
                aspect=QueryAspect.COST_ANALYSIS,
                priority=QueryPriority.HIGH.value,
                k_value=4,
                filters={}
            ),
            QueryPattern(
                keywords=["troubleshoot", "debug", "fix", "problem", "issue", "error"],
                aspect=QueryAspect.TROUBLESHOOTING,
                priority=QueryPriority.CRITICAL.value,
                k_value=4,
                filters={}
            ),
            QueryPattern(
                keywords=["best practice", "guideline", "recommendation", "tip"],
                aspect=QueryAspect.BEST_PRACTICES,
                priority=QueryPriority.LOW.value,
                k_value=3,
                filters={}
            ),
            QueryPattern(
                keywords=["monitor", "watch", "track", "observe", "alert"],
                aspect=QueryAspect.MONITORING,
                priority=QueryPriority.LOW.value,
                k_value=3,
                filters={}
            )
        ]
    
    def _initialize_aspect_keywords(self) -> Dict[str, List[str]]:
        """Initialize keywords for each query aspect."""
        return {
            "sql_optimization": [
                "select", "from", "where", "join", "group by", "order by",
                "having", "subquery", "union", "intersect", "minus"
            ],
            "database_performance": [
                "buffer", "cache", "memory", "cpu", "i/o", "disk", "network",
                "connection", "session", "lock", "wait", "blocking"
            ],
            "indexing": [
                "b-tree", "bitmap", "function-based", "composite", "unique",
                "primary key", "foreign key", "clustering"
            ]
        }
    
    def _identify_aspects(self, prompt_lower: str) -> List[tuple[QueryAspect, int]]:
        """Identify query aspects based on keywords."""
        identified_aspects = []
        
        for pattern in self.patterns:
            if any(keyword in prompt_lower for keyword in pattern.keywords):
                identified_aspects.append((pattern.aspect, pattern.priority))
        
        # If no specific aspects identified, add general ones based on content
        if not identified_aspects:
            identified_aspects.extend(self._identify_general_aspects(prompt_lower))
        
        return identified_aspects
    
    def _identify_general_aspects(self, prompt_lower: str) -> List[tuple[QueryAspect, int]]:
        """Identify general aspects based on prompt content."""
        aspects = []
        
        # Check for SQL content
        if any(sql_keyword in prompt_lower for sql_keyword in self.aspect_keywords["sql_optimization"]):
            aspects.append((QueryAspect.OPTIMIZATION, QueryPriority.MEDIUM.value))
        
        # Check for performance-related content
        if any(perf_keyword in prompt_lower for perf_keyword in self.aspect_keywords["database_performance"]):
            aspects.append((QueryAspect.PERFORMANCE_ANALYSIS, QueryPriority.MEDIUM.value))
        
        # Check for complex queries (multiple clauses)
        clause_count = sum([
            prompt_lower.count("where"),
            prompt_lower.count("join"),
            prompt_lower.count("group by"),
            prompt_lower.count("order by"),
            prompt_lower.count("having")
        ])
        
        if clause_count >= 3:
            aspects.append((QueryAspect.COST_ANALYSIS, QueryPriority.MEDIUM.value))
            aspects.append((QueryAspect.INDEX_RECOMMENDATIONS, QueryPriority.MEDIUM.value))
        
        return aspects
    
    def _create_aspect_query(self, original_prompt: str, aspect: QueryAspect, 
                           priority: int, context: Dict[str, Any]) -> Optional[RAGQuery]:
        """Create a specific query for a given aspect."""
        try:
            # Generate aspect-specific query text
            aspect_query = self._generate_aspect_query_text(original_prompt, aspect)
            
            if not aspect_query:
                return None
            
            # Create filters based on aspect
            aspect_filters = self._create_aspect_filters(aspect, context)
            
            # Determine k value based on aspect
            k_value = self._determine_k_value(aspect)
            
            return RAGQuery(
                query=aspect_query,
                k=k_value,
                filters=aspect_filters,
                priority=priority,
                aspect=aspect
            )
            
        except Exception as e:
            logger.warning(f"Failed to create aspect query for {aspect}: {e}")
            return None
    
    def _generate_aspect_query_text(self, original_prompt: str, aspect: QueryAspect) -> str:
        """Generate aspect-specific query text."""
        base_query = original_prompt.strip()
        
        aspect_queries = {
            QueryAspect.PERFORMANCE_ANALYSIS: f"performance analysis and optimization tips for: {base_query}",
            QueryAspect.OPTIMIZATION: f"SQL optimization techniques and best practices for: {base_query}",
            QueryAspect.INDEX_RECOMMENDATIONS: f"index recommendations and database tuning for: {base_query}",
            QueryAspect.TESTING_STEPS: f"testing and validation steps for: {base_query}",
            QueryAspect.COST_ANALYSIS: f"execution cost analysis and performance impact for: {base_query}",
            QueryAspect.TROUBLESHOOTING: f"troubleshooting and debugging guidance for: {base_query}",
            QueryAspect.BEST_PRACTICES: f"best practices and guidelines for: {base_query}",
            QueryAspect.MONITORING: f"monitoring and alerting strategies for: {base_query}"
        }
        
        return aspect_queries.get(aspect, base_query)
    
    def _create_aspect_filters(self, aspect: QueryAspect, context: Dict[str, Any]) -> Dict[str, Any]:
        """Create filters specific to the query aspect."""
        filters = context.copy()
        
        # Add aspect-specific filters
        aspect_filters = {
            QueryAspect.PERFORMANCE_ANALYSIS: {"category": "performance", "type": "analysis"},
            QueryAspect.OPTIMIZATION: {"category": "optimization", "type": "techniques"},
            QueryAspect.INDEX_RECOMMENDATIONS: {"category": "indexing", "type": "recommendations"},
            QueryAspect.TESTING_STEPS: {"category": "testing", "type": "validation"},
            QueryAspect.COST_ANALYSIS: {"category": "cost", "type": "analysis"},
            QueryAspect.TROUBLESHOOTING: {"category": "troubleshooting", "type": "debugging"},
            QueryAspect.BEST_PRACTICES: {"category": "best_practices", "type": "guidelines"},
            QueryAspect.MONITORING: {"category": "monitoring", "type": "strategies"}
        }
        
        filters.update(aspect_filters.get(aspect, {}))
        return filters
    
    def _determine_k_value(self, aspect: QueryAspect) -> int:
        """Determine appropriate k value for the aspect."""
        k_values = {
            QueryAspect.PERFORMANCE_ANALYSIS: 5,
            QueryAspect.OPTIMIZATION: 5,
            QueryAspect.INDEX_RECOMMENDATIONS: 3,
            QueryAspect.TESTING_STEPS: 3,
            QueryAspect.COST_ANALYSIS: 4,
            QueryAspect.TROUBLESHOOTING: 4,
            QueryAspect.BEST_PRACTICES: 3,
            QueryAspect.MONITORING: 3
        }
        
        return k_values.get(aspect, 5)
    
    def get_decomposition_stats(self, prompt: str) -> Dict[str, Any]:
        """Get statistics about query decomposition."""
        queries = self.decompose_query(prompt)
        
        return {
            "total_queries": len(queries),
            "aspects_identified": len(set(q.aspect for q in queries if q.aspect)),
            "priority_distribution": {
                priority: len([q for q in queries if q.priority == priority])
                for priority in range(1, 11)
            },
            "k_value_distribution": {
                k: len([q for q in queries if q.k == k])
                for k in set(q.k for q in queries)
            }
        }
