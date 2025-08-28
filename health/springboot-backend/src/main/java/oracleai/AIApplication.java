package oracleai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AIApplication {

	public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
	public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
	public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
	public static final String ORDS_ENDPOINT_URL = System.getenv("ORDS_ENDPOINT_URL");
	public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT");
	public static final String OCI_SPEECH_SERVICE_ENDPOINT = System.getenv("OCI_SPEECH_SERVICE_ENDPOINT");
	public static final String OCI_GENAI_SERVICE_ENDPOINT = System.getenv("OCI_GENAI_SERVICE_ENDPOINT");
    public static final String VISIONAI_XRAY_BREASTCANCER_MODEL_OCID = System.getenv("VISIONAI_XRAY_BREASTCANCER_MODEL_OCID");
    public static final String VISIONAI_XRAY_LUNGCANCER_MODEL_OCID = System.getenv("VISIONAI_XRAY_LUNGCANCER_MODEL_OCID");
    public static final String VISIONAI_XRAY_PNEUMONIA_MODEL_OCID = System.getenv("VISIONAI_XRAY_PNEUMONIA_MODEL_OCID");

    static {
		System.out.println("AIApplication.static initializer SPRING_DATASOURCE_USERNAME:" + System.getenv("SPRING_DATASOURCE_USERNAME"));
		System.out.println("AIApplication.static initializer spring.datasource.username:" + System.getenv("spring.datasource.username"));
		System.out.println("AIApplication.static initializer spring.datasource.url:" + System.getenv("spring.datasource.url"));
		System.out.println("AIApplication.static initializer COMPARTMENT_ID:" + COMPARTMENT_ID);
		System.out.println("AIApplication.static initializer VISIONAI_XRAY_BREASTCANCER_MODEL_OCID:" + VISIONAI_XRAY_BREASTCANCER_MODEL_OCID);
		System.out.println("AIApplication.static initializer VISIONAI_XRAY_LUNGCANCER_MODEL_OCID:" + VISIONAI_XRAY_LUNGCANCER_MODEL_OCID);
		System.out.println("AIApplication.static initializer VISIONAI_XRAY_PNEUMONIA_MODEL_OCID:" + VISIONAI_XRAY_PNEUMONIA_MODEL_OCID);
		System.out.println("AIApplication.static initializer OBJECTSTORAGE_NAMESPACE:" + OBJECTSTORAGE_NAMESPACE);
		System.out.println("AIApplication.static initializer OBJECTSTORAGE_BUCKETNAME:" + OBJECTSTORAGE_BUCKETNAME);
		System.out.println("AIApplication.static initializer ORDS_ENDPOINT_URL:" + ORDS_ENDPOINT_URL);
		System.out.println("AIApplication.static initializer OCI_VISION_SERVICE_ENDPOINT:" + OCI_VISION_SERVICE_ENDPOINT);
	}

	public static void main(String[] args) {
		SpringApplication.run(AIApplication.class, args);
	}

}
