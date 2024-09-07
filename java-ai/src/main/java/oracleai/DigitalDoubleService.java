package oracleai;

import org.springframework.beans.factory.annotation.Autowired;
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

//    private final RestTemplate restTemplate;
//
//    @Autowired
//    public DigitalDoubleService(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }

    public void updateDigitalDoubleData(DigitalDoubleDownloadInfo info) {
        String url = AIApplication.ORDS_OMLOPSENDPOINT_URL + "update_digital_double_data/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("p_participant_email", info.getEmail());
        requestBody.put("p_modelglburl_out", info.getModelGlbUrl());
        requestBody.put("p_modelfbxurl_out", info.getModelFbxUrl());
        requestBody.put("p_modelusdzurl_out", info.getModelUsdzUrl());
        requestBody.put("p_thumbnailurl_out", info.getThumbnailUrl());
        requestBody.put("p_videourl_out", info.getAnimatedVideoLocation());
        requestBody.put("p_video_out", "");
        requestBody.put("p_similar_image_out", "");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("Request successful: " + response.getBody());
        } else {
            System.err.println("Request failed with status code: " + response.getStatusCode());
        }
    }
}