package oracleai.digitaldouble;

import oracleai.AIApplication;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DigitalDoubleService {

    public void updateDigitalDoubleData(DigitalDoubleDownloadInfo info) {
        String url = AIApplication.ORDS_OMLOPSENDPOINT_URL + "update_digital_double_data/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("p_participant_email", info.getEmail());
        requestBody.put("p_modelglburl_out", info.getModelGlbUrl());
        requestBody.put("p_modelfbxurl_out", info.modelFbxUrl);
        requestBody.put("p_modelusdzurl_out", info.modelUsdzUrl);
        requestBody.put("p_thumbnailurl_out", info.thumbnailUrl);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response =
                new RestTemplate().exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("Request successful: " + response.getBody());
        } else {
            System.err.println("Request failed with status code: " + response.getStatusCode());
        }

    }
}