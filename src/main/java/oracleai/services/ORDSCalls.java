package oracleai.services;

import oracleai.AIApplication;
import oracleai.ImageStore;
import oracleai.ImageStoreWrapper;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    @Nullable
    private static String callTextSearch(String ordsEndpoint, String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        ResponseEntity<String> response = new RestTemplate().exchange(ordsEndpoint, HttpMethod.POST, entity, String.class);
        System.out.println("ORDSCalls.analyzeImageInObjectStore response.getBody():" + response.getBody());
        return response.getBody();
    }

  public static ResponseEntity<String> uploadImage(MultipartFile image) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            Map<String, String> payload = new HashMap<>();
            payload.put("p_image_name", image.getOriginalFilename());
            payload.put("p_image", base64Image);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();
            String uploadUrl = AIApplication.ORDS_ENDPOINT_URL + "insert_image/";
            return restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }


    public static ImageStore[] getImageStoreData() {
        String url = AIApplication.ORDS_ENDPOINT_URL + "image_store/";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ImageStoreWrapper> response = restTemplate.getForEntity(url, ImageStoreWrapper.class);
        ImageStoreWrapper wrapper = response.getBody();
        if (wrapper != null) {
            for (ImageStore imageStore : wrapper.getItems()) {
                System.out.println("Image Name: " + imageStore.getImageName());
            }
            return wrapper.getItems().toArray(new ImageStore[0]);
        } else {
            return new ImageStore[0];
        }
    }
}

