package oracleai.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Collections;

@Service
public class ORDSCalls {

    public static String callAnalyzeImageInline(String ordsEndpoint, String visionServiceIndpoint,
                                             String compartmentOcid, MultipartFile imageFile)
            throws Exception {
        RestTemplate restTemplate = new RestTemplate();
            String base64ImageData =  Base64.getEncoder().encodeToString(imageFile.getBytes());
        String jsonBody = String.format("{\"p_compartment_ocid\": \"%s\", \"p_endpoint\": \"%s\", \"p_image_data\": \"%s\"}",
                compartmentOcid, visionServiceIndpoint, base64ImageData);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(ordsEndpoint, HttpMethod.POST, requestEntity, String.class);
        return response.getBody();
    }


    //As written only supports one feature type per call
    public static String analyzeImageInObjectStore(
            String ordsEndpoint, String visionServiceEndpoint, String compartmentOcid,
            String bucketName, String namespaceName, String objectName, String featureType, String label) {
        System.out.println("ORDSCalls.analyzeImageInObjectStore");
        System.out.println("ordsEndpoint = " + ordsEndpoint + ", visionServiceEndpoint = " + visionServiceEndpoint + ", compartmentOcid = " + compartmentOcid + ", bucketName = " + bucketName + ", namespaceName = " + namespaceName + ", objectName = " + objectName + ", featureType = " + featureType + ", label = " + label);
        RestTemplate restTemplate = new RestTemplate();
        String jsonPayload = String.format(
                "{\"p_bucketname\": \"%s\", \"p_compartment_ocid\": \"%s\", \"p_endpoint\": \"%s\", " +
                        "\"p_namespacename\": \"%s\", \"p_objectname\": \"%s\", \"p_featuretype\": \"%s\", \"p_label\": \"%s\"}",
                bucketName, compartmentOcid, visionServiceEndpoint, namespaceName, objectName, featureType, label);
        System.out.println("ORDSCalls.analyzeImageInObjectStore jsonPayload:" + jsonPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<String> response = restTemplate.exchange(ordsEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println("ORDSCalls.analyzeImageInObjectStore response.getBody():" + response.getBody());
        return response.getBody();
    }
    public static String executeTextSearchContains(String ordsEndpoint, String sql) {
        String jsonPayload = String.format( "{\"p_sql\": \"%s\"}", sql);
        return callTextSearch(ordsEndpoint, jsonPayload);
    }
    public static String executeTextSearchOR(String ordsEndpoint, String sql, String sql2) {
        String jsonPayload = String.format( "{\"p_sql\": \"%s\", \"p_sql\": \"%s\"}", sql, sql2);
        return callTextSearch(ordsEndpoint, jsonPayload);
    }

    private static String callTextSearch(String ordsEndpoint, String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<String> response = new RestTemplate().exchange(ordsEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println("ORDSCalls.analyzeImageInObjectStore response.getBody():" + response.getBody());
        return response.getBody();
    }


}

