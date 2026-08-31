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
import com.oracle.ojdbc.agentmemory.records.MessageRecord;
import com.oracle.ojdbc.agentmemory.records.ScopedRecord;
import java.util.List;

public final class BasicMemoryExample {
  private BasicMemoryExample() {}

  public static void main(String[] args) {

    OracleAgentMemory agentMemory = AgentMemoryProperties.load().createMemory();

    User user = User.builder()
        .id("user_1")
        .information("Prefers concise answers")
        .build();

    Agent agent = Agent.builder()
        .id("agent_1")
        .information("Support assistant")
        .build();

    agentMemory.addUser(user);
    agentMemory.addAgent(agent);

    // Resets all demo data so the sample output is predictable across repeated runs.
    agentMemory.clearAll();

    // Alternative: delete only this sample thread instead of clearing all managed demo data.
    //memory.deleteThread("thread_examples");

	  // Creates a (conversation) thread
    OracleThread thread = agentMemory
		    .createThread("thread_1", user.id(), agent.id(), null, null);

    // Persists raw messages and, when extraction is enabled, lets the LLM map them into typed records.
    thread.addMessages(List.of(
        new Message("user", "I like orange juice with breakfast and I prefer short answers."),
        new Message("assistant", "Noted. I will keep suggestions concise.")));

		// Adds an explicit durable memory from the application, separate from LLM-extracted records.
    thread.addMemory(Memory.builder()
        .content("The user likes orange juice with breakfast.")
        .build());

		// Show the records from the (conversation) thread
	  // This will include the messages (with its role), the durable memory
	  // and the guidelines extracted from concise responses
	  System.out.println("Thread records:");
	  thread
			  .getRecords()
			  .forEach(r -> System.out.printf(" - [%s] %s - %s\n",
					  r.recordType(), r.content(),
					  r instanceof MessageRecord message ? message.role() : ""));

	  // Show scoped retrieval; with database embedding enabled, this uses semantic vector search.
    List<OracleSearchResult> results = agentMemory.search(
        "orange juice concise breakfast",
        SearchScope.userScope(user.id()),
        5,
        List.of("memory", "message", "guideline", "fact", "preference"));
    for (OracleSearchResult result : results) {
      System.out.println("- [" + result.record().recordType() + "] score=" + result.score() + " " + result.content());
    }
  }
}
