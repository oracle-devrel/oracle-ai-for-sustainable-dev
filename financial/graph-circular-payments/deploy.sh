#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  graph-circular-payments-service-ClusterIP.yaml  -n financial
kubectl delete deployment graph-circular-payments  -n financial

cp graph-circular-payments-deployment_template.yaml graph-circular-payments-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" graph-circular-payments-deployment.yaml
kubectl apply -f graph-circular-payments-deployment.yaml -n financial

