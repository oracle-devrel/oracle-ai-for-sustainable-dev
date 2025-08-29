#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

export TAG=0.1.$(date +%s)
echo TAG = $TAG

export IMAGE_NAME=healthai-backend-springboot
export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=$DOCKER_REGISTRY
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

# ./build_and_push ...

export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=$DOCKER_REGISTRY

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



# ./deploy.sh...


#kubectl delete deployment healthai-backend-springboot-deployment -n financial

cp healthai-backend-springboot-deployment_template.yaml healthai-backend-springboot-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" healthai-backend-springboot-deployment.yaml
kubectl apply -f healthai-backend-springboot-deployment.yaml -n health


kubectl apply -f healthai-backend-service.yaml -n health
