"""Multi-tier RAG cache manager following SOLID principles."""

import asyncio
import logging
import time
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, List
from collections import OrderedDict
from datetime import datetime, timedelta, timezone

from .models import (
    CacheKey, CacheEntry, RAGResult, CacheStatistics, 
    CacheConfig, CacheMetrics, CacheLevel
)
from .cache_key_generator import CacheKeyGenerator, CacheKeyGeneratorFactory

logger = logging.getLogger(__name__)


class CacheStore(ABC):
    """Abstract base class for cache storage implementations."""
    
    @abstractmethod
    async def get(self, key: CacheKey) -> Optional[CacheEntry]:
        """Get cache entry by key."""
        pass
    
    @abstractmethod
    async def set(self, key: CacheKey, entry: CacheEntry) -> bool:
        """Set cache entry."""
        pass
    
    @abstractmethod
    async def delete(self, key: CacheKey) -> bool:
        """Delete cache entry."""
        pass
    
    @abstractmethod
    async def clear(self) -> bool:
        """Clear all cache entries."""
        pass
    
    @abstractmethod
    async def size(self) -> int:
        """Get current cache size."""
        pass


class InMemoryCacheStore(CacheStore):
    """In-memory cache store implementation."""
    
    def __init__(self, max_size: int = 1000):
        """Initialize in-memory cache store."""
        self.max_size = max_size
        self._cache: OrderedDict[CacheKey, CacheEntry] = OrderedDict()
        self._lock = asyncio.Lock()
    
    async def get(self, key: CacheKey) -> Optional[CacheEntry]:
        """Get cache entry by key."""
        async with self._lock:
            if key in self._cache:
                entry = self._cache[key]
                if entry.is_expired():
                    del self._cache[key]
                    return None
                
                # Move to end (LRU)
                self._cache.move_to_end(key)
                entry.touch()
                return entry
            
            return None
    
    async def set(self, key: CacheKey, entry: CacheEntry) -> bool:
        """Set cache entry."""
        async with self._lock:
            # Remove oldest entry if at capacity
            if len(self._cache) >= self.max_size:
                oldest_key = next(iter(self._cache))
                del self._cache[oldest_key]
            
            self._cache[key] = entry
            return True
    
    async def delete(self, key: CacheKey) -> bool:
        """Delete cache entry."""
        async with self._lock:
            if key in self._cache:
                del self._cache[key]
                return True
            return False
    
    async def clear(self) -> bool:
        """Clear all cache entries."""
        async with self._lock:
            self._cache.clear()
            return True
    
    async def size(self) -> int:
        """Get current cache size."""
        async with self._lock:
            return len(self._cache)
    
    async def cleanup_expired(self) -> int:
        """Remove expired entries and return count removed."""
        async with self._lock:
            expired_keys = [
                key for key, entry in self._cache.items() 
                if entry.is_expired()
            ]
            
            for key in expired_keys:
                del self._cache[key]
            
            return len(expired_keys)


class DistributedCacheStore(CacheStore):
    """Distributed cache store implementation (placeholder for Redis, etc.)."""
    
    def __init__(self, connection_string: str = ""):
        """Initialize distributed cache store."""
        self.connection_string = connection_string
        self._connected = False
    
    async def get(self, key: CacheKey) -> Optional[CacheEntry]:
        """Get cache entry by key."""
        # Placeholder implementation
        # In real implementation, this would connect to Redis/Memcached
        logger.debug(f"Distributed cache get: {key}")
        return None
    
    async def set(self, key: CacheKey, entry: CacheEntry) -> bool:
        """Set cache entry."""
        # Placeholder implementation
        logger.debug(f"Distributed cache set: {key}")
        return True
    
    async def delete(self, key: CacheKey) -> bool:
        """Delete cache entry."""
        # Placeholder implementation
        logger.debug(f"Distributed cache delete: {key}")
        return True
    
    async def clear(self) -> bool:
        """Clear all cache entries."""
        # Placeholder implementation
        logger.debug("Distributed cache clear")
        return True
    
    async def size(self) -> int:
        """Get current cache size."""
        # Placeholder implementation
        return 0


class RAGCacheManager:
    """Multi-tier RAG cache manager."""
    
    def __init__(self, config: CacheConfig = None):
        """Initialize RAG cache manager."""
        self.config = config or CacheConfig()
        self.config.validate()
        
        # Initialize cache stores
        self.l1_cache = InMemoryCacheStore(self.config.max_size)
        self.l2_cache = DistributedCacheStore()
        
        # Initialize key generator
        self.key_generator = CacheKeyGeneratorFactory.create_basic_generator()
        
        # Statistics tracking
        self.stats = CacheStatistics()
        self.metrics = CacheMetrics()
        
        # Start cleanup task only if event loop is running
        self._cleanup_task = None
        self._start_cleanup_task()
    
    async def get_cached_result(self, prompt: str, context: Dict[str, Any]) -> Optional[RAGResult]:
        """Get cached RAG result for prompt and context."""
        start_time = time.time()
        
        try:
            # Generate cache key
            cache_key = self.key_generator.generate_key(prompt, context)
            
            # Try L1 cache first (fastest)
            l1_result = await self._get_from_l1(cache_key)
            if l1_result:
                await self._record_hit(CacheLevel.L1)
                return l1_result
            
            # Try L2 cache (distributed)
            l2_result = await self._get_from_l2(cache_key)
            if l2_result:
                # Populate L1 cache
                await self._set_in_l1(cache_key, l2_result)
                await self._record_hit(CacheLevel.L2)
                return l2_result
            
            # Cache miss
            await self._record_miss()
            return None
            
        except Exception as e:
            logger.error(f"Error getting cached result: {e}")
            await self._record_miss()
            return None
        finally:
            # Update metrics
            response_time = (time.time() - start_time) * 1000  # Convert to ms
            self._update_response_time_metrics(response_time)
    
    async def set_cached_result(self, prompt: str, context: Dict[str, Any], 
                               result: RAGResult, ttl_seconds: Optional[int] = None) -> bool:
        """Set cached RAG result for prompt and context."""
        try:
            # Generate cache key
            cache_key = self.key_generator.generate_key(prompt, context)
            
            # Create cache entry
            ttl = ttl_seconds or self.config.default_ttl_seconds
            entry = CacheEntry(
                key=cache_key,
                result=result,
                ttl_seconds=ttl
            )
            
            # Set in both L1 and L2 caches
            l1_success = await self._set_in_l1(cache_key, entry)
            l2_success = await self._set_in_l2(cache_key, entry)
            
            if l1_success or l2_success:
                self.stats.total_entries += 1
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error setting cached result: {e}")
            return False
    
    async def invalidate_cache(self, prompt: str, context: Dict[str, Any]) -> bool:
        """Invalidate cached result for prompt and context."""
        try:
            # Generate cache key
            cache_key = self.key_generator.generate_key(prompt, context)
            
            # Remove from both caches
            l1_success = await self.l1_cache.delete(cache_key)
            l2_success = await self.l2_cache.delete(cache_key)
            
            if l1_success or l2_success:
                self.stats.total_invalidations += 1
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error invalidating cache: {e}")
            return False
    
    async def get_cache_statistics(self) -> Dict[str, Any]:
        """Get cache statistics."""
        # Update current metrics
        self.metrics.current_size = await self.l1_cache.size()
        
        return {
            **self.stats.to_dict(),
            **self.metrics.to_dict()
        }
    
    async def clear_cache(self) -> bool:
        """Clear all caches."""
        try:
            l1_success = await self.l1_cache.clear()
            l2_success = await self.l2_cache.clear()
            
            if l1_success or l2_success:
                self.stats.total_entries = 0
                self.metrics.current_size = 0
                return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error clearing cache: {e}")
            return False
    
    async def _get_from_l1(self, cache_key: CacheKey) -> Optional[RAGResult]:
        """Get result from L1 cache."""
        entry = await self.l1_cache.get(cache_key)
        if entry and not entry.is_expired():
            return entry.result
        return None
    
    async def _get_from_l2(self, cache_key: CacheKey) -> Optional[RAGResult]:
        """Get result from L2 cache."""
        entry = await self.l2_cache.get(cache_key)
        if entry and not entry.is_expired():
            return entry.result
        return None
    
    async def _set_in_l1(self, cache_key: CacheKey, entry: CacheEntry) -> bool:
        """Set entry in L1 cache."""
        return await self.l1_cache.set(cache_key, entry)
    
    async def _set_in_l2(self, cache_key: CacheKey, entry: CacheEntry) -> bool:
        """Set entry in L2 cache."""
        return await self.l2_cache.set(cache_key, entry)
    
    async def _record_hit(self, level: CacheLevel) -> None:
        """Record cache hit."""
        self.stats.total_hits += 1
        self._update_hit_rate_metrics()
    
    async def _record_miss(self) -> None:
        """Record cache miss."""
        self.stats.total_misses += 1
        self._update_hit_rate_metrics()
    
    def _update_hit_rate_metrics(self) -> None:
        """Update hit rate metrics."""
        # This would implement sliding window hit rate calculation
        # For now, just use overall hit rate
        pass
    
    def _update_response_time_metrics(self, response_time_ms: float) -> None:
        """Update response time metrics."""
        # Simple moving average
        if self.metrics.avg_response_time_ms == 0:
            self.metrics.avg_response_time_ms = response_time_ms
        else:
            self.metrics.avg_response_time_ms = (
                self.metrics.avg_response_time_ms * 0.9 + response_time_ms * 0.1
            )
    
    def _start_cleanup_task(self) -> None:
        """Start background cleanup task only if event loop is running."""
        try:
            # Check if we're in an async context
            loop = asyncio.get_running_loop()
            if loop and loop.is_running():
                self._cleanup_task = asyncio.create_task(self._cleanup_loop())
                logger.debug("Started background cleanup task")
            else:
                logger.debug("No running event loop, skipping cleanup task")
        except RuntimeError:
            # No running event loop
            logger.debug("No running event loop, skipping cleanup task")
    
    async def _cleanup_loop(self) -> None:
        """Background cleanup loop."""
        while True:
            try:
                await asyncio.sleep(self.config.cleanup_interval_seconds)
                await self._perform_cleanup()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in cleanup loop: {e}")
    
    async def _perform_cleanup(self) -> None:
        """Perform cache cleanup."""
        try:
            # Clean up expired entries
            l1_removed = await self.l1_cache.cleanup_expired()
            l2_removed = await self.l2_cache.cleanup_expired()
            
            total_removed = l1_removed + l2_removed
            if total_removed > 0:
                self.stats.total_evictions += total_removed
                logger.info(f"Cache cleanup removed {total_removed} expired entries")
            
            # Update metrics
            self.metrics.last_cleanup = datetime.now(timezone.utc)
            self.metrics.next_cleanup = (
                datetime.now(timezone.utc) + timedelta(seconds=self.config.cleanup_interval_seconds)
            )
            
        except Exception as e:
            logger.error(f"Error during cache cleanup: {e}")
    
    async def close(self) -> None:
        """Close cache manager and cleanup resources."""
        if self._cleanup_task:
            self._cleanup_task.cancel()
            try:
                await self._cleanup_task
            except asyncio.CancelledError:
                pass
        
        await self.l1_cache.clear()
        await self.l2_cache.clear()
