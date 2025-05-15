# About
MicroTx LockFree LRA demo , demonstration of a Java microservice for Flight booking built on the SpringBoot framework.
Default MicroTx LRA coordinator URL is "http://localhost:9000/api/v1/lra-coordinator"

## Introduction
Prerequisite

1. This application connects to an Oracle Database 23c with lock free reservation support. If you choose to use Autonomous database, then download the client credential wallet and copy the contents into the Database_Wallet folder
   in the root director
2. A running instance of MicroTx transaction coordinator

### Configurations
application.properties in the resources folder can be used to provide the database configurations.
flights.sql can be used to initialise data in the database.

## Quick Start
To run build:

```
mvn clean package
```

To run the application:
```
java -jar target/flight-sb.jar
```
To run the application with different MicroTx coordinator assign the URL to mp.lra.coordinator.url system variable:
```
java -jar -Dmp.lra.coordinator.url=<url> flight.jar
```
To create a docker image:
```
docker image build -t=<image_name> .
```
To run the docker image:
```
docker container run <image_name>
```
To run the docker image with different MicroTx coordinator assign the URL to MP_LRA_COORDINATOR_URL environment variable:
```
docker container run -e MP_LRA_COORDINATOR_URL=<URL> <image_name>
```
To change the Max Booking Count:
```
#set the count value as required

curl --location --request PUT 'http://localhost:8083/flightService/api/maxbookings?count=7'
```