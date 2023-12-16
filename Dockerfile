FROM maven:3.6.3-openjdk-11 AS maven_build
COPY pom.xml /tmp/pom.xml
COPY env.properties /tmp/env.properties
COPY src /tmp/src
COPY lib /tmp/lib
COPY lib/oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar /tmp/lib/oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar
WORKDIR /tmp/

RUN mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/tmp/lib/oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar
RUN mvn -f /tmp/pom.xml clean package

FROM openjdk
EXPOSE 8080

CMD ls /tmp
COPY --from=maven_build /tmp/target/oracleai-0.0.1-SNAPSHOT.jar /app/oracleai-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/app/oracleai-0.0.1-SNAPSHOT.jar"]