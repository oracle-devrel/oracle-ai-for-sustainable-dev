"""Semantic caching system for RAG performance optimization."""

import logging
import hashlib
import json
import time
from abc import ABC, abstractmethod
from typing import Dict, List, Any, Optional, Tuple, Union
from dataclasses import dataclass, field
from enum import Enum
import asyncio
from collections import OrderedDict

logger = logging.getLogger(__name__)


class CacheStrategy(Enum):
    """Cache strategy types."""
    LRU = "lru"  # Least Recently Used
    LFU = "lfu"  # Least Frequently Used
    TTL = "ttl"  # Time To Live
    SEMANTIC = "semantic"  # Semantic similarity


@dataclass
class CacheEntry:
    """Cache entry with metadata."""
    key: str
    value: Any
    timestamp: float
    access_count: int = 0
    last_accessed: float = field(default_factory=time.time)
    metadata: Optional[Dict[str, Any]] = None
    semantic_vector: Optional[List[float]] = None


@dataclass
class CacheConfig:
    """Cache configuration."""
    max_size: int = 1000
    strategy: CacheStrategy = CacheStrategy.LRU
    ttl_seconds: int = 3600  # 1 hour default
    semantic_threshold: float = 0.8
    cleanup_interval: int = 300  # 5 minutes
    enable_compression: bool = False
    enable_persistence: bool = False


class CacheBackend(ABC):
    """Abstract cache backend interface."""
    
    @abstractmethod
    async def get(self, key: str) -> Optional[Any]:
        """Get value from cache."""
        pass
    
    @abstractmethod
    async def set(self, key: str, value: Any, metadata: Optional[Dict[str, Any]] = None) -> bool:
        """Set value in cache."""
        pass
    
    @abstractmethod
    async def delete(self, key: str) -> bool:
        """Delete value from cache."""
        pass
    
    @abstractmethod
    async def clear(self) -> bool:
        """Clear all cache entries."""
        pass
    
    @abstractmethod
    async def size(self) -> int:
        """Get current cache size."""
        pass


class InMemoryCacheBackend(CacheBackend):
    """In-memory cache backend."""
    
    def __init__(self, config: CacheConfig):
        """Initialize in-memory cache backend.
        
        Args:
            config: Cache configuration
        """
        self.config = config
        self.cache: Dict[str, CacheEntry] = {}
        self.access_order: List[str] = []
        self.access_frequency: Dict[str, int] = {}
    
    async def get(self, key: str) -> Optional[Any]:
        """Get value from cache."""
        if key in self.cache:
            entry = self.cache[key]
            
            # Check TTL
            if self.config.strategy == CacheStrategy.TTL:
                if time.time() - entry.timestamp > self.config.ttl_seconds:
                    await self.delete(key)
                    return None
            
            # Update access metadata
            entry.access_count += 1
            entry.last_accessed = time.time()
            
            # Update access order for LRU
            if self.config.strategy == CacheStrategy.LRU:
                if key in self.access_order:
                    self.access_order.remove(key)
                self.access_order.append(key)
            
            # Update access frequency for LFU
            if self.config.strategy == CacheStrategy.LFU:
                self.access_frequency[key] = self.access_frequency.get(key, 0) + 1
            
            return entry.value
        
        return None
    
    async def set(self, key: str, value: Any, metadata: Optional[Dict[str, Any]] = None) -> bool:
        """Set value in cache."""
        try:
            # Check cache size and evict if necessary
            if len(self.cache) >= self.config.max_size:
                await self._evict_entry()
            
            # Create cache entry
            entry = CacheEntry(
                key=key,
                value=value,
                timestamp=time.time(),
                metadata=metadata
            )
            
            # Store entry
            self.cache[key] = entry
            
            # Update access order for LRU
            if self.config.strategy == CacheStrategy.LRU:
                if key in self.access_order:
                    self.access_order.remove(key)
                self.access_order.append(key)
            
            # Initialize access frequency for LFU
            if self.config.strategy == CacheStrategy.LFU:
                self.access_frequency[key] = 0
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to set cache entry: {e}")
            return False
    
    async def delete(self, key: str) -> bool:
        """Delete value from cache."""
        try:
            if key in self.cache:
                del self.cache[key]
                
                # Update access order for LRU
                if key in self.access_order:
                    self.access_order.remove(key)
                
                # Update access frequency for LFU
                if key in self.access_frequency:
                    del self.access_frequency[key]
                
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Failed to delete cache entry: {e}")
            return False
    
    async def clear(self) -> bool:
        """Clear all cache entries."""
        try:
            self.cache.clear()
            self.access_order.clear()
            self.access_frequency.clear()
            return True
            
        except Exception as e:
            logger.error(f"Failed to clear cache: {e}")
            return False
    
    async def size(self) -> int:
        """Get current cache size."""
        return len(self.cache)
    
    async def _evict_entry(self) -> None:
        """Evict an entry based on cache strategy."""
        if not self.cache:
            return
        
        if self.config.strategy == CacheStrategy.LRU:
            # Remove least recently used
            if self.access_order:
                key_to_evict = self.access_order[0]
                await self.delete(key_to_evict)
        
        elif self.config.strategy == CacheStrategy.LFU:
            # Remove least frequently used
            if self.access_frequency:
                key_to_evict = min(self.access_frequency.items(), key=lambda x: x[1])[0]
                await self.delete(key_to_evict)
        
        elif self.config.strategy == CacheStrategy.TTL:
            # Remove oldest entry
            oldest_key = min(self.cache.keys(), key=lambda k: self.cache[k].timestamp)
            await self.delete(oldest_key)
        
        else:
            # Default: remove random entry
            key_to_evict = next(iter(self.cache.keys()))
            await self.delete(key_to_evict)


class SemanticCache:
    """Semantic cache for RAG search results."""
    
    def __init__(self, config: CacheConfig, embedding_provider=None):
        """Initialize semantic cache.
        
        Args:
            config: Cache configuration
            embedding_provider: Embedding provider for semantic similarity
        """
        self.config = config
        self.embedding_provider = embedding_provider
        self.backend = InMemoryCacheBackend(config)
        
        # Cache statistics
        self.stats = {
            'hits': 0,
            'misses': 0,
            'evictions': 0,
            'total_queries': 0
        }
        
        # Start cleanup task
        if config.cleanup_interval > 0:
            asyncio.create_task(self._cleanup_task())
    
    async def get(self, query: str, strategy: str = "semantic") -> Optional[Any]:
        """Get value from cache using semantic similarity.
        
        Args:
            query: Search query
            strategy: Cache strategy
            
        Returns:
            Cached value if found, None otherwise
        """
        self.stats['total_queries'] += 1
        
        if strategy == "semantic":
            return await self._semantic_get(query)
        else:
            # Use exact key matching
            key = self._generate_key(query)
            result = await self.backend.get(key)
            if result:
                self.stats['hits'] += 1
            else:
                self.stats['misses'] += 1
            return result
    
    async def set(self, query: str, value: Any, metadata: Optional[Dict[str, Any]] = None) -> bool:
        """Set value in cache.
        
        Args:
            query: Search query
            value: Value to cache
            metadata: Additional metadata
            
        Returns:
            True if successful, False otherwise
        """
        key = self._generate_key(query)
        
        # Generate semantic vector if embedding provider is available
        semantic_vector = None
        if self.embedding_provider:
            try:
                semantic_vector = await self._generate_embedding(query)
            except Exception as e:
                logger.warning(f"Failed to generate semantic vector: {e}")
        
        # Add semantic vector to metadata
        if metadata is None:
            metadata = {}
        if semantic_vector:
            metadata['semantic_vector'] = semantic_vector
        
        return await self.backend.set(key, value, metadata)
    
    async def delete(self, query: str) -> bool:
        """Delete value from cache.
        
        Args:
            query: Search query
            
        Returns:
            True if successful, False otherwise
        """
        key = self._generate_key(query)
        return await self.backend.delete(key)
    
    async def clear(self) -> bool:
        """Clear all cache entries."""
        return await self.backend.clear()
    
    async def size(self) -> int:
        """Get current cache size."""
        return await self.backend.size()
    
    async def _semantic_get(self, query: str) -> Optional[Any]:
        """Get value using semantic similarity."""
        if not self.embedding_provider:
            return None
        
        try:
            # Generate query embedding
            query_vector = await self._generate_embedding(query)
            if not query_vector:
                return None
            
            # Find most similar cached entry
            best_match = None
            best_similarity = 0.0
            
            for key, entry in self.backend.cache.items():
                if entry.metadata and 'semantic_vector' in entry.metadata:
                    cached_vector = entry.metadata['semantic_vector']
                    similarity = self._calculate_cosine_similarity(query_vector, cached_vector)
                    
                    if similarity > best_similarity and similarity >= self.config.semantic_threshold:
                        best_similarity = similarity
                        best_match = entry
            
            if best_match:
                self.stats['hits'] += 1
                # Update access metadata
                best_match.access_count += 1
                best_match.last_accessed = time.time()
                return best_match.value
            
            self.stats['misses'] += 1
            return None
            
        except Exception as e:
            logger.error(f"Semantic cache lookup failed: {e}")
            self.stats['misses'] += 1
            return None
    
    async def _generate_embedding(self, text: str) -> Optional[List[float]]:
        """Generate embedding for text."""
        if not self.embedding_provider:
            return None
        
        try:
            # Use embedding provider to generate vector
            embedding = await self.embedding_provider.embed(text)
            return embedding
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            return None
    
    def _calculate_cosine_similarity(self, vector1: List[float], vector2: List[float]) -> float:
        """Calculate cosine similarity between two vectors."""
        try:
            if len(vector1) != len(vector2):
                return 0.0
            
            # Calculate dot product
            dot_product = sum(a * b for a, b in zip(vector1, vector2))
            
            # Calculate magnitudes
            magnitude1 = sum(a * a for a in vector1) ** 0.5
            magnitude2 = sum(b * b for b in vector2) ** 0.5
            
            if magnitude1 == 0 or magnitude2 == 0:
                return 0.0
            
            # Calculate cosine similarity
            similarity = dot_product / (magnitude1 * magnitude2)
            return max(-1.0, min(1.0, similarity))  # Clamp to [-1, 1]
            
        except Exception as e:
            logger.error(f"Failed to calculate cosine similarity: {e}")
            return 0.0
    
    def _generate_key(self, query: str) -> str:
        """Generate cache key for query."""
        # Create hash of query for consistent key generation
        return hashlib.md5(query.encode('utf-8')).hexdigest()
    
    async def _cleanup_task(self) -> None:
        """Background task for cache cleanup."""
        while True:
            try:
                await asyncio.sleep(self.config.cleanup_interval)
                await self._cleanup_expired_entries()
            except Exception as e:
                logger.error(f"Cache cleanup task failed: {e}")
    
    async def _cleanup_expired_entries(self) -> None:
        """Clean up expired cache entries."""
        if self.config.strategy != CacheStrategy.TTL:
            return
        
        current_time = time.time()
        expired_keys = []
        
        for key, entry in self.backend.cache.items():
            if current_time - entry.timestamp > self.config.ttl_seconds:
                expired_keys.append(key)
        
        for key in expired_keys:
            await self.backend.delete(key)
            self.stats['evictions'] += 1
        
        if expired_keys:
            logger.info(f"Cleaned up {len(expired_keys)} expired cache entries")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        hit_rate = 0.0
        if self.stats['total_queries'] > 0:
            hit_rate = self.stats['hits'] / self.stats['total_queries']
        
        return {
            **self.stats,
            'hit_rate': hit_rate,
            'cache_size': len(self.backend.cache),
            'max_size': self.config.max_size
        }
    
    def clear_stats(self) -> None:
        """Clear cache statistics."""
        self.stats = {
            'hits': 0,
            'misses': 0,
            'evictions': 0,
            'total_queries': 0
        }


class CacheManager:
    """Manager for multiple cache instances."""
    
    def __init__(self):
        """Initialize cache manager."""
        self.caches: Dict[str, SemanticCache] = {}
    
    def create_cache(self, name: str, config: CacheConfig, embedding_provider=None) -> SemanticCache:
        """Create a new cache instance.
        
        Args:
            name: Cache name
            config: Cache configuration
            embedding_provider: Embedding provider for semantic caching
            
        Returns:
            Created cache instance
        """
        cache = SemanticCache(config, embedding_provider)
        self.caches[name] = cache
        logger.info(f"Created cache '{name}' with strategy {config.strategy}")
        return cache
    
    def get_cache(self, name: str) -> Optional[SemanticCache]:
        """Get cache instance by name.
        
        Args:
            name: Cache name
            
        Returns:
            Cache instance if found, None otherwise
        """
        return self.caches.get(name)
    
    def list_caches(self) -> List[str]:
        """List all cache names."""
        return list(self.caches.keys())
    
    def remove_cache(self, name: str) -> bool:
        """Remove cache instance.
        
        Args:
            name: Cache name
            
        Returns:
            True if successful, False otherwise
        """
        if name in self.caches:
            cache = self.caches[name]
            asyncio.create_task(cache.clear())
            del self.caches[name]
            logger.info(f"Removed cache '{name}'")
            return True
        return False
    
    def get_all_stats(self) -> Dict[str, Dict[str, Any]]:
        """Get statistics for all caches."""
        return {name: cache.get_stats() for name, cache in self.caches.items()}
    
    def clear_all_caches(self) -> None:
        """Clear all caches."""
        for name, cache in self.caches.items():
            asyncio.create_task(cache.clear())
        logger.info("Cleared all caches")
