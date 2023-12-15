FROM maven:3.6.3-openjdk-11 AS maven_build
#COPY pom.xml /tmp/
#COPY src /tmp/src/
COPY * /tmp/
WORKDIR /tmp/
FROM openjdk
EXPOSE 8080
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} oracleai-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/oracleai-0.0.1-SNAPSHOT.jar"]
#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]



#FROM openjdk:11-jre-slim
#
#ENTRYPOINT ["java", "-jar", "/usr/share/oracleai/oracleai.jar"]
#
#ADD target/oracleai-0.0.1-SNAPSHOT.jar /usr/share/oracleai/oracleai.jar