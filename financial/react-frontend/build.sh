#!/bin/bash

# Source environment variables from parent financial/.env file
if [ -f "../.env" ]; then
    source ../.env
    echo "Environment variables loaded from ../.env"
else
    echo "Warning: ../.env file not found, using default values"
fi

export IMAGE_VERSION=$TAG
# export DOCKER_REGISTRY=eu-frankfurt-1.ocir.io/oradbclouducm/financial
#eg us-ashburn-1.ocir.io/oradbclouducm/financial/frontend:0.

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}

#oci artifacts container repository create --compartment-id ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq  --display-name financial/frontend  --is-public true


echo about to build...
#podman build -t=$IMAGE .
#podman buildx build --platform linux/amd64 --build-arg REACT_APP_BACKEND_URL=https://oracledatabase-financial.org -t $IMAGE .
#podman buildx build --platform linux/amd64 -t $IMAGE --load .
podman buildx build --memory=8g  --platform linux/amd64 \
  --build-arg REACT_APP_BACKEND_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_MICROTX_TRANSFER_SERVICE_URL=${REACT_APP_MICROTX_TRANSFER_SERVICE_URL} \
  --build-arg REACT_APP_MICROTX_ACCOUNT_SERVICE_URL=${REACT_APP_MICROTX_ACCOUNT_SERVICE_URL} \
  --build-arg REACT_APP_MERN_BACKEND_SERVICE_URL=https://oracleai-financial.org/mern-backend \
  --build-arg REACT_APP_MERN_MONGODB_SERVICE_URL=${REACT_APP_MERN_MONGODB_SERVICE_URL} \
  --build-arg REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL=${REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL} \
  --build-arg REACT_APP_MERN_SQL_ORACLE_SERVICE_URL=${REACT_APP_MERN_SQL_ORACLE_SERVICE_URL} \
  --build-arg REACT_APP_JAVA_ACCOUNTDETAIL_SERVICE_URL=${REACT_APP_JAVA_ACCOUNTDETAIL_SERVICE_URL} \
  --build-arg REACT_APP_GRAPH_LAUNDERING_SERVICE_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_TRUECACHE_STOCK_SERVICE_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_SHARDING_SPATIAL_CC_SERVICE_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_STOCK_SERVICE_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_KAFKA_TXEVENTQ_SERVICE_URL=https://oracledatabase-financial.org \
  --build-arg REACT_APP_AIAGENT_VECTOR_ADVISOR_SERVICE_URL=${REACT_APP_AIAGENT_VECTOR_ADVISOR_SERVICE_URL} \
  --build-arg REACT_APP_SPEECH_SELECTAI_QUERY_SERVICE_URL=${REACT_APP_SPEECH_SELECTAI_QUERY_SERVICE_URL} \
  --build-arg REACT_APP_AI_AGENTS_BACKEND_URL=${REACT_APP_AI_AGENTS_BACKEND_URL} \
  --build-arg REACT_APP_ORDS_BASE_URL=${REACT_APP_ORDS_BASE_URL} \
  -t $IMAGE .


echo about to push ${IMAGE} $IMAGE...
podman push --format docker "$IMAGE"
#podman push  --tls-verify=false "$IMAGE"

#podman run --rm -p 8080:8080 $IMAGE
# podman run --rm -p 8080:8080 us-ashburn-1.ocir.io/oradbclouducm/financial/frontend:0.9

