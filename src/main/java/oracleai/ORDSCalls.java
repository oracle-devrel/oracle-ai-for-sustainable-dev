package oracleai;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Collections;

@Service
public class ORDSCalls {

    public static String callAnalyzeImageApi(String ordsEndpoint, String visionServiceIndpoint,
                                             String compartmentOcid, MultipartFile imageFile)
            throws Exception {
        RestTemplate restTemplate = new RestTemplate();

            String base64ImageData =  Base64.getEncoder().encodeToString(imageFile.getBytes());
        // Construct the JSON request body
        String jsonBody = String.format("{\"p_compartment_ocid\": \"%s\", \"p_endpoint\": \"%s\", \"p_image_data\": \"%s\"}",
                compartmentOcid, visionServiceIndpoint, base64ImageData);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build the request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(ordsEndpoint, HttpMethod.POST, requestEntity, String.class);

        // Return the response body
        return response.getBody();
    }



    public static String analyzeImageInObjectStore(
            String ordsEndpoint, String visionServiceIndpoint, String compartmentOcid,
            String bucketName, String namespaceName, String objectName) {
        RestTemplate restTemplate = new RestTemplate();
        String jsonPayload = String.format(
                "{\"p_bucketname\": \"%s\", \"p_compartment_ocid\": \"%s\", \"p_endpoint\": \"%s\", " +
                        "\"p_namespacename\": \"%s\", \"p_objectname\": \"%s\"}",
                bucketName, compartmentOcid, visionServiceIndpoint, namespaceName, objectName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<String> response = restTemplate.exchange(ordsEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println("ORDSCalls.analyzeImageInObjectStore response.getBody():" + response.getBody());
        return response.getBody();
    }

//
//    public static String callAnalyzeImageApi(String compartmentOcid, String endpoint, String base64ImageData) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        // Construct the JSON request body
//        String jsonBody = String.format(
//                "{\"p_compartment_ocid\": \"%s\", \"p_endpoint\": \"%s\", \"p_image_data\": \"%s\"}",
//                compartmentOcid, endpoint, base64ImageData);
//
//        // Set headers
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // Build the request entity
//        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
//
//        // Define the API endpoint
//        String apiEndpoint = "https://rddainsuh6u1okc-gd740878851.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/call_analyze_image_api/";
//
//        // Send POST request
//        ResponseEntity<String> response = restTemplate.exchange(apiEndpoint, HttpMethod.POST, requestEntity, String.class);
//
//        // Return the response body
//        return response.getBody();
//    }


}

