package com.oracle.demo.memory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DatabaseTableInspector {
    private static final List<TableSpec> TABLES = List.of(
            new TableSpec(
                    "OAMJ_CONCIERGE_ORACLEAGENTMEMORY_SCHEMA_META",
                    "METADATA_KEY",
                    "Managed-schema version and record-indexing mode.",
                    """
                    SELECT metadata_key, metadata_value,
                           TO_CHAR(updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS updated_at
                      FROM OAMJ_CONCIERGE_ORACLEAGENTMEMORY_SCHEMA_META
                     ORDER BY metadata_key
                    """),
            new TableSpec(
                    "OAMJ_CONCIERGE_THREAD",
                    "RECORD_ID",
                    "Thread identity, scope, runtime configuration, and extraction state.",
                    """
                    SELECT record_id, user_id, agent_id, space_id,
                           JSON_SERIALIZE(metadata RETURNING VARCHAR2(2000)) AS metadata,
                           JSON_SERIALIZE(runtime_config RETURNING VARCHAR2(2000)) AS runtime_config,
                           JSON_SERIALIZE(runtime_state RETURNING VARCHAR2(2000)) AS runtime_state,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS created_at
                      FROM OAMJ_CONCIERGE_THREAD
                     ORDER BY created_at
                    """),
            new TableSpec(
                    "OAMJ_CONCIERGE_ACTOR_PROFILE",
                    "ACTOR_ID",
                    "Optional user and agent profile records.",
                    """
                    SELECT actor_id, actor_type, space_id, order_seq,
                           DBMS_LOB.SUBSTR(information, 1000, 1) AS information,
                           JSON_SERIALIZE(metadata RETURNING VARCHAR2(2000)) AS metadata,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS created_at
                      FROM OAMJ_CONCIERGE_ACTOR_PROFILE
                     ORDER BY order_seq
                    """),
            new TableSpec(
                    "OAMJ_CONCIERGE_MESSAGE",
                    "RECORD_ID",
                    "Raw visitor and assistant messages retained by Oracle Agent Memory.",
                    """
                    SELECT record_id,
                           order_seq,
                           thread_id,
                           user_id,
                           agent_id,
                           message_role,
                           DBMS_LOB.SUBSTR(content, 1000, 1) AS content,
                           timestamp AS event_time,
                           JSON_SERIALIZE(metadata RETURNING VARCHAR2(2000)) AS metadata,
                           TO_CHAR(expires_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS expires_at,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM OAMJ_CONCIERGE_MESSAGE
                     ORDER BY order_seq, created_at
                    """),
            new TableSpec(
                    "OAMJ_CONCIERGE_MEMORY",
                    "RECORD_ID",
                    "Typed durable memories; vectors are normalized into RECORD_CHUNKS.",
                    """
                    SELECT record_id,
                           order_seq,
                           memory_type,
                           DBMS_LOB.SUBSTR(content, 1000, 1) AS content,
                           user_id,
                           agent_id,
                           thread_id,
                           timestamp AS event_time,
                           JSON_SERIALIZE(metadata RETURNING VARCHAR2(2000)) AS metadata,
                           TO_CHAR(expires_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS expires_at,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM OAMJ_CONCIERGE_MEMORY
                     ORDER BY order_seq, created_at
                    """),
            new TableSpec(
                    "OAMJ_CONCIERGE_RECORD_CHUNKS",
                    "CHUNK_ID",
                    "Normalized message, memory, and profile chunks with Oracle vectors.",
                    """
                    SELECT chunk_id, source_id, source_record_type, source_emb_column,
                           chunk_seq, DBMS_LOB.SUBSTR(chunk_text, 1000, 1) AS chunk_text,
                           thread_id, user_id, agent_id, space_id,
                           CASE WHEN embedding IS NULL THEN NULL
                                ELSE VECTOR_DIMENSION_COUNT(embedding)
                           END AS embedding_dimensions
                      FROM OAMJ_CONCIERGE_RECORD_CHUNKS
                     ORDER BY chunk_id
                    """),
            new TableSpec(
                    "AIM_DEMO_GUESTS",
                    "GUEST_ID",
                    "Visitor identities used by the repeatable lifecycle walkthrough.",
                    """
                    SELECT guest_id,
                           display_name,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM AIM_DEMO_GUESTS
                     ORDER BY guest_id
                    """),
            new TableSpec(
                    "AIM_DEMO_MEMORIES",
                    "MEMORY_ID",
                    "Versioned semantic, episodic, and operational teaching records.",
                    """
                    SELECT memory_id,
                           guest_id,
                           agent_id,
                           memory_type,
                           scope_type,
                           memory_key,
                           content,
                           status,
                           version_no,
                           TO_CHAR(valid_from, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS valid_from,
                           TO_CHAR(expires_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS expires_at,
                           superseded_by,
                           source,
                           DBMS_LOB.SUBSTR(metadata_json, 1000, 1) AS metadata_json,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM AIM_DEMO_MEMORIES
                     ORDER BY memory_id
                    """),
            new TableSpec(
                    "AIM_DEMO_RECALL_AUDIT",
                    "RECALL_ID",
                    "Evidence of which scoped memory was recalled for a request and why.",
                    """
                    SELECT recall_id,
                           guest_id,
                           query_text,
                           memory_id,
                           recall_reason,
                           TO_CHAR(recalled_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS recalled_at
                      FROM AIM_DEMO_RECALL_AUDIT
                     ORDER BY recall_id
                    """),
            new TableSpec(
                    "AIM_DEMO_TRACES",
                    "TRACE_ID",
                    "Experience traces used as evidence for procedural learning.",
                    """
                    SELECT trace_id,
                           guest_id,
                           asked,
                           retrieved_summary,
                           action_taken,
                           outcome,
                           was_successful,
                           shareable_pattern,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM AIM_DEMO_TRACES
                     ORDER BY trace_id
                    """),
            new TableSpec(
                    "AIM_DEMO_SKILLS",
                    "SKILL_ID",
                    "Candidate and approved procedural memories with governance evidence.",
                    """
                    SELECT skill_id,
                           skill_name,
                           trigger_text,
                           DBMS_LOB.SUBSTR(instructions_json, 2000, 1)
                             AS instructions_json,
                           status,
                           source_episode_count,
                           approved_by,
                           TO_CHAR(approved_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS approved_at,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM')
                             AS created_at
                      FROM AIM_DEMO_SKILLS
                     ORDER BY skill_id
                    """),
            new TableSpec(
                    "AIM_PARK_PARTY_MEMBERS", "MEMBERSHIP_ID",
                    "Consent-bounded guest-to-party edges used by the SQL property graph.",
                    """
                    SELECT membership_id, party_id, guest_id,
                           TO_CHAR(consent_until, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS consent_until
                      FROM AIM_PARK_PARTY_MEMBERS ORDER BY membership_id
                    """),
            new TableSpec(
                    "AIM_PARK_PLACES", "PLACE_ID",
                    "Spatial park nodes with local map coordinates and accessibility properties.",
                    """
                    SELECT place_id, place_name, place_type, zone_name, x_m, y_m,
                           accessible, covered, quiet_score, lore_summary
                      FROM AIM_PARK_PLACES ORDER BY place_id
                    """),
            new TableSpec(
                    "AIM_PARK_PATHS", "PATH_ID",
                    "Directed connectivity edges traversed by SQL Property Graph route planning.",
                    """
                    SELECT path_id, from_place_id, to_place_id, path_name, distance_m,
                           accessible, covered, status
                      FROM AIM_PARK_PATHS ORDER BY path_id
                    """),
            new TableSpec(
                    "AIM_PARK_QUEST_STEPS", "STEP_ID",
                    "Ordered quest-to-place relationships exposed in the park property graph.",
                    """
                    SELECT step_id, quest_id, place_id, step_order, clue_text
                      FROM AIM_PARK_QUEST_STEPS ORDER BY step_order
                    """),
            new TableSpec(
                    "AIM_PARK_KNOWLEDGE", "KNOWLEDGE_ID",
                    "Park knowledge embedded in Oracle AI Database for vector retrieval before graph expansion.",
                    """
                    SELECT knowledge_id, place_id, title, content,
                           VECTOR_DIMENSION_COUNT(embedding) AS embedding_dimensions
                      FROM AIM_PARK_KNOWLEDGE ORDER BY knowledge_id
                    """),
            new TableSpec(
                    "AIM_PARK_PROGRESS", "PROGRESS_ID",
                    "Transactional per-guest quest state, checkpoint progress, and points.",
                    """
                    SELECT progress_id, guest_id, quest_id, current_step, status, points_earned,
                           TO_CHAR(started_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS started_at,
                           TO_CHAR(completed_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS completed_at
                      FROM AIM_PARK_PROGRESS ORDER BY progress_id
                    """),
            new TableSpec(
                    "AIM_PARK_GUEST_BADGES", "GUEST_BADGE_ID",
                    "Badge awards linked to guests and the quest that earned them.",
                    """
                    SELECT guest_badge_id, guest_id, badge_id, quest_id,
                           TO_CHAR(awarded_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS awarded_at
                      FROM AIM_PARK_GUEST_BADGES ORDER BY guest_badge_id
                    """),
            new TableSpec(
                    "AIM_PARK_REWARD_AUDIT", "AUDIT_ID",
                    "Immutable teaching evidence for quest, checkpoint, points, and reward events.",
                    """
                    SELECT audit_id, guest_id, quest_id, event_type, points_delta, details,
                           TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS created_at
                      FROM AIM_PARK_REWARD_AUDIT ORDER BY audit_id
                    """));

    private final DataSource dataSource;

    DatabaseTableInspector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Map<String, Object> snapshot() {
        try (Connection connection = dataSource.getConnection()) {
            List<Map<String, Object>> tables = new ArrayList<>();
            for (TableSpec spec : TABLES) {
                tables.add(readTable(connection, spec));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("refreshedAt", OffsetDateTime.now().toString());
            result.put("tables", tables);
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to inspect memory demo tables: " + exception.getMessage(),
                    exception);
        }
    }

    private static Map<String, Object> readTable(
            Connection connection,
            TableSpec spec) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(spec.sql())) {
            ResultSetMetaData metadata = results.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                columns.add(metadata.getColumnLabel(index));
            }
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= columns.size(); index++) {
                    row.put(columns.get(index - 1), results.getObject(index));
                }
                rows.add(row);
            }
        }
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("name", spec.name());
        table.put("keyColumn", spec.keyColumn());
        table.put("description", spec.description());
        table.put("columns", columns);
        table.put("rows", rows);
        return table;
    }

    private record TableSpec(
            String name,
            String keyColumn,
            String description,
            String sql) {
    }
}
