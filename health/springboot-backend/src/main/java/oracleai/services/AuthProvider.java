package oracleai.services;

import java.io.IOException;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

public class AuthProvider {

    public static AuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
        // return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
    }

}
