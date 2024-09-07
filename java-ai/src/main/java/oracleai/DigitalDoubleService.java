package oracleai;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Service
public class DigitalDoubleService {

    private final RestTemplate restTemplate;

    public DigitalDoubleService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void updateDigitalDoubleData(DigitalDoubleDownloadInfo info) {
        String url = "https://rddainsuh6u1okc-ragdb.adb.us-ashburn-1.oraclecloudapps.com/ords/omlopsuser/update_digital_double_data/";

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Prepare the payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("p_participant_email", info.getEmail());
        requestBody.put("p_modelglburl_out", info.getModelGlbUrl());
        requestBody.put("p_modelfbxurl_out", info.getModelFbxUrl());
        requestBody.put("p_modelusdzurl_out", info.getModelUsdzUrl());
        requestBody.put("p_thumbnailurl_out", info.getThumbnailUrl());
        requestBody.put("p_videourl_out", info.getAnimatedVideoLocation());
        requestBody.put("p_video_out", null); // Optional field
        requestBody.put("p_similar_image_out", info.getSimilarImageUrl());

        // Create the HttpEntity that includes headers and the body
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Make the POST request
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // Handle the response (optional)
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("Request successful: " + response.getBody());
        } else {
            System.err.println("Request failed with status code: " + response.getStatusCode());
        }
    }
}