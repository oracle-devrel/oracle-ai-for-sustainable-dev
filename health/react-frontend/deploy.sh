#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

kubectl apply -f healthai-frontend-flutter-deployment.yaml -n healthai
kubectl apply -f healthai-frontend-service.yaml -n healthai
