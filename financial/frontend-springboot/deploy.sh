#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


kubectl apply -f  frontend-springboot-service.yaml  -n financial
kubectl delete deployment frontend-springboot  -n financial
kubectl apply -f  frontend-springboot-deployment.yaml  -n financial
