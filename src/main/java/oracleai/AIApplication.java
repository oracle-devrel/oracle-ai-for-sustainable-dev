package oracleai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AIApplication {

    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
    public static final String ORDS_ENDPOINT_ANALYZE_IMAGE_OBJECTSTORE = System.getenv("ORDS_ENDPOINT_ANALYZE_IMAGE_OBJECTSTORE");
    public static final String ORDS_ENDPOINT_ANALYZE_IMAGE_INLINE = System.getenv("ORDS_ENDPOINT_ANALYZE_IMAGE_INLINE");
    public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT"); ;

    public static void main(String[] args) {
		SpringApplication.run(AIApplication.class, args);
	}

}
