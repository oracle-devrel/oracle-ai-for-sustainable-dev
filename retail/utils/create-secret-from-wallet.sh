#!/bin/bash

kubectl create -f - -n msdataworkshop <<!
apiVersion: v1
data:
  README: $(base64 < README | tr -d '\n')
  cwallet.sso: $(base64 < cwallet.sso | tr -d '\n')
  ewallet.p12: $(base64 < ewallet.p12 | tr -d '\n')
  keystore.jks: $(base64 < keystore.jks | tr -d '\n')
  ojdbc.properties: $(base64 < ojdbc.properties | tr -d '\n')
  sqlnet.ora: $(base64 < sqlnet.ora | tr -d '\n')
  tnsnames.ora: $(base64 < tnsnames.ora | tr -d '\n')
  truststore.jks: $(base64 < truststore.jks | tr -d '\n')
kind: Secret
metadata:
  name: inventory-db-tns-admin-secret
!