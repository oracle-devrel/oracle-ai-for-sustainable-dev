#!/bin/bash

# Source environment variables from parent financial/.env file
if [ -f "../.env" ]; then
    source ../.env
    echo "Environment variables loaded from ../.env"
else
    echo "Warning: ../.env file not found, using default values"
fi

export IMAGE_VERSION=$TAG
# export DOCKER_REGISTRY=eu-frankfurt-1.ocir.io/<tenancy-namespace>/financial
#eg us-ashburn-1.ocir.io/<tenancy-namespace>/financial/frontend:0.

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
echo ${IMAGE}
PUBLIC_HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
APP_BASE_PATH="${APP_BASE_PATH:-/financial}"
API_BASE_PATH="${API_BASE_PATH:-${APP_BASE_PATH}/api}"
ACCOUNTS_API_BASE_PATH="${ACCOUNTS_API_BASE_PATH:-${APP_BASE_PATH}/accounts-api}"
TRANSFER_API_BASE_PATH="${TRANSFER_API_BASE_PATH:-${APP_BASE_PATH}/transfer-api}"
PUBLIC_ORIGIN="${PUBLIC_ORIGIN:-https://${PUBLIC_HOSTNAME}}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-${PUBLIC_ORIGIN}${APP_BASE_PATH}}"

#oci artifacts container repository create --compartment-id ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq  --display-name financial/frontend  --is-public true


echo about to build...
#podman build -t=$IMAGE .
#podman buildx build --platform linux/amd64 --build-arg REACT_APP_BACKEND_URL=${API_BASE_PATH} -t $IMAGE .
#podman buildx build --platform linux/amd64 -t $IMAGE --load .
podman buildx build --memory=8g  --platform linux/amd64 \
  --build-arg PUBLIC_URL=${PUBLIC_URL:-${APP_BASE_PATH}} \
  --build-arg REACT_APP_BASE_PATH=${REACT_APP_BASE_PATH:-${APP_BASE_PATH}} \
  --build-arg REACT_APP_API_BASE_PATH=${REACT_APP_API_BASE_PATH:-${API_BASE_PATH}} \
  --build-arg REACT_APP_PUBLIC_BASE_URL=${REACT_APP_PUBLIC_BASE_URL:-${PUBLIC_BASE_URL}} \
  --build-arg REACT_APP_BACKEND_URL=${REACT_APP_BACKEND_URL:-${API_BASE_PATH}} \
  --build-arg REACT_APP_MICROTX_TRANSFER_SERVICE_URL=${REACT_APP_MICROTX_TRANSFER_SERVICE_URL:-${TRANSFER_API_BASE_PATH}} \
  --build-arg REACT_APP_MICROTX_ACCOUNT_SERVICE_URL=${REACT_APP_MICROTX_ACCOUNT_SERVICE_URL:-${ACCOUNTS_API_BASE_PATH}} \
  --build-arg REACT_APP_MERN_BACKEND_SERVICE_URL=${REACT_APP_MERN_BACKEND_SERVICE_URL:-${APP_BASE_PATH}/mern-backend} \
  --build-arg REACT_APP_MERN_MONGODB_SERVICE_URL=${REACT_APP_MERN_MONGODB_SERVICE_URL:-${ACCOUNTS_API_BASE_PATH}} \
  --build-arg REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL=${REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL:-${APP_BASE_PATH}/mern-backend} \
  --build-arg REACT_APP_MERN_SQL_ORACLE_SERVICE_URL=${REACT_APP_MERN_SQL_ORACLE_SERVICE_URL:-${ACCOUNTS_API_BASE_PATH}} \
  --build-arg REACT_APP_JAVA_ACCOUNTDETAIL_SERVICE_URL=${REACT_APP_JAVA_ACCOUNTDETAIL_SERVICE_URL:-${ACCOUNTS_API_BASE_PATH}} \
  --build-arg REACT_APP_GRAPH_LAUNDERING_SERVICE_URL=${REACT_APP_GRAPH_LAUNDERING_SERVICE_URL:-${API_BASE_PATH}} \
  --build-arg REACT_APP_TRUECACHE_STOCK_SERVICE_URL=${REACT_APP_TRUECACHE_STOCK_SERVICE_URL:-${API_BASE_PATH}/truecache} \
  --build-arg REACT_APP_SHARDING_SPATIAL_CC_SERVICE_URL=${REACT_APP_SHARDING_SPATIAL_CC_SERVICE_URL:-${API_BASE_PATH}} \
  --build-arg REACT_APP_STOCK_SERVICE_URL=${REACT_APP_STOCK_SERVICE_URL:-${API_BASE_PATH}} \
  --build-arg REACT_APP_KAFKA_TXEVENTQ_SERVICE_URL=${REACT_APP_KAFKA_TXEVENTQ_SERVICE_URL:-${API_BASE_PATH}/kafka} \
  --build-arg REACT_APP_AIAGENT_VECTOR_ADVISOR_SERVICE_URL=${REACT_APP_AIAGENT_VECTOR_ADVISOR_SERVICE_URL:-${APP_BASE_PATH}/aiagent} \
  --build-arg REACT_APP_SPEECH_SELECTAI_QUERY_SERVICE_URL=${REACT_APP_SPEECH_SELECTAI_QUERY_SERVICE_URL:-${APP_BASE_PATH}/selectai} \
  --build-arg REACT_APP_AI_AGENTS_BACKEND_URL=${REACT_APP_AI_AGENTS_BACKEND_URL:-${APP_BASE_PATH}/aiagent} \
  --build-arg REACT_APP_ORDS_BASE_URL=${REACT_APP_ORDS_BASE_URL} \
  --build-arg REACT_APP_GRAFANA_URL=${REACT_APP_GRAFANA_URL:-${APP_BASE_PATH}/grafana} \
  -t $IMAGE .


echo about to push ${IMAGE} $IMAGE...
podman push --format docker "$IMAGE"
#podman push  --tls-verify=false "$IMAGE"

#podman run --rm -p 8080:8080 $IMAGE
# podman run --rm -p 8080:8080 us-ashburn-1.ocir.io/<tenancy-namespace>/financial/frontend:0.9
