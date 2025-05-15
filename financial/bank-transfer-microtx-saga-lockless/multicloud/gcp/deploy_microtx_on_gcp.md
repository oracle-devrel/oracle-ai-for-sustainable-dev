# Deploy MicroTx in Google Cloud

You can deploy the MicroTx transaction coordinator in Google Cloud using Cloud Run.

## Prerequisites

You must create an account on Google Cloud.

Ensure that you have installed the following required software on your local machine.


## Push the MicroTx Transaction Coordinator Image to Artifact Registry

MicroTx transaction coordinator image is available in Oracle Container Registry. See [https://docs.oracle.com/pls/topic/lookup?ctx=microtx-latest&id=TMMDG-GUID-E97C922B-75A8-4E0E-9E22-4F0CB310B310](Download the MicroTx image from Oracle Container Registry).

1. Create a project link. See [https://cloud.google.com/resource-manager/docs/creating-managing-projects]
(https://cloud.google.com/resource-manager/docs/creating-managing-projects).

2. Pull the MicroTx transaction coordinator image to your local machine.

    ```text
    <copy>
    docker pull container-registry.oracle.com/database/otmm:latest
    </copy>
    ```

3. Push the image to the Artifact Registry in Google Cloud. See [https://cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling](https://cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling).


## Deploy MicroTx Transaction Coordinator in Google Cloud

You can deploy MicroTx transaction coordinator after uploading the docker image of MicroTx.

1. Click **Deploy Container Button** to create Cloud Run Service.

2. Select the docker image that you have uploaded to Artifact Registory.

3. Note down the **Endpoint URL**. We will use this URL to set the internal and external address.

4. Edit the container port to 9000 and add the environment variables.

5. Deploy the service.

6. Check the health of the MicroTx transaction coordinator. Provide the host name and port number based on your environment.

    ```text
    <copy>
    curl https://<externalHostname>:<externalPort>/health
    </copy>
    ``

    If the MicroTx coordinator is healthy, you will see the following response.

    ```text
    {
    "coordinators": {
        "xaCoordinator": "Healthy",
        "lraCoordinator": "Healthy",
        "tccCoordinator": "Healthy"
        },
    "version": "",
    "started": "2024-11-11T11:22:44.343082+05:30"
    }

