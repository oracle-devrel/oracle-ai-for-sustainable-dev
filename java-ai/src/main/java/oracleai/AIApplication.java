package oracleai;

import com.oracle.bmc.retrier.RetryConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
public class AIApplication {


    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
    public static final String ORDS_ENDPOINT_URL = System.getenv("ORDS_ENDPOINT_URL");
    public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT");
    public static final String OCI_SPEECH_SERVICE_ENDPOINT = System.getenv("OCI_SPEECH_SERVICE_ENDPOINT");
    public static final String OCI_GENAI_SERVICE_ENDPOINT = System.getenv("OCI_GENAI_SERVICE_ENDPOINT");
    public static final String OCICONFIG_FILE = System.getenv("OCICONFIG_FILE");
    public static final String OCICONFIG_PROFILE = System.getenv("OCICONFIG_PROFILE");

    public static void main(String[] args) {
//        RetryConfiguration retryConfiguration = RetryConfiguration.builder()
//                .terminationStrategy(RetryUtils.createExponentialBackoffStrategy(500, 5)) // Configure limits
//                .build();
        SpringApplication.run(AIApplication.class, args);
	}

}
