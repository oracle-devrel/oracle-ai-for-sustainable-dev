package oracleai.services;

import oracleai.AIApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class DigitalDoubleORDS {

    public void insertDigitalDoubleData(DigitalDoubleDataRequest request) {
        String url = AIApplication.ORDS_OMLOPSENDPOINT_URL +  "insert_digital_double_data/";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<DigitalDoubleDataRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        System.out.println(response.getBody());
    }
}