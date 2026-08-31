package com.oracle.ojdbc.agentmemory.examples;

import com.oracle.ojdbc.agentmemory.Agent;
import com.oracle.ojdbc.agentmemory.Message;
import com.oracle.ojdbc.agentmemory.OracleAgentMemory;
import com.oracle.ojdbc.agentmemory.OracleSearchResult;
import com.oracle.ojdbc.agentmemory.OracleThread;
import com.oracle.ojdbc.agentmemory.SearchScope;
import com.oracle.ojdbc.agentmemory.User;
import com.oracle.ojdbc.agentmemory.config.AgentMemoryProperties;
import com.oracle.ojdbc.agentmemory.internal.JsonStrings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public final class BenchmarkExample {
  private BenchmarkExample() {}

  public static void main(String[] args) throws IOException {
    AgentMemoryProperties properties = AgentMemoryProperties.load();
    DataSource dataSource = properties.createDataSource();
    OracleAgentMemory agentMemory = OracleAgentMemory.builder()
        .dataSource(dataSource)
        .tablePrefix(properties.schema().tablePrefix())
        .autoCreateSchema(properties.schema().autoCreate())
        .embedder(properties.createEmbedder(dataSource))
        .llm(properties.createLlm())
        .extractMemories(properties.memory().extractMemories())
        .build();
    String runId = Long.toHexString(System.currentTimeMillis());

    for (BenchmarkCase benchmarkCase : loadCases()) {
      User user = User.builder()
          .id(benchmarkCase.userId() + "_" + runId)
          .information("Benchmark user")
          .build();

      Agent agent = Agent.builder()
          .id(benchmarkCase.agentId())
          .information("Benchmark assistant")
          .build();

      agentMemory.addUser(user);
      agentMemory.addAgent(agent);

      OracleThread thread = agentMemory.createThread(
          benchmarkCase.threadId() + "_" + runId,
          user.id(),
          agent.id(),
          null,
          null);

      // Replays the benchmark chat history so raw messages land in OAMJ_MESSAGE and extracted durable memories land in OAMJ_RECORDS.
      thread.addMessages(benchmarkCase.messages());

      List<OracleSearchResult> results = agentMemory.search(
          benchmarkCase.question(),
          SearchScope.userScope(user.id()),
          10,
          List.of("memory", "guideline", "fact", "preference"));

      boolean hit = hasExpectedSnippets(results, benchmarkCase.expectedMemorySnippets());

      System.out.println("question_id=" + benchmarkCase.questionId());
      System.out.println("question_type=" + benchmarkCase.questionType());
      System.out.println("question=" + benchmarkCase.question());
      System.out.println("expected_answer=" + benchmarkCase.expectedAnswer());
      System.out.println("hit=" + hit);
      for (OracleSearchResult result : results) {
        System.out.println("- [" + result.record().recordType() + "] " + result.content());
      }
      System.out.println();

			System.out.println("--- context card ---");
			System.out.println(thread.getContextCard().content());

	    System.out.println("--- summary ---");
	    System.out.println(thread.getSummary().content());
    }
  }

  private static boolean hasExpectedSnippets(
      List<OracleSearchResult> results,
      List<String> expectedMemorySnippets) {
    for (String snippet : expectedMemorySnippets) {
      boolean found = false;
      for (OracleSearchResult result : results) {
        if (result.content().toLowerCase().contains(snippet.toLowerCase())) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  private static List<BenchmarkCase> loadCases() throws IOException {
    try (InputStream inputStream =
             BenchmarkExample.class.getResourceAsStream("/longmemeval_oracle_common.json")) {
      if (inputStream == null) {
        throw new IllegalStateException("Benchmark resource not found");
      }
      String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return parseCases(json);
    }
  }

  private static List<BenchmarkCase> parseCases(String json) {
    List<BenchmarkCase> out = new ArrayList<>();
    String casesArray = arrayBody(json, "cases");
    for (String caseJson : splitTopLevelObjects(casesArray)) {
      out.add(new BenchmarkCase(
          JsonStrings.extractString(caseJson, "question_id"),
          JsonStrings.extractString(caseJson, "question_type"),
          JsonStrings.extractString(caseJson, "user_id"),
          JsonStrings.extractString(caseJson, "agent_id"),
          JsonStrings.extractString(caseJson, "thread_id"),
          parseMessages(arrayBody(caseJson, "messages")),
          JsonStrings.extractString(caseJson, "question"),
          JsonStrings.extractString(caseJson, "expected_answer"),
          parseStringArray(arrayBody(caseJson, "expected_memory_snippets"))));
    }
    return out;
  }

  private static List<Message> parseMessages(String json) {
    List<Message> messages = new ArrayList<>();
    for (String messageJson : splitTopLevelObjects(json)) {
      messages.add(new Message(
          JsonStrings.extractString(messageJson, "role"),
          JsonStrings.extractString(messageJson, "content")));
    }
    return messages;
  }

  private static List<String> parseStringArray(String json) {
    List<String> out = new ArrayList<>();
    boolean inString = false;
    boolean escaped = false;
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      if (escaped) {
        current.append(c == 'n' ? '\n' : c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        if (inString) {
          out.add(current.toString());
          current.setLength(0);
        }
        inString = !inString;
      } else if (inString) {
        current.append(c);
      }
    }
    return out;
  }

  private static String arrayBody(String json, String key) {
    String marker = "\"" + key + "\"";
    int keyIndex = json.indexOf(marker);
    if (keyIndex < 0) {
      return "";
    }
    int start = json.indexOf('[', keyIndex);
    if (start < 0) {
      return "";
    }
    int depth = 0;
    for (int i = start; i < json.length(); i++) {
      char c = json.charAt(i);
      if (c == '[') {
        depth++;
      } else if (c == ']') {
        depth--;
        if (depth == 0) {
          return json.substring(start + 1, i);
        }
      }
    }
    return "";
  }

  private static List<String> splitTopLevelObjects(String json) {
    List<String> out = new ArrayList<>();
    int objectStart = -1;
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      if (escaped) {
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        inString = !inString;
      } else if (!inString && c == '{') {
        if (depth == 0) {
          objectStart = i;
        }
        depth++;
      } else if (!inString && c == '}') {
        depth--;
        if (depth == 0 && objectStart >= 0) {
          out.add(json.substring(objectStart, i + 1));
          objectStart = -1;
        }
      }
    }
    return out;
  }

  private record BenchmarkCase(
      String questionId,
      String questionType,
      String userId,
      String agentId,
      String threadId,
      List<Message> messages,
      String question,
      String expectedAnswer,
      List<String> expectedMemorySnippets) {}
}
