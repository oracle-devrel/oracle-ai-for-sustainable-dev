#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


#kubectl apply -f  frontend-springboot-service.yaml  -n financial
#kubectl apply -f  frontend-loadbalancer-service.yaml  -n financial
kubectl delete deployment frontend-react  -n financial

cp frontend-react-deployment_template.yaml frontend-react-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" frontend-react-deployment.yaml
kubectl apply -f frontend-react-deployment.yaml -n financial

kubectl apply -f  frontend-react-deployment.yaml  -n financial
