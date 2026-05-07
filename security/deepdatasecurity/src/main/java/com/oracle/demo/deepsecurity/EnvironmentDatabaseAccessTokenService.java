package com.oracle.demo.deepsecurity;

import org.springframework.stereotype.Service;

@Service
public class EnvironmentDatabaseAccessTokenService implements DatabaseAccessTokenService {

    private final DeepDataSecurityProperties properties;

    EnvironmentDatabaseAccessTokenService(DeepDataSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getDatabaseAccessToken(String endUserToken) {
        String token = properties.getDatabaseAccessToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Set DEEPSEC_DATABASE_ACCESS_TOKEN for this demo, or replace "
                            + "EnvironmentDatabaseAccessTokenService with a real IAM token exchange");
        }
        return token;
    }
}
