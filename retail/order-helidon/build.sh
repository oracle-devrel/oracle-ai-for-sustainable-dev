#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


SCRIPT_DIR=$(dirname $0)

IMAGE_NAME=order-helidon
IMAGE_VERSION=0.1

echo DOCKER_REGISTRY is $DOCKER_REGISTRY

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "DOCKER_REGISTRY not set. Will get it with state_get"
  export DOCKER_REGISTRY=$(state_get DOCKER_REGISTRY)
fi

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
export IMAGE_VERSION=$IMAGE_VERSION

# First, compile and package without Docker build to avoid credential issues
echo "Building JAR..."
mvn clean compile package -Ddockerfile.skip=true

# If JAR build successful, then build Docker image with podman
if [ $? -eq 0 ]; then
    echo "JAR build successful, building Docker image with podman..."
    # Use podman to build the image to avoid Docker credential issues
    podman build -t $IMAGE --build-arg JAR_FILE=order-helidon.jar .
    
    if [ $? -eq 0 ]; then
        echo "Docker image build successful, pushing with podman..."
        podman push $IMAGE
        if [ $? -eq 0 ]; then
            podman rmi ${IMAGE}
        fi
    else
        echo "Docker image build failed"
        exit 1
    fi
else
    echo "JAR build failed"
    exit 1
fi