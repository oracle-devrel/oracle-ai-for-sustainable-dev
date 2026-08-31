"""Consent-aware AR facade for the theme park Oracle Agent Memory demo."""

from __future__ import annotations

import json
import re
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any


AGENT_ID = "FLYNNS_THEME_PARK_CONCIERGE"
SAFE_ID = re.compile(r"^[A-Za-z0-9_-]{1,128}$")


class ArExperienceService:
    """Backs Lens Studio and the browser AR simulator with governed state."""

    def __init__(self, pool: Any, memory: Any) -> None:
        self.pool = pool
        self.memory = memory
        self._initialize_schema()

    def start_session(self, request: dict[str, Any]) -> dict[str, Any]:
        guest_id = self._identifier(request.get("guestId", "AVA"), "guestId")
        recording = bool(request.get("mediaRecording", False))
        camera_sensing = bool(request.get("cameraSensing", True))
        location = bool(request.get("locationSharing", False))
        retention_days = self._retention_days(request.get("retentionDays", 7))
        session_id = "ar-" + uuid.uuid4().hex
        token = uuid.uuid4().hex
        expires_at = datetime.now(timezone.utc) + timedelta(hours=4)
        thread_id = self._thread_id(guest_id)
        try:
            self.memory.get_thread(thread_id)
        except Exception:
            self.memory.create_thread(
                thread_id=thread_id,
                user_id=guest_id,
                agent_id=AGENT_ID,
                metadata={"scenario": "theme-park-ar", "consent_scoped": True},
            )
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO AIM_AR_SESSIONS (
                      session_id, session_token, guest_id, camera_sensing,
                      media_recording, location_sharing, retention_days,
                      status, expires_at
                    ) VALUES (:1, :2, :3, :4, :5, :6, :7, 'ACTIVE', :8)
                    """,
                    [
                        session_id,
                        token,
                        guest_id,
                        int(camera_sensing),
                        int(recording),
                        int(location),
                        retention_days,
                        expires_at,
                    ],
                )
                self._audit(
                    cursor,
                    session_id,
                    guest_id,
                    "SESSION_STARTED",
                    {
                        "cameraSensing": camera_sensing,
                        "mediaRecording": recording,
                        "locationSharing": location,
                        "retentionDays": retention_days,
                    },
                )
            connection.commit()
        return {
            "sessionId": session_id,
            "sessionToken": token,
            "guestId": guest_id,
            "expiresAt": expires_at.isoformat(),
            "privacy": {
                "cameraSensing": camera_sensing,
                "mediaRecording": recording,
                "locationSharing": location,
                "retentionDays": retention_days,
            },
            "overlay": "AR sensing active. Recording is "
            + ("on with consent." if recording else "off."),
        }

    def remember(self, request: dict[str, Any]) -> dict[str, Any]:
        session = self._active_session(request)
        text = self._text(request.get("text"), "text", 2000)
        source = request.get("source", "voice")
        if source not in {"voice", "gesture", "browser", "spectacles"}:
            raise ValueError("source must be voice, gesture, browser, or spectacles")
        memory_id = "ar-memory-" + uuid.uuid4().hex
        self.memory.add_memory(
            text,
            memory_type="memory",
            user_id=session["guestId"],
            agent_id=AGENT_ID,
            thread_id=self._thread_id(session["guestId"]),
            memory_id=memory_id,
            metadata={
                "kind": "ar_observation",
                "source": source,
                "session_id": session["sessionId"],
                "guest_confirmed": True,
                "camera_recording": session["mediaRecording"],
            },
            ttl_days=session["retentionDays"],
        )
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                self._audit(
                    cursor,
                    session["sessionId"],
                    session["guestId"],
                    "MEMORY_RETAINED",
                    {"memoryId": memory_id, "source": source, "text": text},
                )
            connection.commit()
        return {
            "memoryId": memory_id,
            "message": "Oracle Agent Memory retained the guest-confirmed observation.",
            "overlay": f"Remembered for {session['retentionDays']} days: {text}",
        }

    def remember_media(self, request: dict[str, Any]) -> dict[str, Any]:
        session = self._active_session(request)
        if not session["mediaRecording"]:
            raise PermissionError(
                "Media recording consent is off. Start a new opted-in session first."
            )
        transcript = self._text(request.get("transcript"), "transcript", 4000)
        media_type = request.get("mediaType", "video-transcript")
        if media_type not in {"video-transcript", "image-caption", "audio-transcript"}:
            raise ValueError("Unsupported mediaType")
        media_id = "media-" + uuid.uuid4().hex
        object_uri = request.get("objectUri")
        if object_uri is not None and not str(object_uri).startswith("https://"):
            raise ValueError("objectUri must use HTTPS")
        expires_at = datetime.now(timezone.utc) + timedelta(
            days=session["retentionDays"]
        )
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO AIM_AR_MEDIA (
                      media_id, session_id, guest_id, media_type, transcript,
                      object_uri, consent_status, embedding, expires_at
                    ) VALUES (
                      :1, :2, :3, :4, :5, :6, 'OPTED_IN',
                      VECTOR_EMBEDDING(allminilm USING :7 AS DATA), :8
                    )
                    """,
                    [
                        media_id,
                        session["sessionId"],
                        session["guestId"],
                        media_type,
                        transcript,
                        object_uri,
                        transcript,
                        expires_at,
                    ],
                )
                self._audit(
                    cursor,
                    session["sessionId"],
                    session["guestId"],
                    "MEDIA_INDEXED",
                    {"mediaId": media_id, "mediaType": media_type},
                )
            connection.commit()
        return {
            "mediaId": media_id,
            "expiresAt": expires_at.isoformat(),
            "message": "The consented media description was embedded for semantic search.",
            "overlay": "Media description indexed. Raw media storage remains optional.",
        }

    def search_media(self, request: dict[str, Any]) -> dict[str, Any]:
        session = self._active_session(request)
        query = self._text(request.get("query"), "query", 1000)
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT media_id, media_type, transcript, object_uri,
                           VECTOR_DISTANCE(
                             embedding,
                             VECTOR_EMBEDDING(allminilm USING :query AS DATA),
                             COSINE
                           ) AS distance,
                           expires_at
                      FROM AIM_AR_MEDIA
                     WHERE guest_id = :guest_id
                       AND consent_status = 'OPTED_IN'
                       AND expires_at > SYSTIMESTAMP
                     ORDER BY distance
                     FETCH FIRST 5 ROWS ONLY
                    """,
                    query=query,
                    guest_id=session["guestId"],
                )
                columns = [item[0] for item in cursor.description]
                hits = [dict(zip(columns, row)) for row in cursor.fetchall()]
                self._audit(
                    cursor,
                    session["sessionId"],
                    session["guestId"],
                    "MEDIA_SEARCHED",
                    {"query": query, "hitCount": len(hits)},
                )
            connection.commit()
        return {
            "query": query,
            "hits": hits,
            "overlay": f"Found {len(hits)} consented media memories.",
        }

    def state(self) -> dict[str, Any]:
        with self.pool.acquire() as connection:
            return {
                "sessions": self._rows(
                    connection,
                    """
                    SELECT session_id, guest_id, camera_sensing, media_recording,
                           location_sharing, retention_days, status, expires_at,
                           created_at
                      FROM AIM_AR_SESSIONS ORDER BY created_at DESC
                      FETCH FIRST 10 ROWS ONLY
                    """,
                ),
                "media": self._rows(
                    connection,
                    """
                    SELECT media_id, session_id, guest_id, media_type,
                           transcript, object_uri, consent_status,
                           VECTOR_DIMENSION_COUNT(embedding) AS embedding_dimensions,
                           expires_at, created_at
                      FROM AIM_AR_MEDIA ORDER BY created_at DESC
                      FETCH FIRST 10 ROWS ONLY
                    """,
                ),
                "audit": self._rows(
                    connection,
                    """
                    SELECT audit_id, session_id, guest_id, event_type,
                           details, created_at
                      FROM AIM_AR_AUDIT ORDER BY audit_id DESC
                      FETCH FIRST 20 ROWS ONLY
                    """,
                ),
            }

    def reset(self) -> dict[str, Any]:
        memory_ids: list[str] = []
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT JSON_VALUE(details, '$.memoryId')
                      FROM AIM_AR_AUDIT
                     WHERE event_type = 'MEMORY_RETAINED'
                    """
                )
                memory_ids = [row[0] for row in cursor if row[0]]
                cursor.execute("DELETE FROM AIM_AR_MEDIA")
                cursor.execute("DELETE FROM AIM_AR_AUDIT")
                cursor.execute("DELETE FROM AIM_AR_SESSIONS")
            connection.commit()
        for memory_id in memory_ids:
            try:
                self.memory.delete_memory(memory_id)
            except Exception:
                pass
        return {"message": "AR sessions, media descriptions, and AR memories reset."}

    def _active_session(self, request: dict[str, Any]) -> dict[str, Any]:
        session_id = self._identifier(request.get("sessionId"), "sessionId")
        token = self._identifier(request.get("sessionToken"), "sessionToken")
        with self.pool.acquire() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT guest_id, media_recording, retention_days
                      FROM AIM_AR_SESSIONS
                     WHERE session_id = :1 AND session_token = :2
                       AND status = 'ACTIVE' AND expires_at > SYSTIMESTAMP
                    """,
                    [session_id, token],
                )
                row = cursor.fetchone()
        if row is None:
            raise PermissionError("AR session is missing, expired, or unauthorized")
        return {
            "sessionId": session_id,
            "guestId": row[0],
            "mediaRecording": bool(row[1]),
            "retentionDays": int(row[2]),
        }

    def _initialize_schema(self) -> None:
        statements = (
            """
            CREATE TABLE AIM_AR_SESSIONS (
              session_id VARCHAR2(128) PRIMARY KEY,
              session_token VARCHAR2(128) NOT NULL,
              guest_id VARCHAR2(128) NOT NULL,
              camera_sensing NUMBER(1) NOT NULL,
              media_recording NUMBER(1) NOT NULL,
              location_sharing NUMBER(1) NOT NULL,
              retention_days NUMBER(4) NOT NULL,
              status VARCHAR2(20) NOT NULL,
              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
              created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
            )
            """,
            """
            CREATE TABLE AIM_AR_MEDIA (
              media_id VARCHAR2(128) PRIMARY KEY,
              session_id VARCHAR2(128) NOT NULL REFERENCES AIM_AR_SESSIONS(session_id),
              guest_id VARCHAR2(128) NOT NULL,
              media_type VARCHAR2(40) NOT NULL,
              transcript CLOB NOT NULL,
              object_uri VARCHAR2(1000),
              consent_status VARCHAR2(20) NOT NULL,
              embedding VECTOR(384, FLOAT32),
              expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
              created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
            )
            """,
            """
            CREATE TABLE AIM_AR_AUDIT (
              audit_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              session_id VARCHAR2(128) REFERENCES AIM_AR_SESSIONS(session_id),
              guest_id VARCHAR2(128) NOT NULL,
              event_type VARCHAR2(40) NOT NULL,
              details JSON,
              created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
            )
            """,
        )
        with self.pool.acquire() as connection:
            for statement in statements:
                try:
                    with connection.cursor() as cursor:
                        cursor.execute(statement)
                except Exception as exception:
                    if "ORA-00955" not in str(exception):
                        raise
            connection.commit()

    @staticmethod
    def _audit(
        cursor: Any,
        session_id: str,
        guest_id: str,
        event_type: str,
        details: dict[str, Any],
    ) -> None:
        cursor.execute(
            """
            INSERT INTO AIM_AR_AUDIT (
              session_id, guest_id, event_type, details
            ) VALUES (:1, :2, :3, :4)
            """,
            [session_id, guest_id, event_type, json.dumps(details)],
        )

    @staticmethod
    def _rows(connection: Any, sql: str) -> list[dict[str, Any]]:
        with connection.cursor() as cursor:
            cursor.execute(sql)
            columns = [item[0] for item in cursor.description]
            return [dict(zip(columns, row)) for row in cursor.fetchall()]

    @staticmethod
    def _identifier(value: Any, name: str) -> str:
        if value is None or not SAFE_ID.fullmatch(str(value)):
            raise ValueError(f"{name} must contain only letters, numbers, underscore, or hyphen")
        return str(value)

    @staticmethod
    def _text(value: Any, name: str, maximum: int) -> str:
        text = str(value or "").strip()
        if not text or len(text) > maximum:
            raise ValueError(f"{name} must contain 1 to {maximum} characters")
        return text

    @staticmethod
    def _retention_days(value: Any) -> int:
        days = int(value)
        if days < 1 or days > 30:
            raise ValueError("retentionDays must be between 1 and 30")
        return days

    @staticmethod
    def _thread_id(guest_id: str) -> str:
        return f"theme_park_{guest_id.lower()}_ar_visit"
