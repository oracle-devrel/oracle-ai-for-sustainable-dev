FROM openjdk:17-alpine
#docker.io/library/openjdk:17-oracle

ADD target/oracleai-0.0.1-SNAPSHOT.jar /usr/share/oracleai/oracleai-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/usr/share/oracleai/oracleai-0.0.1-SNAPSHOT.jar"]
