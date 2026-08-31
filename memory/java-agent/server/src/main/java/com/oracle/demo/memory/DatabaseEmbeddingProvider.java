package com.oracle.demo.memory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import oracle.jdbc.agentmemory.internal.VectorValues;
import oracle.jdbc.agentmemory.spi.EmbeddingProvider;
import oracle.jdbc.agentmemory.spi.EmbeddingRequest;
import oracle.jdbc.agentmemory.spi.EmbeddingResponse;
import oracle.sql.VECTOR;

final class DatabaseEmbeddingProvider implements EmbeddingProvider {
    private final DataSource dataSource;
    private final String model;

    DatabaseEmbeddingProvider(DataSource dataSource, String model) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource must not be null");
        }
        if (model == null || !model.matches("[A-Za-z][A-Za-z0-9_$#]*")) {
            throw new IllegalArgumentException("model must be an unquoted SQL identifier");
        }
        this.dataSource = dataSource;
        this.model = model;
    }

    @Override
    public String providerName() {
        return "oracle-database-vector-embedding";
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        List<double[]> vectors = new ArrayList<>();
        for (String input : request.inputs()) {
            vectors.add(embedOne(input));
        }
        return EmbeddingResponse.of(vectors);
    }

    private double[] embedOne(String input) {
        String sql = "SELECT VECTOR_EMBEDDING(" + model + " USING ? AS DATA) FROM DUAL";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, input);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new IllegalStateException("Database embedding returned no rows");
                }
                return VectorValues.toDoubleArray(results.getObject(1, VECTOR.class));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Database embedding failed for model " + model,
                    exception);
        }
    }
}
