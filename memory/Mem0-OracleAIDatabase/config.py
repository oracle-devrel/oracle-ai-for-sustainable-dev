"""Configuration helpers for Mem0 with Oracle AI Database."""

from __future__ import annotations

import os
import re
from typing import Any, Dict, Mapping, Optional


_ORACLE_IDENTIFIER = re.compile(r"^[A-Za-z][A-Za-z0-9_$#]{0,127}$")


def _required(env: Mapping[str, str], name: str) -> str:
    value = env.get(name, "").strip()
    if not value:
        raise ValueError(f"{name} is required")
    return value


def _boolean(env: Mapping[str, str], name: str, default: bool) -> bool:
    raw = env.get(name)
    if raw is None:
        return default
    normalized = raw.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    raise ValueError(f"{name} must be true or false")


def _positive_int(env: Mapping[str, str], name: str, default: int) -> int:
    try:
        value = int(env.get(name, str(default)))
    except ValueError as exc:
        raise ValueError(f"{name} must be an integer") from exc
    if value <= 0:
        raise ValueError(f"{name} must be greater than zero")
    return value


def oracle_connection_params(env: Optional[Mapping[str, str]] = None) -> Dict[str, Any]:
    """Return arguments accepted by python-oracledb connect/create_pool."""
    values = os.environ if env is None else env
    params: Dict[str, Any] = {
        "user": _required(values, "ORACLE_USER"),
        "password": _required(values, "ORACLE_PASSWORD"),
        "dsn": _required(values, "ORACLE_DSN"),
    }

    optional = {
        "ORACLE_CONFIG_DIR": "config_dir",
        "ORACLE_WALLET_LOCATION": "wallet_location",
        "ORACLE_WALLET_PASSWORD": "wallet_password",
    }
    for environment_name, argument_name in optional.items():
        value = values.get(environment_name, "").strip()
        if value:
            params[argument_name] = value
    return params


def collection_name(env: Optional[Mapping[str, str]] = None) -> str:
    """Return a validated, unquoted Oracle collection-table name."""
    values = os.environ if env is None else env
    name = values.get("MEM0_ORACLE_COLLECTION", "MEM0_ORACLE_MEMORIES").strip()
    if not _ORACLE_IDENTIFIER.fullmatch(name):
        raise ValueError(
            "MEM0_ORACLE_COLLECTION must be an unquoted Oracle identifier "
            "containing only letters, digits, _, $, or #"
        )
    return name.upper()


def mem0_config(env: Optional[Mapping[str, str]] = None) -> Dict[str, Any]:
    """Build a Memory.from_config dictionary using the official provider."""
    values = os.environ if env is None else env
    dimensions = _positive_int(values, "MEM0_EMBEDDING_DIMS", 1536)
    accuracy = _positive_int(values, "MEM0_VECTOR_INDEX_ACCURACY", 95)
    if accuracy > 100:
        raise ValueError("MEM0_VECTOR_INDEX_ACCURACY must be between 1 and 100")

    index_type = values.get("MEM0_VECTOR_INDEX_TYPE", "HNSW").strip().upper()
    if index_type not in {"HNSW", "IVF"}:
        raise ValueError("MEM0_VECTOR_INDEX_TYPE must be HNSW or IVF")

    return {
        "vector_store": {
            "provider": "oracledb",
            "config": {
                "connection_params": oracle_connection_params(values),
                "use_connection_pool": True,
                "collection_name": collection_name(values),
                "embedding_model_dims": dimensions,
                "distance_metric": "COSINE",
                "do_create_index": _boolean(values, "MEM0_CREATE_VECTOR_INDEX", True),
                "index_type": index_type,
                "index_accuracy": accuracy,
            },
        },
        "llm": {
            "provider": "openai",
            "config": {
                "model": values.get("MEM0_LLM_MODEL", "gpt-4.1-mini"),
                "temperature": 0.1,
            },
        },
        "embedder": {
            "provider": "openai",
            "config": {
                "model": values.get("MEM0_EMBEDDING_MODEL", "text-embedding-3-small"),
                "embedding_dims": dimensions,
            },
        },
    }
