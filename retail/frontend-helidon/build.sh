#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

# Fail on error
set -e

SCRIPT_DIR=$(dirname $0)

IMAGE_NAME=frontend-helidon
IMAGE_VERSION=0.1

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}

echo "Building Maven project..."
mvn clean package

echo "Building container image: ${IMAGE}"
podman build --platform linux/amd64 \
    --build-arg JAR_FILE=frontend-helidon.jar \
    -t ${IMAGE} .

echo "Pushing container image: ${IMAGE}"
podman push ${IMAGE}

if [ $? -eq 0 ]; then
    echo "Successfully pushed ${IMAGE}"
    # Optionally remove local image to save space
    # podman rmi ${IMAGE}
else
    echo "Failed to push ${IMAGE}"
    exit 1
fi
