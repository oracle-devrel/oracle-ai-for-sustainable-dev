#!/bin/bash
## Copyright (c) 2021 Oracle and/or its affiliates.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/


kubectl delete deployment healthai-frontend-flutter -n healthai
#service is for all implementations and so commented/noted here...
#kubectl delete service healthai-frontend -n healthai
