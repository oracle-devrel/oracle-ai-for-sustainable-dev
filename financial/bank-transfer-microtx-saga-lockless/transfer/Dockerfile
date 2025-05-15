FROM ghcr.io/graalvm/jdk:ol7-java17-22.2.0

ENTRYPOINT ["java", "-jar", "/usr/share/springservice/springservice.jar"]

ADD target/transfer.jar   /usr/share/springservice/springservice.jar
