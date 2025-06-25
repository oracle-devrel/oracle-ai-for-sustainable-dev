#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  transfer-springboot-service-ClusterIP.yaml  -n financial
kubectl delete deployment transfer-springboot  -n financial

cp transfer-springboot-deployment_template.yaml transfer-springboot-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" transfer-springboot-deployment.yaml
kubectl apply -f transfer-springboot-deployment.yaml -n financial

