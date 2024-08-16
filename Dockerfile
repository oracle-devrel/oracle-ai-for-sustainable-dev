FROM maven:3.8.4-openjdk-17 AS maven_build
COPY pom.xml /tmp/pom.xml
COPY env.properties /tmp/env.properties
COPY src /tmp/src
WORKDIR /tmp/

RUN mvn -f /tmp/pom.xml clean package

FROM openjdk:17
EXPOSE 8080

CMD ls /tmp
COPY --from=maven_build /tmp/target/oracleai-0.0.1-SNAPSHOT.jar /app/oracleai-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/app/oracleai-0.0.1-SNAPSHOT.jar"]