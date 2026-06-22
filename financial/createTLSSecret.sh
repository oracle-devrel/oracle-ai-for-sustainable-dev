#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
TLS_SECRET_NAME="${TLS_SECRET_NAME:-oracledev-ai-tls}"

#kubectl create secret tls backend-tls --cert=/path/to/cert.crt --key=/path/to/cert.key -n financial
kubectl create secret tls "${TLS_SECRET_NAME}" --cert="${HOSTNAME}.cert" --key="${HOSTNAME}.key" -n financial
