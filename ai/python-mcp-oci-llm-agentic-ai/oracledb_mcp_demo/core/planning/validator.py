from typing import List, Tuple, Set
from core.task_system import Task, TaskType


class PlanValidator:
    """Validates task execution plans."""
    
    def __init__(self):
        self.db_action_tools = {
            "mcp_oracle-sqlcl-mcp_run-sql",
            "mcp_oracle-sqlcl-mcp_connect",
        }
    
    def validate(self, tasks: List[Task]) -> Tuple[bool, List[str]]:
        """Validate a task plan.
        
        Args:
            tasks: List of tasks to validate
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []
        
        # Check for duplicate task IDs
        task_ids = [task.id for task in tasks]
        if len(task_ids) != len(set(task_ids)):
            errors.append("Duplicate task IDs found")
        
        # Validate individual tasks
        for task in tasks:
            task_errors = self._validate_task(task, tasks)
            errors.extend(task_errors)
        
        # Check for circular dependencies
        if self._has_circular_dependencies(tasks):
            errors.append("Circular dependency detected")
        
        # Check for missing dependencies
        missing_deps = self._find_missing_dependencies(tasks)
        if missing_deps:
            errors.append(f"Missing dependencies: {missing_deps}")
        
        # Check connect dependencies
        connect_deps = self._check_connect_dependencies(tasks)
        errors.extend(connect_deps)
        
        return len(errors) == 0, errors
    
    def _validate_task(self, task: Task, all_tasks: List[Task]) -> List[str]:
        """Validate a single task."""
        errors = []
        
        # Check task ID
        if not task.id or task.id.strip() == "":
            errors.append(f"Task ID cannot be empty")
        
        # Check task type
        if not isinstance(task.task_type, TaskType):
            errors.append(f"Invalid task type: {task.task_type}")
        
        # Check tool name
        if not task.tool_name or task.tool_name.strip() == "":
            errors.append(f"Tool name cannot be empty for task {task.id}")
        
        # Check description
        if not task.description or task.description.strip() == "":
            errors.append(f"Task description cannot be empty for task {task.id}")
        
        return errors
    
    def _has_circular_dependencies(self, tasks: List[Task]) -> bool:
        """Check for circular dependencies using DFS."""
        task_map = {task.id: task for task in tasks}
        visited = set()
        rec_stack = set()
        
        def dfs(task_id: str) -> bool:
            if task_id in rec_stack:
                return True  # Circular dependency found
            if task_id in visited:
                return False
            
            visited.add(task_id)
            rec_stack.add(task_id)
            
            task = task_map.get(task_id)
            if task:
                for dep_id in task.dependencies:
                    if dfs(dep_id):
                        return True
            
            rec_stack.remove(task_id)
            return False
        
        for task in tasks:
            if task.id not in visited:
                if dfs(task.id):
                    return True
        
        return False
    
    def _find_missing_dependencies(self, tasks: List[Task]) -> List[str]:
        """Find dependencies that reference non-existent tasks."""
        task_ids = {task.id for task in tasks}
        missing = []
        
        for task in tasks:
            for dep_id in task.dependencies:
                if dep_id not in task_ids:
                    missing.append(f"{task.id} -> {dep_id}")
        
        return missing
    
    def _check_connect_dependencies(self, tasks: List[Task]) -> List[str]:
        """Check that DB actions depend on connect tasks."""
        errors = []
        connect_tasks = {task.id for task in tasks if task.task_type == TaskType.CONNECT}
        
        for task in tasks:
            if (task.tool_name in self.db_action_tools and 
                task.task_type != TaskType.CONNECT and
                not any(dep in connect_tasks for dep in task.dependencies)):
                errors.append(f"Task {task.id} must depend on connect task")
        
        return errors
