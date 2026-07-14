"""Pluggable analysis system for Oracle Mcp.

This module provides a framework for different types of database analysis
without hardcoding specific analysis logic into the core orchestrator.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Dict, List, Any, Optional, Pattern, TYPE_CHECKING
import re
from enum import Enum

from .logging_config import get_logger

if TYPE_CHECKING:
    from .task_system import Task, TaskType

logger = get_logger(__name__)


class AnalysisIntent(Enum):
    """Different types of analysis intents."""
    KNOWLEDGE_REQUEST = "knowledge"      # General tips, best practices
    LIVE_ANALYSIS = "analysis"          # Analyze actual database data  
    MONITORING = "monitoring"           # Real-time monitoring
    COMPARISON = "comparison"           # Compare periods/databases


@dataclass
class AnalysisContext:
    """Context for analysis request."""
    original_prompt: str
    intent: AnalysisIntent
    analysis_type: str
    database_required: bool = True
    estimated_duration: str = "medium"
    

class AnalysisPlugin(ABC):
    """Base class for analysis plugins."""
    
    @property
    @abstractmethod
    def plugin_name(self) -> str:
        """Name of the analysis plugin."""
        pass
    
    @property
    @abstractmethod
    def supported_keywords(self) -> List[str]:
        """Keywords that this plugin can handle."""
        pass
    
    @property 
    @abstractmethod
    def intent_patterns(self) -> Dict[AnalysisIntent, List[Pattern]]:
        """Regex patterns for different analysis intents."""
        pass
    
    @abstractmethod
    def can_handle(self, prompt: str) -> Optional[AnalysisContext]:
        """Check if this plugin can handle the given prompt.
        
        Args:
            prompt: User prompt to analyze
            
        Returns:
            AnalysisContext if can handle, None otherwise
        """
        pass
    
    @abstractmethod
    def decompose_to_tasks(self, context: AnalysisContext) -> List[Dict[str, Any]]:
        """Decompose analysis request into task specifications.
        
        Args:
            context: Analysis context
            
        Returns:
            List of task specifications (dicts) to be converted to Task objects
        """
        pass


class AWRAnalysisPlugin(AnalysisPlugin):
    """Plugin for AWR (Automatic Workload Repository) analysis."""
    
    def __init__(self):
        self.task_counter = 0
    
    @property
    def plugin_name(self) -> str:
        return "awr_analysis"
    
    @property
    def supported_keywords(self) -> List[str]:
        return ["awr", "workload", "repository", "snapshot"]
    
    @property
    def intent_patterns(self) -> Dict[AnalysisIntent, List[Pattern]]:
        return {
            AnalysisIntent.KNOWLEDGE_REQUEST: [
                re.compile(r"what\s+is\s+awr", re.IGNORECASE),
                re.compile(r"awr\s+(tips|best\s+practices|guide)", re.IGNORECASE),
                re.compile(r"how\s+to\s+use\s+awr", re.IGNORECASE),
            ],
            AnalysisIntent.LIVE_ANALYSIS: [
                re.compile(r"generate\s+awr\s+report", re.IGNORECASE),
                re.compile(r"analyze\s+awr", re.IGNORECASE),
                re.compile(r"show\s+awr\s+(data|results)", re.IGNORECASE),
                re.compile(r"create\s+awr\s+report", re.IGNORECASE),
            ],
            AnalysisIntent.MONITORING: [
                re.compile(r"monitor\s+workload", re.IGNORECASE),
                re.compile(r"real.?time\s+awr", re.IGNORECASE),
            ]
        }
    
    def can_handle(self, prompt: str) -> Optional[AnalysisContext]:
        """Check if prompt is AWR-related and determine intent."""
        # Check if any AWR keywords are present
        if not any(keyword in prompt.lower() for keyword in self.supported_keywords):
            return None
        
        # Determine intent based on patterns
        for intent, patterns in self.intent_patterns.items():
            for pattern in patterns:
                if pattern.search(prompt):
                    return AnalysisContext(
                        original_prompt=prompt,
                        intent=intent,
                        analysis_type="awr",
                        database_required=(intent != AnalysisIntent.KNOWLEDGE_REQUEST),
                        estimated_duration="long" if intent == AnalysisIntent.LIVE_ANALYSIS else "short"
                    )
        
        return None
    
    def decompose_to_tasks(self, context: AnalysisContext) -> List[Dict[str, Any]]:
        """Create tasks for AWR analysis based on intent."""
        if context.intent == AnalysisIntent.KNOWLEDGE_REQUEST:
            # For knowledge requests, use LLM to provide information
            return [
                {
                    "id": "awr_knowledge",
                    "task_type": "generate",
                    "description": "Provide AWR knowledge and best practices",
                    "tool_name": "generate_knowledge_response",
                    "parameters": {
                        "topic": "awr",
                        "prompt": context.original_prompt,
                        "knowledge_type": "oracle_awr"
                    },
                    "dependencies": [],
                    "can_parallel": True
                }
            ]
        
        elif context.intent == AnalysisIntent.LIVE_ANALYSIS:
            # For live analysis, generate actual AWR report
            return self._create_awr_analysis_tasks(context)
        
        else:
            # Default fallback
            return []
    
    def _create_awr_analysis_tasks(self, context: AnalysisContext) -> List[Dict[str, Any]]:
        """Create tasks for live AWR analysis."""
        tasks = []
        self.task_counter = 0
        
        # Task 1: Connect
        tasks.append({
            "id": "connect",
            "task_type": "connect",
            "description": "Connect to database",
            "tool_name": "mcp_oracle-sqlcl-mcp_connect",
            "parameters": {"connection_name": "tmbl"},
            "dependencies": [],
            "can_parallel": False
        })
        
        # Task 2: Check AWR availability
        check_task_id = self._get_next_id("check_awr")
        tasks.append({
            "id": check_task_id,
            "task_type": "query",
            "description": "Check AWR snapshot availability",
            "tool_name": "mcp_oracle-sqlcl-mcp_run-sql",
            "parameters": {"sql": "SELECT COUNT(*) as snapshot_count FROM dba_hist_snapshot WHERE ROWNUM <= 1"},
            "dependencies": ["connect"],
            "can_parallel": False
        })
        
        # Task 3: Get recent snapshots
        snapshots_task_id = self._get_next_id("get_snapshots")
        tasks.append({
            "id": snapshots_task_id,
            "task_type": "query",
            "description": "Get recent AWR snapshots",
            "tool_name": "mcp_oracle-sqlcl-mcp_run-sql",
            "parameters": {"sql": "SELECT snap_id, begin_interval_time FROM dba_hist_snapshot ORDER BY snap_id DESC FETCH FIRST 10 ROWS ONLY"},
            "dependencies": [check_task_id],
            "can_parallel": False
        })
        
        # Task 4: Generate AWR report
        tasks.append({
            "id": self._get_next_id("generate_awr"),
            "task_type": "analyze",
            "description": "Generate AWR report",
            "tool_name": "awr_report_generator",  # Specialized tool
            "parameters": {
                "prompt": context.original_prompt,
                "snapshots_from": snapshots_task_id,
                "analysis_type": "full_report"
            },
            "dependencies": [snapshots_task_id],
            "can_parallel": False
        })
        
        return tasks
    
    def _get_next_id(self, prefix: str) -> str:
        """Generate next task ID."""
        self.task_counter += 1
        return f"{prefix}_{self.task_counter}"


class PerformanceAnalysisPlugin(AnalysisPlugin):
    """Plugin for general performance analysis (non-AWR)."""
    
    @property
    def plugin_name(self) -> str:
        return "performance_analysis"
    
    @property
    def supported_keywords(self) -> List[str]:
        return ["performance", "tuning", "optimization", "slow", "bottleneck"]
    
    @property
    def intent_patterns(self) -> Dict[AnalysisIntent, List[Pattern]]:
        return {
            AnalysisIntent.KNOWLEDGE_REQUEST: [
                re.compile(r"performance\s+(tips|best\s+practices|guide|tuning)", re.IGNORECASE),
                re.compile(r"how\s+to\s+(optimize|tune|improve)", re.IGNORECASE),
                re.compile(r"what\s+are.*performance", re.IGNORECASE),
                re.compile(r"oracle\s+tuning", re.IGNORECASE),
            ],
            AnalysisIntent.LIVE_ANALYSIS: [
                re.compile(r"analyze\s+(current\s+)?performance", re.IGNORECASE),
                re.compile(r"check\s+performance", re.IGNORECASE),
                re.compile(r"find\s+(slow|bottleneck)", re.IGNORECASE),
                re.compile(r"performance\s+(issues|problems)", re.IGNORECASE),
            ]
        }
    
    def can_handle(self, prompt: str) -> Optional[AnalysisContext]:
        """Check if prompt is performance-related and determine intent."""
        # Skip if AWR-specific (let AWR plugin handle it)
        if any(awr_keyword in prompt.lower() for awr_keyword in ["awr", "workload", "repository"]):
            return None
        
        # Check if any performance keywords are present
        if not any(keyword in prompt.lower() for keyword in self.supported_keywords):
            return None
        
        # Determine intent based on patterns
        for intent, patterns in self.intent_patterns.items():
            for pattern in patterns:
                if pattern.search(prompt):
                    return AnalysisContext(
                        original_prompt=prompt,
                        intent=intent,
                        analysis_type="performance",
                        database_required=(intent == AnalysisIntent.LIVE_ANALYSIS),
                        estimated_duration="short" if intent == AnalysisIntent.KNOWLEDGE_REQUEST else "medium"
                    )
        
        return None
    
    def decompose_to_tasks(self, context: AnalysisContext) -> List[Dict[str, Any]]:
        """Create tasks for performance analysis based on intent."""
        if context.intent == AnalysisIntent.KNOWLEDGE_REQUEST:
            # For knowledge requests, provide general performance tips
            return [
                {
                    "id": "performance_knowledge",
                    "task_type": "generate",
                    "description": "Provide Oracle performance tuning tips",
                    "tool_name": "generate_knowledge_response",
                    "parameters": {
                        "topic": "performance",
                        "prompt": context.original_prompt,
                        "knowledge_type": "oracle_performance"
                    },
                    "dependencies": [],
                    "can_parallel": True
                }
            ]
        
        elif context.intent == AnalysisIntent.LIVE_ANALYSIS:
            # For live analysis, check current performance metrics
            return [
                {
                    "id": "connect",
                    "task_type": "connect",
                    "description": "Connect to database",
                    "tool_name": "mcp_oracle-sqlcl-mcp_connect",
                    "parameters": {"connection_name": "tmbl"},
                    "dependencies": [],
                    "can_parallel": False
                },
                {
                    "id": "check_performance",
                    "task_type": "analyze",
                    "description": "Check current performance metrics",
                    "tool_name": "performance_analyzer",
                    "parameters": {
                        "prompt": context.original_prompt,
                        "analysis_type": "current_metrics"
                    },
                    "dependencies": ["connect"],
                    "can_parallel": False
                }
            ]
        
        return []


class AnalysisPluginManager:
    """Manages analysis plugins and routes requests."""
    
    def __init__(self):
        """Initialize with default plugins."""
        self.plugins: List[AnalysisPlugin] = [
            AWRAnalysisPlugin(),
            PerformanceAnalysisPlugin(),
        ]
        logger.info(f"Initialized {len(self.plugins)} analysis plugins")
    
    def register_plugin(self, plugin: AnalysisPlugin) -> None:
        """Register a new analysis plugin."""
        self.plugins.append(plugin)
        logger.info(f"Registered plugin: {plugin.plugin_name}")
    
    def find_handler(self, prompt: str) -> Optional[tuple[AnalysisPlugin, AnalysisContext]]:
        """Find a plugin that can handle the given prompt.
        
        Args:
            prompt: User prompt to analyze
            
        Returns:
            Tuple of (plugin, context) if found, None otherwise
        """
        for plugin in self.plugins:
            context = plugin.can_handle(prompt)
            if context:
                logger.info(f"Plugin '{plugin.plugin_name}' can handle prompt with intent: {context.intent.value}")
                return plugin, context
        
        logger.debug(f"No plugin found to handle prompt: {prompt[:50]}...")
        return None
    
    def decompose_analysis_request(self, prompt: str) -> Optional[List[Dict[str, Any]]]:
        """Decompose analysis request into tasks using appropriate plugin.
        
        Args:
            prompt: User prompt to decompose
            
        Returns:
            List of tasks if handled by a plugin, None otherwise
        """
        handler_info = self.find_handler(prompt)
        if not handler_info:
            return None
        
        plugin, context = handler_info
        tasks = plugin.decompose_to_tasks(context)
        
        logger.info(f"Plugin '{plugin.plugin_name}' decomposed prompt into {len(tasks)} task specifications")
        return tasks
    
    def get_supported_analysis_types(self) -> Dict[str, List[str]]:
        """Get all supported analysis types and their keywords."""
        analysis_types = {}
        for plugin in self.plugins:
            analysis_types[plugin.plugin_name] = plugin.supported_keywords
        return analysis_types