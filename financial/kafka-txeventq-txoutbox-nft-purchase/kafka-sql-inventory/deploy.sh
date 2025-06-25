#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#kubectl apply -f  backend-springboot-service-ClusterIP.yaml  -n financial
kubectl delete deployment kafka-sql-inventory  -n financial

cp kafka-sql-inventory-deployment_template.yaml kafka-sql-inventory-deployment.yaml
sed -i '' "s|IMAGE_PLACEHOLDER|$IMAGE|g" kafka-sql-inventory-deployment.yaml
kubectl apply -f kafka-sql-inventory-deployment.yaml -n financial

