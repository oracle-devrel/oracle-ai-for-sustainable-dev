package oracleai;

import com.oracle.bmc.retrier.RetryConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AIApplication {

    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
    public static final String ORDS_ENDPOINT_URL = System.getenv("ORDS_ENDPOINT_URL");
    public static final String ORDS_OMLOPSENDPOINT_URL= System.getenv("ORDS_ENDPOINT_URL") + "/omlopsuser/";
    public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT");
    public static final String OCICONFIG_FILE = System.getenv("OCICONFIG_FILE");
    public static final String OCICONFIG_PROFILE = System.getenv("OCICONFIG_PROFILE");
    public static final String DIGITAL_DOUBLES_IMAGES_ENDPOINT = System.getenv("DIGITAL_DOUBLES_IMAGES_ENDPOINT");
    public static final String THREEDEY = "msy_mykey";

    static {
        System.out.println("AIApplication.static initializer COMPARTMENT_ID:" + COMPARTMENT_ID);
        System.out.println("AIApplication.static initializer OBJECTSTORAGE_NAMESPACE:" + OBJECTSTORAGE_NAMESPACE);
        System.out.println("AIApplication.static initializer OBJECTSTORAGE_BUCKETNAME:" + OBJECTSTORAGE_BUCKETNAME);
        System.out.println("AIApplication.static initializer ORDS_ENDPOINT_URL:" + ORDS_ENDPOINT_URL);
        System.out.println("AIApplication.static initializer OCI_VISION_SERVICE_ENDPOINT:" + OCI_VISION_SERVICE_ENDPOINT);
    }

    public static void main(String[] args) {
//        RetryConfiguration retryConfiguration = RetryConfiguration.builder()
//                .terminationStrategy(RetryUtils.createExponentialBackoffStrategy(500, 5)) // Configure limits
//                .build();
        SpringApplication.run(AIApplication.class, args);
    }

}
