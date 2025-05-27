#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  backend-springboot-service-ClusterIP.yaml  -n financial
kubectl delete deployment backend-springboot  -n financial

cp globallydistributeddatabase-springboot-deployment_template.yaml globallydistributeddatabase-springboot-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" globallydistributeddatabase-springboot-deployment.yaml
kubectl apply -f globallydistributeddatabase-springboot-deployment.yaml -n financial

kubectl apply -f  globallydistributeddatabase-springboot-deployment.yaml  -n financial
