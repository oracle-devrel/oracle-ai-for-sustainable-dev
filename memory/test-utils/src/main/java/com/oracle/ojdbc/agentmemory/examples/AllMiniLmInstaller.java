package com.oracle.ojdbc.agentmemory.examples;

import com.oracle.ojdbc.agentmemory.config.AgentMemoryProperties;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import javax.sql.DataSource;

public final class AllMiniLmInstaller {
  private static final String DEFAULT_MODEL_URI =
      "https://adwc4pm.objectstorage.us-ashburn-1.oci.customer-oci.com/p/"
          + "eLddQappgBJ7jNi6Guz9m9LOtYe2u8LWY19GfgU8flFK4N9YgP4kTlrE9Px3pE12/"
          + "n/adwc4pm/b/OML-Resources/o/all_MiniLM_L12_v2.onnx";

  private AllMiniLmInstaller() {}

  public static void main(String[] args) throws Exception {
    AgentMemoryProperties properties = AgentMemoryProperties.load();
    DataSource dataSource = properties.createOracleDataSource();
    String modelName = validatedIdentifier(properties.embedding().model());
    String modelUri = System.getProperty("oam.model.uri", DEFAULT_MODEL_URI);

    try (Connection connection = dataSource.getConnection()) {
      if (!modelExists(connection, modelName)) {
        System.out.println("Loading " + modelName + " from Oracle's public ONNX object...");
        try (CallableStatement statement = connection.prepareCall("""
            begin
              dbms_vector.load_onnx_model_cloud(
                model_name => ?,
                credential => null,
                uri => ?
              );
            end;
            """)) {
          statement.setString(1, modelName);
          statement.setString(2, modelUri);
          statement.execute();
        }
      } else {
        System.out.println("Database model " + modelName + " already exists.");
      }

      try (PreparedStatement statement = connection.prepareStatement(
          "select vector_dimension_count(vector_embedding("
              + modelName
              + " using 'orange juice with breakfast' as data)) from dual");
           ResultSet rows = statement.executeQuery()) {
        rows.next();
        System.out.println(
            "Database model " + modelName + " is ready, dimensions=" + rows.getInt(1));
      }
    }
  }

  private static boolean modelExists(Connection connection, String modelName) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
        "select count(*) from user_mining_models where upper(model_name) = ?")) {
      statement.setString(1, modelName.toUpperCase(Locale.ROOT));
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        return rows.getLong(1) > 0;
      }
    }
  }

  private static String validatedIdentifier(String value) {
    if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_$#]{0,122}")) {
      throw new IllegalArgumentException("Invalid database model identifier: " + value);
    }
    return value;
  }
}
