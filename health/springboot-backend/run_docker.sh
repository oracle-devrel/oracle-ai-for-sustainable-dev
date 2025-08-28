#!/bin/bash

export IMAGE_NAME=oracleai
export IMAGE_VERSION=0.1


if [ -z "$DOCKER_REGISTRY" ]; then
    echo "DOCKER_REGISTRY not set. Will set it to 'test"
  export DOCKER_REGISTRY=test
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}

docker run --env-file env.properties -p 127.0.0.1:8080:8080/tcp $IMAGE