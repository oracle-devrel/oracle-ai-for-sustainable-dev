from typing import List, Set, Tuple

from core.task_system import Task, TaskType
from core.context.store import InMemoryContextStore


Dependency = Tuple[str, str]


class DependencyRule:
    def apply(
        self,
        tasks: List[Task],
        deps: Set[Dependency],
        context_store: InMemoryContextStore,
    ) -> tuple[List[Task], Set[Dependency]]:
        """Return (injected_tasks, injected_deps)."""
        raise NotImplementedError


class RequireConnectRule(DependencyRule):
    DB_ACTION_TOOLS = {
        "mcp_oracle-sqlcl-mcp_run-sql",
        # extend with other db tools as needed
    }

    def apply(self, tasks: List[Task], deps: Set[Dependency], context_store: InMemoryContextStore):
        injected_deps: Set[Dependency] = set()
        # find a connect task id if present
        connect_ids = [t.id for t in tasks if t.task_type == TaskType.CONNECT]
        if not connect_ids:
            return [], set()
        connect_id = connect_ids[0]
        for t in tasks:
            if t.id == connect_id:
                continue
            if t.tool_name in self.DB_ACTION_TOOLS and (t.id, connect_id) not in deps:
                injected_deps.add((t.id, connect_id))
        return [], injected_deps


class ChainedAnalysisRule(DependencyRule):
    def apply(self, tasks: List[Task], deps: Set[Dependency], context_store: InMemoryContextStore):
        injected_deps: Set[Dependency] = set()
        # simple heuristic: if there is a generate_* task, make it depend on the last QUERY task
        last_query = None
        for t in tasks:
            if t.task_type == TaskType.QUERY:
                last_query = t.id
        for t in tasks:
            if t.task_type == TaskType.GENERATE and last_query and (t.id, last_query) not in deps:
                injected_deps.add((t.id, last_query))
        return [], injected_deps


