package oracleai;

//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.ai.vectorstore.oracle.OracleVectorStore;
//import org.springframework.ai.vectorstore.oracle.OracleVectorStore.*;

public class OracleVectorStoreBean {

//    @Bean
//    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
//        return OracleVectorStore.builder(jdbcTemplate, embeddingModel)
//                .tableName("my_vectors")
//                .indexType(OracleVectorStoreIndexType.IVF)
//                .distanceType(OracleVectorStoreDistanceType.COSINE)
//                .dimensions(1536)
//                .searchAccuracy(95)
//                .initializeSchema(true)
//                .build();
//    }
}