package oracleai.services;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

import java.io.IOException;

public class AuthProvider {

    public static AuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                    System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
    //    InstancePrincipalsAuthenticationDetailsProvider.builder().build();
    }
}
