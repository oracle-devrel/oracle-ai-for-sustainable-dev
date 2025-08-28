#!/bin/bash
# Simply copy this file to and run it from your wallet dir...
if kubectl apply -f - ; then
    echo "secret applied for wallet in the msdataworkshop namespace."
else
    echo "Error: Failure to create ragdb-wallet-secret."
fi <<!
apiVersion: v1
data:
  README: $(base64 -i ./README)
  cwallet.sso: $(base64 -i ./cwallet.sso)
  ewallet.p12: $(base64 -i ./ewallet.p12)
  keystore.jks: $(base64 -i ./keystore.jks)
  ojdbc.properties: $(base64 -i ./ojdbc.properties)
  sqlnet.ora: $(base64 -i ./sqlnet.ora)
  tnsnames.ora: $(base64 -i ./tnsnames.ora)
  truststore.jks: $(base64 -i ./truststore.jks)
kind: Secret
metadata:
  name: ragdb-wallet-secret
  namespace: msdataworkshop
!