#!/bin/bash

export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=eu-frankfurt-1.ocir.io/oradbclouducm/financial

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

mvn clean package

echo about to build...
#podman buildx build --platform linux/amd64 -t $IMAGE .
#podman build -t my-jupyter-microservice .
podman buildx build --platform linux/amd64  -t my-jupyter-microservice .
#podman run -p 8888:8888 my-jupyter-microservice

echo about to push ${IMAGE} $IMAGE...
podman push --format docker "$IMAGE"

