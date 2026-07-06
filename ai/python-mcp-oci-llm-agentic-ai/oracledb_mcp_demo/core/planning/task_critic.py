import json
from dataclasses import dataclass
from typing import List
from core.task_system import Task


@dataclass
class GenerationAnalysis:
    """Analysis result for generation tasks."""
    quality_score: float
    feedback: str
    suggestions: List[str]
    should_retry: bool


@dataclass
class ErrorAnalysis:
    """Analysis result for tool errors."""
    error_type: str
    repairable: bool
    suggested_fix: str
    confidence: float


class LLMTaskCritic:
    """Analyzes task results and provides feedback for improvement."""
    
    def __init__(self, llm_provider):
        self.llm_provider = llm_provider
    
    def analyze_generation_task(self, task: Task, result: str) -> GenerationAnalysis:
        """Analyze the quality of a generation task result.
        
        Args:
            task: The generation task that was executed
            result: The generated result
            
        Returns:
            Analysis with quality score, feedback, and retry recommendation
        """
        prompt = self._build_generation_prompt(task, result)
        
        response = self.llm_provider.generate(prompt)
        
        try:
            data = json.loads(response.content)
        except json.JSONDecodeError:
            raise ValueError("Invalid JSON response from LLM")
        
        # Validate required fields
        required_fields = ["quality_score", "feedback", "suggestions", "should_retry"]
        for field in required_fields:
            if field not in data:
                raise ValueError(f"Missing required field '{field}' in analysis")
        
        return GenerationAnalysis(
            quality_score=float(data["quality_score"]),
            feedback=data["feedback"],
            suggestions=data["suggestions"],
            should_retry=bool(data["should_retry"])
        )
    
    def analyze_tool_error(self, task: Task, error: str) -> ErrorAnalysis:
        """Analyze a tool execution error.
        
        Args:
            task: The task that failed
            error: The error message
            
        Returns:
            Analysis with error type, repairability, and suggested fix
        """
        prompt = self._build_error_prompt(task, error)
        
        response = self.llm_provider.generate(prompt)
        
        try:
            data = json.loads(response.content)
        except json.JSONDecodeError:
            raise ValueError("Invalid JSON response from LLM")
        
        # Validate required fields
        required_fields = ["error_type", "repairable", "suggested_fix", "confidence"]
        for field in required_fields:
            if field not in data:
                raise ValueError(f"Missing required field '{field}' in analysis")
        
        return ErrorAnalysis(
            error_type=data["error_type"],
            repairable=bool(data["repairable"]),
            suggested_fix=data["suggested_fix"],
            confidence=float(data["confidence"])
        )
    
    def _build_generation_prompt(self, task: Task, result: str) -> str:
        """Build prompt for generation task analysis."""
        return f"""
You are a task quality analyzer. Evaluate the following generation task result:

Task Request: {task.parameters.get('request', 'Unknown')}
Generated Result: {result}

Provide a JSON response with:
- quality_score: float (0-10, where 10 is perfect)
- feedback: string (detailed feedback on quality)
- suggestions: list of strings (specific improvements)
- should_retry: boolean (whether to retry with improvements)

Focus on:
- Correctness and completeness
- Adherence to Oracle SQL standards
- Best practices and conventions
- Potential issues or missing elements

Response format:
{{
    "quality_score": 8.5,
    "feedback": "Good SQL generation with proper data types",
    "suggestions": ["Add primary key constraint", "Consider adding indexes"],
    "should_retry": false
}}
"""
    
    def _build_error_prompt(self, task: Task, error: str) -> str:
        """Build prompt for error analysis."""
        return f"""
You are an error analyzer. Analyze the following tool execution error:

Task: {task.description}
Tool: {task.tool_name}
Parameters: {task.parameters}
Error: {error}

Provide a JSON response with:
- error_type: string (e.g., "ORA-00904", "ORA-12541")
- repairable: boolean (whether this can be fixed programmatically)
- suggested_fix: string (specific fix or explanation)
- confidence: float (0-1, confidence in the analysis)

Focus on:
- Oracle error codes and their meanings
- Whether the error is due to user input vs system issues
- Specific fixes for repairable errors
- Clear explanations for non-repairable errors

Response format:
{{
    "error_type": "ORA-00904",
    "repairable": true,
    "suggested_fix": "Use correct column name 'table_name' instead of 'name'",
    "confidence": 0.9
}}
"""
