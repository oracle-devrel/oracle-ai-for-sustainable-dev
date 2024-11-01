package oracleai;

import com.oracle.bmc.retrier.RetryConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AIApplication {

    public static final String COMPARTMENT_ID="ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq";
    public static final String OBJECTSTORAGE_NAMESPACE="oradbclouducm";
    public static final String OBJECTSTORAGE_BUCKETNAME="doc";
    public static final String ORDS_OMLOPSENDPOINT_URL="https://rddainsuh6u1okc-ragdb.adb.us-ashburn-1.oraclecloudapps.com/ords/omlopsuser/";
    public static final String ORDS_ENDPOINT_URL="https://rddainsuh6u1okc-gd740878851.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/";
    public static final String OCI_VISION_SERVICE_ENDPOINT="https://vision.aiservice.myregion.oci.oraclecloud.com";
    public static final String OCI_SPEECH_SERVICE_ENDPOINT="https://speech.aiservice.myregion.oci.oraclecloud.com";
    public static final String OCI_GENAI_SERVICE_ENDPOINT="https://inference.generativeai.us-chicago-1.oci.oraclecloud.com";
    public static final String OPENAI_KEY="sk-proj-708e3KQqGY9fGfoJ4edWT3BlbkFJMGcsVq7JBOWPg4mxn0Y8";
    public static final String THREEDEY = "msy_oCS1X5nuRxS06AjdlTJ0vCHg3OFyOhpaCMoa";
    public static String OCICONFIG_FILE = "~/.oci/config";
    public static final String OCICONFIG_PROFILE = "DEFAULT";
//    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
//    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
//    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
//    public static final String ORDS_ENDPOINT_URL = System.getenv("ORDS_ENDPOINT_URL");
//    public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT");
//    public static final String OCI_SPEECH_SERVICE_ENDPOINT = System.getenv("OCI_SPEECH_SERVICE_ENDPOINT");
//    public static final String OCI_GENAI_SERVICE_ENDPOINT = System.getenv("OCI_GENAI_SERVICE_ENDPOINT");
//    public static final String OCICONFIG_FILE = System.getenv("OCICONFIG_FILE");
//    public static final String OCICONFIG_PROFILE = System.getenv("OCICONFIG_PROFILE");

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
