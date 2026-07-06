"""Error classification for recovery strategy selection."""

import logging
from enum import Enum
from typing import Optional
from core.task_orchestrator import TaskExecutionResult

logger = logging.getLogger(__name__)


class ErrorType(Enum):
    """Enumeration of error types for recovery strategy selection."""
    TOOL_ASSIGNMENT_ERROR = "tool_assignment_error"
    EXECUTION_FAILURE = "execution_failure"
    EMPTY_RESULT_ERROR = "empty_result_error"
    UNKNOWN_ERROR = "unknown_error"


class ErrorClassifier:
    """Classify errors for appropriate recovery strategy selection.
    
    This class follows the Single Responsibility Principle by focusing solely
    on error classification.
    """
    
    def __init__(self):
        """Initialize the error classifier."""
        logger.debug("Initialized ErrorClassifier")
    
    def classify_error(self, task_result: TaskExecutionResult) -> ErrorType:
        """Classify the error type for a failed task.
        
        Args:
            task_result: Task execution result to classify
            
        Returns:
            ErrorType indicating the type of error
        """
        try:
            # Check error types in priority order (most specific first)
            if self._is_tool_assignment_error(task_result):
                return ErrorType.TOOL_ASSIGNMENT_ERROR
            elif self._is_execution_failure(task_result):
                return ErrorType.EXECUTION_FAILURE
            elif self._is_empty_result_error(task_result):
                return ErrorType.EMPTY_RESULT_ERROR
            else:
                return ErrorType.UNKNOWN_ERROR
                
        except Exception as e:
            logger.error(f"Error classifying error for task {task_result.task_id}: {e}")
            return ErrorType.UNKNOWN_ERROR
    
    def _is_tool_assignment_error(self, result: TaskExecutionResult) -> bool:
        """Check if error is related to tool assignment.
        
        Args:
            result: Task execution result
            
        Returns:
            True if tool assignment error, False otherwise
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
    
    def _is_execution_failure(self, result: TaskExecutionResult) -> bool:
        """Check if error is related to execution failure (suspicious timing).
        
        Args:
            result: Task execution result
            
        Returns:
            True if execution failure, False otherwise
        """
        # Check for suspicious execution time (too fast)
        if hasattr(result, 'execution_time') and result.execution_time is not None:
            return result.execution_time < 0.01
        
        return False
    
    def _is_empty_result_error(self, result: TaskExecutionResult) -> bool:
        """Check if error is related to empty or meaningless results.
        
        Args:
            result: Task execution result
            
        Returns:
            True if empty result error, False otherwise
        """
        if result.result is None:
            return True
        
        result_str = str(result.result).strip()
        return result_str == ""

