package com.oracle.demo.memory;

import oracle.jdbc.agentmemory.MessageRole;
import oracle.jdbc.agentmemory.OracleAgentMemory;
import oracle.jdbc.agentmemory.OracleThread;
import oracle.jdbc.agentmemory.RecordType;
import oracle.jdbc.agentmemory.SchemaPolicy;
import oracle.jdbc.agentmemory.SearchRequest;
import oracle.jdbc.agentmemory.SearchResult;
import oracle.jdbc.agentmemory.SearchScope;
import oracle.jdbc.agentmemory.records.MessageRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AgentMemoryLibraryDemo {
    private static final String AGENT_ID = "FLYNNS_THEME_PARK_CONCIERGE";
    private static final String AVA_ID = "AVA";
    private static final String LEO_ID = "LEO";
    private static final String THREAD_ID = "flynns_theme_park_ava_visit";
    private static final String MEMORY_STORE_ID = "OAMJ_CONCIERGE";
    private static final EnumSet<RecordType> RECORD_TYPES = EnumSet.of(
            RecordType.MEMORY,
            RecordType.GUIDELINE,
            RecordType.FACT,
            RecordType.PREFERENCE);

    private final DataSource dataSource;
    private final OracleAgentMemory memory;
    private List<Map<String, Object>> lastSearchResults = List.of();
    private String lastContextCard;

    AgentMemoryLibraryDemo(DataSource pooledDataSource, Map<String, String> environment) {
        this.dataSource = new AutoCommitDataSource(pooledDataSource);
        LegacyMemorySchemaMigration.migrateIfNecessary(dataSource);
        String embeddingModel =
                environment.getOrDefault("MEMORY_EMBEDDING_MODEL", "allminilm");
        String ollamaUrl =
                environment.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String ollamaModel =
                environment.getOrDefault("OLLAMA_MODEL", "llama3.2:latest");
        this.memory = OracleAgentMemory.builder()
                .connection(dataSource)
                .memoryStoreId(MEMORY_STORE_ID)
                .schemaPolicy(SchemaPolicy.CREATE_IF_NECESSARY)
                .extractMemories(true)
                .embeddingProvider(new DatabaseEmbeddingProvider(dataSource, embeddingModel))
                .llmProvider(new OllamaLlmProvider(ollamaUrl, ollamaModel, 0.0d))
                .build();
    }

    synchronized Map<String, Object> reset() {
        clearManagedRows();
        lastSearchResults = List.of();
        lastContextCard = null;
        Map<String, Object> state = state();
        state.put(
                "message",
                "The library-managed message and memory tables are empty. "
                        + "No browser-only state is carrying the demonstration.");
        return state;
    }

    synchronized Map<String, Object> retain() {
        clearManagedRows();
        OracleThread thread = memory.createThread(THREAD_ID, AVA_ID, AGENT_ID);
        thread.addMessages(List.of(
                MessageRecord.of(
                        MessageRole.USER,
                        "I prefer a quiet breakfast before 8 AM, need routes with minimal "
                                + "stairs, and want to see the lantern show. Keep itineraries "
                                + "short and use bullet points."),
                MessageRecord.of(
                        MessageRole.ASSISTANT,
                        "I will remember those preferences for your Flynn's Theme Park visit.")));
        thread.addMemory(
                "The covered atrium route worked well during Ava's previous rainy visit.",
                RecordType.MEMORY);
        lastSearchResults = List.of();
        lastContextCard = thread.getContextCard().content();

        Map<String, Object> state = state();
        state.put(
                "message",
                "addMessages() persisted the conversation, Ollama extracted durable records, "
                        + "and Oracle AI Database generated an embedding for every memory.");
        return state;
    }

    synchronized Map<String, Object> recall(String query) {
        String resolvedQuery = query == null || query.isBlank()
                ? "Plan an accessible quiet morning and rainy evening"
                : query.trim();
        OracleThread thread = memory.thread(THREAD_ID, AVA_ID, AGENT_ID);
        lastSearchResults = searchRows(thread.search(resolvedQuery, 8));
        lastContextCard = thread.getContextCard().content();
        int leoResults = memory.search(SearchRequest.builder(resolvedQuery, 8)
                .scope(SearchScope.builder()
                        .userId(LEO_ID)
                        .exactUserMatch(true)
                        .build())
                .recordTypes(RECORD_TYPES)
                .build()).size();

        Map<String, Object> state = state();
        state.put("query", resolvedQuery);
        state.put("leoPrivateResults", leoResults);
        state.put(
                "message",
                "search() embedded the request inside Oracle AI Database, ranked Ava's scoped "
                        + "records by vector distance, and returned zero Ava records for Leo.");
        return state;
    }

    synchronized Map<String, Object> state() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("library", "ojdbc-agent-memory");
        result.put("memoryStoreId", MEMORY_STORE_ID);
        result.put("embeddingModel", "ALLMINILM");
        result.put("embeddingDimensions", embeddingDimensions());
        result.put("messages", messages());
        result.put("records", records());
        result.put("searchResults", lastSearchResults);
        result.put("contextCard", lastContextCard);
        return result;
    }

    private List<Map<String, Object>> messages() {
        return queryRows("""
                SELECT message_role AS role,
                       DBMS_LOB.SUBSTR(content, 1000, 1) AS content
                  FROM OAMJ_CONCIERGE_MESSAGE
                 WHERE thread_id = 'flynns_theme_park_ava_visit'
                 ORDER BY order_seq
                """);
    }

    private List<Map<String, Object>> records() {
        return queryRows("""
                SELECT record_id AS id,
                       memory_type AS type,
                       DBMS_LOB.SUBSTR(content, 1000, 1) AS content,
                       user_id AS "userId",
                       agent_id AS "agentId",
                       thread_id AS "threadId"
                  FROM OAMJ_CONCIERGE_MEMORY
                 WHERE thread_id = 'flynns_theme_park_ava_visit'
                 ORDER BY order_seq
                """);
    }

    private static List<Map<String, Object>> searchRows(
            List<SearchResult> results) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (SearchResult result : results) {
            rows.add(Map.of(
                    "rank", rank++,
                    "type", result.record().recordType().value(),
                    "content", result.content(),
                    "distance", result.distance()));
        }
        return rows;
    }

    private void clearManagedRows() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     BEGIN
                       DELETE FROM OAMJ_CONCIERGE_RECORD_CHUNKS;
                       DELETE FROM OAMJ_CONCIERGE_MESSAGE;
                       DELETE FROM OAMJ_CONCIERGE_MEMORY;
                       DELETE FROM OAMJ_CONCIERGE_ACTOR_PROFILE;
                       DELETE FROM OAMJ_CONCIERGE_THREAD;
                     END;
                     """)) {
            statement.execute();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to reset Oracle Agent Memory rows", exception);
        }
    }

    private List<Map<String, Object>> queryRows(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            int columnCount = results.getMetaData().getColumnCount();
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int column = 1; column <= columnCount; column++) {
                    row.put(results.getMetaData().getColumnLabel(column), results.getObject(column));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read Oracle Agent Memory rows", exception);
        }
    }

    private int embeddingDimensions() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT VECTOR_DIMENSION_COUNT("
                             + "VECTOR_EMBEDDING(allminilm USING "
                             + "'Flynn''s Theme Park concierge' AS DATA)) FROM DUAL");
             ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to verify ALLMINILM: " + exception.getMessage(),
                    exception);
        }
    }
}
