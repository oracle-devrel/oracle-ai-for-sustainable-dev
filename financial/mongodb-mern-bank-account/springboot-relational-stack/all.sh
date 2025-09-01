#!/bin/bash

export TAG=0.1.$(date +%s)
echo TAG = $TAG

export IMAGE_NAME=bank-account
export IMAGE_VERSION=$TAG
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}



./build.sh
./deploy.sh
echo logpod account
logpod account
