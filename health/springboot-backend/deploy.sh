#!/bin/bash

kubectl apply -f k8s/healthai-backend-springboot-deployment.yaml -n healthai
kubectl apply -f k8s/healthai-backend-service.yaml -n healthai
