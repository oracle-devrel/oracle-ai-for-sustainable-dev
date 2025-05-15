**Table of Contents**

<!-- TOC start (generated with https://github.com/derlin/bitdowntoc) -->

- [Before You Begin](#before-you-begin)
- [Build the Lockfree Springboot Trip Booking Service in Local Environment](#run-the-lockfree-Springboot-trip-booking-service-in-local-environment)
- [Build Docker Images for Lockfree Springboot Trip Booking Application](#build-docker-images-for-lockfree-springboot-trip-booking-application)

<!-- TOC end -->

# Before You Begin

This readme explain only about building the lockfree springboot sample applications. Before building the application, execute .sql file in each application present in application root directory and update the database connection details in application.properties file present in resources directory. 
For rest of the steps [follow this README](../../lrademo/README.md)

## Build and Run Individual Microservices in Local Environment


1. Run the following command to build and run the Flight Booking application.

   ```
   cd /samples/lra/lockfree/springboot/flight-springboot
   mvn clean package 
   java -jar target target/flight-sb.jar
   
   ```

2. Run the following command to build and run the Hotel Booking application.

   ```
   cd /samples/lra/lockfree/springboot/hotel-springboot
   mvn clean package 
   java -jar target target/hotel-sb.jar
   
   ```
3. Run the following command to build and run the Trip Manager application.

   ```
   cd /samples/lra/lockfree/springboot/trip-manager-springboot
   mvn clean package 
   java -jar target/trip-manager.jar
   ```

## Build Docker Images for Trip Booking Application

Perform the following steps to build Docker images for each microservice in the Trip Booking application:

1. Run the following commands to build the Docker image for the Hotel Booking service.

   ```
   cd samples/lra/lockfree/springboot/hotel-springboot
   docker image build -t hotel-lockfree-springboot:1.0 .
   ```

   When the image is successfully built, the following message is displayed.
   Successfully tagged hotel-lockfree-springboot:1.0

2. Run the following commands to build the Docker image for the Flight Booking service.

   ```
   cd samples/lra/lockfree/springboot/flight-springboot
   docker image build -t flight-lockfree-springboot:1.0 .
   ```

   When the image is successfully built, the following message is displayed.
   Successfully tagged flight-lockfree-springboot:1.0

3. Run the following commands to build the Docker image for the Trip Manager application.

   ```
   cd samples/lra/lockfree/springboot/trip-manager-springboot
   docker image build -t trip-manager-lockfree-springboot:1.0 .
   ```

   When the image is successfully built, the following message is displayed.

   Successfully tagged trip-manager-lockfree-springboot:1.0
