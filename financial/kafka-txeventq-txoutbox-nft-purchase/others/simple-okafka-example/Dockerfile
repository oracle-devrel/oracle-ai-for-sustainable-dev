FROM ghcr.io/oracle/openjdk-image-obaas:17
LABEL authors="Copyright (c) 2025, Oracle and/or its affiliates."
WORKDIR /app
VOLUME /tmp

ARG USERNAME=earnest
ARG USER_UID=12000
ARG USER_GID=12000

EXPOSE 8086

# Don't run as root
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME
COPY --chown=$USERNAME:$USERNAME ./target/earnest-payment-service-1.0.0-SNAPSHOT.jar /app/earnest-payment-service-1.0.0-SNAPSHOT.jar
USER $USERNAME

# Run application
ENTRYPOINT ["java", "-jar", "/app/earnest-payment-service-1.0.0-SNAPSHOT.jar"]