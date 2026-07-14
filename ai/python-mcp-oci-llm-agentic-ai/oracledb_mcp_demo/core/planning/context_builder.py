from dataclasses import dataclass, asdict
from typing import Dict, List, Any, Set
from core.context.store import InMemoryContextStore


@dataclass
class PlanningContext:
    """Context for LLM planning."""
    prompt: str
    recent_tasks: List[Dict[str, Any]]
    resource_states: Dict[str, Dict[str, Set[str]]]
    key_values: Dict[str, Any]
    capabilities: List[str]
    constraints: List[str]
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return asdict(self)


class PatternContextBuilder:
    """Builds planning context from pattern layer."""
    
    def __init__(self, context_store: InMemoryContextStore):
        self.context_store = context_store
    
    def build_context(self, prompt: str) -> PlanningContext:
        """Build planning context from current state."""
        # Get recent task history (last 5 tasks)
        recent_tasks = self.context_store._task_history[-5:] if self.context_store._task_history else []
        
        # Get current resource states - create a copy to avoid iteration issues
        resource_states = {}
        db_data_copy = dict(self.context_store._resource_state)
        for db_name, db_data in db_data_copy.items():
            resource_states[db_name] = {}
            resource_type_copy = dict(db_data)
            for resource_type, rec in resource_type_copy.items():
                if self.context_store.get_resource_state(db_name, resource_type):
                    resource_states[db_name][resource_type] = rec.get("names", set())
        
        # Get current key-value pairs
        key_values = {}
        kv_copy = dict(self.context_store._kv)
        for key, rec in kv_copy.items():
            value = self.context_store.recall(key)
            if value is not None:
                key_values[key] = value
        
        # Define capabilities and constraints
        capabilities = ["connect", "query", "generate", "analyze"]
        constraints = ["must_connect_first", "no_circular_deps"]
        
        return PlanningContext(
            prompt=prompt,
            recent_tasks=recent_tasks,
            resource_states=resource_states,
            key_values=key_values,
            capabilities=capabilities,
            constraints=constraints
        )
