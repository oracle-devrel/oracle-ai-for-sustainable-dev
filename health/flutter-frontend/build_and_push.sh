#!/bin/bash

export IMAGE_NAME=healthai-frontend-flutter
export IMAGE_VERSION=0.1
export IMAGE=${DOCKER_REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}


docker build -t=$IMAGE .

docker push "$IMAGE"
if [  $? -eq 0 ]; then
    docker rmi "$IMAGE"
fi




#docker build . -t flutter_docker

#docker run -i -p 8090:5000 -td us-ashburn-1.ocir.io/oradbclouducm/gd74087885/healthai-frontend-flutter:0.1

