package oracleai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AIApplication {

    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");

    public static void main(String[] args) {
		SpringApplication.run(AIApplication.class, args);
	}

}
