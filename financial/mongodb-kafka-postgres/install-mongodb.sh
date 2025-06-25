#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/

#previous way...
#kubectl create -f mongodata-persistentvolumeclaim.yaml -n msdataworkshop
#kubectl create -f mongodb-deployment.yaml -n msdataworkshop
#kubectl create -f mongodb-service.yaml -n msdataworkshop


helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install mongodb bitnami/mongodb

kubectl get secret mongodb -o jsonpath="{.data.mongodb-root-password}" | base64 --decode
kubectl get pods -l app.kubernetes.io/name=mongodb

mongodb://<username>:<password>@<host>:<port>/<authDatabase>?authSource=<authDatabase>
mongodb://root:mySecurePass@mongodb.default.svc.cluster.local:27017/admin
heL5SZ9N7T%