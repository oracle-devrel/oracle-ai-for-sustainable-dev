import time
from typing import Any, Dict, List, Optional, Set


SENSITIVE_KEYS = {"password", "pass", "pwd", "secret", "api_key", "token"}


def _redact(value: Any) -> Any:
    if isinstance(value, str):
        return value.replace("secret", "***").replace("password", "***")
    if isinstance(value, dict):
        return {k: ("***" if k.lower() in SENSITIVE_KEYS else _redact(v)) for k, v in value.items()}
    return value


class InMemoryContextStore:
    def __init__(
        self,
        max_task_history: int = 100,
        default_ttl_seconds: float = 300.0,
        max_attachment_bytes: int = 1024 * 1024,
    ) -> None:
        self.max_task_history = max_task_history
        self.default_ttl_seconds = default_ttl_seconds
        self.max_attachment_bytes = max_attachment_bytes

        self._task_history: List[Dict[str, Any]] = []
        self._resource_state: Dict[str, Dict[str, Dict[str, Any]]] = {}
        self._kv: Dict[str, Dict[str, Any]] = {}

    # Task history
    def add_task_result(
        self,
        task_id: str,
        intent: str,
        tool_name: str,
        parameters: Dict[str, Any],
        result_text: str,
        success: bool,
        duration_s: float,
    ) -> None:
        entry = {
            "task_id": task_id,
            "intent": intent,
            "tool_name": tool_name,
            "parameters": _redact(parameters),
            "result_snippet": (result_text or "")[:1024],
            "success": success,
            "duration_s": duration_s,
            "ts": time.time(),
        }
        self._task_history.append(entry)
        if len(self._task_history) > self.max_task_history:
            self._task_history = self._task_history[-self.max_task_history :]

    def get_last_result(self) -> Optional[Dict[str, Any]]:
        return self._task_history[-1] if self._task_history else None

    # Resource state (per db_name → resource_type → names with TTL)
    def upsert_resource_state(
        self,
        db_name: str,
        resource_type: str,
        names: Set[str],
        ttl_seconds: Optional[float] = None,
    ) -> None:
        ttl = ttl_seconds if ttl_seconds is not None else self.default_ttl_seconds
        db = self._resource_state.setdefault(db_name, {})
        db[resource_type] = {"names": set(names), "expire_ts": time.time() + ttl}

    def get_resource_state(self, db_name: str, resource_type: str) -> Optional[Set[str]]:
        db = self._resource_state.get(db_name)
        if not db:
            return None
        rec = db.get(resource_type)
        if not rec:
            return None
        if time.time() > rec.get("expire_ts", 0):
            # expired
            db.pop(resource_type, None)
            return None
        return set(rec.get("names", set()))

    # Simple KV with TTL
    def remember(self, key: str, value: Any, ttl_seconds: Optional[float] = None) -> None:
        ttl = ttl_seconds if ttl_seconds is not None else self.default_ttl_seconds
        self._kv[key] = {"value": value, "expire_ts": time.time() + ttl}

    def recall(self, key: str) -> Optional[Any]:
        rec = self._kv.get(key)
        if not rec:
            return None
        if time.time() > rec.get("expire_ts", 0):
            self._kv.pop(key, None)
            return None
        return rec.get("value")


