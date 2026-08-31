package com.oracle.demo.memory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

final class ParkExperienceRepository {
    private static final String QUEST = "COVERED_CONSTELLATIONS";
    private final DataSource dataSource;

    ParkExperienceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Map<String, Object> state() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("places", places(connection));
            state.put("paths", graphPaths(connection));
            state.put("party", partyGraph(connection));
            state.put("quest", questGraph(connection));
            state.put("progress", progress(connection));
            state.put("badges", badges(connection));
            state.put("audit", audit(connection));
            return state;
        } catch (SQLException exception) {
            throw failure("load Memory Quest state", exception);
        }
    }

    Map<String, Object> reset() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            execute(connection, "DELETE FROM AIM_PARK_GUEST_BADGES");
            execute(connection, "DELETE FROM AIM_PARK_REWARD_AUDIT");
            execute(connection, "DELETE FROM AIM_PARK_PROGRESS");
            connection.commit();
            Map<String, Object> result = state();
            result.put("message", "Memory Quest reset. The park graph and knowledge remain; guest progress, badges, and reward audit are empty.");
            return result;
        } catch (SQLException exception) {
            throw failure("reset Memory Quest", exception);
        }
    }

    Map<String, Object> plan() {
        try (Connection connection = dataSource.getConnection()) {
            List<Map<String, Object>> pathRows = graphPaths(connection);
            Route route = shortestAccessibleRoute(pathRows, "ENTRANCE", "LANTERN_GARDEN");
            Map<String, Object> result = state();
            result.put("route", Map.of(
                    "placeIds", route.placeIds(),
                    "distanceMeters", route.distance(),
                    "spatialStraightLineMeters", spatialDistance(connection, "ENTRANCE", "LANTERN_GARDEN"),
                    "explanation", "SQL Property Graph supplies open, accessible relationships; Oracle Spatial measures physical separation. The route avoids the inaccessible Summit Steps."));
            result.put("message", "Accessible quest route planned across " + (route.placeIds().size() - 1)
                    + " graph edges (" + route.distance() + " m). Spatial straight-line distance is shown for comparison.");
            return result;
        } catch (SQLException exception) {
            throw failure("plan the accessible quest route", exception);
        }
    }

    Map<String, Object> startQuest() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            if (scalar(connection, "SELECT COUNT(*) FROM AIM_PARK_PROGRESS WHERE guest_id='AVA' AND quest_id='" + QUEST + "'") == 0) {
                execute(connection, """
                        INSERT INTO AIM_PARK_PROGRESS
                          (progress_id, guest_id, quest_id, current_step, status, points_earned)
                        VALUES (AIM_PARK_PROGRESS_SEQ.NEXTVAL, 'AVA', 'COVERED_CONSTELLATIONS', 0, 'ACTIVE', 0)
                        """);
                audit(connection, "QUEST_STARTED", 0,
                        "Ava accepted Covered Constellations after the route passed accessibility checks.");
            }
            connection.commit();
            Map<String, Object> result = state();
            result.put("message", "Quest started transactionally. Ava has consented party context, zero completed checkpoints, and an auditable start event.");
            return result;
        } catch (SQLException exception) {
            throw failure("start the quest", exception);
        }
    }

    Map<String, Object> completeNextStep() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            int current;
            String status;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT current_step, status FROM AIM_PARK_PROGRESS
                     WHERE guest_id='AVA' AND quest_id=? FOR UPDATE
                    """)) {
                statement.setString(1, QUEST);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalArgumentException("Start the quest before completing a checkpoint.");
                    current = result.getInt(1); status = result.getString(2);
                }
            }
            if ("COMPLETED".equals(status)) throw new IllegalArgumentException("The quest is already complete. Reset to run it again.");
            int next = current + 1;
            String placeName = stepPlace(connection, next);
            if (placeName == null) throw new IllegalArgumentException("No next checkpoint exists.");
            boolean complete = next == scalar(connection,
                    "SELECT COUNT(*) FROM AIM_PARK_QUEST_STEPS WHERE quest_id='" + QUEST + "'");
            int points = complete ? 300 : 50;
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE AIM_PARK_PROGRESS
                       SET current_step=?, status=?, points_earned=points_earned+?,
                           completed_at=CASE WHEN ?='COMPLETED' THEN SYSTIMESTAMP END
                     WHERE guest_id='AVA' AND quest_id=?
                    """)) {
                update.setInt(1, next); update.setString(2, complete ? "COMPLETED" : "ACTIVE");
                update.setInt(3, points); update.setString(4, complete ? "COMPLETED" : "ACTIVE");
                update.setString(5, QUEST); update.executeUpdate();
            }
            audit(connection, complete ? "QUEST_COMPLETED" : "CHECKPOINT_COMPLETED", points,
                    "Checkpoint " + next + " verified at " + placeName + ".");
            if (complete) {
                execute(connection, """
                        INSERT INTO AIM_PARK_GUEST_BADGES
                          (guest_badge_id, guest_id, badge_id, quest_id)
                        VALUES (AIM_PARK_GUEST_BADGE_SEQ.NEXTVAL, 'AVA',
                          'LANTERN_PATHFINDER', 'COVERED_CONSTELLATIONS')
                        """);
            }
            connection.commit();
            Map<String, Object> result = state();
            result.put("message", complete
                    ? "Final checkpoint verified. The same transaction completed the quest, awarded 300 points, issued the Lantern Pathfinder badge, and wrote the reward audit."
                    : "Checkpoint " + next + " verified at " + placeName + ". Progress, 50 points, and the audit event committed together.");
            return result;
        } catch (SQLException exception) {
            throw failure("complete the next quest checkpoint", exception);
        }
    }

    Map<String, Object> graphRag(String query) {
        String safeQuery = query == null || query.isBlank()
                ? "Find a quiet accessible rainy route with founder stories" : query.trim();
        try (Connection connection = dataSource.getConnection()) {
            List<Map<String, Object>> hits = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT knowledge_id, place_id, title, content,
                           VECTOR_DISTANCE(embedding,
                             VECTOR_EMBEDDING(ALLMINILM USING ? AS DATA), COSINE) AS distance
                      FROM AIM_PARK_KNOWLEDGE
                     ORDER BY distance
                     FETCH FIRST 3 ROWS ONLY
                    """)) {
                statement.setString(1, safeQuery);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        Map<String, Object> hit = new LinkedHashMap<>();
                        hit.put("knowledgeId", result.getString("knowledge_id"));
                        hit.put("placeId", result.getString("place_id"));
                        hit.put("title", result.getString("title"));
                        hit.put("content", result.getString("content"));
                        hit.put("distance", result.getDouble("distance"));
                        hit.put("graphNeighbors", graphNeighbors(connection, result.getString("place_id")));
                        hit.put("quests", graphQuestsForPlace(connection, result.getString("place_id")));
                        hits.add(hit);
                    }
                }
            }
            Map<String, Object> result = state();
            result.put("graphRag", Map.of(
                    "query", safeQuery,
                    "hits", hits,
                    "answer", "Use the quiet café before 8 AM, continue through covered step-free connectors, and follow the founder-symbol clues to Lantern Garden. The vector hits provide relevant knowledge; graph expansion adds connected places and quest context."));
            result.put("message", "GraphRAG retrieved three vector-ranked knowledge records, then expanded each hit through SQL Property Graph relationships.");
            return result;
        } catch (SQLException exception) {
            throw failure("run GraphRAG", exception);
        }
    }

    private static List<Map<String, Object>> places(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT place_id, place_name, place_type, zone_name, x_m, y_m,
                       accessible, covered, quiet_score, lore_summary
                  FROM AIM_PARK_PLACES ORDER BY place_name
                """);
    }

    private static List<Map<String, Object>> graphPaths(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (a IS place)-[e IS connects]->(b IS place)
                  COLUMNS (
                    a.place_id AS from_place_id,
                    b.place_id AS to_place_id,
                    e.path_id AS path_id,
                    e.path_name AS path_name,
                    e.distance_m AS distance_m,
                    e.accessible AS accessible,
                    e.covered AS covered,
                    e.status AS status))
                ORDER BY path_id
                """);
    }

    private static List<Map<String, Object>> partyGraph(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT guest_id, display_name, party_id, party_name,
                       TO_CHAR(consent_until, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS consent_until
                  FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (g IS guest)-[m IS member_of]->(p IS party)
                  COLUMNS (g.guest_id AS guest_id, g.display_name AS display_name,
                           p.party_id AS party_id, p.party_name AS party_name,
                           m.consent_until AS consent_until))
                ORDER BY guest_id
                """);
    }

    private static List<Map<String, Object>> questGraph(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (q IS quest)-[s IS quest_step]->(p IS place)
                  COLUMNS (q.quest_id AS quest_id, q.quest_name AS quest_name,
                           q.description AS description, q.reward_points AS reward_points,
                           s.step_order AS step_order, s.clue_text AS clue_text,
                           p.place_id AS place_id, p.place_name AS place_name))
                 WHERE quest_id='COVERED_CONSTELLATIONS' ORDER BY step_order
                """);
    }

    private static List<Map<String, Object>> progress(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT progress_id, guest_id, quest_id, current_step, status, points_earned,
                       TO_CHAR(started_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS started_at,
                       TO_CHAR(completed_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS completed_at
                  FROM AIM_PARK_PROGRESS ORDER BY progress_id
                """);
    }

    private static List<Map<String, Object>> badges(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT gb.guest_badge_id, gb.guest_id, b.badge_name, b.description, gb.quest_id,
                       TO_CHAR(gb.awarded_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS awarded_at
                  FROM AIM_PARK_GUEST_BADGES gb JOIN AIM_PARK_BADGES b ON b.badge_id=gb.badge_id
                 ORDER BY gb.guest_badge_id
                """);
    }

    private static List<Map<String, Object>> audit(Connection connection) throws SQLException {
        return rows(connection, """
                SELECT audit_id, guest_id, quest_id, event_type, points_delta, details,
                       TO_CHAR(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3TZH:TZM') AS created_at
                  FROM AIM_PARK_REWARD_AUDIT ORDER BY audit_id
                """);
    }

    private static List<Map<String, Object>> graphNeighbors(Connection connection, String placeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (a IS place)-[e IS connects]->(b IS place)
                  COLUMNS (a.place_id AS from_id, b.place_id AS place_id,
                           b.place_name AS place_name, e.path_name AS relationship,
                           e.distance_m AS distance_m))
                 WHERE from_id=? AND ROWNUM <= 3
                """)) {
            statement.setString(1, placeId); return rows(statement);
        }
    }

    private static List<Map<String, Object>> graphQuestsForPlace(Connection connection, String placeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM GRAPH_TABLE (AIM_PARK_GRAPH
                  MATCH (q IS quest)-[s IS quest_step]->(p IS place)
                  COLUMNS (p.place_id AS place_id, q.quest_name AS quest_name,
                           s.step_order AS step_order))
                 WHERE place_id=?
                """)) {
            statement.setString(1, placeId); return rows(statement);
        }
    }

    private static double spatialDistance(Connection connection, String first, String second)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT SDO_GEOM.SDO_DISTANCE(a.location, b.location, 0.005)
                  FROM AIM_PARK_PLACES a CROSS JOIN AIM_PARK_PLACES b
                 WHERE a.place_id=? AND b.place_id=?
                """)) {
            statement.setString(1, first); statement.setString(2, second);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getDouble(1); }
        }
    }

    private static Route shortestAccessibleRoute(List<Map<String, Object>> paths, String start, String end) {
        Map<String, List<Edge>> graph = new HashMap<>();
        for (Map<String, Object> row : paths) {
            if (number(row.get("ACCESSIBLE")) != 1 || !"OPEN".equals(row.get("STATUS"))) continue;
            graph.computeIfAbsent(string(row.get("FROM_PLACE_ID")), ignored -> new ArrayList<>())
                    .add(new Edge(string(row.get("TO_PLACE_ID")), number(row.get("DISTANCE_M"))));
        }
        Map<String, Integer> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::distance));
        distance.put(start, 0); queue.add(new Node(start, 0));
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.distance() != distance.getOrDefault(node.id(), Integer.MAX_VALUE)) continue;
            for (Edge edge : graph.getOrDefault(node.id(), List.of())) {
                int candidate = node.distance() + edge.distance();
                if (candidate < distance.getOrDefault(edge.to(), Integer.MAX_VALUE)) {
                    distance.put(edge.to(), candidate); previous.put(edge.to(), node.id());
                    queue.add(new Node(edge.to(), candidate));
                }
            }
        }
        if (!distance.containsKey(end)) throw new IllegalStateException("No accessible route found");
        List<String> ids = new ArrayList<>();
        for (String at = end; at != null; at = previous.get(at)) ids.add(0, at);
        return new Route(ids, distance.get(end));
    }

    private static String stepPlace(Connection connection, int step) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT p.place_name FROM AIM_PARK_QUEST_STEPS s
                  JOIN AIM_PARK_PLACES p ON p.place_id=s.place_id
                 WHERE s.quest_id=? AND s.step_order=?
                """)) {
            statement.setString(1, QUEST); statement.setInt(2, step);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getString(1) : null; }
        }
    }

    private static void audit(Connection connection, String type, int points, String details)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_PARK_REWARD_AUDIT
                  (audit_id, guest_id, quest_id, event_type, points_delta, details)
                VALUES (AIM_PARK_REWARD_SEQ.NEXTVAL, 'AVA', ?, ?, ?, ?)
                """)) {
            statement.setString(1, QUEST); statement.setString(2, type);
            statement.setInt(3, points); statement.setString(4, details); statement.executeUpdate();
        }
    }

    private static List<Map<String, Object>> rows(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return rows(result);
        }
    }

    private static List<Map<String, Object>> rows(PreparedStatement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery()) { return rows(result); }
    }

    private static List<Map<String, Object>> rows(ResultSet result) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        int columns = result.getMetaData().getColumnCount();
        while (result.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 1; index <= columns; index++) {
                row.put(result.getMetaData().getColumnLabel(index), result.getObject(index));
            }
            rows.add(row);
        }
        return rows;
    }

    private static int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next(); return result.getInt(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.executeUpdate(sql); }
    }

    private static int number(Object value) { return value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString()); }
    private static String string(Object value) { return value == null ? "" : value.toString(); }
    private static IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Unable to " + action + ": " + exception.getMessage(), exception);
    }

    private record Edge(String to, int distance) {}
    private record Node(String id, int distance) {}
    private record Route(List<String> placeIds, int distance) {}
}
