# Deploy Sample Apps  in Microsoft Azure Container Apps

You can deploy the LRA Sample apps in Microsoft Azure using Azure Container Apps.

## Prerequisites

You must have an active Azure subscription, with either access to the Free account or sufficient credits to deploy and run container services.

Ensure that you have installed the following required software on your local machine.

* Docker (Rancher desktop) or Podman.
* Latest version of the Azure CLI. See [https://learn.microsoft.com/en-us/cli/azure/get-started-with-azure-cli](https://learn.microsoft.com/en-us/cli/azure/get-started-with-azure-cli).
* Azure Container Apps extension for the CLI. See [https://learn.microsoft.com/en-us/azure/container-apps/tutorial-deploy-first-app-cli?tabs=bash](https://learn.microsoft.com/en-us/azure/container-apps/tutorial-deploy-first-app-cli?tabs=bash).
* Follow the Readme `microtx-samples/lra/azure/deploy_microtx_on_azure.md` for deploying MicroTx Free on Azure.
## Create Azure Container Environment

1. Azure login and adding the containerapp extension
```bash
   az login 
   
   az extension add --name containerapp --upgrade   
```


2. Create an Azure Container Apps environment, an environment with secure boundaries. A following sample command creates an Azure Container Apps environment with the name `sampleapps1`.

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


3. Create a docker registry, provide resourceGroupName and name for container registry. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli).
```text
    az acr create --resource-group <resourceGroupName> --name <azureContainerRegistryName> --sku basic     
```


4. Sign in to the Azure container registry with the az acr login command. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#log-in-to-registry](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#log-in-to-registry).
```text
    az acr login --name <azureContainerRegistryName>     
```

## Deploy Samples Apps in Microsoft Azure Container Apps


1. Create docker images for the following spring sample apps present in `microtx-samples/lra/lrademo`.

Hotel Service :

```bash 
   cd microtx-samples/lra/lrademo/hotel-springboot/
   docker image build -t hotel:local .
```

Flight Service :

```bash 
   cd microtx-samples/lra/lrademo/flight-springboot/
   docker image build -t flight:local .
```

Trip-Manger Service :

```bash 
   cd microtx-samples/lra/lrademo/trip-manager-springboot/
   docker image build -t trip-manager:local .
```
    
2. In the Azure portal, enable the admin user for the registry that you have created. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication?tabs=azure-cli#admin-account](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication?tabs=azure-cli#admin-account).

3. Push the Sample apps  image to the Azure container registry. Provide your registry name <microtxRegistryName>. See [https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#push-image-to-registry](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli#push-image-to-registry).
   ```bash
      docker tag hotel:local <microtxRegistryName>/hotel:latest
      docker push <microtxRegistryName>/hotel:latest
   
      docker tag flight:local <microtxRegistryName>/flight:latest
      docker push <microtxRegistryName>/flight:latest
   
      docker tag trip-manager:local <microtxRegistryName>/trip-manager:latest
      docker push <microtxRegistryName>/trip-manager:latest
   ```
4. Verify the image was successfully pushed to Azure Container Registry. Run the `az acr repository list` command to list all the repositories in a registry.

5. Deploy the Sample apps to the Azure Container Apps environment that you have created. See https://learn.microsoft.com/en-us/azure/container-apps/quickstart-portal
   
   Below are the properties for the following service.
   In each service note down the Application URL under overview section
   #### Hotel Service 
   - Target port :8082
   - Ingress rule : Limited to container App environment
   - In the Environment Variable section add these variable with appropriate values.
   
   | SPRING_MICROTX_LRA_COORDINATOR_URL  | <microtx-application-url>/api/v1/lra-coordinator |
   |---|---------------------------|
   | SPRING_MICROTX_LRA_PARTICIPANT_URL  | <hotel-application-url>   |
   | SERVICE_NAME  | Hotel                     |

#### Flight Service
- Target port :8083
- Ingress rule : Limited to container App environment
- In the Environment Variable section add these variable with appropriate values.

| SPRING_MICROTX_LRA_COORDINATOR_URL  | <microtx-application-url>/api/v1/lra-coordinator |
   |---|---------------------------|
| SPRING_MICROTX_LRA_PARTICIPANT_URL  | <flight-application-url>  |
| SERVICE_NAME  | flight                    |

#### Trip Manager Service
- Target port :8081
- Ingress rule : Accepting traffic to anywhere 
- In the Environment Variable section add these variable with appropriate values.

| SPRING_MICROTX_LRA_COORDINATOR_URL  | <microtx-application-url>/api/v1/lra-coordinator  |
   |---|---------------------------------------------------|
| SPRING_MICROTX_LRA_PARTICIPANT_URL  | <trip-manager-application-url>                    |
| HOTEL_SERVICE_URL  | <hotel-application-url>/hotelService/api/hotel    |
| FLIGHT_SERVICE_URL| <flight-application-url>/flightService/api/flight |
|SERVICE_NAME | tripManager                                       |

## Saga example with MicroTx

1. For running saga use the trip-client java sample app `microtx-samples/lra/lrademo/trip-client`. Follow the instruction in README.md file.
   Export the Trip-Manager application public url 
   ```bash
   export TRIP_SERVICE_URL=http://<trip-manager-application-url>/trip-service/api/sync/trip
   ```