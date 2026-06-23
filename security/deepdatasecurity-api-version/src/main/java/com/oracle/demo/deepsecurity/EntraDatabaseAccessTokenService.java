package com.oracle.demo.deepsecurity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class EntraDatabaseAccessTokenService implements DatabaseAccessTokenService {

    private final DeepDataSecurityProperties properties;
    private final RestClient restClient;

    EntraDatabaseAccessTokenService(DeepDataSecurityProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getDatabaseAccessToken(String endUserToken) {
        DeepDataSecurityProperties.EntraId entraId = properties.getEntraId();
        validateRequired(entraId.getTenantId(), "DEEPSEC_ENTRA_TENANT_ID");
        validateRequired(entraId.getClientId(), "DEEPSEC_ENTRA_CLIENT_ID");
        validateRequired(entraId.getClientSecret(), "DEEPSEC_ENTRA_CLIENT_SECRET");
        validateRequired(entraId.getDatabaseScope(), "DEEPSEC_ENTRA_DATABASE_SCOPE");
        validateRequired(endUserToken, "signed-in end-user access token");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("client_id", entraId.getClientId());
        form.add("client_secret", entraId.getClientSecret());
        form.add("scope", entraId.getDatabaseScope());
        form.add("assertion", endUserToken);
        form.add("requested_token_use", "on_behalf_of");

        TokenResponse response = restClient.post()
                .uri(tokenEndpoint(entraId.getTenantId()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Microsoft Entra ID token response did not include access_token");
        }

        return response.accessToken();
    }

    private static String tokenEndpoint(String tenantId) {
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
    }

    private static void validateRequired(String value, String envVar) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + envVar + " so the app can request the database-access token");
        }
    }

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType) {
    }
}
