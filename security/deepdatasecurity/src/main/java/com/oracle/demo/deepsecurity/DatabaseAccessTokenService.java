package com.oracle.demo.deepsecurity;

public interface DatabaseAccessTokenService {

    String getDatabaseAccessToken(String endUserToken);
}
