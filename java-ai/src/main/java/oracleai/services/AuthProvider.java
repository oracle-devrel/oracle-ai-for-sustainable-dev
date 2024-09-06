package oracleai.services;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import java.io.IOException;

public class AuthProvider {

    public static BasicAuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
        return ResourcePrincipalAuthenticationDetailsProvider.builder().build();
    }

    private static boolean isRunningInOKE() {
        return true; //System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION") != null;
    }

    public static AuthenticationDetailsProvider getConfigFileAuthenticationDetailsProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                    System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
    //    InstancePrincipalsAuthenticationDetailsProvider.builder().build();
    }
}
