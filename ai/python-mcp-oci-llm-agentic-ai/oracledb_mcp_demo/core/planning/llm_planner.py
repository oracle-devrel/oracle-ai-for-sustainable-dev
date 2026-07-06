import json
from typing import List, Optional, Dict, Any
from core.planning.context_builder import PlanningContext
from core.task_system import Task, TaskType
import logging


class LLMPlanner:
    """Generates task execution plans using LLM with optional RAG context enhancement."""
    
    def __init__(self, llm_provider, rag_context_provider=None):
        self.llm_provider = llm_provider
        self.rag_context_provider = rag_context_provider
    
    def generate_plan(self, context: PlanningContext, rag_context: Optional[Dict[str, Any]] = None) -> List[Task]:
        """Generate a task execution plan using LLM with optional RAG context.
        
        Args:
            context: Planning context with prompt and state
            rag_context: Optional RAG context to enhance planning
            
        Returns:
            List of tasks for execution
            
        Raises:
            ValueError: If LLM response is invalid
        """
        prompt = self._build_enhanced_prompt(context, rag_context)
        
        # Log the planning prompt
        from ..logging_config import get_logger
        logger = get_logger(__name__)
        logger.info(f"🤖 LLM Planning: {context.prompt[:100]}...")
        
        # Log RAG context usage (only if available)
        if rag_context and rag_context.get("has_context"):
            logger.info(f"🎯 RAG Context: {rag_context.get('result_count', 0)} patterns found")
            logger.info("📋 RAG Context Details:")
            logger.info("=" * 40)
            if rag_context.get('formatted_results'):
                formatted_lines = rag_context['formatted_results'].split('\n')
                for i, line in enumerate(formatted_lines, 1):
                    if line.strip():
                        logger.info(f"  {i}. {line}")
            logger.info("=" * 40)
        else:
            logger.debug("ℹ️  No RAG context available for planning")
        
        # Call LLM
        response = self.llm_provider.generate(prompt)
        
        # Parse response - handle markdown-wrapped JSON
        content = response.content.strip()
        
        # Only log detailed response info when there's a failure
        content_length = len(content)
        if content_length == 0:
            logger.error("╭───────────────────────────── 🚨 EMPTY LLM RESPONSE ─────────────────────────────╮")
            logger.error("│ ❌ LLM returned empty response")
            logger.error("╰─────────────────────────────────────────────────────────────────────────────────────╯")
            raise ValueError("Empty response from LLM")
        
        # Remove markdown code blocks if present
        if content.startswith("```json"):
            content = content[7:]  # Remove ```json
        if content.startswith("```"):
            content = content[3:]   # Remove ```
        if content.endswith("```"):
            content = content[:-3]  # Remove trailing ```
        
        content = content.strip()
        
        try:
            json_data = json.loads(content)
        except json.JSONDecodeError as e:
            # Only log detailed content when JSON parsing fails
            logger.error("╭───────────────────────────── 🚨 JSON PARSE FAILURE ─────────────────────────────╮")
            logger.error(f"│ ❌ JSON Error: {e}")
            logger.error(f"│ 📝 Content length: {len(content)} characters")
            logger.error(f"│ 📄 Content (first 500 chars): {content[:500]}")
            if len(content) > 500:
                logger.error(f"│ 📄 Content (last 500 chars): {content[-500:]}")
            logger.error("╰─────────────────────────────────────────────────────────────────────────────────────╯")
            raise ValueError("Invalid JSON response from LLM")
        
        if "tasks" not in json_data:
            logger.error(f"❌ No tasks found in LLM response: {json_data}")
            raise ValueError("No tasks found in LLM response")
        
        # Parse tasks
        tasks = self._parse_tasks_from_json(json_data)
        
        # Log the LLM planner response in a clear box format
        logger.info(f"🤖 LLM Planner: Generated {len(tasks)} tasks for '{context.prompt[:50]}...'")
        
        # Log detailed breakdown only at debug level
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug("╭───────────────────────────── 🤖 LLM Planner Response ─────────────────────────────╮")
            logger.debug(f"│ 📋 REQUEST: {context.prompt}")
            logger.debug(f"│ 📊 TASK COUNT: {len(tasks)} tasks generated")
            if rag_context and rag_context.get("has_context"):
                logger.debug(f"│ 🎯 RAG ENHANCED: {rag_context.get('result_count', 0)} patterns used")
            logger.debug("│")
            logger.debug("│ 🔍 DETAILED TASK BREAKDOWN:")
            for i, task in enumerate(tasks):
                logger.debug(f"│ ┌─ Task {i+1}: {task.id}")
                logger.debug(f"│ │  📝 Type: {task.task_type.value}")
                logger.debug(f"│ │  🛠️  Tool: {task.tool_name}")
                logger.debug(f"│ │  🔗 Dependencies: {task.dependencies if task.dependencies else 'None'}")
                logger.debug(f"│ │  📄 Description: {task.description}")
                logger.debug(f"│ │  ⚙️  Parameters: {task.parameters}")
                logger.debug(f"│ │  🔄 Can Parallel: {task.can_parallel}")
                logger.debug(f"│ └─")
                if i < len(tasks) - 1:
                    logger.debug("│")
            
            # Log dependency graph
            logger.debug("│")
            logger.debug("│ 🔗 DEPENDENCY GRAPH:")
            for task in tasks:
                if task.dependencies:
                    for dep in task.dependencies:
                        logger.debug(f"│    {dep} → {task.id}")
                else:
                    logger.debug(f"│    {task.id} (no dependencies)")
            
            # Log execution order
            logger.debug("│")
            logger.debug("│ 📈 EXECUTION ORDER:")
            for i, task in enumerate(tasks):
                logger.debug(f"│    {i}. {task.id} ({task.task_type.value})")
            logger.debug("╰─────────────────────────────────────────────────────────────────────────────────────╯")
        
        return tasks
    
    def _build_enhanced_prompt(self, context: PlanningContext, rag_context: Optional[Dict[str, Any]] = None) -> str:
        """Build enhanced planning prompt with RAG context.
        
        Args:
            context: Planning context
            rag_context: Optional RAG context to enhance planning
            
        Returns:
            Enhanced planning prompt
        """
        prompt_parts = [
            "You are an expert Oracle database task planner. Your job is to break down complex database requests into a series of executable tasks.",
            "",
            "USER REQUEST:",
            context.prompt,
            "",
            "AVAILABLE TOOLS:",
            *[f"- {tool}" for tool in context.capabilities],
            "",
            "CONSTRAINTS:",
            *[f"- {constraint}" for constraint in context.constraints],
            "",
            "RESOURCE STATES:",
            *[f"- {resource}: {state}" for resource, state in context.resource_states.items()],
            "",
            "KEY VALUES:",
            *[f"- {key}: {value}" for key, value in context.key_values.items()],
            ""
        ]
        
        # Add RAG context if available
        if rag_context and rag_context.get("has_context"):
            prompt_parts.extend([
                "RELEVANT KNOWLEDGE BASE PATTERNS:",
                "Use these proven patterns and best practices to guide your task planning:",
                "",
                rag_context.get("formatted_results", ""),
                "",
                "PLANNING INSTRUCTIONS:",
                "1. Analyze the user request and available tools",
                "2. Use the knowledge base patterns above to create an optimal task sequence",
                "3. Leverage proven approaches from similar database optimization requests",
                "4. Ensure tasks follow the established best practices shown in the patterns",
                "5. Create tasks that will produce actionable, enterprise-grade results",
                "6. Consider the constraints and resource states when planning dependencies",
                "7. Generate tasks that can be executed in parallel where possible",
                ""
            ])
        else:
            prompt_parts.extend([
                "PLANNING INSTRUCTIONS:",
                "1. Analyze the user request and available tools",
                "2. Create a logical sequence of tasks to accomplish the goal",
                "3. Consider the constraints and resource states when planning dependencies",
                "4. Generate tasks that can be executed in parallel where possible",
                ""
            ])
        
        # Add task generation format
        prompt_parts.extend([
            "TASK GENERATION FORMAT:",
            "Generate a JSON response with this structure:",
            "{",
            '  "tasks": [',
            '    {',
            '      "id": "unique_task_id",',
            '      "task_type": "connect|query|analyze|generate",',
            '      "tool_name": "tool_name_from_capabilities",',
            '      "description": "Clear description of what this task does",',
            '      "parameters": {"param1": "value1"},',
            '      "dependencies": ["task_id1", "task_id2"],',
            '      "can_parallel": true/false',
            '      "priority": 1-10',
            '    }',
            '  ]',
            "}",
            "",
            "TASK TYPE GUIDELINES:",
            "- 'connect': Database connection tasks",
            "- 'query': SQL execution and data retrieval",
            "- 'analyze': Performance analysis and diagnostics",
            "- 'generate': SQL generation and recommendations",
            "",
            "For SQL optimization requests, use this proven 3-task structure:",
            "1. 'analyze_original_query' (analyze) - Concise analysis of key performance issues",
            "2. 'generate_optimized_sql' (generate) - 2-3 optimized SQL variants with brief explanations",
            "3. 'final_recommendations' (generate) - Key index recommendations and action steps (concise)",
            "",
            "CONCISE MODE INSTRUCTIONS:",
            "- Keep all responses concise and actionable",
            "- Focus on key insights and essential recommendations",
            "- Users can ask 'show me more details' for expanded explanations",
            "- Prioritize clarity over comprehensiveness",
            "",
            "Rules:",
            "- 'connect': Use 'mcp_oracle-sqlcl-mcp_connect' tool",
            "- 'query': Use 'mcp_oracle-sqlcl-mcp_run-sql' tool",
            "- 'analyze': Use 'llm_analyzer' tool for analysis tasks",
            "- 'generate': Use 'llm_analyzer' tool for generating optimized SQL, recommendations, and analysis",
            "- Dependencies must be valid task IDs",
            "- Can parallel should be true for independent tasks",
            "- Priority 1-10 (1=highest priority)",
            ""
        ])
        
        return "\n".join(prompt_parts)
    
    def _parse_tasks_from_json(self, json_data: dict) -> List[Task]:
        """Parse tasks from JSON response."""
        tasks = []
        
        for task_data in json_data["tasks"]:
            # Validate required fields
            required_fields = ["id", "task_type", "description", "tool_name"]
            for field in required_fields:
                if field not in task_data:
                    raise ValueError(f"Missing required field '{field}' in task")
            
            # Parse task type
            try:
                task_type = TaskType(task_data["task_type"])
            except ValueError:
                raise ValueError(f"Invalid task type: {task_data['task_type']}")
            
            # Create task
            task = Task(
                id=task_data["id"],
                task_type=task_type,
                description=task_data["description"],
                tool_name=task_data["tool_name"],
                parameters=task_data.get("parameters", {}),
                dependencies=task_data.get("dependencies", []),
                can_parallel=task_data.get("can_parallel", True)
            )
            
            tasks.append(task)
        
        return tasks
