# Stage 1: Build the app
FROM --platform=linux/amd64 maven:3.6.3-openjdk-11 AS maven_build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM --platform=linux/amd64 openjdk:11-jre-slim
WORKDIR /app
COPY --from=maven_build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
