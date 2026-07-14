"""Dependency analysis and execution planning for task-centric system."""

from dataclasses import dataclass
from typing import Dict, List, Set, Optional
from abc import ABC, abstractmethod

from .task_system import Task
from .logging_config import get_logger

logger = get_logger(__name__)


@dataclass
class ExecutionPhase:
    """Represents a phase in task execution plan."""
    phase_number: int
    tasks: List[Task]
    can_parallel: bool
    
    def __post_init__(self):
        """Validate phase after initialization."""
        if self.phase_number < 1:
            raise ValueError("Phase number must be >= 1")
        if not self.tasks:
            raise ValueError("Phase must have at least one task")


class ExecutionPlan(ABC):
    """Base class for execution plans."""
    
    def __init__(self, plan_type: str, phases: List[ExecutionPhase]):
        """Initialize execution plan."""
        self.plan_type = plan_type
        self.phases = phases
        
    @property
    def total_tasks(self) -> int:
        """Get total number of tasks in the plan."""
        return sum(len(phase.tasks) for phase in self.phases)
    
    @property
    def total_phases(self) -> int:
        """Get total number of phases in the plan."""
        return len(self.phases)
    
    @property
    def estimated_time_savings(self) -> float:
        """Estimate time savings from parallelization (0-1 scale)."""
        if not self.phases:
            return 0.0
            
        total_tasks = self.total_tasks
        if total_tasks <= 1:
            return 0.0
            
        # Calculate potential parallel tasks
        parallel_tasks = sum(
            len(phase.tasks) for phase in self.phases 
            if phase.can_parallel and len(phase.tasks) > 1
        )
        
        # Rough estimate: percentage of tasks that can run in parallel
        return min(0.8, parallel_tasks / total_tasks)  # Cap at 80% savings
    
    def get_summary(self) -> str:
        """Get human-readable summary of execution plan."""
        summary_parts = [
            f"Execution Plan: {self.plan_type}",
            f"Total: {self.total_tasks} tasks in {self.total_phases} phases"
        ]
        
        if self.estimated_time_savings > 0:
            summary_parts.append(f"Estimated time savings: {self.estimated_time_savings:.1%}")
        
        # Add phase details
        for phase in self.phases:
            phase_type = "parallel" if phase.can_parallel and len(phase.tasks) > 1 else "sequential"
            task_names = [task.id for task in phase.tasks]
            summary_parts.append(f"Phase {phase.phase_number}: {phase_type} ({', '.join(task_names)})")
        
        return "\n".join(summary_parts)


class SequentialPlan(ExecutionPlan):
    """Execution plan where all tasks run sequentially."""
    
    def __init__(self, tasks: List[Task]):
        """Create sequential plan from tasks."""
        if not tasks:
            phases = []
        else:
            # Sort tasks by dependencies (validation should be done before this)
            analyzer = DependencyAnalyzer()
            sorted_tasks = analyzer._topological_sort(tasks)
            
            # Create one phase per task
            phases = [
                ExecutionPhase(
                    phase_number=i + 1,
                    tasks=[task],
                    can_parallel=False
                )
                for i, task in enumerate(sorted_tasks)
            ]
        
        super().__init__("sequential", phases)


class ParallelPlan(ExecutionPlan):
    """Execution plan where all tasks can run in parallel."""
    
    def __init__(self, tasks: List[Task]):
        """Create parallel plan from tasks."""
        if not tasks:
            phases = []
        else:
            phases = [
                ExecutionPhase(
                    phase_number=1,
                    tasks=tasks,
                    can_parallel=True
                )
            ]
        
        super().__init__("parallel", phases)


class MixedPlan(ExecutionPlan):
    """Execution plan with mixed sequential and parallel phases."""
    
    def __init__(self, tasks: List[Task], dependency_graph: Dict[str, Set[str]]):
        """Create mixed plan from tasks and dependency graph."""
        phases = self._group_by_dependency_level(tasks, dependency_graph)
        super().__init__("mixed", phases)
    
    def _group_by_dependency_level(self, tasks: List[Task], graph: Dict[str, Set[str]]) -> List[ExecutionPhase]:
        """Group tasks by dependency level into execution phases."""
        if not tasks:
            return []
            
        # Create task lookup
        task_map = {task.id: task for task in tasks}
        
        # Find tasks with no dependencies (level 0)
        remaining_tasks = set(task.id for task in tasks)
        completed_tasks = set()
        phases = []
        phase_number = 1
        
        while remaining_tasks:
            # Find tasks that can run now (all dependencies completed)
            ready_tasks = []
            for task_id in remaining_tasks:
                dependencies = graph.get(task_id, set())
                if dependencies.issubset(completed_tasks):
                    ready_tasks.append(task_map[task_id])
            
            if not ready_tasks:
                # This shouldn't happen if there are no circular dependencies
                remaining_task_list = list(remaining_tasks)
                raise ValueError(f"Circular dependency detected involving tasks: {remaining_task_list}")
            
            # Create phase for ready tasks
            can_parallel = len(ready_tasks) > 1
            phase = ExecutionPhase(
                phase_number=phase_number,
                tasks=ready_tasks,
                can_parallel=can_parallel
            )
            phases.append(phase)
            
            # Mark these tasks as completed
            for task in ready_tasks:
                completed_tasks.add(task.id)
                remaining_tasks.remove(task.id)
            
            phase_number += 1
        
        return phases


class DependencyAnalyzer:
    """Analyzes task dependencies and creates execution plans."""
    
    def analyze(self, tasks: List[Task]) -> ExecutionPlan:
        """Analyze tasks and create optimal execution plan.
        
        Args:
            tasks: List of tasks to analyze
            
        Returns:
            Execution plan (Sequential, Parallel, or Mixed)
            
        Raises:
            ValueError: If circular dependencies or missing dependencies detected
        """
        if not tasks:
            return ParallelPlan([])
        
        logger.info(f"🔄 Planning execution for {len(tasks)} tasks")
        
        # Build dependency graph and validate first
        graph = self._build_dependency_graph(tasks)
        logger.info(f"📊 Built dependency graph with {len(graph)} nodes")
        
        self._validate_dependencies(tasks, graph)
        logger.info("✅ Dependencies validated")
        
        if len(tasks) == 1:
            if tasks[0].dependencies:
                logger.info("📈 Creating SEQUENTIAL plan (single task with dependencies)")
                return SequentialPlan(tasks)
            else:
                logger.info("📈 Creating PARALLEL plan (single task without dependencies)")
                return ParallelPlan(tasks)
        
        # Determine plan type
        if self._is_sequential(graph):
            logger.info("📈 Creating SEQUENTIAL plan")
            return SequentialPlan(tasks)
        elif self._is_parallel(graph):
            logger.info("📈 Creating PARALLEL plan")
            return ParallelPlan(tasks)
        else:
            logger.info("📈 Creating MIXED plan")
            return MixedPlan(tasks, graph)
    
    def _build_dependency_graph(self, tasks: List[Task]) -> Dict[str, Set[str]]:
        """Build dependency graph from tasks.
        
        Args:
            tasks: List of tasks
            
        Returns:
            Dictionary mapping task ID to set of dependency IDs
        """
        graph = {}
        
        for task in tasks:
            graph[task.id] = set(task.dependencies)
        
        return graph
    
    def _validate_dependencies(self, tasks: List[Task], graph: Dict[str, Set[str]]) -> None:
        """Validate that all dependencies exist and there are no cycles.
        
        Args:
            tasks: List of tasks
            graph: Dependency graph
            
        Raises:
            ValueError: If validation fails
        """
        task_ids = {task.id for task in tasks}
        
        # Check for missing dependencies
        for task_id, dependencies in graph.items():
            for dep_id in dependencies:
                if dep_id not in task_ids:
                    raise ValueError(f"Missing dependency: task '{task_id}' depends on '{dep_id}' which doesn't exist")
        
        # Check for circular dependencies using DFS
        self._detect_cycles(graph)
    
    def _detect_cycles(self, graph: Dict[str, Set[str]]) -> None:
        """Detect circular dependencies using DFS.
        
        Args:
            graph: Dependency graph
            
        Raises:
            ValueError: If circular dependency detected
        """
        WHITE = 0  # Unvisited
        GRAY = 1   # Visiting (in current path)
        BLACK = 2  # Visited (completely processed)
        
        colors = {task_id: WHITE for task_id in graph}
        
        def dfs(task_id: str, path: List[str]) -> None:
            if colors[task_id] == GRAY:
                # Found a cycle
                cycle_start = path.index(task_id)
                cycle = path[cycle_start:] + [task_id]
                raise ValueError(f"Circular dependency detected: {' -> '.join(cycle)}")
            
            if colors[task_id] == BLACK:
                return  # Already processed
            
            colors[task_id] = GRAY
            path.append(task_id)
            
            for dep_id in graph[task_id]:
                dfs(dep_id, path)
            
            path.pop()
            colors[task_id] = BLACK
        
        for task_id in graph:
            if colors[task_id] == WHITE:
                dfs(task_id, [])
    
    def _is_sequential(self, graph: Dict[str, Set[str]]) -> bool:
        """Check if all tasks must run sequentially.
        
        Args:
            graph: Dependency graph
            
        Returns:
            True if sequential execution required
        """
        if len(graph) <= 1:
            return len(graph) == 1 and list(graph.values())[0]  # Single task with dependencies
            
        # Sequential if we can form a single chain
        # Find task with no dependencies
        roots = [task_id for task_id, deps in graph.items() if not deps]
        
        if len(roots) != 1:
            return False  # Multiple roots or no roots means not purely sequential
        
        # Check if we can form a single chain from root
        return self._can_form_single_chain(graph, roots[0])
    
    def _can_form_single_chain(self, graph: Dict[str, Set[str]], start: str) -> bool:
        """Check if tasks can form a single dependency chain.
        
        Args:
            graph: Dependency graph
            start: Starting task ID
            
        Returns:
            True if single chain possible
        """
        visited = {start}
        current = start
        
        while True:
            # Find tasks that depend only on current task
            next_tasks = [
                task_id for task_id, deps in graph.items()
                if deps == {current} and task_id not in visited
            ]
            
            if len(next_tasks) == 0:
                # End of chain - check if we visited all tasks
                return len(visited) == len(graph)
            elif len(next_tasks) == 1:
                # Continue chain
                current = next_tasks[0]
                visited.add(current)
            else:
                # Multiple tasks depend on current - not a single chain
                return False
    
    def _is_parallel(self, graph: Dict[str, Set[str]]) -> bool:
        """Check if all tasks can run in parallel.
        
        Args:
            graph: Dependency graph
            
        Returns:
            True if parallel execution possible
        """
        # Parallel if no task has dependencies
        return all(not deps for deps in graph.values())
    
    def _topological_sort(self, tasks: List[Task]) -> List[Task]:
        """Sort tasks in topological order based on dependencies.
        
        Args:
            tasks: List of tasks to sort
            
        Returns:
            Topologically sorted list of tasks
        """
        if not tasks:
            return []
        
        # Build graph and in-degree count
        graph = self._build_dependency_graph(tasks)
        task_map = {task.id: task for task in tasks}
        
        # Build reverse graph (who depends on whom)
        reverse_graph = {task_id: set() for task_id in graph}
        in_degree = {task_id: 0 for task_id in graph}
        
        for task_id, dependencies in graph.items():
            in_degree[task_id] = len(dependencies)
            for dep_id in dependencies:
                if dep_id in reverse_graph:  # Only add if dependency exists
                    reverse_graph[dep_id].add(task_id)
        
        # Kahn's algorithm
        queue = [task_id for task_id, degree in in_degree.items() if degree == 0]
        result = []
        
        while queue:
            # Sort queue for deterministic results
            queue.sort()
            current = queue.pop(0)
            result.append(task_map[current])
            
            # Remove edges from current task
            for dependent in reverse_graph[current]:
                in_degree[dependent] -= 1
                if in_degree[dependent] == 0:
                    queue.append(dependent)
        
        if len(result) != len(tasks):
            raise ValueError("Circular dependency detected during topological sort")
        
        return result
    
    def _find_task_by_id(self, tasks: List[Task], task_id: str) -> Task:
        """Find task by ID in task list.
        
        Args:
            tasks: List of tasks to search
            task_id: ID to find
            
        Returns:
            Task with matching ID
            
        Raises:
            ValueError: If task not found
        """
        for task in tasks:
            if task.id == task_id:
                return task
        
        raise ValueError(f"Task with id '{task_id}' not found")