# Stage 1: Build the app
FROM --platform=linux/amd64 maven:3.9.4-eclipse-temurin-21 AS maven_build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM --platform=linux/amd64 eclipse-temurin:21-jre
WORKDIR /app
COPY --from=maven_build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
