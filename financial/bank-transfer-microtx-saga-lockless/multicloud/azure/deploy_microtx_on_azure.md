# Deploy MicroTx in Microsoft Azure

You can deploy the MicroTx transaction coordinator in Microsoft Azure using Azure Container Apps.

## Prerequisites

You must have an active Azure subscription, with either access to the Free account or sufficient credits to deploy and run the MicroTx transaction coordinator.

Ensure that you have installed the following required software on your local machine.

* Docker (Rancher desktop) or Podman.
* Latest version of the Azure CLI. See [https://learn.microsoft.com/en-us/cli/azure/get-started-with-azure-cli](https://learn.microsoft.com/en-us/cli/azure/get-started-with-azure-cli).
* Azure Container Apps extension for the CLI. See [https://learn.microsoft.com/en-us/azure/container-apps/tutorial-deploy-first-app-cli?tabs=bash](https://learn.microsoft.com/en-us/azure/container-apps/tutorial-deploy-first-app-cli?tabs=bash).

## Deploy MicroTx Transaction Coordinator Image in Microsoft Azure

1. Download the MicroTx Image from Oracle Container Registry. See [https://docs.oracle.com/pls/topic/lookup?ctx=microtx-latest&id=TMMDG-GUID-E97C922B-75A8-4E0E-9E22-4F0CB310B310](Download the MicroTx image from Oracle Container Registry).

2. Create a container registry in Azure. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli).

    Note down the names of the resource group and container registry that you create as you will need to provide this information later.

3. Log in to registry that you have created. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#log-in-to-registry](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#log-in-to-registry).

4. In the Azure portal, enable the admin user for the registry that you have created. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication?tabs=azure-cli#admin-account](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication?tabs=azure-cli#admin-account).

5. Push the MicroTx coordinator image to the Azure container registry. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#push-image-to-registry](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#push-image-to-registry).

6. Verify the image was successfully pushed to Azure Container Registry. Run the `az acr repository list` command to list all the repositories in a registry.

7. Create an Azure Container Apps environment, an environment with secure boundaries. A following sample command creates an Azure Container Apps environment with the name `sampleapps1`.

    ```text
    <copy>
    az containerapp env create \
        --name <sampleapps1> \
        --resource-group <sampleapps> \
        --location "<East US>"
    </copy>
    ```

    Where,
    `<sampleapps1>` is the name of the Azure Container Apps environment.
    `<sampleapps>` is the name of the resource group that you have created earlier.

    Replace these values based on your environment. For more information about running the `az containerapp env create` command, see [https://learn.microsoft.com/en-us/cli/azure/containerapp/env?view=azure-cli-latest#az-containerapp-env-create](https://learn.microsoft.com/en-us/cli/azure/containerapp/env?view=azure-cli-latest#az-containerapp-env-create).

8. Deploy the MicroTx transaction coordinator to the Azure Container Apps environment that you have created. See [https://learn.microsoft.com/en-us/azure/container-apps/tutorial-code-to-cloud?tabs=bash%2Ccsharp&pivots=acr-remote#deploy-your-image-to-a-container-app](https://learn.microsoft.com/en-us/azure/container-apps/tutorial-code-to-cloud?tabs=bash%2Ccsharp&pivots=acr-remote#deploy-your-image-to-a-container-app).
   Also provide the below environment variables :

   | Environment Variable    | Value                        |
   |-------------------------|-----------------------------|
   | LISTEN_ADDR             | 0.0.0.0:9000                |
   | STORAGE_TYPE            | memory                      |
   | EXTERNAL_ADDR           | <microtx-application-url>   |
   | INTERNAL_ADDR           | <microtx-application-url>   |
   | LRA_COORDINATOR_ENABLED | true                        |
   | XA_COORDINATOR_ENABLED  | true                        |
   | TCC_COORDINATOR_ENABLED | true                        |
   | LOGGING_LEVEL           | info                        |
   | SERVE_TLS_ENABLED       | false                       |


9. Note down the application's fully qualified domain name (FQDN) from the output, and save the name in an environment variable, such as `APP_FQDN`.

## Complete the Post Deployment Tasks

1. Run the following cURL command using the FQDN.

    ```
    <copy>
    curl "https://$APP_FQDN"
    </copy>
    ```

    The details of the MicroTx transaction coordinator are displayed on the Azure Portal. Note down the application URL. You will need to provide this in the next step

2. Deploy the MicroTx transaction coordinator to the Azure Container Apps environment that you have created. See [https://learn.microsoft.com/en-us/azure/container-apps/tutorial-code-to-cloud?tabs=bash%2Ccsharp&pivots=acr-remote#deploy-your-image-to-a-container-app](https://learn.microsoft.com/en-us/azure/container-apps/tutorial-code-to-cloud?tabs=bash%2Ccsharp&pivots=acr-remote#deploy-your-image-to-a-container-app).

3. Access the application URL in the browser to verify that the application has been deployed successfully.