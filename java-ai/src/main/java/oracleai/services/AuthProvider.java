package oracleai.services;

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import oracleai.AIApplication;

import java.io.IOException;

public class AuthProvider {

    public static BasicAuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
        if (isRunningInOKE()) return InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        else return new ConfigFileAuthenticationDetailsProvider(
//                "~/.oci/config", "DEFAULT");
                AIApplication.OCICONFIG_FILE, AIApplication.OCICONFIG_PROFILE);
    }

    private static boolean isRunningInOKE() {
        return false; //System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION") != null;
    }

}
