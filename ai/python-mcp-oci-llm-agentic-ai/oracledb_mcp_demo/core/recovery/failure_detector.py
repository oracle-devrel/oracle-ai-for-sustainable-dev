"""Task failure detection based on execution patterns."""

import logging
from typing import Optional
from core.task_orchestrator import TaskExecutionResult

logger = logging.getLogger(__name__)


class TaskFailureDetector:
    """Detect task execution failures based on execution patterns.
    
    This class follows the Single Responsibility Principle by focusing solely
    on detecting task execution failures.
    """
    
    def __init__(self, suspicious_execution_threshold: float = 0.01):
        """Initialize the failure detector.
        
        Args:
            suspicious_execution_threshold: Minimum execution time (seconds) 
                below which execution is considered suspicious
        """
        self.suspicious_execution_threshold = suspicious_execution_threshold
        logger.debug(f"Initialized TaskFailureDetector with threshold: {suspicious_execution_threshold}s")
    
    def detect_failure(self, task_result: TaskExecutionResult) -> bool:
        """Detect if a task has failed based on execution patterns.
        
        Args:
            task_result: Task execution result to analyze
            
        Returns:
            True if failure is detected, False otherwise
        """
        try:
            # Check multiple failure indicators
            failure_indicators = [
                self._is_suspicious_execution_time(task_result),
                self._is_tool_error(task_result),
                self._is_empty_result(task_result),
                self._is_explicit_failure(task_result)
            ]
            
            # Any failure indicator suggests a problem
            has_failure = any(failure_indicators)
            
            if has_failure:
                logger.debug(f"Failure detected for task {task_result.task_id}: {failure_indicators}")
            
            return has_failure
            
        except Exception as e:
            logger.error(f"Error detecting failure for task {task_result.task_id}: {e}")
            # If we can't detect failure, assume it's OK
            return False
    
    def _is_suspicious_execution_time(self, result: TaskExecutionResult) -> bool:
        """Check if execution time is suspiciously fast.
        
        Args:
            result: Task execution result
            
        Returns:
            True if execution time is suspicious, False otherwise
        """
        if not hasattr(result, 'execution_time') or result.execution_time is None:
            return False
        
        return result.execution_time < self.suspicious_execution_threshold
    
    def _is_tool_error(self, result: TaskExecutionResult) -> bool:
        """Check if result indicates a tool-related error.
        
        Args:
            result: Task execution result
            
        Returns:
            True if tool error is detected, False otherwise
        """
        if not result.result:
            return False
        
        result_str = str(result.result).lower()
        tool_error_indicators = [
            "unknown tool",
            "tool not found",
            "invalid tool",
            "tool error"
        ]
        
        return any(indicator in result_str for indicator in tool_error_indicators)
    
    def _is_empty_result(self, result: TaskExecutionResult) -> bool:
        """Check if result is empty or meaningless.
        
        Args:
            result: Task execution result
            
        Returns:
            True if result is empty, False otherwise
        """
        if result.result is None:
            return True
        
        result_str = str(result.result).strip()
        return result_str == ""
    
    def _is_explicit_failure(self, result: TaskExecutionResult) -> bool:
        """Check if task was explicitly marked as failed.
        
        Args:
            result: Task execution result
            
        Returns:
            True if explicitly failed, False otherwise
        """
        return hasattr(result, 'success') and result.success is False

