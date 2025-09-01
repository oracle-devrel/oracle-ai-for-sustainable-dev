#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

# Source environment variables from .env file
if [ -f "../.env" ]; then
    echo "Loading environment variables from .env file..."
    source "../.env"
else
    echo "Warning: .env file not found. Using environment variables from shell."
fi

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
sed -i '' "s|COMPARTMENT_ID_PLACEHOLDER|$COMPARTMENT_ID|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OBJECTSTORAGE_NAMESPACE_PLACEHOLDER|$OBJECTSTORAGE_NAMESPACE|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OBJECTSTORAGE_BUCKETNAME_PLACEHOLDER|$OBJECTSTORAGE_BUCKETNAME|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OBJECTSTORAGE_REGION_PLACEHOLDER|$OBJECTSTORAGE_REGION|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|ORDS_ENDPOINT_URL_PLACEHOLDER|$ORDS_ENDPOINT_URL|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OCI_VISION_SERVICE_ENDPOINT_PLACEHOLDER|$OCI_VISION_SERVICE_ENDPOINT|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OCI_SPEECH_SERVICE_ENDPOINT_PLACEHOLDER|$OCI_SPEECH_SERVICE_ENDPOINT|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OCI_GENAI_SERVICE_ENDPOINT_PLACEHOLDER|$OCI_GENAI_SERVICE_ENDPOINT|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|OPENAI_API_KEY_PLACEHOLDER|$OPENAI_API_KEY|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|VISIONAI_XRAY_BREASTCANCER_MODEL_OCID_PLACEHOLDER|$VISIONAI_XRAY_BREASTCANCER_MODEL_OCID|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|VISIONAI_XRAY_LUNGCANCER_MODEL_OCID_PLACEHOLDER|$VISIONAI_XRAY_LUNGCANCER_MODEL_OCID|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|VISIONAI_XRAY_PNEUMONIA_MODEL_OCID_PLACEHOLDER|$VISIONAI_XRAY_PNEUMONIA_MODEL_OCID|g" healthai-backend-springboot-deployment.yaml
sed -i '' "s|VISIONAI_XRAY_PNEUMONIA_MODEL_DEEP_LEARNING_OCID_PLACEHOLDER|$VISIONAI_XRAY_PNEUMONIA_MODEL_DEEP_LEARNING_OCID|g" healthai-backend-springboot-deployment.yaml
kubectl apply -f healthai-backend-springboot-deployment.yaml -n health


kubectl apply -f healthai-backend-service.yaml -n health
