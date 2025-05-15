# Deploy MicroTx Transaction Coordinator on Amazon Web Services (AWS)

You can deploy the MicroTx transaction coordinator on Amazon ECS clusters in AWS.

## Prerequisites

You must have an active AWS subscription, with either access to the Free account or sufficient credits to deploy and run the MicroTx transaction coordinator.

Ensure that you have installed the following required software on your local machine.

* Docker (Rancher desktop) or Podman
* Latest version of the AWS Command Line Interface (CLI)

## Deploy MicroTx Transaction Coordinator Image on Amazon ECS clusters in AWS

MicroTx transaction coordinator image is available in Oracle Container Registry. See [https://docs.oracle.com/pls/topic/lookup?ctx=microtx-latest&id=TMMDG-GUID-E97C922B-75A8-4E0E-9E22-4F0CB310B310](Download the MicroTx image from Oracle Container Registry).

1. Create an ECS cluster or use an existing one.

2. Pull the MicroTx transaction coordinator image to your local machine, and then push the image to Amazon Elastic Container Registry (ECR).

    ```text
    <copy>
    docker pull container-registry.oracle.com/database/otmm:latest
    </copy>
    ```

    Another option is to configure access to the Oracle Container Registry from your ECS cluster, and then use the image directly.

3. In your ECS cluster, create an Amazon ECS task definition to deploy the MicroTx Coordinator image.
   For your reference, `microtx-taskdef.json`, a sample task definition file is available in the [https://github.com/oracle-samples/microtx-samples](microtx-samples) GitHub repository under the `lra/aws/ecs-task-definition` folder. This file contains sample values for the environment variables, port mapping, network mode and container resources. For information about environment variables that provide configuration details for the MicroTx transaction coordinator, see [https://docs.oracle.com/pls/topic/lookup?ctx=microtx-latest&id=TMMDG-GUID-2A285E76-8489-4CD0-8775-636C10822755](Environment Variables for Transaction Coordinator). You must provide the required information based on your environment.

4. Configure the required repository policies to access ECR and deploy the task definition for the MicroTx transaction coordinator image.

5. Deploy the MicroTx transaction coordinator using the task definition that you have created.
    When the MicroTx microservice is deployed, note down the host IP address and ensure that you can access the MicroTx transaction coordinator.

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
    ```

After verifying that the coordinator is running on the ECS cluster, you can build and configure a sample application.

## Build and Run a Sample Application

Run a Travel Agent application that uses the Saga transaction protocol to book a trip and understand how you can use Oracle Transaction Manager for Microservices (MicroTx) to coordinate the transactions. The Saga transaction protocol is based on Eclipse MicroProfile Long Running Actions (LRA).

### About the Travel Agent Application

The following figure shows a Travel Agent application, which contains several microservices, to demonstrate how you can develop microservices that participate in Saga transactions while using MicroTx to coordinate the transactions. When you run the application, it makes a provisional booking by reserving a hotel room and a flight ticket. The Flight Booking and Hotel Booking applications store the booking or reservation information in memory.

![Microservices in sample Saga application](./images/lra-sample-app.png)

Only when you provide approval to confirm the provisional booking, the booking of the hotel room and flight ticket is confirmed. If you cancel the provisional booking, the hotel room and flight ticket that was blocked is released and the booking is canceled. The Flight Booking application in this example allows only two confirmed bookings by default. To test the failure scenario, the Flight Booking applications rejects any additional booking requests after two confirmed bookings. This leads to the cancellation (compensation) of a provisionally booked hotel within the trip and the trip is not booked.

For more details, see [About the Sample Saga Application](https://docs.oracle.com/pls/topic/lookup?ctx=microtx-latest&id=TMMDG-GUID-C5332159-BD13-4210-A02E-475107919FD9) in the *Transaction Manager for Microservices Developer Guide*.

### Build the Travel Agent Application

1. Clone the code for the Travel Agent application which is available in the [https://github.com/oracle-samples/microtx-samples](microtx-samples) GitHub repository under the `lra/lrademo` folder. 
 The `trip-manager-springboot` folder contains the source code for the transaction initator service. While, the `hotel-springboot` and `flight-springboot` folders are the transaction participant services. The MicroTx library files are already integrated with the application code.

2. Build the Docker image for each participant service and push it to AWS ECR.

### Create Task Definition for Each Service

1. In your ECS cluster, create an Amazon ECS task definition to deploy each microservice.
   For your reference, a sample task definition file for these applications is available in the [https://github.com/oracle-samples/microtx-samples](microtx-samples) GitHub repository under the `lra/aws/ecs-task-definition` folder.
2. Ensure that the task definition for each application mentions the following environment variables.

The following table provides details for the `trip-manager-springboot`, a transaction initiator service.

| Environment Variable               | Sample Value                                                         | Description                                                                                     |
| ---------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| SPRING_MICROTX_LRA_COORDINATOR_URL | http://microtx-9000.microtx-ecs-cluster:9000/api/v1/lra-coordinator  | Resolvable coordinator service URL. Ensure that the host and port of the MicroTx coordinator service URL are correct. |
| SPRING_MICROTX_LRA_PARTICIPANT_URL | http://trip-manger-8081.microtx-ecs-cluster:8081                     | Resolvable trip-manager service URL. Ensure that the MicroTx transaction coordinator can access this service.           |
| FLIGHT_SERVICE_URL                 | http://flight-8083.microtx-ecs-cluster:8083/flightService/api/flight | Resolvable Flight service URL.                                                                   |
| HOTEL_SERVICE_URL                  | http://hotel-8082.microtx-ecs-cluster:8082/hotelService/api/hotel    | Resolvable Hotel service URL.

The following table provides details for the `hotel-springboot`, a transaction participant service.

| Environment Variable               | Sample Value                                                        | Description                                                                                     |
| ---------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| SPRING_MICROTX_LRA_COORDINATOR_URL | http://microtx-9000.microtx-ecs-cluster:9000/api/v1/lra-coordinator | Resolvable coordinator service URL. Ensure that the host and port of the MicroTx coordinator service URL are correct.|
| SPRING_MICROTX_LRA_PARTICIPANT_URL | http://hotel-8082.microtx-ecs-cluster:8082                          | Resolvable hotel service URL. Ensure that the MicroTx transaction coordinator can access this service.             |

The following table provides details for the `flight-springboot`, a transaction participant service.

| Environment Variable               | Sample Value                                                        | Description                                                                                     |
| ---------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| SPRING_MICROTX_LRA_COORDINATOR_URL | http://microtx-9000.microtx-ecs-cluster:9000/api/v1/lra-coordinator | Resolvable coordinator service URL. nsure that the host and port of the MicroTx coordinator service URL are correct. |
| SPRING_MICROTX_LRA_PARTICIPANT_URL | http://flight-8083.microtx-ecs-cluster:8083                         | Resolvable hotel service URL. Ensure that the MicroTx transaction coordinator can access this service.                |

### Deploy and Run the Travel Agent Application

1. Deploy the applications using the task definitions that you have created. Note down the URLs to access the service.
2. Use the APIs exposed by `trip-manager-springboot`, a transaction initiator service, to run the travel agent application. See [https://github.com/oracle-samples/microtx-samples/tree/main/lra/lrademo](https://github.com/oracle-samples/microtx-samples/tree/main/lra/lrademo).