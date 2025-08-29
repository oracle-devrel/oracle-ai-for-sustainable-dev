#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

export TAG=0.1.$(date +%s)
echo TAG = $TAG

export IMAGE_NAME=healthai-frontend-flutter
export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=$DOCKER_REGISTRY
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

# ./build_and_push ...

export IMAGE_NAME=healthai-frontend-flutter
export IMAGE_VERSION=0.1
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}


podman build -t=$IMAGE .

podman push "$IMAGE"
if [  $? -eq 0 ]; then
    podman rmi "$IMAGE"
fi


# ./deploy.sh...


#kubectl delete deployment healthai-frontend-flutter-deployment -n financial

cp healthai-frontend-flutter-deployment_template.yaml healthai-frontend-flutter-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" healthai-frontend-flutter-deployment.yaml
kubectl apply -f healthai-frontend-flutter-deployment.yaml -n health


kubectl apply -f healthai-frontend-service.yaml -n health

