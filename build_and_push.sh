#!/bin/bash

IMAGE_NAME=oracleai
IMAGE_VERSION=0.1

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
export IMAGE_VERSION=$IMAGE_VERSION

mvn clean package

docker build -t=$IMAGE .

docker push "$IMAGE"
if [  $? -eq 0 ]; then
    docker rmi "$IMAGE"
fi

