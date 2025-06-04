#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  account-springboot-service-ClusterIP.yaml  -n financial
kubectl delete deployment account-springboot  -n financial

cp account-springboot-deployment_template.yaml account-springboot-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" account-springboot-deployment.yaml
kubectl apply -f account-springboot-deployment.yaml -n financial
