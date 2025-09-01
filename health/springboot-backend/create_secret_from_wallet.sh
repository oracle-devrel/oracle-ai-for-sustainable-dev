#!/bin/bash

if kubectl apply -f - ; then
    echo "secret applied for wallet."
else
    echo "Error: Failure to create healthai-backend-db-tns-admin-secret."
fi <<!
apiVersion: v1
data:
  README: $(base64 < README)
  cwallet.sso: $(base64 < cwallet.sso)
  ewallet.p12: $(base64 < ewallet.p12)
  keystore.jks: $(base64 < keystore.jks)
  ojdbc.properties: $(base64 < ojdbc.properties)
  sqlnet.ora: $(base64 < sqlnet.ora)
  tnsnames.ora: $(base64 < tnsnames.ora)
  truststore.jks: $(base64 < truststore.jks)
kind: Secret
metadata:
  name: healthai-backend-db-tns-admin-secret
!

