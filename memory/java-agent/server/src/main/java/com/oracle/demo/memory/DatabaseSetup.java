package com.oracle.demo.memory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

final class DatabaseSetup {
    private final DataSource dataSource;

    DatabaseSetup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void initialize() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            createTable(connection, "AIM_DEMO_GUESTS", """
                    CREATE TABLE AIM_DEMO_GUESTS (
                      guest_id VARCHAR2(40) PRIMARY KEY,
                      display_name VARCHAR2(120) NOT NULL,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
                    )
                    """);
            createSequence(connection, "AIM_DEMO_MEMORY_SEQ");
            createTable(connection, "AIM_DEMO_MEMORIES", """
                    CREATE TABLE AIM_DEMO_MEMORIES (
                      memory_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40),
                      agent_id VARCHAR2(60) NOT NULL,
                      memory_type VARCHAR2(20) NOT NULL,
                      scope_type VARCHAR2(12) NOT NULL,
                      memory_key VARCHAR2(80) NOT NULL,
                      content VARCHAR2(1000) NOT NULL,
                      status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
                      version_no NUMBER DEFAULT 1 NOT NULL,
                      valid_from TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      expires_at TIMESTAMP WITH TIME ZONE,
                      superseded_by NUMBER,
                      source VARCHAR2(60) NOT NULL,
                      metadata_json CLOB,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      CONSTRAINT aim_mem_type_ck CHECK (
                        memory_type IN ('EPISODIC','SEMANTIC','OPERATIONAL')
                      ),
                      CONSTRAINT aim_mem_scope_ck CHECK (
                        scope_type IN ('PRIVATE','SHARED')
                      ),
                      CONSTRAINT aim_mem_status_ck CHECK (
                        status IN ('ACTIVE','SUPERSEDED','EXPIRED')
                      ),
                      CONSTRAINT aim_mem_json_ck CHECK (metadata_json IS JSON),
                      CONSTRAINT aim_mem_guest_fk FOREIGN KEY (guest_id)
                        REFERENCES AIM_DEMO_GUESTS (guest_id)
                    )
                    """);
            createSequence(connection, "AIM_DEMO_TRACE_SEQ");
            createTable(connection, "AIM_DEMO_TRACES", """
                    CREATE TABLE AIM_DEMO_TRACES (
                      trace_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40),
                      asked VARCHAR2(500) NOT NULL,
                      retrieved_summary VARCHAR2(1000),
                      action_taken VARCHAR2(1000) NOT NULL,
                      outcome VARCHAR2(1000) NOT NULL,
                      was_successful NUMBER(1) DEFAULT 0 NOT NULL,
                      shareable_pattern VARCHAR2(80),
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      CONSTRAINT aim_trace_success_ck CHECK (was_successful IN (0,1))
                    )
                    """);
            createSequence(connection, "AIM_DEMO_SKILL_SEQ");
            createTable(connection, "AIM_DEMO_SKILLS", """
                    CREATE TABLE AIM_DEMO_SKILLS (
                      skill_id NUMBER PRIMARY KEY,
                      skill_name VARCHAR2(100) NOT NULL,
                      trigger_text VARCHAR2(500) NOT NULL,
                      instructions_json CLOB NOT NULL,
                      status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
                      source_episode_count NUMBER NOT NULL,
                      approved_by VARCHAR2(120),
                      approved_at TIMESTAMP WITH TIME ZONE,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      CONSTRAINT aim_skill_status_ck CHECK (
                        status IN ('PENDING','APPROVED','REJECTED')
                      ),
                      CONSTRAINT aim_skill_json_ck CHECK (instructions_json IS JSON)
                    )
                    """);
            createSequence(connection, "AIM_DEMO_RECALL_SEQ");
            createTable(connection, "AIM_DEMO_RECALL_AUDIT", """
                    CREATE TABLE AIM_DEMO_RECALL_AUDIT (
                      recall_id NUMBER PRIMARY KEY,
                      guest_id VARCHAR2(40) NOT NULL,
                      query_text VARCHAR2(500) NOT NULL,
                      memory_id NUMBER NOT NULL,
                      recall_reason VARCHAR2(500) NOT NULL,
                      recalled_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
                      CONSTRAINT aim_recall_memory_fk FOREIGN KEY (memory_id)
                        REFERENCES AIM_DEMO_MEMORIES (memory_id)
                    )
                    """);
            connection.commit();
            ensureGuests(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize the memory demo schema", exception);
        }
    }

    private static void createTable(Connection connection, String name, String ddl)
            throws SQLException {
        if (!exists(connection, "USER_TABLES", "TABLE_NAME", name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            }
        }
    }

    private static void createSequence(Connection connection, String name)
            throws SQLException {
        if (!exists(connection, "USER_SEQUENCES", "SEQUENCE_NAME", name)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE SEQUENCE " + name + " START WITH 1 INCREMENT BY 1 NOCACHE");
            }
        }
    }

    private static boolean exists(
            Connection connection,
            String view,
            String column,
            String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + view + " WHERE " + column + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1) > 0;
            }
        }
    }

    private static void ensureGuests(Connection connection) throws SQLException {
        try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM AIM_DEMO_GUESTS");
             ResultSet results = count.executeQuery()) {
            results.next();
            if (results.getInt(1) > 0) return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO AIM_DEMO_GUESTS (guest_id, display_name) VALUES (?, ?)")) {
            for (List<String> guest : List.of(
                    List.of("AVA", "Ava"),
                    List.of("LEO", "Leo"))) {
                insert.setString(1, guest.get(0));
                insert.setString(2, guest.get(1));
                insert.addBatch();
            }
            insert.executeBatch();
        }
        connection.commit();
    }
}
