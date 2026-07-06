"""Cache key generation for RAG system following SOLID principles."""

import hashlib
import json
import re
from abc import ABC, abstractmethod
from typing import Dict, Any, List
from dataclasses import asdict

from .models import CacheKey


class TextNormalizer(ABC):
    """Abstract base class for text normalization strategies."""
    
    @abstractmethod
    def normalize(self, text: str) -> str:
        """Normalize text according to specific strategy."""
        pass


class BasicTextNormalizer(TextNormalizer):
    """Basic text normalization strategy."""
    
    def normalize(self, text: str) -> str:
        """Normalize text by removing extra whitespace and converting to lowercase."""
        if not text:
            return ""
        
        # Remove extra whitespace and convert to lowercase
        normalized = re.sub(r'\s+', ' ', text.strip()).lower()
        return normalized


class SQLTextNormalizer(TextNormalizer):
    """SQL-specific text normalization strategy."""
    
    def normalize(self, text: str) -> str:
        """Normalize SQL text by standardizing common patterns."""
        if not text:
            return ""
        
        # Start with basic normalization
        normalized = BasicTextNormalizer().normalize(text)
        
        # Standardize common SQL patterns
        normalized = re.sub(r'\bselect\b', 'SELECT', normalized)
        normalized = re.sub(r'\bfrom\b', 'FROM', normalized)
        normalized = re.sub(r'\bwhere\b', 'WHERE', normalized)
        normalized = re.sub(r'\band\b', 'AND', normalized)
        normalized = re.sub(r'\bor\b', 'OR', normalized)
        normalized = re.sub(r'\border\s+by\b', 'ORDER BY', normalized)
        normalized = re.sub(r'\bgroup\s+by\b', 'GROUP BY', normalized)
        
        # Remove common SQL noise
        normalized = re.sub(r'[;,]', '', normalized)
        
        return normalized


class FeatureExtractor(ABC):
    """Abstract base class for feature extraction strategies."""
    
    @abstractmethod
    def extract_features(self, text: str) -> Dict[str, Any]:
        """Extract features from text."""
        pass


class PromptFeatureExtractor(FeatureExtractor):
    """Feature extractor for general prompts."""
    
    def extract_features(self, text: str) -> Dict[str, Any]:
        """Extract semantic features from prompt text."""
        if not text:
            return {}
        
        features = {
            "length": len(text),
            "word_count": len(text.split()),
            "has_sql": self._has_sql_keywords(text),
            "has_performance_keywords": self._has_performance_keywords(text),
            "has_troubleshooting_keywords": self._has_troubleshooting_keywords(text),
            "complexity": self._assess_complexity(text)
        }
        
        return features
    
    def _has_sql_keywords(self, text: str) -> bool:
        """Check if text contains SQL keywords."""
        sql_keywords = ['select', 'from', 'where', 'join', 'table', 'index', 'sql', 'query']
        text_lower = text.lower()
        return any(keyword in text_lower for keyword in sql_keywords)
    
    def _has_performance_keywords(self, text: str) -> bool:
        """Check if text contains performance-related keywords."""
        perf_keywords = ['performance', 'optimize', 'slow', 'fast', 'cost', 'efficient', 'speed']
        text_lower = text.lower()
        return any(keyword in text_lower for keyword in perf_keywords)
    
    def _has_troubleshooting_keywords(self, text: str) -> bool:
        """Check if text contains troubleshooting keywords."""
        trouble_keywords = ['error', 'problem', 'issue', 'fix', 'resolve', 'debug', 'troubleshoot']
        text_lower = text.lower()
        return any(keyword in text_lower for keyword in trouble_keywords)
    
    def _assess_complexity(self, text: str) -> str:
        """Assess text complexity."""
        word_count = len(text.split())
        if word_count < 10:
            return "simple"
        elif word_count < 25:
            return "moderate"
        else:
            return "complex"


class ContextFeatureExtractor(FeatureExtractor):
    """Feature extractor for context information."""
    
    def extract_features(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """Extract features from context dictionary."""
        if not context:
            return {}
        
        features = {
            "user_id": context.get("user_id"),
            "database": context.get("database"),
            "session_id": context.get("session_id"),
            "timestamp": context.get("timestamp"),
            "context_keys": list(context.keys()) if isinstance(context, dict) else []
        }
        
        # Remove None values
        features = {k: v for k, v in features.items() if v is not None}
        
        return features


class CacheKeyGenerator:
    """Generates cache keys based on prompt and context."""
    
    def __init__(self, 
                 text_normalizer: TextNormalizer = None,
                 prompt_feature_extractor: FeatureExtractor = None,
                 context_feature_extractor: FeatureExtractor = None):
        """Initialize cache key generator with strategies."""
        self.text_normalizer = text_normalizer or BasicTextNormalizer()
        self.prompt_feature_extractor = prompt_feature_extractor or PromptFeatureExtractor()
        self.context_feature_extractor = context_feature_extractor or ContextFeatureExtractor()
    
    def generate_key(self, prompt: str, context: Dict[str, Any]) -> CacheKey:
        """Generate cache key from prompt and context."""
        # Normalize prompt
        normalized_prompt = self.text_normalizer.normalize(prompt)
        
        # Extract features
        prompt_features = self.prompt_feature_extractor.extract_features(normalized_prompt)
        context_features = self.context_feature_extractor.extract_features(context)
        
        # Combine features for key generation
        combined_features = {
            "prompt": normalized_prompt,
            "prompt_features": prompt_features,
            "context_features": context_features
        }
        
        # Generate hash-based key
        key_string = json.dumps(combined_features, sort_keys=True, default=str)
        key_hash = hashlib.sha256(key_string.encode('utf-8')).hexdigest()
        
        return CacheKey(key_hash)
    
    def generate_simple_key(self, prompt: str) -> CacheKey:
        """Generate simple cache key from prompt only."""
        normalized_prompt = self.text_normalizer.normalize(prompt)
        key_hash = hashlib.sha256(normalized_prompt.encode('utf-8')).hexdigest()
        return CacheKey(key_hash)
    
    def generate_distributed_key(self, prompt: str, context: Dict[str, Any]) -> CacheKey:
        """Generate distributed cache key including context."""
        return self.generate_key(prompt, context)


class CacheKeyGeneratorFactory:
    """Factory for creating cache key generators with different strategies."""
    
    @staticmethod
    def create_basic_generator() -> CacheKeyGenerator:
        """Create basic cache key generator."""
        return CacheKeyGenerator()
    
    @staticmethod
    def create_sql_optimized_generator() -> CacheKeyGenerator:
        """Create SQL-optimized cache key generator."""
        return CacheKeyGenerator(
            text_normalizer=SQLTextNormalizer(),
            prompt_feature_extractor=PromptFeatureExtractor(),
            context_feature_extractor=ContextFeatureExtractor()
        )
    
    @staticmethod
    def create_custom_generator(text_normalizer: TextNormalizer,
                               prompt_feature_extractor: FeatureExtractor,
                               context_feature_extractor: FeatureExtractor) -> CacheKeyGenerator:
        """Create custom cache key generator with specified strategies."""
        return CacheKeyGenerator(
            text_normalizer=text_normalizer,
            prompt_feature_extractor=prompt_feature_extractor,
            context_feature_extractor=context_feature_extractor
        )
