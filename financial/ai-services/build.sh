#!/bin/bash

export IMAGE_NAME=frontend
export IMAGE_VERSION=0.4
export DOCKER_REGISTRY=us-ashburn-1.ocir.io/oradbclouducm/financial
#export DOCKER_REGISTRY0=us-ashburn-1.ocir.io/oradbclouducm/gd35252210

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

#oci artifacts container repository create --compartment-id ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq  --display-name financial/frontend  --is-public true


#mvn clean package spring-boot:repackage
mvn clean package

echo about to build...
#podman build -t=$IMAGE .
podman buildx build --platform linux/amd64 -t $IMAGE .
#podman buildx build --platform linux/amd64 -t $IMAGE --load .


podman push "$IMAGE"

#podman run --rm -p 8080:8080 $IMAGE
# podman run --rm -p 8080:8080 us-ashburn-1.ocir.io/oradbclouducm/financial/frontend:0.1

