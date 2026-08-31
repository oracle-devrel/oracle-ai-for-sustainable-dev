"""Oracle AI Database graph, spatial, vector, and quest operations."""

from __future__ import annotations

import heapq
from datetime import datetime, timezone
from typing import Any


QUEST_ID = "COVERED_CONSTELLATIONS"


def rows(cursor: Any) -> list[dict[str, Any]]:
    columns = [item[0] for item in cursor.description]
    return [dict(zip(columns, row, strict=True)) for row in cursor.fetchall()]


class ParkExperienceService:
    """Python equivalent of the Java Memory Quest repository."""

    def __init__(self, pool: Any) -> None:
        self.pool = pool

    def verify_schema(self) -> None:
        required = {
            "AIM_PARK_PLACES",
            "AIM_PARK_PATHS",
            "AIM_PARK_QUESTS",
            "AIM_PARK_QUEST_STEPS",
            "AIM_PARK_KNOWLEDGE",
            "AIM_PARK_PROGRESS",
            "AIM_PARK_GUEST_BADGES",
            "AIM_PARK_REWARD_AUDIT",
        }
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            cursor.execute(
                "SELECT table_name FROM user_tables WHERE table_name LIKE 'AIM_PARK_%'"
            )
            available = {row[0] for row in cursor}
        missing = sorted(required - available)
        if missing:
            raise RuntimeError(
                "Memory Quest schema is missing: " + ", ".join(missing)
                + ". Run memory/java-agent once to initialize the shared demo schema."
            )

    def state(self) -> dict[str, Any]:
        with self.pool.acquire() as connection:
            return self._state(connection)

    def reset(self) -> dict[str, Any]:
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            for table in (
                "AIM_PARK_GUEST_BADGES",
                "AIM_PARK_REWARD_AUDIT",
                "AIM_PARK_PROGRESS",
            ):
                cursor.execute(f"DELETE FROM {table}")
            connection.commit()
            result = self._state(connection)
        result["message"] = (
            "Memory Quest reset. Graph and knowledge remain; progress, badges, "
            "and reward audit are empty."
        )
        return result

    def plan(self) -> dict[str, Any]:
        with self.pool.acquire() as connection:
            state = self._state(connection)
            route, distance = self._shortest_route(
                state["paths"], "ENTRANCE", "LANTERN_GARDEN"
            )
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT SDO_GEOM.SDO_DISTANCE(a.location, b.location, 0.005)
                      FROM AIM_PARK_PLACES a CROSS JOIN AIM_PARK_PLACES b
                     WHERE a.place_id=:first AND b.place_id=:second
                    """,
                    first="ENTRANCE",
                    second="LANTERN_GARDEN",
                )
                spatial_distance = cursor.fetchone()[0]
        state["route"] = {
            "placeIds": route,
            "distanceMeters": distance,
            "spatialStraightLineMeters": spatial_distance,
            "explanation": (
                "SQL Property Graph supplies open, accessible relationships; "
                "Oracle Spatial measures physical separation. The route avoids "
                "the inaccessible Summit Steps."
            ),
        }
        state["message"] = (
            f"Accessible quest route planned across {len(route) - 1} graph edges "
            f"({distance} m). Spatial straight-line distance is shown for comparison."
        )
        return state

    def start(self) -> dict[str, Any]:
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            cursor.execute(
                "SELECT COUNT(*) FROM AIM_PARK_PROGRESS "
                "WHERE guest_id='AVA' AND quest_id=:quest",
                quest=QUEST_ID,
            )
            if cursor.fetchone()[0] == 0:
                cursor.execute(
                    """
                    INSERT INTO AIM_PARK_PROGRESS
                      (progress_id, guest_id, quest_id, current_step, status, points_earned)
                    VALUES (AIM_PARK_PROGRESS_SEQ.NEXTVAL, 'AVA', :quest, 0, 'ACTIVE', 0)
                    """,
                    quest=QUEST_ID,
                )
                self._audit(
                    cursor,
                    "QUEST_STARTED",
                    0,
                    "Ava accepted Covered Constellations after accessibility checks.",
                )
            connection.commit()
            result = self._state(connection)
        result["message"] = (
            "Quest started transactionally with consent-bounded party context, "
            "zero completed checkpoints, and an auditable start event."
        )
        return result

    def complete_next_step(self) -> dict[str, Any]:
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT current_step, status FROM AIM_PARK_PROGRESS
                 WHERE guest_id='AVA' AND quest_id=:quest FOR UPDATE
                """,
                quest=QUEST_ID,
            )
            row = cursor.fetchone()
            if row is None:
                raise ValueError("Start the quest before completing a checkpoint.")
            current, status = row
            if status == "COMPLETED":
                raise ValueError("The quest is already complete. Reset to run it again.")
            next_step = current + 1
            cursor.execute(
                """
                SELECT p.place_name FROM AIM_PARK_QUEST_STEPS s
                  JOIN AIM_PARK_PLACES p ON p.place_id=s.place_id
                 WHERE s.quest_id=:quest AND s.step_order=:step
                """,
                quest=QUEST_ID,
                step=next_step,
            )
            place_row = cursor.fetchone()
            if place_row is None:
                raise ValueError("No next checkpoint exists.")
            place_name = place_row[0]
            cursor.execute(
                "SELECT COUNT(*) FROM AIM_PARK_QUEST_STEPS WHERE quest_id=:quest",
                quest=QUEST_ID,
            )
            complete = next_step == cursor.fetchone()[0]
            points = 300 if complete else 50
            new_status = "COMPLETED" if complete else "ACTIVE"
            cursor.execute(
                """
                UPDATE AIM_PARK_PROGRESS
                   SET current_step=:step, status=:status,
                       points_earned=points_earned+:points,
                       completed_at=CASE WHEN :status='COMPLETED'
                                         THEN SYSTIMESTAMP END
                 WHERE guest_id='AVA' AND quest_id=:quest
                """,
                step=next_step,
                status=new_status,
                points=points,
                quest=QUEST_ID,
            )
            event = "QUEST_COMPLETED" if complete else "CHECKPOINT_COMPLETED"
            self._audit(cursor, event, points, f"Checkpoint {next_step} verified at {place_name}.")
            if complete:
                cursor.execute(
                    """
                    INSERT INTO AIM_PARK_GUEST_BADGES
                      (guest_badge_id, guest_id, badge_id, quest_id)
                    VALUES (AIM_PARK_GUEST_BADGE_SEQ.NEXTVAL, 'AVA',
                            'LANTERN_PATHFINDER', :quest)
                    """,
                    quest=QUEST_ID,
                )
            connection.commit()
            result = self._state(connection)
        result["message"] = (
            "Final checkpoint verified. The transaction completed the quest, "
            "awarded 300 points, issued the Lantern Pathfinder badge, and wrote "
            "the reward audit."
            if complete
            else f"Checkpoint {next_step} verified at {place_name}. Progress, "
            "50 points, and the audit event committed together."
        )
        return result

    def graph_rag(self, query: str | None) -> dict[str, Any]:
        safe_query = query or "Find a quiet accessible rainy route with founder stories"
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT knowledge_id, place_id, title, content,
                       VECTOR_DISTANCE(embedding,
                         VECTOR_EMBEDDING(ALLMINILM USING :query AS DATA), COSINE) distance
                  FROM AIM_PARK_KNOWLEDGE
                 ORDER BY distance FETCH FIRST 3 ROWS ONLY
                """,
                query=safe_query,
            )
            hits = rows(cursor)
            for hit in hits:
                hit["graphNeighbors"] = self._neighbors(connection, hit["PLACE_ID"])
                hit["quests"] = self._quests(connection, hit["PLACE_ID"])
                hit.update(
                    {
                        "knowledgeId": hit.pop("KNOWLEDGE_ID"),
                        "placeId": hit.pop("PLACE_ID"),
                        "title": hit.pop("TITLE"),
                        "content": hit.pop("CONTENT"),
                        "distance": hit.pop("DISTANCE"),
                    }
                )
            result = self._state(connection)
        result["graphRag"] = {
            "query": safe_query,
            "hits": hits,
            "answer": (
                "Use the quiet café before 8 AM, continue through covered "
                "step-free connectors, and follow founder-symbol clues to Lantern "
                "Garden. Vector hits supply relevant knowledge; graph expansion "
                "adds connected places and quest context."
            ),
        }
        result["message"] = (
            "GraphRAG retrieved three vector-ranked knowledge records and expanded "
            "each hit through SQL Property Graph relationships."
        )
        return result

    def _state(self, connection: Any) -> dict[str, Any]:
        return {
            "places": self._query(connection, """
                SELECT place_id, place_name, place_type, zone_name, x_m, y_m,
                       accessible, covered, quiet_score, lore_summary
                  FROM AIM_PARK_PLACES ORDER BY place_name"""),
            "paths": self._query(connection, """
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (a IS place)-[e IS connects]->(b IS place)
                  COLUMNS (a.place_id AS from_place_id, b.place_id AS to_place_id,
                    e.path_id AS path_id, e.path_name AS path_name,
                    e.distance_m AS distance_m, e.accessible AS accessible,
                    e.covered AS covered, e.status AS status)) ORDER BY path_id"""),
            "party": self._query(connection, """
                SELECT guest_id, display_name, party_id, party_name,
                  TO_CHAR(consent_until, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') consent_until
                FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (g IS guest)-[m IS member_of]->(p IS party)
                  COLUMNS (g.guest_id AS guest_id, g.display_name AS display_name,
                    p.party_id AS party_id, p.party_name AS party_name,
                    m.consent_until AS consent_until)) ORDER BY guest_id"""),
            "quest": self._query(connection, """
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (q IS quest)-[s IS quest_step]->(p IS place)
                  COLUMNS (q.quest_id AS quest_id, q.quest_name AS quest_name,
                    q.description AS description, q.reward_points AS reward_points,
                    s.step_order AS step_order, s.clue_text AS clue_text,
                    p.place_id AS place_id, p.place_name AS place_name))
                 WHERE quest_id='COVERED_CONSTELLATIONS' ORDER BY step_order"""),
            "progress": self._query(connection, """
                SELECT progress_id, guest_id, quest_id, current_step, status,
                       points_earned,
                  TO_CHAR(started_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') started_at,
                  TO_CHAR(completed_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') completed_at
                  FROM AIM_PARK_PROGRESS ORDER BY progress_id"""),
            "badges": self._query(connection, """
                SELECT gb.guest_badge_id, gb.guest_id, b.badge_name, b.description,
                       gb.quest_id,
                  TO_CHAR(gb.awarded_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') awarded_at
                  FROM AIM_PARK_GUEST_BADGES gb JOIN AIM_PARK_BADGES b
                    ON b.badge_id=gb.badge_id ORDER BY gb.guest_badge_id"""),
            "audit": self._query(connection, """
                SELECT audit_id, guest_id, quest_id, event_type, points_delta,
                       details,
                  TO_CHAR(created_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') created_at
                  FROM AIM_PARK_REWARD_AUDIT ORDER BY audit_id"""),
        }

    @staticmethod
    def _query(connection: Any, sql: str, **binds: Any) -> list[dict[str, Any]]:
        with connection.cursor() as cursor:
            cursor.execute(sql, binds)
            return rows(cursor)

    def _neighbors(self, connection: Any, place_id: str) -> list[dict[str, Any]]:
        return self._query(connection, """
            SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
              MATCH (a IS place)-[e IS connects]->(b IS place)
              COLUMNS (a.place_id AS from_id, b.place_id AS place_id,
                b.place_name AS place_name, e.path_name AS relationship,
                e.distance_m AS distance_m))
             WHERE from_id=:place_id AND ROWNUM <= 3""", place_id=place_id)

    def _quests(self, connection: Any, place_id: str) -> list[dict[str, Any]]:
        return self._query(connection, """
            SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
              MATCH (q IS quest)-[s IS quest_step]->(p IS place)
              COLUMNS (p.place_id AS place_id, q.quest_name AS quest_name,
                       s.step_order AS step_order))
             WHERE place_id=:place_id""", place_id=place_id)

    @staticmethod
    def _audit(cursor: Any, event: str, points: int, details: str) -> None:
        cursor.execute(
            """
            INSERT INTO AIM_PARK_REWARD_AUDIT
              (audit_id, guest_id, quest_id, event_type, points_delta, details)
            VALUES (AIM_PARK_REWARD_SEQ.NEXTVAL, 'AVA', :quest, :event,
                    :points, :details)
            """,
            quest=QUEST_ID,
            event=event,
            points=points,
            details=details,
        )

    @staticmethod
    def _shortest_route(
        paths: list[dict[str, Any]], start: str, end: str
    ) -> tuple[list[str], int]:
        graph: dict[str, list[tuple[str, int]]] = {}
        for path in paths:
            if int(path["ACCESSIBLE"]) != 1 or path["STATUS"] != "OPEN":
                continue
            graph.setdefault(path["FROM_PLACE_ID"], []).append(
                (path["TO_PLACE_ID"], int(path["DISTANCE_M"]))
            )
        distances = {start: 0}
        previous: dict[str, str] = {}
        queue: list[tuple[int, str]] = [(0, start)]
        while queue:
            distance, node = heapq.heappop(queue)
            if distance != distances[node]:
                continue
            for target, weight in graph.get(node, []):
                candidate = distance + weight
                if candidate < distances.get(target, 2**63 - 1):
                    distances[target] = candidate
                    previous[target] = node
                    heapq.heappush(queue, (candidate, target))
        if end not in distances:
            raise RuntimeError("No accessible route found")
        route = [end]
        while route[0] != start:
            route.insert(0, previous[route[0]])
        return route, distances[end]


class DatabaseInspector:
    """Returns the same teaching tables shown by the Java app."""

    TABLES = (
        ("MAGIC_PY_MESSAGE", "RECORD_ID", "Messages managed by the Python SDK"),
        ("MAGIC_PY_MEMORY", "RECORD_ID", "Durable memories managed by the Python SDK"),
        ("AIM_DEMO_MEMORIES", "MEMORY_ID", "Application lifecycle comparison rows"),
        ("AIM_DEMO_TRACES", "TRACE_ID", "Successful experience traces"),
        ("AIM_DEMO_SKILLS", "SKILL_ID", "Human-governed learned procedures"),
        ("AIM_PARK_PROGRESS", "PROGRESS_ID", "Transactional quest progress"),
        ("AIM_PARK_GUEST_BADGES", "GUEST_BADGE_ID", "Earned quest badges"),
        ("AIM_PARK_REWARD_AUDIT", "AUDIT_ID", "Audited reward events"),
        ("AIM_AR_SESSIONS", "SESSION_ID", "Consent and retention state for AR sessions"),
        ("AIM_AR_MEDIA", "MEDIA_ID", "Consented media descriptions and vector embeddings"),
        ("AIM_AR_AUDIT", "AUDIT_ID", "AR memory, media, search, and consent events"),
    )

    def __init__(self, pool: Any) -> None:
        self.pool = pool

    def snapshot(self) -> dict[str, Any]:
        tables = []
        with self.pool.acquire() as connection, connection.cursor() as cursor:
            for name, key, description in self.TABLES:
                cursor.execute(
                    "SELECT COUNT(*) FROM user_tables WHERE table_name=:name", name=name
                )
                if cursor.fetchone()[0] == 0:
                    continue
                cursor.execute(f"SELECT * FROM {name} ORDER BY {key}")
                columns = [item[0] for item in cursor.description]
                tables.append(
                    {
                        "name": name,
                        "keyColumn": key,
                        "description": description,
                        "columns": columns,
                        "rows": rows(cursor),
                    }
                )
        return {"refreshedAt": datetime.now(timezone.utc).isoformat(), "tables": tables}
