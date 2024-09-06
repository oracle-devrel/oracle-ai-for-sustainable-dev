package oracleai.services;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import java.io.IOException;

public class AuthProvider {

    public static BasicAuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
        if (isRunningInOKE()) return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        else return new ConfigFileAuthenticationDetailsProvider(
                System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
    }

    private static boolean isRunningInOKE() {
        return true; //System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION") != null;
    }

}
