from typing import Iterable, List, Set, Tuple

from core.task_system import Task
from core.context.store import InMemoryContextStore
from .rules import DependencyRule


Dependency = Tuple[str, str]  # (from_task_id, to_task_id)


class DependencyRuleEngine:
    def __init__(self, rules: Iterable[DependencyRule]) -> None:
        self._rules = list(rules)

    def apply(
        self,
        tasks: List[Task],
        deps: Set[Dependency],
        context_store: InMemoryContextStore,
    ) -> tuple[List[Task], Set[Dependency]]:
        current_tasks = list(tasks)
        current_deps = set(deps)
        for rule in self._rules:
            injected_tasks, injected_deps = rule.apply(current_tasks, current_deps, context_store)
            if injected_tasks:
                # dedup by id
                existing = {t.id for t in current_tasks}
                for t in injected_tasks:
                    if t.id not in existing:
                        current_tasks.append(t)
                        existing.add(t.id)
            if injected_deps:
                current_deps |= injected_deps
        return current_tasks, current_deps


