#!/bin/bash
## Copyright (c) 2025 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


#kubectl create secret tls backend-tls --cert=/path/to/cert.crt --key=/path/to/cert.key -n financial
#kubectl create secret tls backend-tls --cert=oracleai-financial.org.cert --key=oracleai-financial.org.key -n financial
kubectl create secret tls backend-tls --cert=oracledatabase-financial.org.cert --key=oracledatabase-financial.key -n financial

