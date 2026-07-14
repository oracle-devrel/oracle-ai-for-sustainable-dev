"""Data models for RAG cache system."""

from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import List, Dict, Any, Optional
from enum import Enum


class CacheKey(str):
    """Type alias for cache keys."""
    pass


class CacheLevel(Enum):
    """Cache levels for multi-tier caching."""
    L1 = "l1"  # In-memory, fastest
    L2 = "l2"  # Distributed, persistent
    L3 = "l3"  # Semantic similarity


@dataclass
class RAGResult:
    """Result from RAG system query."""
    query: str
    results: List[Dict[str, Any]]
    total_found: int
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def is_empty(self) -> bool:
        """Check if result contains no data."""
        return len(self.results) == 0 or self.total_found == 0
    
    def get_relevance_score(self) -> float:
        """Get average relevance score from results."""
        if not self.results:
            return 0.0
        
        scores = [
            result.get("relevance_score", 0.0) 
            for result in self.results
        ]
        return sum(scores) / len(scores) if scores else 0.0


@dataclass
class CacheEntry:
    """Cache entry with metadata."""
    key: CacheKey
    result: RAGResult
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    accessed_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    access_count: int = 0
    ttl_seconds: Optional[int] = None
    
    def is_expired(self) -> bool:
        """Check if cache entry has expired."""
        if self.ttl_seconds is None:
            return False
        
        expiry_time = self.created_at + timedelta(seconds=self.ttl_seconds)
        return datetime.now(timezone.utc) > expiry_time
    
    def touch(self) -> None:
        """Update access timestamp and count."""
        self.accessed_at = datetime.now(timezone.utc)
        self.access_count += 1
    
    def get_age_seconds(self) -> float:
        """Get age of cache entry in seconds."""
        return (datetime.now(timezone.utc) - self.created_at).total_seconds()
    
    def get_time_since_last_access(self) -> float:
        """Get time since last access in seconds."""
        return (datetime.now(timezone.utc) - self.accessed_at).total_seconds()


@dataclass
class CacheStatistics:
    """Cache performance statistics."""
    total_entries: int = 0
    total_hits: int = 0
    total_misses: int = 0
    total_evictions: int = 0
    total_invalidations: int = 0
    cache_size_bytes: int = 0
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    
    @property
    def hit_rate(self) -> float:
        """Calculate cache hit rate."""
        total_requests = self.total_hits + self.total_misses
        return self.total_hits / total_requests if total_requests > 0 else 0.0
    
    @property
    def miss_rate(self) -> float:
        """Calculate cache miss rate."""
        return 1.0 - self.hit_rate
    
    @property
    def eviction_rate(self) -> float:
        """Calculate cache eviction rate."""
        total_entries_ever = self.total_entries + self.total_evictions
        return self.total_evictions / total_entries_ever if total_entries_ever > 0 else 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert statistics to dictionary."""
        return {
            "total_entries": self.total_entries,
            "total_hits": self.total_hits,
            "total_misses": self.total_misses,
            "total_evictions": self.total_evictions,
            "total_invalidations": self.total_invalidations,
            "cache_size_bytes": self.cache_size_bytes,
            "hit_rate": self.hit_rate,
            "miss_rate": self.miss_rate,
            "eviction_rate": self.eviction_rate,
            "created_at": self.created_at.isoformat(),
            "uptime_seconds": (datetime.now(timezone.utc) - self.created_at).total_seconds()
        }


@dataclass
class CacheConfig:
    """Configuration for cache system."""
    max_size: int = 1000
    default_ttl_seconds: int = 3600  # 1 hour
    cleanup_interval_seconds: int = 300  # 5 minutes
    max_memory_mb: int = 100
    enable_compression: bool = True
    enable_encryption: bool = False
    compression_threshold_bytes: int = 1024  # 1KB
    
    def validate(self) -> None:
        """Validate cache configuration."""
        if self.max_size <= 0:
            raise ValueError("max_size must be positive")
        if self.default_ttl_seconds <= 0:
            raise ValueError("default_ttl_seconds must be positive")
        if self.cleanup_interval_seconds <= 0:
            raise ValueError("cleanup_interval_seconds must be positive")
        if self.max_memory_mb <= 0:
            raise ValueError("max_memory_mb must be positive")
        if self.compression_threshold_bytes <= 0:
            raise ValueError("compression_threshold_bytes must be positive")


@dataclass
class CacheMetrics:
    """Real-time cache metrics."""
    current_size: int = 0
    current_memory_mb: float = 0.0
    hit_rate_1min: float = 0.0
    hit_rate_5min: float = 0.0
    hit_rate_1hour: float = 0.0
    avg_response_time_ms: float = 0.0
    last_cleanup: Optional[datetime] = None
    next_cleanup: Optional[datetime] = None
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary."""
        return {
            "current_size": self.current_size,
            "hit_rate_1min": self.hit_rate_1min,
            "hit_rate_5min": self.hit_rate_5min,
            "hit_rate_1hour": self.hit_rate_1hour,
            "avg_response_time_ms": self.avg_response_time_ms,
            "last_cleanup": self.last_cleanup.isoformat() if self.last_cleanup else None,
            "next_cleanup": self.next_cleanup.isoformat() if self.next_cleanup else None
        }
