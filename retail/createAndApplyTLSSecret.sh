#/bin/bash


# Fail on error
set -e


# Create SSL Certs
mkdir tls
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout tls/tls.key -out tls/tls.crt -subj "/CN=grabdish/O=grabdish"


# Create SSL Secret
kubectl create secret tls ssl-certificate-secret --key tls/tls.key --cert tls/tls.crt -n msdataworkshop