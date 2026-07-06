"""Recovery strategies for different types of task failures."""

import logging
from abc import ABC, abstractmethod
from typing import Optional, List, Dict, Any
from core.recovery.error_classifier import ErrorType
from core.task_system import Task, TaskType

logger = logging.getLogger(__name__)


class RecoveryStrategy(ABC):
    """Abstract base class for recovery strategies.
    
    This follows the Open/Closed Principle by allowing new recovery strategies
    to be added without modifying existing code.
    """
    
    @abstractmethod
    def can_handle(self, error_type: ErrorType) -> bool:
        """Check if this strategy can handle the given error type.
        
        Args:
            error_type: Type of error to check
            
        Returns:
            True if strategy can handle this error type, False otherwise
        """
        pass
    
    @abstractmethod
    def attempt_recovery(self, failed_task: Task, context: Dict[str, Any]) -> Optional[Any]:
        """Attempt to recover from the failure.
        
        Args:
            failed_task: The task that failed
            context: Context information from previous tasks
            
        Returns:
            Recovered task(s) or None if recovery failed
        """
        pass


class ToolCorrectionStrategy(RecoveryStrategy):
    """Recovery strategy for tool assignment errors.
    
    This strategy automatically corrects tool assignments based on task descriptions.
    """
    
    def __init__(self):
        """Initialize the tool correction strategy."""
        # Map intended actions to available tools
        self.tool_mapping = {
            'generate_optimized_sql': 'llm_analyzer',
            'generate optimized': 'llm_analyzer',
            'sql_analysis': 'llm_analyzer',
            'performance_review': 'llm_analyzer',
            'analyze_sql': 'llm_analyzer',
            'optimize_query': 'llm_analyzer',
            'cost_analysis': 'llm_analyzer'
        }
        logger.debug("Initialized ToolCorrectionStrategy")
    
    def can_handle(self, error_type: ErrorType) -> bool:
        """Check if this strategy can handle tool assignment errors.
        
        Args:
            error_type: Type of error to check
            
        Returns:
            True if strategy can handle tool assignment errors, False otherwise
        """
        return error_type == ErrorType.TOOL_ASSIGNMENT_ERROR
    
    def attempt_recovery(self, failed_task: Task, context: Dict[str, Any]) -> Optional[Task]:
        """Attempt to recover by correcting tool assignment.
        
        Args:
            failed_task: The task that failed due to tool assignment
            context: Context information from previous tasks
            
        Returns:
            Corrected task or None if recovery failed
        """
        try:
            intended_action = failed_task.description.lower()
            
            # Find matching action pattern
            for action_pattern, fallback_tool in self.tool_mapping.items():
                if action_pattern in intended_action:
                    logger.info(f"Correcting tool assignment for task {failed_task.id}: "
                              f"{failed_task.tool_name} -> {fallback_tool}")
                    
                    return self._create_corrected_task(failed_task, fallback_tool)
            
            logger.warning(f"No tool mapping found for task: {failed_task.description}")
            return None
            
        except Exception as e:
            logger.error(f"Error attempting tool correction recovery: {e}")
            return None
    
    def _create_corrected_task(self, failed_task: Task, correct_tool: str) -> Task:
        """Create a corrected version of the failed task.
        
        Args:
            failed_task: Original failed task
            correct_tool: Correct tool to use
            
        Returns:
            New task with corrected tool assignment
        """
        # Create new task with corrected tool, preserving all other attributes
        corrected_task = Task(
            id=failed_task.id,
            task_type=failed_task.task_type,
            description=failed_task.description,
            tool_name=correct_tool,
            parameters=getattr(failed_task, 'parameters', {}),
            dependencies=getattr(failed_task, 'dependencies', [])
        )
        
        return corrected_task


class TaskRecompositionStrategy(RecoveryStrategy):
    """Recovery strategy for execution failures by recomposing complex tasks.
    
    This strategy breaks down complex tasks into simpler subtasks.
    """
    
    def __init__(self):
        """Initialize the task recomposition strategy."""
        # Map task patterns to decomposition strategies
        self.decomposition_patterns = {
            'generate_optimized_sql': self._decompose_sql_optimization,
            'sql_analysis': self._decompose_sql_analysis,
            'performance_review': self._decompose_performance_review
        }
        logger.debug("Initialized TaskRecompositionStrategy")
    
    def can_handle(self, error_type: ErrorType) -> bool:
        """Check if this strategy can handle execution failures.
        
        Args:
            error_type: Type of error to check
            
        Returns:
            True if strategy can handle execution failures, False otherwise
        """
        return error_type in [ErrorType.EXECUTION_FAILURE, ErrorType.EMPTY_RESULT_ERROR]
    
    def attempt_recovery(self, failed_task: Task, context: Dict[str, Any]) -> Optional[List[Task]]:
        """Attempt to recover by recomposing the failed task.
        
        Args:
            failed_task: The task that failed
            context: Context information from previous tasks
            
        Returns:
            List of recomposed subtasks or None if recovery failed
        """
        try:
            task_description = failed_task.description.lower()
            
            # Find matching decomposition pattern
            for pattern, decomposition_func in self.decomposition_patterns.items():
                if pattern in task_description:
                    logger.info(f"Recomposing task {failed_task.id} using pattern: {pattern}")
                    
                    return decomposition_func(failed_task, context)
            
            logger.warning(f"No decomposition pattern found for task: {failed_task.description}")
            return None
            
        except Exception as e:
            logger.error(f"Error attempting task recomposition recovery: {e}")
            return None
    
    def _decompose_sql_optimization(self, failed_task: Task, context: Dict[str, Any]) -> List[Task]:
        """Decompose SQL optimization task into subtasks.
        
        Args:
            failed_task: Original failed task
            context: Context information from previous tasks
            
        Returns:
            List of subtasks for SQL optimization
        """
        # Create subtasks for SQL optimization
        subtasks = [
            Task(
                id=f"{failed_task.id}_analyze",
                task_type=TaskType.ANALYZE,
                description="analyze_original_sql",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=getattr(failed_task, 'dependencies', [])
            ),
            Task(
                id=f"{failed_task.id}_generate",
                task_type=TaskType.GENERATE,
                description="generate_optimized_version",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_analyze"]
            ),
            Task(
                id=f"{failed_task.id}_explain",
                task_type=TaskType.ANALYZE,
                description="explain_optimizations",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_generate"]
            )
        ]
        
        return subtasks
    
    def _decompose_sql_analysis(self, failed_task: Task, context: Dict[str, Any]) -> List[Task]:
        """Decompose SQL analysis task into subtasks.
        
        Args:
            failed_task: Original failed task
            context: Context information from previous tasks
            
        Returns:
            List of subtasks for SQL analysis
        """
        # Create subtasks for SQL analysis
        subtasks = [
            Task(
                id=f"{failed_task.id}_parse",
                task_type=TaskType.ANALYZE,
                description="parse_sql_structure",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=getattr(failed_task, 'dependencies', [])
            ),
            Task(
                id=f"{failed_task.id}_identify",
                task_type=TaskType.ANALYZE,
                description="identify_optimization_opportunities",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_parse"]
            ),
            Task(
                id=f"{failed_task.id}_recommend",
                task_type=TaskType.ANALYZE,
                description="recommend_improvements",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_identify"]
            )
        ]
        
        return subtasks
    
    def _decompose_performance_review(self, failed_task: Task, context: Dict[str, Any]) -> List[Task]:
        """Decompose performance review task into subtasks.
        
        Args:
            failed_task: Original failed task
            context: Context information from previous tasks
            
        Returns:
            List of subtasks for performance review
        """
        # Create subtasks for performance review
        subtasks = [
            Task(
                id=f"{failed_task.id}_assess",
                task_type=TaskType.ANALYZE,
                description="assess_current_performance",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=getattr(failed_task, 'dependencies', [])
            ),
            Task(
                id=f"{failed_task.id}_identify",
                task_type=TaskType.ANALYZE,
                description="identify_bottlenecks",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_assess"]
            ),
            Task(
                id=f"{failed_task.id}_plan",
                task_type=TaskType.ANALYZE,
                description="plan_optimization_strategy",
                tool_name="llm_analyzer",
                parameters=getattr(failed_task, 'parameters', {}),
                dependencies=[f"{failed_task.id}_identify"]
            )
        ]
        
        return subtasks
