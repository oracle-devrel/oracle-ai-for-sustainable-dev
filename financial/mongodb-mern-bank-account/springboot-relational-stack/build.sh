#!/bin/bash

export IMAGE_VERSION=$TAG

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

mvn clean package

echo about to build...
podman buildx build --platform linux/amd64 -t $IMAGE .


echo about to push ${IMAGE} $IMAGE...
podman push --format docker "$IMAGE"

