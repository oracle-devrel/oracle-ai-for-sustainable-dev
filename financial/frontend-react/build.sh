#!/bin/bash

export IMAGE_NAME=frontend
export IMAGE_VERSION=$TAG
export DOCKER_REGISTRY=us-ashburn-1.ocir.io/oradbclouducm/financial
#eg us-ashburn-1.ocir.io/oradbclouducm/financial/frontend:0.9

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

#oci artifacts container repository create --compartment-id ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq  --display-name financial/frontend  --is-public true


echo about to build...
#podman build -t=$IMAGE .
podman buildx build --platform linux/amd64 -t $IMAGE .
#podman buildx build --platform linux/amd64 -t $IMAGE --load .


echo about to push ${IMAGE} $IMAGE...
podman push --format docker "$IMAGE"
#podman push  --tls-verify=false "$IMAGE"

#podman run --rm -p 8080:8080 $IMAGE
# podman run --rm -p 8080:8080 us-ashburn-1.ocir.io/oradbclouducm/financial/frontend:0.9

