#!/bin/bash

export IMAGE_NAME=healthai-backend-springboot
export IMAGE_VERSION=0.1

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: podman_REGISTRY env variable needs to be set!"
    exit 1
fi


export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}

mvn clean package spring-boot:repackage
podman build -t=$IMAGE .

podman push "$IMAGE"
if [  $? -eq 0 ]; then
    podman rmi "$IMAGE"
fi
