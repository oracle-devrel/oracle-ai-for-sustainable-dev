package oracleai;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class DigitalDoubleORDS {

    public void insertDigitalDoubleData(DigitalDoubleDataRequest request) {
        String url = "https://rddainsuh6u1okc-ragdb.adb.us-ashburn-1.oraclecloudapps.com/ords/omlopsuser/insert_digital_double_data/";

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