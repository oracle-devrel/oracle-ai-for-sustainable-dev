#!/bin/bash

export OPENAI_KEY="sk-nMyourkeyz43HSWvb2HgV"
export COHERE_KEY="oJfPT7nhQyourkeyVRz7"
export OCICONFIG_FILE=~/.oci/config
export OCICONFIG_PROFILE=DEFAULT

export KUBECONFIG=~/.kube/config-healthai
export DOCKER_REGISTRY=us-ashburn-1.ocir.io/oradbclouducm/gdyourocirrepos

export SPRING_DATASOURCE_USERNAME="TESTUSER1"
export SPRING_DATASOURCE_URL="jdbc:oracle:thin:@yourdb_high?TNS_ADMIN=/Users/pparkins/Downloads/Wallet_IndADW"
export SPRING_DATASOURCE_PASSWORD="yourpw"
echo dbcloud tenancy dev env configured

