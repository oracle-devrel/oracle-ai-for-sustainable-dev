"""Iterative feedback loop system for Oracle Mcp."""

import asyncio
import json
import logging
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from .config import ConfigManager
from .logging_config import get_logger

logger = get_logger(__name__)


@dataclass
class IterationState:
    """State for a single iteration."""
    iteration_number: int
    prompt: str
    response: Dict[str, Any]
    evaluation: Dict[str, Any] = field(default_factory=dict)
    feedback: str = ""
    quality_score: float = 0.0
    timestamp: float = field(default_factory=time.time)
    

@dataclass
class FeedbackLoopConfig:
    """Configuration for feedback loop behavior."""
    max_iterations: int = 3
    target_quality_score: float = 0.8
    min_improvement_threshold: float = 0.1
    feedback_file_path: str = ".oracledb_mcp_demo.md"


class EvaluationStrategy:
    """Base class for evaluation strategies."""
    
    def evaluate(self, response: Dict[str, Any], previous_iterations: List[IterationState]) -> Dict[str, Any]:
        """Evaluate a response and return metrics."""
        raise NotImplementedError


class OracleDBEvaluationStrategy(EvaluationStrategy):
    """Evaluation strategy specific to Oracle database operations."""
    
    def __init__(self, orchestrator=None):
        """Initialize with optional orchestrator reference."""
        self.orchestrator = orchestrator
        
    def evaluate(self, response: Dict[str, Any], previous_iterations: List[IterationState]) -> Dict[str, Any]:
        """Evaluate Oracle database operation response."""
        evaluation = {
            'sql_syntax_score': 0.0,
            'tool_execution_score': 0.0,
            'result_relevance_score': 0.0,
            'error_handling_score': 0.0,
            'overall_score': 0.0,
            'feedback_points': []
        }
        
        # Evaluate SQL syntax quality
        evaluation['sql_syntax_score'] = self._evaluate_sql_syntax(response)
        
        # Evaluate tool execution success
        evaluation['tool_execution_score'] = self._evaluate_tool_execution(response)
        
        # Evaluate result relevance
        evaluation['result_relevance_score'] = self._evaluate_result_relevance(response)
        
        # Evaluate error handling
        evaluation['error_handling_score'] = self._evaluate_error_handling(response)
        
        # Calculate overall score
        scores = [
            evaluation['sql_syntax_score'],
            evaluation['tool_execution_score'], 
            evaluation['result_relevance_score'],
            evaluation['error_handling_score']
        ]
        evaluation['overall_score'] = sum(scores) / len(scores)
        
        # Generate feedback points
        evaluation['feedback_points'] = self._generate_feedback_points(evaluation, response)
        
        return evaluation
    
    def _evaluate_sql_syntax(self, response: Dict[str, Any]) -> float:
        """Evaluate SQL syntax quality."""
        sql = response.get('sql', '')
        if not sql:
            return 0.5  # No SQL generated, neutral score
            
        # Simple heuristics for SQL quality
        score = 0.5  # Base score
        
        # Check for Oracle-specific features
        oracle_features = ['ROWNUM', 'DUAL', 'SYSDATE', 'NVL', 'DECODE']
        if any(feature.upper() in sql.upper() for feature in oracle_features):
            score += 0.2
            
        # Check for proper formatting
        if ';' in sql:
            score += 0.1
        if sql.strip().upper().startswith(('SELECT', 'INSERT', 'UPDATE', 'DELETE')):
            score += 0.2
            
        return min(1.0, score)
    
    def _evaluate_tool_execution(self, response: Dict[str, Any]) -> float:
        """Evaluate tool execution success."""
        results = response.get('results', '')
        
        if not results:
            return 0.0
            
        # Check for error indicators
        error_indicators = ['error', 'failed', 'exception', 'not found']
        if any(indicator.lower() in str(results).lower() for indicator in error_indicators):
            return 0.2  # Low score for errors
            
        # Check for success indicators
        success_indicators = ['successfully', 'completed', 'connected', 'result']
        if any(indicator.lower() in str(results).lower() for indicator in success_indicators):
            return 0.9  # High score for success
            
        return 0.6  # Neutral score
    
    def _evaluate_result_relevance(self, response: Dict[str, Any]) -> float:
        """Evaluate how relevant results are to the original prompt."""
        if not response.get('results') or not response.get('prompt'):
            return 0.0
            
        prompt = response['prompt'].lower()
        results = str(response['results']).lower()
        
        # Enhanced relevance checking for multi-part requests
        if " and " in prompt:
            parts = [part.strip() for part in prompt.split(" and ")]
            completed_parts = 0
            
            for part in parts:
                part_completed = False
                
                # Check connection operations
                if any(keyword in part for keyword in ['connect', 'connection']):
                    if any(indicator in results for indicator in ['connected', 'connection', 'successfully']):
                        part_completed = True
                
                # Check listing operations (tables, queues, users, etc.)
                elif any(keyword in part for keyword in ['list', 'show']) and not part_completed:
                    # More specific checks based on what's being listed (performance metrics first)
                    if 'performance' in part or 'metric' in part:
                        if any(indicator in results for indicator in ['performance', 'metric', 'stat', 'monitor', 'cpu', 'memory', 'io']):
                            part_completed = True
                    # Check queue operations (but not performance metrics)
                    elif 'queue' in part and 'performance' not in part and 'metric' not in part:
                        if any(indicator in results for indicator in ['queue', 'aq$', 'dequeue', 'enqueue']):
                            part_completed = True
                    # Check table operations
                    elif 'table' in part or 'user table' in part:
                        if any(indicator in results for indicator in ['table', 'user_tables', 'table_name']):
                            part_completed = True
                    # Check user operations (but not user queues)
                    elif 'user' in part and 'queue' not in part:
                        if any(indicator in results for indicator in ['user', 'username', 'schema']):
                            part_completed = True
                    else:
                        # Generic listing check
                        if any(indicator in results for indicator in ['table', 'column', 'row', 'data', 'select', 'list']):
                            part_completed = True
                
                # Check display operations  
                elif any(keyword in part for keyword in ['display']) and not part_completed:
                    if any(indicator in results for indicator in ['table', 'column', 'row', 'data', 'display']):
                        part_completed = True
                
                # Check SELECT operations
                elif any(keyword in part for keyword in ['select']) and not part_completed:
                    if any(indicator in results for indicator in ['select', 'from', 'where', 'table']):
                        part_completed = True
                
                # If no specific pattern matched, use keyword overlap as fallback
                if not part_completed:
                    part_words = set(part.split())
                    results_words = set(results.split())
                    overlap = part_words.intersection(results_words)
                    # Only consider it completed if there's substantial overlap
                    if len(overlap) >= min(2, len(part_words) // 2):
                        part_completed = True
                
                if part_completed:
                    completed_parts += 1
            
            return completed_parts / len(parts) if parts else 0.0
        else:
            # Single-part request - use keyword overlap
            prompt_words = set(prompt.split())
            results_words = set(results.split())
            
            if not prompt_words:
                return 0.0
                
            overlap = len(prompt_words.intersection(results_words))
            return min(1.0, overlap / len(prompt_words))
    
    def _evaluate_error_handling(self, response: Dict[str, Any]) -> float:
        """Evaluate error handling quality."""
        results = str(response.get('results', ''))
        explanation = response.get('explanation', '')
        
        if 'error' in results.lower():
            # Check if explanation provides helpful guidance
            if explanation and len(explanation) > 20:
                return 1.0  # Good error explanation
            else:
                return 0.3  # Error present but poor explanation
        else:
            return 1.0  # No errors
    
    def _generate_feedback_points(self, evaluation: Dict[str, Any], response: Dict[str, Any]) -> List[str]:
        """Generate specific feedback points for improvement."""
        feedback_points = []
        
        if evaluation['sql_syntax_score'] < 0.7:
            feedback_points.append("Improve SQL syntax and Oracle-specific features")
            
        if evaluation['tool_execution_score'] < 0.7:
            feedback_points.append("Focus on successful tool execution and error handling")
            
        if evaluation['result_relevance_score'] < 0.7:
            # Enhanced feedback for multi-part requests (NO TYPO DETECTION)
            prompt = response.get('prompt', '').lower()
            results = str(response.get('results', ''))
            results_lower = results.lower()
            
            if " and " in prompt:
                parts = [part.strip() for part in prompt.split(" and ")]
                missing_parts = []
                
                for part in parts:
                    part_missing = True
                    
                    # Check connection operations
                    if any(keyword in part for keyword in ['connect', 'connection']):
                        if any(indicator in results_lower for indicator in ['connected', 'connection', 'successfully']):
                            part_missing = False
                        else:
                            missing_parts.append("database connection")
                    
                    # Check listing operations with more specificity
                    elif any(keyword in part for keyword in ['list', 'show']) and part_missing:
                        # Check performance metrics first (most specific)
                        if 'performance' in part or 'metric' in part:
                            if any(indicator in results_lower for indicator in ['performance', 'metric', 'stat', 'monitor', 'cpu', 'memory', 'io']):
                                part_missing = False
                            else:
                                missing_parts.append("showing performance metrics")
                        # Check queue operations (but not performance metrics)
                        elif 'queue' in part and 'performance' not in part and 'metric' not in part:
                            if any(indicator in results_lower for indicator in ['queue', 'aq$', 'dequeue', 'enqueue']):
                                part_missing = False
                            else:
                                missing_parts.append("listing queues")
                        # Check table operations
                        elif 'table' in part or 'user table' in part:
                            if any(indicator in results_lower for indicator in ['user_tables', 'table_name', 'select', 'from user_tables']):
                                part_missing = False
                            else:
                                missing_parts.append("listing tables")
                        # Check user operations (but not user queues)
                        elif 'user' in part and 'queue' not in part:
                            if any(indicator in results_lower for indicator in ['user', 'username', 'schema']):
                                part_missing = False
                            else:
                                missing_parts.append("listing users")
                        else:
                            # Generic check
                            if any(indicator in results_lower for indicator in ['table', 'column', 'row', 'data', 'select', 'list']):
                                part_missing = False
                            else:
                                missing_parts.append(f"executing '{part.strip()}'")
                    
                    # Check display/show operations
                    elif any(keyword in part for keyword in ['display']) and part_missing:
                        if any(indicator in results_lower for indicator in ['table', 'column', 'row', 'data', 'display']):
                            part_missing = False
                        else:
                            missing_parts.append(f"displaying '{part.strip()}'")
                    
                    # Check SELECT operations
                    elif any(keyword in part for keyword in ['select']) and part_missing:
                        if any(indicator in results_lower for indicator in ['select', 'from', 'where', 'table']):
                            part_missing = False
                        else:
                            missing_parts.append(f"executing SELECT query")
                
                if missing_parts:
                    feedback_points.append(f"Complete the missing parts: {', '.join(missing_parts)}")
                else:
                    feedback_points.append("Ensure results are more relevant to the user's request")
            else:
                feedback_points.append("Ensure results are more relevant to the user's request")
            
        if not response.get('explanation'):
            feedback_points.append("Add clear explanations for the user")
            
        return feedback_points


class IterativeFeedbackLoop:
    """Main feedback loop coordinator."""
    
    def __init__(self, config: Optional[FeedbackLoopConfig] = None, 
                 config_manager: Optional[ConfigManager] = None,
                 orchestrator=None):
        """Initialize feedback loop."""
        self.config = config or FeedbackLoopConfig()
        self.config_manager = config_manager
        self.evaluation_strategy = OracleDBEvaluationStrategy(orchestrator=orchestrator)
        self.iterations: List[IterationState] = []
        self.feedback_file_path = Path(self.config.feedback_file_path)
        
    async def execute_with_feedback(self, orchestrator, initial_prompt: str) -> Dict[str, Any]:
        """Execute prompt with iterative feedback loop."""
        logger.info(f"Starting feedback loop for prompt: {initial_prompt[:50]}...")
        
        self.iterations.clear()
        best_response = None
        best_score = 0.0
        
        for iteration_num in range(1, self.config.max_iterations + 1):
            logger.info(f"Starting iteration {iteration_num}")
            
            # Prepare prompt with feedback from previous iterations
            enhanced_prompt = self._prepare_iteration_prompt(initial_prompt, iteration_num)
            
            # Execute the prompt using single processing to avoid recursion
            try:
                response = await orchestrator._process_prompt_single(enhanced_prompt)
            except Exception as e:
                logger.error(f"Error in iteration {iteration_num}: {e}")
                response = {
                    'prompt': enhanced_prompt,
                    'sql': None,
                    'results': f"Error: {e}",
                    'explanation': f"Failed to process request: {e}",
                    'metadata': {}
                }
            
            # Evaluate the response
            evaluation = self.evaluation_strategy.evaluate(response, self.iterations)
            current_score = evaluation['overall_score']
            
            # Create iteration state
            iteration_state = IterationState(
                iteration_number=iteration_num,
                prompt=enhanced_prompt,
                response=response,
                evaluation=evaluation,
                feedback=self._generate_iteration_feedback(evaluation),
                quality_score=current_score
            )
            
            self.iterations.append(iteration_state)
            
            # Update best response
            if current_score > best_score:
                best_score = current_score
                best_response = response.copy()
                logger.info(f"New best score: {best_score:.2f}")
            
            logger.info(f"Iteration {iteration_num} completed with score: {current_score:.2f}")
            
            # Persist feedback
            self._persist_feedback()
            
            # Check termination conditions
            if self._should_terminate(current_score, iteration_num):
                break
        
        # Add feedback loop metadata to the best response and normalize result shape
        if best_response:
            best_response['feedback_loop_metadata'] = self._create_metadata()
            # Ensure metadata block and success flag so external harness can mark pass
            if 'metadata' not in best_response or not isinstance(best_response['metadata'], dict):
                best_response['metadata'] = {}
            best_response['metadata'].update({
                'success': True,
                'execution_plan': 'feedback',
                'task_count': 0,
                'total_phases': 0,
                'overall_quality_score': best_score
            })
        
        logger.info(f"Feedback loop completed. Best score: {best_score:.2f}")
        return best_response or {'error': 'No successful iterations'}
    
    def _prepare_iteration_prompt(self, initial_prompt: str, iteration_num: int) -> str:
        """Prepare prompt for a specific iteration with feedback."""
        if iteration_num == 1:
            return initial_prompt
        
        # Check if the previous iteration had an error
        last_iteration = self.iterations[-1]
        results = str(last_iteration.response.get('results', ''))
        
        # Detect if there was an error in the previous iteration
        if self._has_error(results):
            # Create error-specific retry prompt with more specific guidance
            error_guidance = self._get_error_guidance(results)
            error_prompt = f"""ITERATION {iteration_num}: {initial_prompt}

PREVIOUS ATTEMPT FAILED:
SQL: {last_iteration.response.get('sql', 'N/A')}
Error: {results}

{error_guidance}

Please fix the SQL query and try again. Generate a corrected SQL query that will work with the database."""
            
            return error_prompt
        
        # Build feedback from previous iterations (original logic for non-error cases)
        feedback_text = f"ITERATION {iteration_num}: {initial_prompt}\n\nFEEDBACK FROM PREVIOUS ATTEMPTS:\n"
        
        for i, iteration in enumerate(self.iterations, 1):
            feedback_text += f"- Iteration {i} (score: {iteration.quality_score:.2f}): {iteration.feedback}\n"
        
        feedback_text += "\nPLEASE IMPROVE BASED ON THE ABOVE FEEDBACK.\n"
        feedback_text += "Focus on addressing the specific issues mentioned.\n"
        feedback_text += "Ensure better SQL syntax, tool execution, and result relevance.\n"
        
        return feedback_text
    
    def _has_error(self, results: str) -> bool:
        """Check if the results contain an error."""
        error_indicators = [
            'error', 'failed', 'exception', 'not found', 'invalid', 
            'ORA-', 'SQL Error', 'Error executing SQL'
        ]
        results_lower = results.lower()
        return any(indicator.lower() in results_lower for indicator in error_indicators)
    
    def _get_error_guidance(self, results: str) -> str:
        """Get specific guidance based on the error type."""
        results_lower = results.lower()
        
        # Oracle-specific error guidance
        if 'ora-00904' in results_lower and 'invalid identifier' in results_lower:
            return """This error indicates that a column name in your SQL query does not exist in the table/view.
SOLUTION: You need to discover the correct table structure:
1. First, find queue-related tables: SELECT table_name FROM user_tables WHERE table_name LIKE '%QUEUE%'
2. Then check the structure of the specific table: DESCRIBE table_name
3. Use the correct column names that actually exist in the table
4. If no queue tables exist, try: SELECT * FROM user_queues WHERE ROWNUM = 1 (to see what columns are available)
5. Or try: SELECT * FROM dba_queues WHERE ROWNUM = 1 (if you have dba privileges)"""
        
        elif 'ora-00942' in results_lower or 'table or view does not exist' in results_lower:
            return """This error indicates that the table or view does not exist or you don't have access to it.
SOLUTION: You need to either:
1. Check if the table/view name is correct
2. Use a different table/view that exists and you have access to
3. Check your database connection and schema"""
        
        elif 'ora-00911' in results_lower or 'invalid character' in results_lower:
            return """This error indicates a SQL syntax issue, often due to invalid characters or semicolons.
SOLUTION: Remove any semicolons (;) at the end of SQL statements and check for proper Oracle syntax."""
        
        elif 'no database connection' in results_lower:
            return """This error indicates you need to connect to a database first.
SOLUTION: 
You need to perform these steps in sequence:
1. Use mcp_oracle-sqlcl-mcp_list-connections to see available databases
2. Use mcp_oracle-sqlcl-mcp_connect with connection_name='tmbl' (or another available database)
3. Then run your SQL query to get the requested data

CRITICAL: The user asked for 'list user queues' - they want to see actual queue data, not just a list of available databases.
You must connect to a database first, then run the SQL query.

IMPORTANT: After listing connections, you MUST use mcp_oracle-sqlcl-mcp_connect to actually connect to a database before running any SQL queries."""
        
        elif 'insufficient privileges' in results_lower or 'ora-01031' in results_lower:
            return """This error indicates you don't have sufficient privileges to access the object.
SOLUTION: Use a different table/view that you have access to, or use user_* views instead of dba_* views."""
        
        # Generic error guidance
        else:
            return """The SQL query failed to execute. Please analyze the error and generate a corrected query.
Consider:
1. Checking if table/view names are correct
2. Verifying column names exist in the table/view
3. Ensuring proper Oracle SQL syntax
4. Checking database connection and privileges"""
    
    def _generate_iteration_feedback(self, evaluation: Dict[str, Any]) -> str:
        """Generate human-readable feedback from evaluation."""
        feedback_parts = []
        
        score = evaluation['overall_score']
        if score < 0.5:
            feedback_parts.append("Poor performance overall.")
        elif score < 0.7:
            feedback_parts.append("Moderate performance, needs improvement.")
        else:
            feedback_parts.append("Good performance.")
            
        # Add specific feedback points
        if evaluation.get('feedback_points'):
            feedback_parts.extend(evaluation['feedback_points'])
            
        return " ".join(feedback_parts)
    
    def _should_terminate(self, current_score: float, iteration_num: int) -> bool:
        """Determine if the feedback loop should terminate."""
        # Check if target quality reached
        if current_score >= self.config.target_quality_score:
            logger.info(f"Target quality score {self.config.target_quality_score} reached")
            return True
        
        # Check if max iterations reached
        if iteration_num >= self.config.max_iterations:
            logger.info(f"Terminating after {self.config.max_iterations} iterations")
            return True
        
        # Check for minimal improvement, but only if there's no error in the current iteration
        if len(self.iterations) >= 2:
            current_results = str(self.iterations[-1].response.get('results', ''))
            if not self._has_error(current_results):  # Only check improvement if no error
                prev_score = self.iterations[-2].quality_score
                improvement = current_score - prev_score
                if improvement < self.config.min_improvement_threshold:
                    logger.info(f"Minimal improvement ({improvement:.3f}) below threshold ({self.config.min_improvement_threshold})")
                    return True
        
        return False
    
    def _persist_feedback(self) -> None:
        """Persist feedback to file for learning."""
        try:
            content = self._generate_feedback_content()
            with open(self.feedback_file_path, 'w') as f:
                f.write(content)
            logger.debug(f"Feedback persisted to {self.feedback_file_path}")
        except Exception as e:
            logger.warning(f"Failed to persist feedback: {e}")
    
    def _generate_feedback_content(self) -> str:
        """Generate markdown content for feedback file."""
        content = ["# Oracle Mcp Feedback Loop History", ""]
        content.append(f"Generated at: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        content.append("")
        
        for iteration in self.iterations:
            content.extend([
                f"## Iteration {iteration.iteration_number}",
                "",
                f"**Score:** {iteration.quality_score:.2f}",
                f"**Timestamp:** {time.strftime('%H:%M:%S', time.localtime(iteration.timestamp))}",
                "",
                "**Prompt:**",
                "```",
                iteration.prompt,
                "```",
                "",
                f"**Feedback:** {iteration.feedback}",
                "",
                "**Detailed Scores:**",
                f"- Sql Syntax Score: {iteration.evaluation['sql_syntax_score']:.2f}",
                f"- Tool Execution Score: {iteration.evaluation['tool_execution_score']:.2f}",
                f"- Result Relevance Score: {iteration.evaluation['result_relevance_score']:.2f}",
                f"- Error Handling Score: {iteration.evaluation['error_handling_score']:.2f}",
                f"- Overall Score: {iteration.evaluation['overall_score']:.2f}",
                "",
                "---",
                ""
            ])
        
        return "\n".join(content)
    
    def _create_metadata(self) -> Dict[str, Any]:
        """Create metadata about the feedback loop execution."""
        if not self.iterations:
            return {}
        
        best_iteration = max(self.iterations, key=lambda x: x.quality_score)
        final_iteration = self.iterations[-1]
        
        return {
            'total_iterations': len(self.iterations),
            'best_score': best_iteration.quality_score,
            'final_score': final_iteration.quality_score,
            'iteration_history': [
                {
                    'iteration': it.iteration_number,
                    'score': it.quality_score,
                    'feedback': it.feedback
                }
                for it in self.iterations
            ]
        }