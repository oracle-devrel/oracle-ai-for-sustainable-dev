"""Task-centric execution system for Oracle Mcp."""

import re
from dataclasses import dataclass, field
from typing import Dict, List, Any, Optional
from enum import Enum

from .logging_config import get_logger
from .analysis_plugins import AnalysisPluginManager

logger = get_logger(__name__)


class TaskType(Enum):
    """Types of tasks that can be executed."""
    CONNECT = "connect"
    QUERY = "query" 
    GENERATE = "generate"
    ANALYZE = "analyze"
    LIST = "list"
    DISCONNECT = "disconnect"
    
    # New RAG types
    RAG_QUERY = "rag_query"           # Retrieve RAG context
    RAG_VALIDATE = "rag_validate"     # Validate with RAG patterns
    RAG_ENHANCE = "rag_enhance"       # Enhance with RAG knowledge
    RAG_LEARN = "rag_learn"           # Learn from user feedback


@dataclass
class Task:
    """Represents a single atomic task in the execution pipeline."""
    id: str
    task_type: TaskType
    description: str
    tool_name: str
    parameters: Dict[str, Any] = field(default_factory=dict)
    dependencies: List[str] = field(default_factory=list)
    can_parallel: bool = True
    priority: int = 0  # Higher number = higher priority
    
    def __post_init__(self):
        """Validate task after initialization."""
        if not self.id:
            raise ValueError("Task ID cannot be empty")
        if not self.description:
            raise ValueError("Task description cannot be empty")
        if not self.tool_name:
            raise ValueError("Task tool_name cannot be empty")
    
    def __eq__(self, other):
        """Check equality based on task ID."""
        if not isinstance(other, Task):
            return False
        return self.id == other.id
    
    def __hash__(self):
        """Make Task hashable based on ID."""
        return hash(self.id)


class TaskDecomposer:
    """Decomposes complex prompts into atomic tasks."""
    
    def __init__(self):
        """Initialize the decomposer."""
        self.task_counter = 0
        self.analysis_manager = AnalysisPluginManager()
        
    def decompose(self, prompt: str) -> List[Task]:
        """Decompose a complex prompt into atomic tasks.
        
        Args:
            prompt: User prompt to decompose
            
        Returns:
            List of tasks to execute
        """
        if not prompt or not prompt.strip():
            raise ValueError("Prompt cannot be empty")
        
        prompt = prompt.strip()
        logger.info(f"🔍 Analyzing prompt: '{prompt}'")
        
        # First, check if any analysis plugins can handle this prompt
        analysis_task_specs = self.analysis_manager.decompose_analysis_request(prompt)
        if analysis_task_specs:
            # Convert task specifications to Task objects
            tasks = [self._create_task_from_spec(spec) for spec in analysis_task_specs]
            logger.info(f"🎯 Analysis plugin handled: {len(tasks)} tasks created")
            return tasks
        
        # If no analysis plugin handles it, use traditional decomposition patterns
        logger.info("🔧 Using traditional decomposition patterns")
        
        if self._is_list_and_pattern(prompt):
            logger.info("📋 Detected: LIST_AND pattern")
            return self._handle_list_and_pattern(prompt)
        elif self._is_multi_part_pattern(prompt):
            logger.info("📋 Detected: MULTI_PART pattern")
            return self._handle_multi_part_pattern(prompt)
        elif self._is_connection_request(prompt):
            logger.info("📋 Detected: CONNECTION pattern")
            return self._handle_connection_request(prompt)
        elif self._is_simple_query(prompt):
            logger.info("📋 Detected: SIMPLE_QUERY pattern")
            return self._handle_simple_query(prompt)
        else:
            logger.info("📋 Detected: SINGLE_TASK fallback")
            return self._handle_single_task(prompt)
    
    def _is_list_and_pattern(self, prompt: str) -> bool:
        """Check if prompt follows 'list X and do Y' pattern."""
        # More specific pattern: "list" followed by content, then "and", then something that's not just listing
        pattern = r"list\s+.*\s+and\s+(give|create|show me|generate|provide)"
        return bool(re.search(pattern, prompt.lower()))
    
    def _is_multi_part_pattern(self, prompt: str) -> bool:
        """Check if prompt has multiple parts connected by 'and'."""
        # Look for patterns like "show X and Y" or "X, Y, and Z"
        has_and = " and " in prompt.lower()
        # Exclude list_and patterns as they have special handling
        return has_and and not self._is_list_and_pattern(prompt)
    
    def _is_connection_request(self, prompt: str) -> bool:
        """Check if prompt is primarily about connecting to database."""
        connection_keywords = ['connect', 'connection', 'database']
        return any(keyword in prompt.lower() for keyword in connection_keywords) and \
               not self._is_multi_part_pattern(prompt)
    
    def _is_simple_query(self, prompt: str) -> bool:
        """Check if prompt is a simple query that needs connection first."""
        query_keywords = ['list', 'show', 'display', 'select', 'find']
        return any(prompt.lower().startswith(keyword) for keyword in query_keywords)
    
    def _handle_list_and_pattern(self, prompt: str) -> List[Task]:
        """Handle 'list X and give me Y' patterns."""
        # Split on first 'and' to separate list action from generation action (case-insensitive)
        import re
        parts = re.split(r'\s+and\s+', prompt, maxsplit=1, flags=re.IGNORECASE)
        if len(parts) < 2:
            # Fallback if split didn't work as expected
            parts = [prompt, "generate sql"]
        list_part = parts[0].strip()
        generate_part = parts[1].strip()
        
        tasks = []
        
        # Task 1: Connect to database
        tasks.append(self._create_connect_task())
        
        # Task 2: Execute the list operation
        list_task_id = self._get_next_id("list")
        list_task = Task(
            id=list_task_id,
            task_type=TaskType.QUERY,
            description=f"Execute: {list_part}",
            tool_name="mcp_oracle-sqlcl-mcp_run-sql",
            parameters={"sql": self._convert_to_sql(list_part)},
            dependencies=["connect"],
            can_parallel=False
        )
        tasks.append(list_task)
        
        # Task 3: Generate SQL or analysis based on list results
        generate_task = Task(
            id=self._get_next_id("generate"),
            task_type=TaskType.GENERATE,
            description=f"Generate: {generate_part}",
            tool_name="generate_sql",  # Custom handler
            parameters={"request": generate_part, "context_from": list_task_id},
            dependencies=[list_task_id],
            can_parallel=False
        )
        tasks.append(generate_task)
        
        return tasks
    
    def _extract_parts(self, prompt: str) -> List[str]:
        """Extract parts from multi-part patterns like 'show X and Y' or 'list A, B, and C'."""
        import re
        
        if ', and ' in prompt.lower():
            # Handle "X, Y, and Z" pattern
            parts = re.split(r',\s+and\s+', prompt, flags=re.IGNORECASE)
            # Split the first part on commas
            first_parts = re.split(r',\s+', parts[0])
            all_parts = first_parts + parts[1:]
        else:
            # Simple "X and Y" pattern
            all_parts = re.split(r'\s+and\s+', prompt, flags=re.IGNORECASE)
        
        # Extract common action verb from first part if present
        action_verb = self._extract_action_verb(all_parts[0] if all_parts else "")
        
        # Process each part
        processed_parts = []
        for i, part in enumerate(all_parts):
            part = part.strip()
            
            # If this part doesn't have an action verb, prepend the common one
            if i > 0 and action_verb and not self._has_action_verb(part):
                part = f"{action_verb} {part}"
            
            processed_parts.append(part)
        
        return processed_parts

    def _handle_multi_part_pattern(self, prompt: str) -> List[Task]:
        """Handle multi-part patterns like 'show X and Y' or 'list A, B, and C'."""
        tasks = []
        
        # Extract parts using regex
        parts = self._extract_parts(prompt)
        logger.info(f"📝 Extracted {len(parts)} parts: {parts}")
        has_connect_part = any(self._is_connection_part(p) for p in parts)
        connect_task_id = None

        # If no explicit connect part, add a standard connect task
        if not has_connect_part:
            connect_task = self._create_connect_task()
            tasks.append(connect_task)
            connect_task_id = connect_task.id
            logger.info(f"🔗 Created connection task: {connect_task.id}")

        # Create intelligent tasks for each part
        for i, part in enumerate(parts, 1):
            task_id = self._get_next_id(f"part_{i}")
            
            # Determine task type and tool based on part content
            if self._is_connection_part(part):
                task = Task(
                    id=task_id,
                    task_type=TaskType.CONNECT,
                    description=f"Connect to: {part}",
                    tool_name="mcp_oracle-sqlcl-mcp_connect",
                    parameters={"connection_name": self._extract_database_name(part)},
                    dependencies=[],
                    can_parallel=False
                )
                connect_task_id = task_id
            else:
                task = Task(
                    id=task_id,
                    task_type=TaskType.QUERY,
                    description=f"Execute: {part}",
                    tool_name="mcp_oracle-sqlcl-mcp_run-sql",
                    parameters={"sql": self._convert_to_sql(part)},
                    dependencies=[connect_task_id] if connect_task_id else [],
                    can_parallel=True
                )
            
            tasks.append(task)
            logger.info(f"📋 Created task {task_id}: {part}")
        
        logger.info(f"✅ Created {len(tasks)} total tasks")
        return tasks
    
    def _is_connection_part(self, part: str) -> bool:
        """Check if a part is a connection instruction."""
        part_lower = part.lower().strip()
        return part_lower.startswith("connect to") or "connect" in part_lower and "to" in part_lower

    def _handle_connection_request(self, prompt: str) -> List[Task]:
        """Handle simple connection requests."""
        # Extract database name from prompt if possible
        db_name = self._extract_database_name(prompt)
        
        return [Task(
            id="connect",  # Always use "connect" for standalone connection requests 
            task_type=TaskType.CONNECT,
            description=f"Connect to database: {db_name}",
            tool_name="mcp_oracle-sqlcl-mcp_connect",
            parameters={"connection_name": db_name},
            dependencies=[],
            can_parallel=False
        )]
    
    def _handle_simple_query(self, prompt: str) -> List[Task]:
        """Handle simple queries that need connection first."""
        tasks = []
        
        # Task 1: Connect
        tasks.append(self._create_connect_task())
        
        # Task 2: Execute query
        query_task = Task(
            id=self._get_next_id("query"),
            task_type=TaskType.QUERY,
            description=f"Execute: {prompt}",
            tool_name="mcp_oracle-sqlcl-mcp_run-sql",
            parameters={"sql": self._convert_to_sql(prompt)},
            dependencies=["connect"],
            can_parallel=False
        )
        tasks.append(query_task)
        
        return tasks
    
    def _handle_single_task(self, prompt: str) -> List[Task]:
        """Handle prompts that don't match any specific pattern."""
        return [Task(
            id=self._get_next_id("single"),
            task_type=TaskType.QUERY,
            description=prompt,
            tool_name="mcp_oracle-sqlcl-mcp_run-sql",
            parameters={"sql": self._convert_to_sql(prompt)},
            dependencies=[],
            can_parallel=False
        )]
    
    def _create_connect_task(self) -> Task:
        """Create a standard connection task."""
        return Task(
            id="connect",
            task_type=TaskType.CONNECT,
            description="Connect to database",
            tool_name="mcp_oracle-sqlcl-mcp_connect",
            parameters={"connection_name": "tmbl"},  # Default database
            dependencies=[],
            can_parallel=False
        )
    
    def _convert_to_sql(self, description: str) -> str:
        """Convert natural language description to SQL."""
        description_lower = description.lower().strip()
        
        # Handle common patterns - more specific patterns first
        if "user tables" in description_lower:
            return "SELECT table_name FROM user_tables ORDER BY table_name"
        elif "user queues" in description_lower or "queues" in description_lower:
            return "SELECT name FROM user_queues ORDER BY name"
        elif "tablespaces" in description_lower:
            return "SELECT tablespace_name FROM dba_tablespaces ORDER BY tablespace_name"
        elif "users" in description_lower:
            return "SELECT username FROM dba_users ORDER BY username"
        elif "tables" in description_lower and "user" not in description_lower:
            return "SELECT table_name FROM user_tables ORDER BY table_name"
        elif "current time" in description_lower or "show time" in description_lower:
            return "SELECT SYSDATE as current_time FROM DUAL"
        elif "connections" in description_lower:
            return "SELECT * FROM v$session WHERE username IS NOT NULL"
        elif "indexes" in description_lower:
            return "SELECT index_name FROM user_indexes ORDER BY index_name"
        else:
            # Fallback: return a safe query that shows basic info
            return "SELECT 'Query not recognized' as message FROM DUAL"
    
    def _extract_database_name(self, prompt: str) -> str:
        """Extract database name from connection prompt."""
        # Look for patterns like "connect to X" or "database X"
        patterns = [
            r"connect\s+to\s+(\w+(?:\s+\w+)?)",  # "connect to tmbl db" or "connect to tmbl"
            r"database\s+(\w+)",
            r"connect\s+(\w+)"
        ]
        
        for pattern in patterns:
            match = re.search(pattern, prompt.lower())
            if match:
                db_name = match.group(1).strip()
                # Map common variations
                if db_name == "tmbl db":
                    return "tmbl_txeventq"  # Map to correct database
                elif db_name == "tmbl":
                    return "tmbl"
                else:
                    return db_name
        
        # Default database
        return "tmbl"
    
    def _extract_action_verb(self, text: str) -> str:
        """Extract action verb from the beginning of text."""
        text_lower = text.lower().strip()
        action_verbs = ['show', 'list', 'display', 'find', 'select', 'get']
        
        for verb in action_verbs:
            if text_lower.startswith(verb):
                return verb
        
        return ""
    
    def _has_action_verb(self, text: str) -> bool:
        """Check if text already has an action verb."""
        text_lower = text.lower().strip()
        action_verbs = ['show', 'list', 'display', 'find', 'select', 'get']
        
        return any(text_lower.startswith(verb) for verb in action_verbs)
    
    def _get_next_id(self, prefix: str) -> str:
        """Generate next task ID with prefix."""
        self.task_counter += 1
        return f"{prefix}_{self.task_counter}" if prefix != "connect" else "connect"
    
    def _create_task_from_spec(self, spec: Dict[str, Any]) -> Task:
        """Create a Task object from a task specification dictionary.
        
        Args:
            spec: Task specification dictionary
            
        Returns:
            Task object
        """
        # Convert string task_type to TaskType enum
        task_type_str = spec.get("task_type", "query")
        task_type = TaskType(task_type_str)
        
        return Task(
            id=spec["id"],
            task_type=task_type,
            description=spec["description"],
            tool_name=spec["tool_name"],
            parameters=spec.get("parameters", {}),
            dependencies=spec.get("dependencies", []),
            can_parallel=spec.get("can_parallel", True)
        )