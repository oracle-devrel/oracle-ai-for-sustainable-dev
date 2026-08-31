#!/usr/bin/env python3
"""Flynn's Theme Park demo using the public Oracle AI Agent Memory SDK."""

from __future__ import annotations

import json
import importlib.metadata
import mimetypes
import os
import re
import threading
from datetime import datetime, timedelta, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable
from urllib.parse import unquote, urlparse

import oracledb
from oracleagentmemory.apis.searchscope import SearchScope
from oracleagentmemory.apis.ttl import TimeToLiveAnchor
from oracleagentmemory.core import (
    MemoryExtractionConfig,
    MemoryRetentionConfig,
    OracleAgentMemory,
    SchemaPolicy,
    SearchIndexSyncMode,
    SearchStrategy,
)

from ar import ArExperienceService
from deep_security import DeepDataSecurityService
from park import DatabaseInspector, ParkExperienceService

AGENT_ID = "FLYNNS_THEME_PARK_CONCIERGE"
AVA_ID = "AVA"
LEO_ID = "LEO"
AVA_THREAD_ID = "flynns_theme_park_ava_visit"
MEMORY_STORE_ID = "MAGIC_PY"
WEB_ROOT = Path(__file__).resolve().parent / "web"

AVA_MEMORY_IDS = (
    "magic-ava-breakfast",
    "magic-ava-mobility",
    "magic-ava-night",
    "magic-ava-episode",
    "magic-ava-route",
)
TRACE_IDS = (
    "magic-trace-rain-1",
    "magic-trace-rain-2",
    "magic-trace-rain-3",
)
SKILL_ID = "magic-skill-rainy-evening"


def utc_iso(days_ago: int = 0) -> str:
    return (
        datetime.now(timezone.utc) - timedelta(days=days_ago)
    ).isoformat()


def result_to_dict(result: Any) -> dict[str, Any]:
    record = result.record
    return {
        "id": record.id,
        "content": result.content,
        "recordType": record.record_type,
        "userId": record.user_id,
        "agentId": record.agent_id,
        "threadId": record.thread_id,
        "timestamp": record.timestamp,
        "metadata": record.metadata or {},
    }


class MagicMemoryService:
    """Scenario operations expressed through Oracle AI Agent Memory 26.6."""

    def __init__(self) -> None:
        password = os.environ["DB_PASSWORD"]
        wallet_password = os.environ.get("DB_WALLET_PASSWORD", password)
        wallet = os.environ["TNS_ADMIN"]
        self.pool = oracledb.create_pool(
            user=os.environ.get("DB_USERNAME", "FINANCIAL"),
            password=password,
            dsn=os.environ.get("DB_SERVICE", "financialdb_high"),
            config_dir=wallet,
            wallet_location=wallet,
            wallet_password=wallet_password,
            min=1,
            max=4,
            increment=1,
        )
        self.memory = OracleAgentMemory(
            connection=self.pool,
            memory_extraction_config=MemoryExtractionConfig(
                extract_memories=False,
                enable_context_summary=False,
            ),
            schema_policy=SchemaPolicy.CREATE_IF_NECESSARY,
            memory_store_id=MEMORY_STORE_ID,
            search_strategy=SearchStrategy.KEYWORD,
            search_index_sync=SearchIndexSyncMode.ON_COMMIT,
            memory_retention_config=MemoryRetentionConfig(
                default_ttl_days=None,
                max_ttl_days=365,
            ),
        )
        self.park = ParkExperienceService(self.pool)
        self.ar = ArExperienceService(self.pool, self.memory)
        self.deep_security = DeepDataSecurityService()
        self.inspector = DatabaseInspector(self.pool)
        self.lock = threading.RLock()
        self.last_context: dict[str, Any] | None = None
        self.last_next_day: dict[str, Any] | None = None
        self.events: list[dict[str, Any]] = []

    def close(self) -> None:
        self.memory.close(timeout=10)
        self.pool.close()

    def health(self) -> dict[str, Any]:
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA'),
                           SYS_CONTEXT('USERENV','DB_NAME')
                      FROM DUAL
                    """
                )
                schema, database = cursor.fetchone()
                cursor.execute(
                    """
                    SELECT COUNT(*)
                      FROM USER_TABLES
                     WHERE TABLE_NAME LIKE 'MAGIC_PY%'
                    """
                )
                managed_count = cursor.fetchone()[0]
        return {
            "status": "UP",
            "database": database,
            "schema": schema,
            "sdk": "oracleagentmemory "
            + importlib.metadata.version("oracleagentmemory"),
            "strategy": "Oracle Text keyword search",
            "memoryStoreId": MEMORY_STORE_ID,
            "managedObjectCount": managed_count,
        }

    def reset(self) -> dict[str, Any]:
        with self.lock:
            try:
                self.memory.delete_thread(AVA_THREAD_ID)
            except Exception:
                pass
            for record_id in AVA_MEMORY_IDS + TRACE_IDS + (SKILL_ID,):
                try:
                    self.memory.delete_memory(record_id)
                except Exception:
                    pass
            self.last_context = None
            self.last_next_day = None
            self.events = [
                {
                    "stage": "reset",
                    "detail": "Deleted the deterministic demo thread and shared records through the SDK.",
                }
            ]
            return {
                "stage": "reset",
                "message": (
                    "The Oracle AI Agent Memory demo is at a cold start. "
                    "No Ava thread, reusable trace, or shared guideline remains."
                ),
            }

    def retain(self) -> dict[str, Any]:
        with self.lock:
            try:
                self.memory.delete_thread(AVA_THREAD_ID)
            except Exception:
                pass
            for record_id in AVA_MEMORY_IDS + TRACE_IDS:
                try:
                    self.memory.delete_memory(record_id)
                except Exception:
                    pass

            thread = self.memory.create_thread(
                thread_id=AVA_THREAD_ID,
                user_id=AVA_ID,
                agent_id=AGENT_ID,
                metadata={"scenario": "flynns-theme-park", "demo": True},
            )
            thread.add_messages(
                [
                    {
                        "role": "user",
                        "content": (
                            "Plan a quiet, mobility-friendly evening. "
                            "I am interested in the lantern show."
                        ),
                    },
                    {
                        "role": "assistant",
                        "content": (
                            "I will retain only the durable preferences and "
                            "keep tonight's closure temporary."
                        ),
                    },
                ]
            )
            message_ids = thread.add_messages(
                [
                    {
                        "role": "user",
                        "content": "Please keep the itinerary concise.",
                    }
                ],
                metadata={"purpose": "message-update-proof"},
            )
            thread.update_message(
                message_ids[0],
                content="Please keep the itinerary concise and use bullet points.",
                metadata={"purpose": "message-update-proof", "updated": True},
            )
            self._add_ava_memory(
                AVA_MEMORY_IDS[0],
                "Ava prefers a quiet breakfast before 8:00 AM.",
                "preference",
                {"kind": "semantic", "source": "guest-confirmed", "version": 1},
            )
            self._add_ava_memory(
                AVA_MEMORY_IDS[1],
                "Ava needs a mobility-friendly route with minimal stairs.",
                "preference",
                {"kind": "semantic", "source": "guest-confirmed", "version": 1},
            )
            self._add_ava_memory(
                AVA_MEMORY_IDS[2],
                "Ava is interested in the fireworks show.",
                "fact",
                {
                    "kind": "semantic",
                    "source": "deliberately-imperfect-extraction",
                    "version": 1,
                    "needs_confirmation": True,
                },
            )
            self._add_ava_memory(
                AVA_MEMORY_IDS[3],
                (
                    "On Ava's previous visit, the indoor mobility route worked "
                    "and the lantern show was the highlight."
                ),
                "memory",
                {"kind": "episodic", "outcome": "positive", "version": 1},
            )
            self.memory.add_memory(
                (
                    "For Ava tonight only, rain closed the garden path. "
                    "Use the covered atrium connector."
                ),
                memory_type="fact",
                user_id=AVA_ID,
                agent_id=AGENT_ID,
                thread_id=AVA_THREAD_ID,
                memory_id=AVA_MEMORY_IDS[4],
                metadata={
                    "kind": "operational",
                    "source": "live-operations",
                    "ttl": "one-day-demo-policy",
                },
                ttl_days=1,
                ttl_anchor=TimeToLiveAnchor.CREATED_AT,
            )
            for index, content in enumerate(
                (
                    "Rain route trace: covered atrium then early dinner succeeded.",
                    "Rain route trace: verify elevator then covered connector succeeded.",
                    "Rain route trace: re-sequence dining before the venue succeeded.",
                )
            ):
                self.memory.add_memory(
                    content,
                    memory_type="memory",
                    user_id=None,
                    agent_id=AGENT_ID,
                    memory_id=TRACE_IDS[index],
                    metadata={
                        "kind": "experience_trace",
                        "pattern": "rainy-evening-reroute",
                        "successful": True,
                        "privacy_safe": True,
                        "contains_direct_identifiers": False,
                        "deidentification_method": "synthetic-generalized-demo-trace",
                    },
                )
            self.events.append(
                {
                    "stage": "retain",
                    "detail": (
                        "create_thread(), add_messages(), add_memory(), "
                        "memory types, metadata, and TTL"
                    ),
                }
            )
            return {
                "stage": "retain",
                "message": (
                    "The SDK stored Ava's thread, four durable memories, "
                    "one TTL-controlled fact, and three synthetic traces for privacy review."
                ),
            }

    def recall(self) -> dict[str, Any]:
        with self.lock:
            results = self._search_ava()
            memories = [result_to_dict(result) for result in results]
            self.last_context = self._context_from_memories(memories)
            self.events.append(
                {
                    "stage": "recall",
                    "detail": (
                        "search() used exact Ava and concierge scopes, "
                        "record types, and Oracle Text keyword ranking."
                    ),
                }
            )
            return {
                "stage": "recall",
                "message": (
                    f"Oracle AI Agent Memory returned {len(memories)} "
                    "eligible records and the app built a compact context card."
                ),
                "contextCard": self.last_context,
            }

    def correct(self) -> dict[str, Any]:
        with self.lock:
            self.memory.update_memory(
                AVA_MEMORY_IDS[2],
                content=(
                    "Ava is interested in the lantern show, "
                    "not the fireworks show."
                ),
                metadata={
                    "kind": "semantic",
                    "source": "guest-correction",
                    "version": 2,
                    "guest_confirmed": True,
                },
            )
            self.last_context = self._context_from_memories(
                [result_to_dict(result) for result in self._search_ava()]
            )
            self.events.append(
                {
                    "stage": "refine",
                    "detail": (
                        "update_memory() replaced the content and metadata "
                        "while preserving the memory scope."
                    ),
                }
            )
            return {
                "stage": "refine",
                "message": (
                    "The public update API corrected fireworks to lantern show "
                    "and retained the original user, agent, and thread scope."
                ),
            }

    def expire(self) -> dict[str, Any]:
        with self.lock:
            try:
                self.memory.update_memory(
                    AVA_MEMORY_IDS[4],
                    timestamp=utc_iso(days_ago=2),
                    ttl_days=1,
                    ttl_anchor=TimeToLiveAnchor.TIMESTAMP,
                    metadata={
                        "kind": "operational",
                        "source": "live-operations",
                        "ttl": "expired-in-demo",
                    },
                )
            except KeyError:
                # 26.6.0 updates the row, then reloads it through an active-only
                # read. A deliberately expired update is therefore persisted but
                # the post-update read reports that no active memory was found.
                pass
            self.last_context = self._context_from_memories(
                [result_to_dict(result) for result in self._search_ava()]
            )
            self.events.append(
                {
                    "stage": "expire",
                    "detail": (
                        "update_memory() moved the TTL anchor into the past; "
                        "the SDK now excludes the expired record."
                    ),
                }
            )
            return {
                "stage": "expire",
                "message": (
                    "The one-day operational memory is now expired by its "
                    "event timestamp and no longer appears in SDK search."
                ),
            }

    def dream(self) -> dict[str, Any]:
        with self.lock:
            traces = self._shared_search(
                "Rain",
                record_types=["memory"],
                metadata_filter={
                    "kind": "experience_trace",
                    "successful": True,
                    "privacy_safe": True,
                },
            )
            governed_traces = [trace for trace in traces if self._trace_is_shareable(trace)]
            if len(governed_traces) < 3:
                return {
                    "stage": "dream",
                    "message": (
                        "At least three successful traces must pass the "
                        "de-identification and scope checks."
                    ),
                }
            try:
                self.memory.delete_memory(SKILL_ID)
            except Exception:
                pass
            self.memory.add_memory(
                (
                    "Rainy evening reroute guideline: check accessibility "
                    "and live closures; prefer covered connectors; "
                    "re-sequence dining; verify venue arrival time."
                ),
                memory_type="guideline",
                user_id=None,
                agent_id=AGENT_ID,
                memory_id=SKILL_ID,
                metadata={
                    "kind": "procedural",
                    "status": "pending",
                    "source_episode_count": len(governed_traces),
                    "private_guest_data_included": False,
                    "privacy_review": "required-before-sharing",
                },
            )
            self.events.append(
                {
                    "stage": "dream",
                    "detail": (
                        "Three unscoped synthetic traces passed identifier and "
                        "scope checks; add_memory() stored a pending guideline."
                    ),
                }
            )
            return {
                "stage": "dream",
                "sourceEpisodes": len(governed_traces),
                "message": (
                    "Three governed traces passed the privacy gate and produced "
                    "a pending guideline. Human approval is still required."
                ),
            }

    def approve(self) -> dict[str, Any]:
        with self.lock:
            approver = "AVA"
            deep_sec_enforced = self.deep_security.approve_guideline(approver)
            if not deep_sec_enforced:
                self.memory.update_memory(
                    SKILL_ID,
                    metadata={
                        "kind": "procedural",
                        "status": "approved",
                        "source_episode_count": 3,
                        "private_guest_data_included": False,
                        "approved_by": approver,
                        "approved_at": utc_iso(),
                    },
                )
            self.events.append(
                {
                    "stage": "approve",
                    "detail": (
                        "Ava's MEMORY_TRIP_ORGANIZER data role authorized the "
                        "approval metadata update in Oracle AI Database."
                        if deep_sec_enforced
                        else "update_memory() promoted the guideline because the "
                        "optional local Deep Data Security setup was unavailable."
                    ),
                }
            )
            return {
                "stage": "approve",
                "deepDataSecurityEnforced": deep_sec_enforced,
                "message": (
                    "Human approval activated the shared guideline under Ava's "
                    "database-enforced organizer role."
                    if deep_sec_enforced
                    else "Human approval activated the shared guideline through "
                    "the memory API; run the DDS bootstrap for database enforcement."
                ),
            }

    def next_day(self) -> dict[str, Any]:
        with self.lock:
            private_results = self.memory.search(
                "quiet breakfast mobility lantern fireworks previous visit",
                scope=SearchScope(
                    user_id=LEO_ID,
                    agent_id=AGENT_ID,
                    exact_user_match=True,
                    exact_agent_match=True,
                    exact_thread_match=False,
                ),
                record_types=["memory", "fact", "preference"],
                max_results=10,
            )
            shared = self._shared_search(
                "Rainy",
                record_types=["guideline"],
                metadata_filter={"kind": "procedural", "status": "approved"},
            )
            security = self.deep_security.state()
            leo_identity = next(
                (
                    identity
                    for identity in security.get("identities", [])
                    if identity.get("id") == LEO_ID
                ),
                {},
            )
            database_leak_count = leo_identity.get("avaPrivateRows")
            self.events.append(
                {
                    "stage": "next-day",
                    "detail": (
                        "Leo's DDS data role returned no Ava rows or raw traces; "
                        "the approved shared guideline remained visible."
                        if security.get("databaseEnforced")
                        else "Exact Leo scope returned no Ava records; the "
                        "approved shared guideline remained visible."
                    ),
                }
            )
            self.last_next_day = {
                "stage": "next-day",
                "guestId": LEO_ID,
                "privateAvaMemoriesVisible": (
                    database_leak_count
                    if database_leak_count is not None
                    else len(private_results)
                ),
                "deepDataSecurityEnforced": security.get("databaseEnforced", False),
                "approvedSharedSkills": [
                    result_to_dict(result) for result in shared
                ],
                "message": (
                    "Leo can reuse the approved guideline while Oracle Deep "
                    "Data Security filters Ava's private memory and raw traces."
                    if security.get("databaseEnforced")
                    else "Leo can reuse the approved guideline while Ava's exact "
                    "user-scoped memory remains unavailable."
                ),
            }
            return self.last_next_day

    def state(self) -> dict[str, Any]:
        with self.lock:
            ava_memories = self._search_ava(max_results=20)
            traces = self._shared_search(
                "Rain",
                record_types=["memory"],
                metadata_filter={"kind": "experience_trace"},
            )
            skills = self._shared_search(
                "Rainy",
                record_types=["guideline"],
                metadata_filter={"kind": "procedural"},
            )
            return {
                "memories": [result_to_dict(item) for item in ava_memories],
                "traces": [result_to_dict(item) for item in traces],
                "skills": [result_to_dict(item) for item in skills],
                "contextCard": self.last_context,
                "events": self.events,
                "latestStage": self.events[-1] if self.events else None,
                "nextDay": self.last_next_day,
            }

    def _add_ava_memory(
        self,
        memory_id: str,
        content: str,
        memory_type: str,
        metadata: dict[str, Any],
    ) -> None:
        self.memory.add_memory(
            content,
            memory_type=memory_type,
            user_id=AVA_ID,
            agent_id=AGENT_ID,
            thread_id=AVA_THREAD_ID,
            memory_id=memory_id,
            metadata=metadata,
        )

    def _search_ava(self, max_results: int = 10) -> list[Any]:
        return self.memory.search(
            "Ava",
            scope=SearchScope(
                user_id=AVA_ID,
                agent_id=AGENT_ID,
                exact_user_match=True,
                exact_agent_match=True,
                exact_thread_match=False,
            ),
            record_types=["memory", "fact", "preference"],
            max_results=max_results,
        )

    @staticmethod
    def _context_from_memories(
        memories: list[dict[str, Any]],
    ) -> dict[str, Any]:
        return {
            "summary": (
                "Returning guest who values quiet settings and "
                "mobility-friendly routes."
            ),
            "relevantMemories": memories,
            "response": (
                "Use a quiet early meal and the covered accessible route, "
                "then use the confirmed lantern-show preference."
            ),
        }

    def _shared_search(
        self,
        query: str,
        *,
        record_types: list[str],
        metadata_filter: dict[str, Any],
    ) -> list[Any]:
        return self.memory.search(
            query,
            scope=SearchScope(
                user_id=None,
                agent_id=AGENT_ID,
                exact_user_match=True,
                exact_agent_match=True,
                exact_thread_match=False,
            ),
            record_types=record_types,
            metadata_filter=metadata_filter,
            max_results=20,
        )

    @staticmethod
    def _trace_is_shareable(result: Any) -> bool:
        """Reject scoped traces or obvious direct identifiers before induction."""
        record = result.record
        metadata = record.metadata or {}
        if record.user_id is not None:
            return False
        if not metadata.get("privacy_safe"):
            return False
        if metadata.get("contains_direct_identifiers") is not False:
            return False
        direct_identifier = re.compile(
            r"\b[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}\b|\b\d{3}[-. ]\d{3}[-. ]\d{4}\b"
        )
        return direct_identifier.search(result.content) is None

    def capabilities(self) -> dict[str, Any]:
        return {
            "sdkVersion": importlib.metadata.version("oracleagentmemory"),
            "used": [
                "update_memory()",
                "update_message()",
                "ttl_days and TimeToLiveAnchor",
                "MemoryRetentionConfig",
                "metadata_filter",
                "exact user and agent scopes",
                "Oracle Text keyword search with ON_COMMIT synchronization",
            ],
            "availableNotExercised": [
                "async add/search/update APIs",
                "background extraction queue controls",
                "context summaries and context cards",
                "hybrid vector plus Oracle Text search",
                "schema-owner read-only deployment",
            ],
        }


class MagicRequestHandler(BaseHTTPRequestHandler):
    server_version = "MagicMemoryPython/1.0"

    @property
    def service(self) -> MagicMemoryService:
        return self.server.service  # type: ignore[attr-defined]

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/api/health":
            self._run(self.service.health)
        elif path == "/api/state":
            self._run(self.service.state)
        elif path == "/api/capabilities":
            self._run(self.service.capabilities)
        elif path == "/api/park/state":
            self._run(self.service.park.state)
        elif path == "/api/ar/state":
            self._run(self.service.ar.state)
        elif path == "/api/security/state":
            self._run(self.service.deep_security.state)
        elif path == "/api/database/tables":
            self._run(self.service.inspector.snapshot)
        else:
            self._static(path)

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        prefix = "/api/actions/"
        park_prefix = "/api/park/actions/"
        ar_prefix = "/api/ar/"
        if path.startswith(ar_prefix):
            self._ar_action(path[len(ar_prefix) :])
            return
        if path.startswith(park_prefix):
            self._park_action(path[len(park_prefix) :])
            return
        if not path.startswith(prefix):
            self._json(
                HTTPStatus.NOT_FOUND,
                {"error": "Unknown API path"},
            )
            return
        action = path[len(prefix) :]
        actions: dict[str, Callable[[], dict[str, Any]]] = {
            "reset": self.service.reset,
            "retain": self.service.retain,
            "recall": self.service.recall,
            "correct": self.service.correct,
            "expire": self.service.expire,
            "dream": self.service.dream,
            "approve": self.service.approve,
            "next-day": self.service.next_day,
        }
        work = actions.get(action)
        if work is None:
            self._json(
                HTTPStatus.BAD_REQUEST,
                {"error": f"Unknown demo action: {action}"},
            )
            return
        self._run(work)

    def do_OPTIONS(self) -> None:
        self.send_response(HTTPStatus.NO_CONTENT)
        self._cors_headers()
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def _ar_action(self, action: str) -> None:
        body = self._request_json()
        actions: dict[str, Callable[[], dict[str, Any]]] = {
            "session/start": lambda: self.service.ar.start_session(body),
            "remember": lambda: self.service.ar.remember(body),
            "media": lambda: self.service.ar.remember_media(body),
            "search": lambda: self.service.ar.search_media(body),
            "reset": self.service.ar.reset,
        }
        work = actions.get(action)
        if work is None:
            self._json(HTTPStatus.BAD_REQUEST, {"error": f"Unknown AR action: {action}"})
            return
        self._run(work)

    def _park_action(self, action: str) -> None:
        body = self._request_json()
        actions: dict[str, Callable[[], dict[str, Any]]] = {
            "reset": self.service.park.reset,
            "plan": self.service.park.plan,
            "start": self.service.park.start,
            "complete-step": self.service.park.complete_next_step,
            "graphrag": lambda: self.service.park.graph_rag(body.get("query")),
        }
        work = actions.get(action)
        if work is None:
            self._json(HTTPStatus.BAD_REQUEST, {"error": f"Unknown park action: {action}"})
            return
        self._run(work)

    def _request_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        if length == 0:
            return {}
        return json.loads(self.rfile.read(length))

    def log_message(self, format_string: str, *args: Any) -> None:
        print(f"[memory-python] {format_string % args}")

    def _run(self, work: Callable[[], dict[str, Any]]) -> None:
        try:
            self._json(HTTPStatus.OK, work())
        except PermissionError as exception:
            self._json(HTTPStatus.FORBIDDEN, {"error": str(exception)})
        except ValueError as exception:
            self._json(HTTPStatus.BAD_REQUEST, {"error": str(exception)})
        except Exception as exception:
            self._json(
                HTTPStatus.INTERNAL_SERVER_ERROR,
                {"error": f"{type(exception).__name__}: {exception}"},
            )

    def _static(self, request_path: str) -> None:
        relative = "index.html" if request_path == "/" else unquote(
            request_path.lstrip("/")
        )
        if ".." in relative or relative.startswith("/"):
            self._json(HTTPStatus.BAD_REQUEST, {"error": "Invalid path"})
            return
        target = (WEB_ROOT / relative).resolve()
        if WEB_ROOT not in target.parents and target != WEB_ROOT:
            self._json(HTTPStatus.BAD_REQUEST, {"error": "Invalid path"})
            return
        if not target.is_file():
            self._json(HTTPStatus.NOT_FOUND, {"error": "Not found"})
            return
        body = target.read_bytes()
        content_type = mimetypes.guess_type(target.name)[0] or "text/plain"
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", f"{content_type}; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self._cors_headers()
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _json(self, status: HTTPStatus, value: dict[str, Any]) -> None:
        body = json.dumps(value, default=str).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self._cors_headers()
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _cors_headers(self) -> None:
        allowed = os.environ.get("AR_ALLOWED_ORIGIN")
        if allowed:
            self.send_header("Access-Control-Allow-Origin", allowed)


def main() -> None:
    port = int(os.environ.get("MEMORY_PYTHON_PORT", "8092"))
    service = MagicMemoryService()
    server = ThreadingHTTPServer(("127.0.0.1", port), MagicRequestHandler)
    server.service = service  # type: ignore[attr-defined]
    print(
        "Python Oracle AI Agent Memory demo listening on "
        f"http://127.0.0.1:{port}"
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping Python Oracle AI Agent Memory demo.")
    finally:
        server.server_close()
        service.close()


if __name__ == "__main__":
    main()
