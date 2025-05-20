#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  mern-backend-service-ClusterIP.yaml  -n financial
kubectl delete deployment mern-backend  -n financial

cp mern-backend-deployment_template.yaml mern-backend-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" mern-backend-deployment.yaml
kubectl apply -f mern-backend-deployment.yaml -n financial

