package com.oracle.demo.memory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MemoryRepository {
    private static final String AGENT_ID = "FLYNNS_THEME_PARK_CONCIERGE";
    private final DataSource dataSource;
    private Map<String, Object> lastNextDay;

    MemoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Map<String, Object> health() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT SYS_CONTEXT('USERENV','DB_NAME'), "
                             + "SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL")) {
            results.next();
            return Map.of(
                    "status", "UP",
                    "database", results.getString(1),
                    "schema", results.getString(2),
                    "pool", "Oracle UCP",
                    "demo", "memories-are-the-magic");
        } catch (SQLException exception) {
            throw failure("check database health", exception);
        }
    }

    Map<String, Object> reset() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (String table : List.of(
                    "AIM_DEMO_RECALL_AUDIT",
                    "AIM_DEMO_SKILLS",
                    "AIM_DEMO_TRACES",
                    "AIM_DEMO_MEMORIES")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM " + table);
                }
            }
            if (count(connection, "SELECT COUNT(*) FROM AIM_DEMO_GUESTS WHERE guest_id='AVA'") == 0) {
                insertGuest(connection, "AVA", "Ava");
            }
            if (count(connection, "SELECT COUNT(*) FROM AIM_DEMO_GUESTS WHERE guest_id='LEO'") == 0) {
                insertGuest(connection, "LEO", "Leo");
            }
            connection.commit();
            lastNextDay = null;
            return Map.of(
                    "stage", "reset",
                    "message", "Demo rows reset. Ava and Leo exist, but no memories or learned skills do.");
        } catch (SQLException exception) {
            throw failure("reset demo state", exception);
        }
    }

    Map<String, Object> retain() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            if (count(connection,
                    "SELECT COUNT(*) FROM AIM_DEMO_MEMORIES "
                            + "WHERE guest_id = 'AVA' AND status = 'ACTIVE'") == 0) {
                insertMemory(
                        connection,
                        "AVA",
                        "SEMANTIC",
                        "PRIVATE",
                        "breakfast-preference",
                        "Prefers a quiet breakfast before 8:00 AM.",
                        "conversation-extraction",
                        null,
                        "{\"confidence\":0.98,\"sensitivity\":\"guest-private\"}");
                insertMemory(
                        connection,
                        "AVA",
                        "SEMANTIC",
                        "PRIVATE",
                        "mobility-route",
                        "Needs a mobility-friendly route with minimal stairs.",
                        "conversation-extraction",
                        null,
                        "{\"confidence\":0.99,\"sensitivity\":\"guest-private\"}");
                insertMemory(
                        connection,
                        "AVA",
                        "SEMANTIC",
                        "PRIVATE",
                        "night-experience",
                        "Interested in the fireworks show.",
                        "conversation-extraction",
                        null,
                        "{\"confidence\":0.66,\"needs_confirmation\":true}");
                insertMemory(
                        connection,
                        "AVA",
                        "EPISODIC",
                        "PRIVATE",
                        "last-visit",
                        "On the previous visit, the indoor mobility route worked and the lantern show was the highlight.",
                        "completed-interaction",
                        null,
                        "{\"outcome\":\"positive\"}");
                insertMemory(
                        connection,
                        "AVA",
                        "OPERATIONAL",
                        "PRIVATE",
                        "rain-route-tonight",
                        "The garden path is closed tonight; use the covered atrium connector.",
                        "live-operations",
                        OffsetDateTime.now().plusHours(8),
                        "{\"ttl_reason\":\"temporary route closure\"}");
            }
            if (count(connection,
                    "SELECT COUNT(*) FROM AIM_DEMO_TRACES "
                            + "WHERE shareable_pattern = 'RAINY_EVENING_REROUTE'") == 0) {
                insertTrace(
                        connection,
                        "How can a guest reach the evening show during rain?",
                        "Accessible covered connectors and venue opening times",
                        "Routed through the atrium and moved dinner earlier",
                        "Guest arrived dry and on time",
                        true,
                        "RAINY_EVENING_REROUTE");
                insertTrace(
                        connection,
                        "The garden path closed; what is the safest alternative?",
                        "Indoor mobility route and elevator status",
                        "Used the atrium connector and verified the elevator",
                        "No missed reservation and no stairs",
                        true,
                        "RAINY_EVENING_REROUTE");
                insertTrace(
                        connection,
                        "Replan a rainy evening without losing the show",
                        "Venue schedule and covered-route timing",
                        "Sequenced indoor dinner, connector, then venue",
                        "Guest reached the show before seating closed",
                        true,
                        "RAINY_EVENING_REROUTE");
            }
            connection.commit();
            return Map.of(
                    "stage", "retain",
                    "message", "Durable guest facts, one episode, and a short-lived operational note were stored.",
                    "workingMemory",
                    "Current ask: Build Ava a rain-safe evening plan. This stays in the active turn, not the durable table.");
        } catch (SQLException exception) {
            throw failure("retain memory", exception);
        }
    }

    Map<String, Object> recall(String guestId, String query) {
        String normalizedGuest = normalizeGuest(guestId);
        String normalizedQuery = query == null || query.isBlank()
                ? "Build a rain-safe evening plan"
                : query.trim();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            List<Map<String, Object>> memories = new ArrayList<>();
            String sql = """
                    SELECT memory_id, memory_type, memory_key, content, expires_at
                      FROM AIM_DEMO_MEMORIES
                     WHERE guest_id = ?
                       AND agent_id = ?
                       AND scope_type = 'PRIVATE'
                       AND status = 'ACTIVE'
                       AND (expires_at IS NULL OR expires_at > SYSTIMESTAMP)
                     ORDER BY CASE memory_type
                                WHEN 'SEMANTIC' THEN 1
                                WHEN 'EPISODIC' THEN 2
                                ELSE 3
                              END,
                              memory_id
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalizedGuest);
                statement.setString(2, AGENT_ID);
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        long memoryId = results.getLong("memory_id");
                        Map<String, Object> memory = new LinkedHashMap<>();
                        memory.put("memoryId", memoryId);
                        memory.put("type", results.getString("memory_type"));
                        memory.put("key", results.getString("memory_key"));
                        memory.put("content", results.getString("content"));
                        memory.put("expiresAt", string(results.getTimestamp("expires_at")));
                        memories.add(memory);
                        auditRecall(connection, normalizedGuest, normalizedQuery, memoryId);
                    }
                }
            }
            insertGuestTrace(
                    connection,
                    normalizedGuest,
                    normalizedQuery,
                    memories.size() + " scoped memories",
                    "Built a compact context card and proposed an itinerary",
                    "Awaiting guest confirmation");
            connection.commit();

            return Map.of(
                    "stage", "recall",
                    "guestId", normalizedGuest,
                    "query", normalizedQuery,
                    "contextCard", Map.of(
                            "summary", normalizedGuest.equals("AVA")
                                    ? "Returning guest who values quiet mornings and accessible routes."
                                    : "No durable private preferences are available for this guest.",
                            "relevantMemories", memories,
                            "proposedResponse",
                            memories.isEmpty()
                                    ? "Ask the guest for preferences before proposing a route."
                                    : "Start with an early quiet meal, take the covered mobility route, then attend the remembered night experience."),
                    "message", "Recall selected only active, unexpired memories in this guest-and-agent scope.");
        } catch (SQLException exception) {
            throw failure("recall memory", exception);
        }
    }

    Map<String, Object> correct() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            Long oldId = null;
            int version = 1;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT memory_id, version_no
                      FROM AIM_DEMO_MEMORIES
                     WHERE guest_id = 'AVA'
                       AND memory_key = 'night-experience'
                       AND status = 'ACTIVE'
                     FOR UPDATE
                    """);
                 ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    oldId = results.getLong(1);
                    version = results.getInt(2);
                }
            }
            if (oldId == null) {
                connection.rollback();
                return Map.of(
                        "stage", "refine",
                        "message", "Run Retain first; there is no active night-experience memory to correct.");
            }
            long newId = insertMemory(
                    connection,
                    "AVA",
                    "SEMANTIC",
                    "PRIVATE",
                    "night-experience",
                    "Interested in the lantern show, not the fireworks show.",
                    "guest-correction",
                    null,
                    "{\"confidence\":1.0,\"correction\":\"guest-confirmed\"}",
                    version + 1);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE AIM_DEMO_MEMORIES
                       SET status = 'SUPERSEDED', superseded_by = ?
                     WHERE memory_id = ?
                    """)) {
                statement.setLong(1, newId);
                statement.setLong(2, oldId);
                statement.executeUpdate();
            }
            connection.commit();
            return Map.of(
                    "stage", "refine",
                    "message", "The old value remains auditable as superseded; version "
                            + (version + 1) + " is now active.",
                    "oldMemoryId", oldId,
                    "newMemoryId", newId);
        } catch (SQLException exception) {
            throw failure("correct memory", exception);
        }
    }

    Map<String, Object> expireOperationalMemory() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE AIM_DEMO_MEMORIES
                        SET status = 'EXPIRED', expires_at = SYSTIMESTAMP
                      WHERE guest_id = 'AVA'
                        AND memory_key = 'rain-route-tonight'
                        AND status = 'ACTIVE'
                     """)) {
            int rows = statement.executeUpdate();
            return Map.of(
                    "stage", "expire",
                    "expiredRows", rows,
                    "message", "The temporary closure no longer participates in recall; durable preferences remain.");
        } catch (SQLException exception) {
            throw failure("expire operational memory", exception);
        }
    }

    Map<String, Object> dream() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            int episodes = count(connection, """
                    SELECT COUNT(*)
                      FROM AIM_DEMO_TRACES
                     WHERE was_successful = 1
                       AND shareable_pattern = 'RAINY_EVENING_REROUTE'
                    """);
            if (episodes < 3) {
                connection.rollback();
                return Map.of(
                        "stage", "dream",
                        "message", "At least three successful shareable traces are required.",
                        "sourceEpisodes", episodes);
            }
            if (count(connection,
                    "SELECT COUNT(*) FROM AIM_DEMO_SKILLS "
                            + "WHERE skill_name = 'rainy-evening-reroute'") == 0) {
                long skillId = nextId(connection, "AIM_DEMO_SKILL_SEQ");
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO AIM_DEMO_SKILLS (
                          skill_id, skill_name, trigger_text, instructions_json,
                          status, source_episode_count
                        ) VALUES (?, ?, ?, ?, 'PENDING', ?)
                        """)) {
                    statement.setLong(1, skillId);
                    statement.setString(2, "rainy-evening-reroute");
                    statement.setString(
                            3,
                            "Use when rain closes an outdoor path before an evening venue.");
                    statement.setString(
                            4,
                            "{\"steps\":["
                                    + "\"Check accessibility and live closures\","
                                    + "\"Prefer covered connectors\","
                                    + "\"Re-sequence dining before the venue\","
                                    + "\"Verify arrival before seating closes\"],"
                                    + "\"private_guest_data_included\":false}");
                    statement.setInt(5, episodes);
                    statement.executeUpdate();
                }
            }
            connection.commit();
            return Map.of(
                    "stage", "dream",
                    "sourceEpisodes", episodes,
                    "message", "A fixed demonstration induction rule found the repeated successful pattern and proposed a skill for human review.");
        } catch (SQLException exception) {
            throw failure("induce skill", exception);
        }
    }

    Map<String, Object> approveSkill(String approver) {
        String actor = approver == null || approver.isBlank()
                ? "demo.presenter@example.com"
                : approver.trim();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE AIM_DEMO_SKILLS
                        SET status = 'APPROVED',
                            approved_by = ?,
                            approved_at = SYSTIMESTAMP
                      WHERE skill_name = 'rainy-evening-reroute'
                        AND status = 'PENDING'
                     """)) {
            statement.setString(1, actor);
            int rows = statement.executeUpdate();
            return Map.of(
                    "stage", "approve",
                    "approvedRows", rows,
                    "approvedBy", actor,
                    "message", rows == 1
                            ? "The proposed shared skill is approved for the next run."
                            : "No pending skill was available; run the Dream step first.");
        } catch (SQLException exception) {
            throw failure("approve skill", exception);
        }
    }

    Map<String, Object> nextDay() {
        try (Connection connection = dataSource.getConnection()) {
            int leakedPrivateMemories = count(connection, """
                    SELECT COUNT(*)
                      FROM (
                        SELECT guest_id AS owner_guest_id
                          FROM AIM_DEMO_MEMORIES
                         WHERE status = 'ACTIVE'
                           AND (expires_at IS NULL OR expires_at > SYSTIMESTAMP)
                           AND (
                             (scope_type = 'PRIVATE' AND guest_id = 'LEO')
                             OR scope_type = 'SHARED'
                           )
                      ) visible_to_leo
                     WHERE owner_guest_id = 'AVA'
                    """);
            List<Map<String, Object>> skills = skills(
                    connection,
                    "WHERE status = 'APPROVED'");
            Map<String, Object> result = Map.of(
                    "stage", "next-day",
                    "guestId", "LEO",
                    "privateAvaMemoriesVisible", leakedPrivateMemories,
                    "approvedSharedSkills", skills,
                    "message", "Leo can reuse the approved operational skill, while Ava's private memories remain outside his scope.");
            lastNextDay = result;
            return result;
        } catch (SQLException exception) {
            throw failure("run next-day proof", exception);
        }
    }

    Map<String, Object> state() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("memories", memories(connection));
            state.put("traces", traces(connection));
            state.put("skills", skills(connection, ""));
            state.put("recalls", recalls(connection));
            state.put("latestStage", lastNextDay == null ? null : "next-day");
            state.put("nextDay", lastNextDay);
            return state;
        } catch (SQLException exception) {
            throw failure("load demo state", exception);
        }
    }

    private static void insertGuest(Connection connection, String id, String name)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO AIM_DEMO_GUESTS (guest_id, display_name) VALUES (?, ?)")) {
            statement.setString(1, id);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private static long insertMemory(
            Connection connection,
            String guestId,
            String type,
            String scope,
            String key,
            String content,
            String source,
            OffsetDateTime expiresAt,
            String metadata) throws SQLException {
        return insertMemory(
                connection,
                guestId,
                type,
                scope,
                key,
                content,
                source,
                expiresAt,
                metadata,
                1);
    }

    private static long insertMemory(
            Connection connection,
            String guestId,
            String type,
            String scope,
            String key,
            String content,
            String source,
            OffsetDateTime expiresAt,
            String metadata,
            int version) throws SQLException {
        long id = nextId(connection, "AIM_DEMO_MEMORY_SEQ");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_DEMO_MEMORIES (
                  memory_id, guest_id, agent_id, memory_type, scope_type,
                  memory_key, content, version_no, expires_at, source, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, guestId);
            statement.setString(3, AGENT_ID);
            statement.setString(4, type);
            statement.setString(5, scope);
            statement.setString(6, key);
            statement.setString(7, content);
            statement.setInt(8, version);
            if (expiresAt == null) {
                statement.setNull(9, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setObject(9, expiresAt);
            }
            statement.setString(10, source);
            statement.setString(11, metadata);
            statement.executeUpdate();
        }
        return id;
    }

    private static void insertTrace(
            Connection connection,
            String asked,
            String retrieved,
            String action,
            String outcome,
            boolean successful,
            String pattern) throws SQLException {
        long id = nextId(connection, "AIM_DEMO_TRACE_SEQ");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_DEMO_TRACES (
                  trace_id, asked, retrieved_summary, action_taken,
                  outcome, was_successful, shareable_pattern
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, asked);
            statement.setString(3, retrieved);
            statement.setString(4, action);
            statement.setString(5, outcome);
            statement.setInt(6, successful ? 1 : 0);
            statement.setString(7, pattern);
            statement.executeUpdate();
        }
    }

    private static void insertGuestTrace(
            Connection connection,
            String guestId,
            String asked,
            String retrieved,
            String action,
            String outcome) throws SQLException {
        long id = nextId(connection, "AIM_DEMO_TRACE_SEQ");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_DEMO_TRACES (
                  trace_id, guest_id, asked, retrieved_summary,
                  action_taken, outcome, was_successful
                ) VALUES (?, ?, ?, ?, ?, ?, 0)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, guestId);
            statement.setString(3, asked);
            statement.setString(4, retrieved);
            statement.setString(5, action);
            statement.setString(6, outcome);
            statement.executeUpdate();
        }
    }

    private static void auditRecall(
            Connection connection,
            String guestId,
            String query,
            long memoryId) throws SQLException {
        long id = nextId(connection, "AIM_DEMO_RECALL_SEQ");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_DEMO_RECALL_AUDIT (
                  recall_id, guest_id, query_text, memory_id, recall_reason
                ) VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, guestId);
            statement.setString(3, query);
            statement.setLong(4, memoryId);
            statement.setString(
                    5,
                    "Active, unexpired memory matched the guest and concierge-agent scope.");
            statement.executeUpdate();
        }
    }

    private static int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            results.next();
            return results.getInt(1);
        }
    }

    private static long nextId(Connection connection, String sequence) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT " + sequence + ".NEXTVAL FROM DUAL")) {
            results.next();
            return results.getLong(1);
        }
    }

    private static List<Map<String, Object>> memories(Connection connection)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("""
                     SELECT memory_id, guest_id, memory_type, scope_type,
                            memory_key, content, status, version_no,
                            expires_at, superseded_by, source, created_at
                       FROM AIM_DEMO_MEMORIES
                      ORDER BY memory_id
                     """)) {
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("memoryId", results.getLong("memory_id"));
                row.put("guestId", results.getString("guest_id"));
                row.put("type", results.getString("memory_type"));
                row.put("scope", results.getString("scope_type"));
                row.put("key", results.getString("memory_key"));
                row.put("content", results.getString("content"));
                row.put("status", results.getString("status"));
                row.put("version", results.getInt("version_no"));
                row.put("expiresAt", string(results.getTimestamp("expires_at")));
                Object superseded = results.getObject("superseded_by");
                row.put("supersededBy", superseded);
                row.put("source", results.getString("source"));
                row.put("createdAt", string(results.getTimestamp("created_at")));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> traces(Connection connection)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("""
                     SELECT trace_id, guest_id, asked, retrieved_summary,
                            action_taken, outcome, was_successful,
                            shareable_pattern, created_at
                       FROM AIM_DEMO_TRACES
                      ORDER BY trace_id
                     """)) {
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("traceId", results.getLong("trace_id"));
                row.put("guestId", results.getString("guest_id"));
                row.put("asked", results.getString("asked"));
                row.put("retrieved", results.getString("retrieved_summary"));
                row.put("action", results.getString("action_taken"));
                row.put("outcome", results.getString("outcome"));
                row.put("successful", results.getInt("was_successful") == 1);
                row.put("pattern", results.getString("shareable_pattern"));
                row.put("createdAt", string(results.getTimestamp("created_at")));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> skills(
            Connection connection,
            String where) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = """
                SELECT skill_id, skill_name, trigger_text, instructions_json,
                       status, source_episode_count, approved_by, approved_at
                  FROM AIM_DEMO_SKILLS
                """ + " " + where + " ORDER BY skill_id";
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("skillId", results.getLong("skill_id"));
                row.put("name", results.getString("skill_name"));
                row.put("trigger", results.getString("trigger_text"));
                row.put("instructions", results.getString("instructions_json"));
                row.put("status", results.getString("status"));
                row.put("sourceEpisodes", results.getInt("source_episode_count"));
                row.put("approvedBy", results.getString("approved_by"));
                row.put("approvedAt", string(results.getTimestamp("approved_at")));
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<Map<String, Object>> recalls(Connection connection)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("""
                     SELECT recall_id, guest_id, query_text, memory_id,
                            recall_reason, recalled_at
                       FROM AIM_DEMO_RECALL_AUDIT
                      ORDER BY recall_id
                     """)) {
            while (results.next()) {
                rows.add(Map.of(
                        "recallId", results.getLong("recall_id"),
                        "guestId", results.getString("guest_id"),
                        "query", results.getString("query_text"),
                        "memoryId", results.getLong("memory_id"),
                        "reason", results.getString("recall_reason"),
                        "recalledAt", string(results.getTimestamp("recalled_at"))));
            }
        }
        return rows;
    }

    private static String normalizeGuest(String guestId) {
        String value = guestId == null || guestId.isBlank()
                ? "AVA"
                : guestId.trim().toUpperCase();
        if (!value.matches("[A-Z0-9_-]{1,40}")) {
            throw new IllegalArgumentException("guestId contains unsupported characters");
        }
        return value;
    }

    private static String string(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Unable to " + action + ": " + exception.getMessage(), exception);
    }
}
