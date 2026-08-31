package com.oracle.ojdbc.agentmemory.examples;

import com.oracle.ojdbc.agentmemory.config.AgentMemoryProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

public final class DatabaseVerificationExample {
  private DatabaseVerificationExample() {}

  public static void main(String[] args) throws Exception {
    AgentMemoryProperties properties = AgentMemoryProperties.load();
    DataSource dataSource = properties.createOracleDataSource();
    String prefix = properties.schema().tablePrefix();

    try (Connection connection = dataSource.getConnection()) {
      printCount(connection, prefix + "MESSAGE");
      printCount(connection, prefix + "RECORDS");
      printEmbeddingModelCount(connection, properties.embedding().model());

      try (PreparedStatement statement = connection.prepareStatement(
          "select record_type, content, user_id, agent_id, thread_id "
              + "from " + prefix + "RECORDS order by created_at");
           ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          System.out.printf(
              "- [%s] %s (user=%s, agent=%s, thread=%s)%n",
              rows.getString("record_type"),
              rows.getString("content"),
              rows.getString("user_id"),
              rows.getString("agent_id"),
              rows.getString("thread_id"));
        }
      }
    }
  }

  private static void printCount(Connection connection, String table) throws Exception {
    try (PreparedStatement statement =
             connection.prepareStatement("select count(*) from " + table);
         ResultSet rows = statement.executeQuery()) {
      rows.next();
      System.out.println(table + " rows=" + rows.getLong(1));
    }
  }

  private static void printEmbeddingModelCount(Connection connection, String model)
      throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
        "select count(*) from user_mining_models where upper(model_name) = upper(?)")) {
      statement.setString(1, model);
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        System.out.println("Database embedding model " + model + " count=" + rows.getLong(1));
      }
    }
  }
}
