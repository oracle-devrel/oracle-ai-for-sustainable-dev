package com.oracle.demo.memory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class ParkDatabaseSetup {
    private final DataSource dataSource;

    ParkDatabaseSetup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void initialize() {
        try (Connection connection = dataSource.getConnection()) {
            createTable(connection, "AIM_PARK_PARTIES", """
                    CREATE TABLE AIM_PARK_PARTIES (
                      party_id VARCHAR2(40) PRIMARY KEY,
                      party_name VARCHAR2(120) NOT NULL
                    )
                    """);
            createSequence(connection, "AIM_PARK_MEMBER_SEQ");
            createTable(connection, "AIM_PARK_PARTY_MEMBERS", """
                    CREATE TABLE AIM_PARK_PARTY_MEMBERS (
                      membership_id NUMBER PRIMARY KEY,
                      party_id VARCHAR2(40) NOT NULL,
                      guest_id VARCHAR2(40) NOT NULL,
                      consent_until TIMESTAMP WITH TIME ZONE,
                      CONSTRAINT aim_party_member_party_fk FOREIGN KEY (party_id)
                        REFERENCES AIM_PARK_PARTIES (party_id),
                      CONSTRAINT aim_party_member_guest_fk FOREIGN KEY (guest_id)
                        REFERENCES AIM_DEMO_GUESTS (guest_id),
                      CONSTRAINT aim_party_member_uq UNIQUE (party_id, guest_id)
                    )
                    """);
            createTable(connection, "AIM_PARK_PLACES", """
                    CREATE TABLE AIM_PARK_PLACES (
                      place_id VARCHAR2(40) PRIMARY KEY,
                      place_name VARCHAR2(120) NOT NULL,
                      place_type VARCHAR2(30) NOT NULL,
                      zone_name VARCHAR2(80) NOT NULL,
                      x_m NUMBER NOT NULL,
                      y_m NUMBER NOT NULL,
                      location SDO_GEOMETRY NOT NULL,
                      accessible NUMBER(1) DEFAULT 1 NOT NULL,
                      covered NUMBER(1) DEFAULT 0 NOT NULL,
                      quiet_score NUMBER(2) DEFAULT 5 NOT NULL,
                      lore_summary VARCHAR2(500),
                      CONSTRAINT aim_place_access_ck CHECK (accessible IN (0,1)),
                      CONSTRAINT aim_place_cover_ck CHECK (covered IN (0,1))
                    )
                    """);
            createSequence(connection, "AIM_PARK_PATH_SEQ");
            createTable(connection, "AIM_PARK_PATHS", """
                    CREATE TABLE AIM_PARK_PATHS (
                      path_id NUMBER PRIMARY KEY,
                      from_place_id VARCHAR2(40) NOT NULL,
                      to_place_id VARCHAR2(40) NOT NULL,
                      path_name VARCHAR2(120) NOT NULL,
                      distance_m NUMBER NOT NULL,
                      accessible NUMBER(1) DEFAULT 1 NOT NULL,
                      covered NUMBER(1) DEFAULT 0 NOT NULL,
                      status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL,
                      CONSTRAINT aim_path_from_fk FOREIGN KEY (from_place_id)
                        REFERENCES AIM_PARK_PLACES (place_id),
                      CONSTRAINT aim_path_to_fk FOREIGN KEY (to_place_id)
                        REFERENCES AIM_PARK_PLACES (place_id),
                      CONSTRAINT aim_path_status_ck CHECK (status IN ('OPEN','CLOSED'))
                    )
                    """);
            createTable(connection, "AIM_PARK_QUESTS", """
                    CREATE TABLE AIM_PARK_QUESTS (
                      quest_id VARCHAR2(40) PRIMARY KEY,
                      quest_name VARCHAR2(120) NOT NULL,
                      description VARCHAR2(500) NOT NULL,
                      difficulty VARCHAR2(20) NOT NULL,
                      reward_points NUMBER NOT NULL,
                      active NUMBER(1) DEFAULT 1 NOT NULL
                    )
                    """);
            createSequence(connection, "AIM_PARK_STEP_SEQ");
            createTable(connection, "AIM_PARK_QUEST_STEPS", """
                    CREATE TABLE AIM_PARK_QUEST_STEPS (
                      step_id NUMBER PRIMARY KEY,
                      quest_id VARCHAR2(40) NOT NULL,
                      place_id VARCHAR2(40) NOT NULL,
                      step_order NUMBER NOT NULL,
                      clue_text VARCHAR2(500) NOT NULL,
                      CONSTRAINT aim_step_quest_fk FOREIGN KEY (quest_id)
                        REFERENCES AIM_PARK_QUESTS (quest_id),
                      CONSTRAINT aim_step_place_fk FOREIGN KEY (place_id)
                        REFERENCES AIM_PARK_PLACES (place_id),
                      CONSTRAINT aim_step_uq UNIQUE (quest_id, step_order)
                    )
                    """);
            createTable(connection, "AIM_PARK_KNOWLEDGE", """
                    CREATE TABLE AIM_PARK_KNOWLEDGE (
                      knowledge_id VARCHAR2(40) PRIMARY KEY,
                      place_id VARCHAR2(40) NOT NULL,
                      title VARCHAR2(160) NOT NULL,
                      content VARCHAR2(1000) NOT NULL,
                      embedding VECTOR(384, FLOAT32),
                      CONSTRAINT aim_knowledge_place_fk FOREIGN KEY (place_id)
                        REFERENCES AIM_PARK_PLACES (place_id)
                    )
                    """);
            createSequence(connection, "AIM_PARK_PROGRESS_SEQ");
            createTable(connection, "AIM_PARK_PROGRESS", """
                    CREATE TABLE AIM_PARK_PROGRESS (
                      progress_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40) NOT NULL,
                      quest_id VARCHAR2(40) NOT NULL,
                      current_step NUMBER DEFAULT 0 NOT NULL,
                      status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
                      points_earned NUMBER DEFAULT 0 NOT NULL,
                      started_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      completed_at TIMESTAMP WITH TIME ZONE,
                      CONSTRAINT aim_progress_guest_fk FOREIGN KEY (guest_id)
                        REFERENCES AIM_DEMO_GUESTS (guest_id),
                      CONSTRAINT aim_progress_quest_fk FOREIGN KEY (quest_id)
                        REFERENCES AIM_PARK_QUESTS (quest_id),
                      CONSTRAINT aim_progress_status_ck CHECK (status IN ('ACTIVE','COMPLETED'))
                    )
                    """);
            createTable(connection, "AIM_PARK_BADGES", """
                    CREATE TABLE AIM_PARK_BADGES (
                      badge_id VARCHAR2(40) PRIMARY KEY,
                      badge_name VARCHAR2(120) NOT NULL,
                      description VARCHAR2(500) NOT NULL
                    )
                    """);
            createSequence(connection, "AIM_PARK_GUEST_BADGE_SEQ");
            createTable(connection, "AIM_PARK_GUEST_BADGES", """
                    CREATE TABLE AIM_PARK_GUEST_BADGES (
                      guest_badge_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40) NOT NULL,
                      badge_id VARCHAR2(40) NOT NULL,
                      quest_id VARCHAR2(40) NOT NULL,
                      awarded_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      CONSTRAINT aim_guest_badge_guest_fk FOREIGN KEY (guest_id)
                        REFERENCES AIM_DEMO_GUESTS (guest_id),
                      CONSTRAINT aim_guest_badge_badge_fk FOREIGN KEY (badge_id)
                        REFERENCES AIM_PARK_BADGES (badge_id),
                      CONSTRAINT aim_guest_badge_quest_fk FOREIGN KEY (quest_id)
                        REFERENCES AIM_PARK_QUESTS (quest_id)
                    )
                    """);
            createSequence(connection, "AIM_PARK_REWARD_SEQ");
            createTable(connection, "AIM_PARK_REWARD_AUDIT", """
                    CREATE TABLE AIM_PARK_REWARD_AUDIT (
                      audit_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40) NOT NULL,
                      quest_id VARCHAR2(40) NOT NULL,
                      event_type VARCHAR2(30) NOT NULL,
                      points_delta NUMBER DEFAULT 0 NOT NULL,
                      details VARCHAR2(1000) NOT NULL,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
                    )
                    """);
            seed(connection);
            createPropertyGraph(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to initialize the Memory Quest schema: " + exception.getMessage(),
                    exception);
        }
    }

    private static void seed(Connection connection) throws SQLException {
        if (count(connection, "AIM_PARK_PLACES") > 0) return;
        connection.setAutoCommit(false);
        execute(connection, "INSERT INTO AIM_PARK_PARTIES VALUES ('AVA_PARTY', 'Ava and Leo')");
        execute(connection, """
                INSERT INTO AIM_PARK_PARTY_MEMBERS
                VALUES (AIM_PARK_MEMBER_SEQ.NEXTVAL, 'AVA_PARTY', 'AVA', SYSTIMESTAMP + INTERVAL '1' DAY)
                """);
        execute(connection, """
                INSERT INTO AIM_PARK_PARTY_MEMBERS
                VALUES (AIM_PARK_MEMBER_SEQ.NEXTVAL, 'AVA_PARTY', 'LEO', SYSTIMESTAMP + INTERVAL '1' DAY)
                """);
        place(connection, "ENTRANCE", "Park Entrance", "GATE", "Arrival", 80, 500, 1, 0, 4,
                "The starting point for all park adventures.");
        place(connection, "QUIET_CAFE", "Quiet Café", "DINING", "Garden Quarter", 220, 380, 1, 1, 9,
                "Early breakfast is calmest before 8 AM.");
        place(connection, "ATRIUM", "Covered Atrium", "CONNECTOR", "Central Arcade", 420, 420, 1, 1, 7,
                "A broad covered connector with step-free access.");
        place(connection, "LANTERN_GARDEN", "Lantern Garden", "ATTRACTION", "Story Gardens", 650, 340, 1, 0, 8,
                "The lantern symbols record the park founders' first journey.");
        place(connection, "SKY_COASTER", "Sky Coaster", "ATTRACTION", "Summit Zone", 620, 650, 0, 0, 2,
                "A high-energy outdoor attraction reached by stairs.");
        place(connection, "RAIN_TUNNEL", "Rain Tunnel", "CONNECTOR", "Central Arcade", 350, 560, 1, 1, 6,
                "A covered step-free route designed for wet weather.");
        place(connection, "MEETING_PLAZA", "Meeting Plaza", "PLAZA", "Central Arcade", 500, 500, 1, 0, 5,
                "A visible regrouping point for parties.");
        path(connection, "ENTRANCE", "QUIET_CAFE", "Garden Walk", 190, 1, 0);
        path(connection, "QUIET_CAFE", "ATRIUM", "Café Arcade", 205, 1, 1);
        path(connection, "ENTRANCE", "RAIN_TUNNEL", "Arrival Tunnel", 275, 1, 1);
        path(connection, "RAIN_TUNNEL", "ATRIUM", "Atrium Link", 120, 1, 1);
        path(connection, "ATRIUM", "MEETING_PLAZA", "Plaza Link", 100, 1, 1);
        path(connection, "MEETING_PLAZA", "LANTERN_GARDEN", "Lantern Promenade", 180, 1, 0);
        path(connection, "MEETING_PLAZA", "SKY_COASTER", "Summit Steps", 195, 0, 0);
        execute(connection, """
                INSERT INTO AIM_PARK_QUESTS VALUES (
                  'COVERED_CONSTELLATIONS', 'Covered Constellations',
                  'Follow accessible connectors, gather three story clues, and reach the Lantern Garden.',
                  'FAMILY', 300, 1)
                """);
        step(connection, "QUIET_CAFE", 1, "Find the calm breakfast marker beside the garden window.");
        step(connection, "ATRIUM", 2, "Use the covered connector and locate the constellation mosaic.");
        step(connection, "LANTERN_GARDEN", 3, "Decode the founder symbol at the Lantern Garden.");
        execute(connection, """
                INSERT INTO AIM_PARK_BADGES VALUES (
                  'LANTERN_PATHFINDER', 'Lantern Pathfinder',
                  'Completed the accessible Covered Constellations quest.')
                """);
        knowledge(connection, "K_QUIET", "QUIET_CAFE", "Quiet breakfast",
                "The Quiet Café is calmest before 8 AM and has step-free indoor seating.");
        knowledge(connection, "K_ATRIUM", "ATRIUM", "Covered accessible route",
                "The Covered Atrium links the Garden Quarter to the central plaza without stairs.");
        knowledge(connection, "K_LANTERN", "LANTERN_GARDEN", "Lantern founder lore",
                "The Lantern Garden symbols tell how the park founders navigated by constellations.");
        knowledge(connection, "K_RAIN", "RAIN_TUNNEL", "Rain-safe connector",
                "The Rain Tunnel stays open in wet weather and provides a covered accessible route.");
        connection.commit();
    }

    private static void createPropertyGraph(Connection connection) throws SQLException {
        if (countWhere(connection, "USER_PROPERTY_GRAPHS", "GRAPH_NAME", "AIM_PARK_GRAPH") > 0) return;
        execute(connection, """
                CREATE PROPERTY GRAPH AIM_PARK_GRAPH
                  VERTEX TABLES (
                    AIM_DEMO_GUESTS KEY (guest_id)
                      LABEL guest PROPERTIES (guest_id, display_name),
                    AIM_PARK_PARTIES KEY (party_id)
                      LABEL party PROPERTIES (party_id, party_name),
                    AIM_PARK_PLACES KEY (place_id)
                      LABEL place PROPERTIES (
                        place_id, place_name, place_type, zone_name, x_m, y_m,
                        accessible, covered, quiet_score, lore_summary),
                    AIM_PARK_QUESTS KEY (quest_id)
                      LABEL quest PROPERTIES (
                        quest_id, quest_name, description, difficulty, reward_points),
                    AIM_PARK_BADGES KEY (badge_id)
                      LABEL badge PROPERTIES (badge_id, badge_name, description)
                  )
                  EDGE TABLES (
                    AIM_PARK_PARTY_MEMBERS KEY (membership_id)
                      SOURCE KEY (guest_id) REFERENCES AIM_DEMO_GUESTS (guest_id)
                      DESTINATION KEY (party_id) REFERENCES AIM_PARK_PARTIES (party_id)
                      LABEL member_of PROPERTIES (membership_id, consent_until),
                    AIM_PARK_PATHS KEY (path_id)
                      SOURCE KEY (from_place_id) REFERENCES AIM_PARK_PLACES (place_id)
                      DESTINATION KEY (to_place_id) REFERENCES AIM_PARK_PLACES (place_id)
                      LABEL connects PROPERTIES (
                        path_id, path_name, distance_m, accessible, covered, status),
                    AIM_PARK_QUEST_STEPS KEY (step_id)
                      SOURCE KEY (quest_id) REFERENCES AIM_PARK_QUESTS (quest_id)
                      DESTINATION KEY (place_id) REFERENCES AIM_PARK_PLACES (place_id)
                      LABEL quest_step PROPERTIES (step_id, step_order, clue_text),
                    AIM_PARK_GUEST_BADGES KEY (guest_badge_id)
                      SOURCE KEY (guest_id) REFERENCES AIM_DEMO_GUESTS (guest_id)
                      DESTINATION KEY (badge_id) REFERENCES AIM_PARK_BADGES (badge_id)
                      LABEL earned PROPERTIES (guest_badge_id, quest_id, awarded_at)
                  )
                """);
    }

    private static void place(Connection connection, String id, String name, String type,
                              String zone, int x, int y, int accessible, int covered,
                              int quiet, String lore) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_PARK_PLACES
                  (place_id, place_name, place_type, zone_name, x_m, y_m, location,
                   accessible, covered, quiet_score, lore_summary)
                VALUES (?, ?, ?, ?, ?, ?, SDO_GEOMETRY(2001, NULL,
                  SDO_POINT_TYPE(?, ?, NULL), NULL, NULL), ?, ?, ?, ?)
                """)) {
            statement.setString(1, id); statement.setString(2, name); statement.setString(3, type);
            statement.setString(4, zone); statement.setInt(5, x); statement.setInt(6, y);
            statement.setInt(7, x); statement.setInt(8, y); statement.setInt(9, accessible);
            statement.setInt(10, covered); statement.setInt(11, quiet); statement.setString(12, lore);
            statement.executeUpdate();
        }
    }

    private static void path(Connection connection, String from, String to, String name,
                             int distance, int accessible, int covered) throws SQLException {
        for (String[] direction : new String[][]{{from, to}, {to, from}}) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO AIM_PARK_PATHS VALUES (
                      AIM_PARK_PATH_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, 'OPEN')
                    """)) {
                statement.setString(1, direction[0]); statement.setString(2, direction[1]);
                statement.setString(3, name); statement.setInt(4, distance);
                statement.setInt(5, accessible); statement.setInt(6, covered);
                statement.executeUpdate();
            }
        }
    }

    private static void step(Connection connection, String place, int order, String clue)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_PARK_QUEST_STEPS VALUES (
                  AIM_PARK_STEP_SEQ.NEXTVAL, 'COVERED_CONSTELLATIONS', ?, ?, ?)
                """)) {
            statement.setString(1, place); statement.setInt(2, order); statement.setString(3, clue);
            statement.executeUpdate();
        }
    }

    private static void knowledge(Connection connection, String id, String place,
                                  String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO AIM_PARK_KNOWLEDGE
                  (knowledge_id, place_id, title, content, embedding)
                VALUES (?, ?, ?, ?, VECTOR_EMBEDDING(ALLMINILM USING ? AS DATA))
                """)) {
            statement.setString(1, id); statement.setString(2, place);
            statement.setString(3, title); statement.setString(4, content);
            statement.setString(5, content); statement.executeUpdate();
        }
    }

    private static void createTable(Connection connection, String name, String ddl)
            throws SQLException {
        if (countWhere(connection, "USER_TABLES", "TABLE_NAME", name) == 0) execute(connection, ddl);
    }

    private static void createSequence(Connection connection, String name) throws SQLException {
        if (countWhere(connection, "USER_SEQUENCES", "SEQUENCE_NAME", name) == 0) {
            execute(connection, "CREATE SEQUENCE " + name + " START WITH 1 INCREMENT BY 1 NOCACHE");
        }
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next(); return result.getInt(1);
        }
    }

    private static int countWhere(Connection connection, String view, String column, String value)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + view + " WHERE " + column + " = ?")) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }
}
