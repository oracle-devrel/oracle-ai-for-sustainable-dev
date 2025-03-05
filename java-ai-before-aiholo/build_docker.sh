#!/bin/bash

export IMAGE_NAME=oracleai
export IMAGE_VERSION=0.1


if [ -z "$DOCKER_REGISTRY" ]; then
    echo "DOCKER_REGISTRY not set. Will set it to 'test"
  export DOCKER_REGISTRY=test
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}

docker build -t=$IMAGE .

#docker push "$IMAGE"
#if [  $? -eq 0 ]; then
#    docker rmi "$IMAGE"
#fi

