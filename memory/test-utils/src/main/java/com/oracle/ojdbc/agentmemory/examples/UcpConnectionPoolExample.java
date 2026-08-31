package com.oracle.ojdbc.agentmemory.examples;

import com.oracle.ojdbc.agentmemory.Agent;
import com.oracle.ojdbc.agentmemory.Memory;
import com.oracle.ojdbc.agentmemory.Message;
import com.oracle.ojdbc.agentmemory.OracleAgentMemory;
import com.oracle.ojdbc.agentmemory.OracleSearchResult;
import com.oracle.ojdbc.agentmemory.OracleThread;
import com.oracle.ojdbc.agentmemory.SearchScope;
import com.oracle.ojdbc.agentmemory.User;
import com.oracle.ojdbc.agentmemory.config.AgentMemoryProperties;
import com.oracle.ojdbc.agentmemory.records.ScopedRecord;
import java.util.List;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

public final class UcpConnectionPoolExample {
  private UcpConnectionPoolExample() {}

  public static void main(String[] args) throws Exception {
    AgentMemoryProperties properties = AgentMemoryProperties.load();
    PoolDataSource pool = properties.createConnectionPool();
    pool.registerConnectionInitializationCallback(connection -> connection.setAutoCommit(true));

    try {
      OracleAgentMemory memory = OracleAgentMemory.builder()
          .connectionPool(pool)
          .tablePrefix(properties.schema().tablePrefix())
          .autoCreateSchema(properties.schema().autoCreate())
          .embedder(properties.createEmbedder(pool))
          .llm(properties.createLlm())
          .extractMemories(properties.memory().extractMemories())
          .build();

      User user = User.builder()
          .id("user_ucp_examples")
          .information("Prefers concise breakfast recommendations")
          .build();
      Agent agent = Agent.builder()
          .id("agent_ucp_examples")
          .information("Food assistant")
          .build();
      memory.addUser(user);
      memory.addAgent(agent);

      // Keeps this sample idempotent without deleting unrelated users, agents, or memories.
      memory.deleteThread("thread_ucp_examples");
      OracleThread thread = memory.createThread("thread_ucp_examples", user.id(), agent.id(), null, null);
      // addMessages stores raw turns and triggers LLM extraction when configured.
      thread.addMessages(List.of(
          new Message("user",
              "I like orange juice with breakfast. Keep answers short and use bullet points."),
          new Message("assistant",
              "Understood. I will keep breakfast suggestions brief and use bullet points.")));
      // Adds an application-controlled durable memory in the same user/agent/thread scope.
      thread.addMemory(Memory.builder()
          .content("The user likes orange juice with breakfast.")
          .build());

      System.out.println("Thread records using UCP:");
      for (ScopedRecord record : thread.getRecords()) {
        System.out.println("- [" + record.recordType() + "] " + record.content());
      }

      System.out.println();
      System.out.println("Search results using UCP:");
      List<OracleSearchResult> results = memory.search(
          "orange juice",
          SearchScope.userScope(user.id()),
          5,
          List.of("memory", "message", "guideline", "fact", "preference"));
      for (OracleSearchResult result : results) {
        System.out.println("- [" + result.record().recordType() + "] score=" + result.score() + " " + result.content());
      }
    } finally {
      UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
          .destroyConnectionPool(pool.getConnectionPoolName());
    }
  }
}
