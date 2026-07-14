"""Performance monitoring and metrics for RAG system."""

import logging
import time
import asyncio
from abc import ABC, abstractmethod
from typing import Dict, List, Any, Optional, Tuple, Union, Callable
from dataclasses import dataclass, field
from enum import Enum
from collections import defaultdict, deque
import statistics
from datetime import datetime, timedelta
import json

from .config import get_config_value

logger = logging.getLogger(__name__)


class MetricType(Enum):
    """Metric types."""
    COUNTER = "counter"
    GAUGE = "gauge"
    HISTOGRAM = "histogram"
    SUMMARY = "summary"


class MetricUnit(Enum):
    """Metric units."""
    COUNT = "count"
    PERCENT = "percent"
    SECONDS = "seconds"
    MILLISECONDS = "milliseconds"
    BYTES = "bytes"
    REQUESTS_PER_SECOND = "requests_per_second"


@dataclass
class MetricValue:
    """Metric value with metadata."""
    value: Union[int, float]
    timestamp: float
    labels: Optional[Dict[str, str]] = None
    metadata: Optional[Dict[str, Any]] = None


@dataclass
class Metric:
    """Metric definition."""
    name: str
    type: MetricType
    unit: MetricUnit
    description: str
    labels: List[str] = field(default_factory=list)
    values: deque = field(default_factory=lambda: deque(maxlen=1000))  # Keep last 1000 values


@dataclass
class PerformanceSnapshot:
    """Performance snapshot at a point in time."""
    timestamp: float
    metrics: Dict[str, MetricValue]
    system_info: Dict[str, Any]


class MetricCollector(ABC):
    """Abstract metric collector interface."""
    
    @abstractmethod
    def collect_metric(self, name: str, value: Union[int, float], 
                      labels: Optional[Dict[str, str]] = None) -> None:
        """Collect a metric value."""
        pass
    
    @abstractmethod
    def get_metric(self, name: str) -> Optional[Metric]:
        """Get metric by name."""
        pass
    
    @abstractmethod
    def get_all_metrics(self) -> Dict[str, Metric]:
        """Get all metrics."""
        pass


class InMemoryMetricCollector(MetricCollector):
    """In-memory metric collector."""
    
    def __init__(self):
        """Initialize in-memory metric collector."""
        self.metrics: Dict[str, Metric] = {}
        self._initialize_default_metrics()
    
    def _initialize_default_metrics(self) -> None:
        """Initialize default metrics."""
        default_metrics = [
            Metric("rag_query_total", MetricType.COUNTER, MetricUnit.COUNT, "Total RAG queries"),
            Metric("rag_query_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "RAG query duration"),
            Metric("rag_cache_hit_rate", MetricType.GAUGE, MetricUnit.PERCENT, "RAG cache hit rate"),
            Metric("rag_search_strategy_usage", MetricType.COUNTER, MetricUnit.COUNT, "Search strategy usage"),
            Metric("rag_vector_search_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Vector search duration"),
            Metric("rag_keyword_search_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Keyword search duration"),
            Metric("rag_semantic_search_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Semantic search duration"),
            Metric("rag_mmr_search_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "MMR search duration"),
            Metric("rag_hybrid_search_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Hybrid search duration"),
            Metric("rag_results_count", MetricType.HISTOGRAM, MetricUnit.COUNT, "Number of results returned"),
            Metric("rag_relevance_score", MetricType.HISTOGRAM, MetricUnit.PERCENT, "Result relevance scores"),
            Metric("rag_cache_size", MetricType.GAUGE, MetricUnit.COUNT, "Current cache size"),
            Metric("rag_cache_evictions", MetricType.COUNTER, MetricUnit.COUNT, "Cache evictions"),
            Metric("rag_embedding_generation_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Embedding generation duration"),
            Metric("rag_database_connection_pool_size", MetricType.GAUGE, MetricUnit.COUNT, "Database connection pool size"),
            Metric("rag_database_query_duration", MetricType.HISTOGRAM, MetricUnit.MILLISECONDS, "Database query duration"),
            Metric("rag_system_memory_usage", MetricType.GAUGE, MetricUnit.BYTES, "System memory usage"),
            Metric("rag_system_cpu_usage", MetricType.GAUGE, MetricUnit.PERCENT, "System CPU usage"),
        ]
        
        for metric in default_metrics:
            self.metrics[metric.name] = metric
    
    def collect_metric(self, name: str, value: Union[int, float], 
                      labels: Optional[Dict[str, str]] = None) -> None:
        """Collect a metric value."""
        if name not in self.metrics:
            logger.warning(f"Unknown metric: {name}")
            return
        
        metric = self.metrics[name]
        metric_value = MetricValue(
            value=value,
            timestamp=time.time(),
            labels=labels,
            metadata={'collector': 'in_memory'}
        )
        
        metric.values.append(metric_value)
        logger.debug(f"Collected metric {name}: {value}")
    
    def get_metric(self, name: str) -> Optional[Metric]:
        """Get metric by name."""
        return self.metrics.get(name)
    
    def get_all_metrics(self) -> Dict[str, Metric]:
        """Get all metrics."""
        return self.metrics.copy()


class PerformanceMonitor:
    """Performance monitor for RAG system."""
    
    def __init__(self, collector: MetricCollector = None):
        """Initialize performance monitor.
        
        Args:
            collector: Metric collector (defaults to in-memory collector)
        """
        self.collector = collector or InMemoryMetricCollector()
        self.start_time = time.time()
        self.query_timers: Dict[str, float] = {}
        self.active_queries: Dict[str, Dict[str, Any]] = {}
        
        # Performance thresholds
        self.thresholds = {
            'query_duration_warning': get_config_value('performance.query_duration_warning_ms', 1000),  # From config
            'query_duration_error': get_config_value('performance.query_duration_error_ms', 5000),      # From config
            'cache_hit_rate_minimum': get_config_value('performance.cache_hit_rate_minimum', 0.5),      # From config
            'memory_usage_warning': get_config_value('performance.memory_usage_warning', 0.8),         # From config
            'cpu_usage_warning': get_config_value('performance.cpu_usage_warning', 0.8)               # From config
        }
        
        # Alert handlers
        self.alert_handlers: List[Callable[[str, Dict[str, Any]], None]] = []
        
        # Start monitoring tasks
        asyncio.create_task(self._system_monitoring_task())
    
    def start_query_timer(self, query_id: str, query_text: str, strategy: str) -> None:
        """Start timing a query.
        
        Args:
            query_id: Unique query identifier
            query_text: Query text
            strategy: Search strategy used
        """
        self.query_timers[query_id] = time.time()
        self.active_queries[query_id] = {
            'text': query_text,
            'strategy': strategy,
            'start_time': time.time()
        }
        
        # Collect query start metric
        self.collector.collect_metric("rag_query_total", 1, {'strategy': strategy})
    
    def stop_query_timer(self, query_id: str, results_count: int = 0, 
                        relevance_scores: Optional[List[float]] = None) -> float:
        """Stop timing a query and collect metrics.
        
        Args:
            query_id: Unique query identifier
            results_count: Number of results returned
            relevance_scores: List of relevance scores
            
        Returns:
            Query duration in milliseconds
        """
        if query_id not in self.query_timers:
            logger.warning(f"Query timer not found for ID: {query_id}")
            return 0.0
        
        start_time = self.query_timers[query_id]
        duration_ms = (time.time() - start_time) * 1000
        
        # Remove timer
        del self.query_timers[query_id]
        
        # Get query info
        query_info = self.active_queries.pop(query_id, {})
        strategy = query_info.get('strategy', 'unknown')
        
        # Collect duration metric
        self.collector.collect_metric("rag_query_duration", duration_ms, {'strategy': strategy})
        
        # Collect results count metric
        if results_count > 0:
            self.collector.collect_metric("rag_results_count", results_count, {'strategy': strategy})
        
        # Collect relevance score metrics
        if relevance_scores:
            for score in relevance_scores:
                self.collector.collect_metric("rag_relevance_score", score * 100, {'strategy': strategy})
        
        # Check performance thresholds
        self._check_performance_thresholds(duration_ms, strategy)
        
        return duration_ms
    
    def record_search_duration(self, strategy: str, duration_ms: float) -> None:
        """Record search strategy duration.
        
        Args:
            strategy: Search strategy name
            duration_ms: Duration in milliseconds
        """
        metric_name = f"rag_{strategy.lower()}_search_duration"
        self.collector.collect_metric(metric_name, duration_ms, {'strategy': strategy})
    
    def record_cache_metrics(self, hit_rate: float, cache_size: int, evictions: int) -> None:
        """Record cache performance metrics.
        
        Args:
            hit_rate: Cache hit rate (0.0 to 1.0)
            cache_size: Current cache size
            evictions: Number of cache evictions
        """
        self.collector.collect_metric("rag_cache_hit_rate", hit_rate * 100)
        self.collector.collect_metric("rag_cache_size", cache_size)
        self.collector.collect_metric("rag_cache_evictions", evictions)
        
        # Check cache hit rate threshold
        if hit_rate < self.thresholds['cache_hit_rate_minimum']:
            self._trigger_alert("cache_hit_rate_low", {
                'current_rate': hit_rate,
                'threshold': self.thresholds['cache_hit_rate_minimum']
            })
    
    def record_embedding_duration(self, duration_ms: float) -> None:
        """Record embedding generation duration.
        
        Args:
            duration_ms: Duration in milliseconds
        """
        self.collector.collect_metric("rag_embedding_generation_duration", duration_ms)
    
    def record_database_metrics(self, connection_pool_size: int, query_duration_ms: float) -> None:
        """Record database performance metrics.
        
        Args:
            connection_pool_size: Current connection pool size
            query_duration_ms: Database query duration in milliseconds
        """
        self.collector.collect_metric("rag_database_connection_pool_size", connection_pool_size)
        self.collector.collect_metric("rag_database_query_duration", query_duration_ms)
    
    def record_system_metrics(self, memory_usage_bytes: int, cpu_usage_percent: float) -> None:
        """Record system performance metrics.
        
        Args:
            memory_usage_bytes: Memory usage in bytes
            cpu_usage_percent: CPU usage percentage
        """
        self.collector.collect_metric("rag_system_memory_usage", memory_usage_bytes)
        self.collector.collect_metric("rag_system_cpu_usage", cpu_usage_percent)
        
        # Check system thresholds
        if memory_usage_percent > self.thresholds['memory_usage_warning']:
            self._trigger_alert("memory_usage_high", {
                'current_usage': memory_usage_percent,
                'threshold': self.thresholds['memory_usage_warning']
            })
        
        if cpu_usage_percent > self.thresholds['cpu_usage_warning']:
            self._trigger_alert("cpu_usage_high", {
                'current_usage': cpu_usage_percent,
                'threshold': self.thresholds['cpu_usage_warning']
            })
    
    def _check_performance_thresholds(self, duration_ms: float, strategy: str) -> None:
        """Check performance thresholds and trigger alerts if needed."""
        if duration_ms > self.thresholds['query_duration_error']:
            self._trigger_alert("query_duration_error", {
                'duration_ms': duration_ms,
                'strategy': strategy,
                'threshold': self.thresholds['query_duration_error']
            })
        elif duration_ms > self.thresholds['query_duration_warning']:
            self._trigger_alert("query_duration_warning", {
                'duration_ms': duration_ms,
                'strategy': strategy,
                'threshold': self.thresholds['query_duration_warning']
            })
    
    def _trigger_alert(self, alert_type: str, alert_data: Dict[str, Any]) -> None:
        """Trigger a performance alert.
        
        Args:
            alert_type: Type of alert
            alert_data: Alert data
        """
        alert = {
            'type': alert_type,
            'timestamp': time.time(),
            'data': alert_data
        }
        
        logger.warning(f"Performance alert: {alert_type} - {alert_data}")
        
        # Call alert handlers
        for handler in self.alert_handlers:
            try:
                handler(alert_type, alert_data)
            except Exception as e:
                logger.error(f"Alert handler failed: {e}")
    
    def add_alert_handler(self, handler: Callable[[str, Dict[str, Any]], None]) -> None:
        """Add an alert handler.
        
        Args:
            handler: Function to call when alerts are triggered
        """
        self.alert_handlers.append(handler)
    
    def get_performance_summary(self) -> Dict[str, Any]:
        """Get performance summary."""
        summary = {
            'uptime_seconds': time.time() - self.start_time,
            'active_queries': len(self.active_queries),
            'total_queries': self._get_total_queries(),
            'average_query_duration_ms': self._get_average_query_duration(),
            'cache_hit_rate': self._get_cache_hit_rate(),
            'search_strategy_usage': self._get_search_strategy_usage(),
            'system_health': self._get_system_health()
        }
        
        return summary
    
    def _get_total_queries(self) -> int:
        """Get total number of queries."""
        metric = self.collector.get_metric("rag_query_total")
        if metric and metric.values:
            return sum(value.value for value in metric.values)
        return 0
    
    def _get_average_query_duration(self) -> float:
        """Get average query duration."""
        metric = self.collector.get_metric("rag_query_duration")
        if metric and metric.values:
            values = [value.value for value in metric.values]
            return statistics.mean(values) if values else 0.0
        return 0.0
    
    def _get_cache_hit_rate(self) -> float:
        """Get current cache hit rate."""
        metric = self.collector.get_metric("rag_cache_hit_rate")
        if metric and metric.values:
            # Get the most recent value
            return metric.values[-1].value / 100.0  # Convert from percentage
        return 0.0
    
    def _get_search_strategy_usage(self) -> Dict[str, int]:
        """Get search strategy usage statistics."""
        usage = defaultdict(int)
        metric = self.collector.get_metric("rag_query_total")
        
        if metric and metric.values:
            for value in metric.values:
                if value.labels and 'strategy' in value.labels:
                    strategy = value.labels['strategy']
                    usage[strategy] += value.value
        
        return dict(usage)
    
    def _get_system_health(self) -> Dict[str, Any]:
        """Get system health indicators."""
        health = {
            'memory_usage': 0.0,
            'cpu_usage': 0.0,
            'database_connections': 0,
            'cache_size': 0
        }
        
        # Get memory usage
        memory_metric = self.collector.get_metric("rag_system_memory_usage")
        if memory_metric and memory_metric.values:
            health['memory_usage'] = memory_metric.values[-1].value
        
        # Get CPU usage
        cpu_metric = self.collector.get_metric("rag_system_cpu_usage")
        if cpu_metric and cpu_metric.values:
            health['cpu_usage'] = cpu_metric.values[-1].value
        
        # Get database connections
        db_metric = self.collector.get_metric("rag_database_connection_pool_size")
        if db_metric and db_metric.values:
            health['database_connections'] = db_metric.values[-1].value
        
        # Get cache size
        cache_metric = self.collector.get_metric("rag_cache_size")
        if cache_metric and cache_metric.values:
            health['cache_size'] = cache_metric.values[-1].value
        
        return health
    
    async def _system_monitoring_task(self) -> None:
        """Background task for system monitoring."""
        while True:
            try:
                # Collect system metrics (this would integrate with actual system monitoring)
                # For now, we'll use placeholder values
                import psutil
                
                memory_usage = psutil.virtual_memory().percent
                cpu_usage = psutil.cpu_percent(interval=1)
                
                self.record_system_metrics(
                    memory_usage_bytes=int(memory_usage * 1024 * 1024),  # Convert to bytes
                    cpu_usage_percent=cpu_usage
                )
                
                await asyncio.sleep(60)  # Update every minute
                
            except ImportError:
                # psutil not available, skip system monitoring
                logger.debug("psutil not available, skipping system monitoring")
                await asyncio.sleep(300)  # Check every 5 minutes
            except Exception as e:
                logger.error(f"System monitoring task failed: {e}")
                await asyncio.sleep(60)
    
    def export_metrics(self, format: str = "json") -> str:
        """Export metrics in specified format.
        
        Args:
            format: Export format (json, prometheus, etc.)
            
        Returns:
            Exported metrics string
        """
        if format.lower() == "json":
            return self._export_json()
        elif format.lower() == "prometheus":
            return self._export_prometheus()
        else:
            raise ValueError(f"Unsupported export format: {format}")
    
    def _export_json(self) -> str:
        """Export metrics as JSON."""
        export_data = {
            'timestamp': datetime.now().isoformat(),
            'metrics': {},
            'summary': self.get_performance_summary()
        }
        
        for name, metric in self.collector.get_all_metrics().items():
            export_data['metrics'][name] = {
                'type': metric.type.value,
                'unit': metric.unit.value,
                'description': metric.description,
                'values': [
                    {
                        'value': value.value,
                        'timestamp': datetime.fromtimestamp(value.timestamp).isoformat(),
                        'labels': value.labels
                    }
                    for value in metric.values
                ]
            }
        
        return json.dumps(export_data, indent=2)
    
    def _export_prometheus(self) -> str:
        """Export metrics in Prometheus format."""
        prometheus_lines = []
        
        for name, metric in self.collector.get_all_metrics().items():
            # Add metric help
            prometheus_lines.append(f"# HELP {name} {metric.description}")
            prometheus_lines.append(f"# TYPE {name} {metric.type.value}")
            
            # Add metric values
            for value in metric.values:
                labels_str = ""
                if value.labels:
                    label_pairs = [f'{k}="{v}"' for k, v in value.labels.items()]
                    labels_str = "{" + ",".join(label_pairs) + "}"
                
                prometheus_lines.append(f"{name}{labels_str} {value.value}")
        
        return "\n".join(prometheus_lines)
    
    def clear_metrics(self) -> None:
        """Clear all collected metrics."""
        for metric in self.collector.get_all_metrics().values():
            metric.values.clear()
        
        logger.info("Cleared all performance metrics")


class PerformanceDecorator:
    """Decorator for measuring function performance."""
    
    def __init__(self, monitor: PerformanceMonitor, metric_name: str, 
                 labels: Optional[Dict[str, str]] = None):
        """Initialize performance decorator.
        
        Args:
            monitor: Performance monitor instance
            metric_name: Name of the metric to collect
            labels: Additional labels for the metric
        """
        self.monitor = monitor
        self.metric_name = metric_name
        self.labels = labels or {}
    
    def __call__(self, func):
        """Decorate function with performance monitoring."""
        async def async_wrapper(*args, **kwargs):
            start_time = time.time()
            try:
                result = await func(*args, **kwargs)
                return result
            finally:
                duration_ms = (time.time() - start_time) * 1000
                self.monitor.collector.collect_metric(
                    self.metric_name, duration_ms, self.labels
                )
        
        def sync_wrapper(*args, **kwargs):
            start_time = time.time()
            try:
                result = func(*args, **kwargs)
                return result
            finally:
                duration_ms = (time.time() - start_time) * 1000
                self.monitor.collector.collect_metric(
                    self.metric_name, duration_ms, self.labels
                )
        
        if asyncio.iscoroutinefunction(func):
            return async_wrapper
        else:
            return sync_wrapper


def monitor_performance(monitor: PerformanceMonitor, metric_name: str, 
                       labels: Optional[Dict[str, str]] = None):
    """Decorator factory for performance monitoring."""
    return PerformanceDecorator(monitor, metric_name, labels)
