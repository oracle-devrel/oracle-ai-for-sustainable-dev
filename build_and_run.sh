#!/bin/bash

## Add exports here... For example...
#export COHERE_KEY="mykeyvalue"
#export OPENAI_KEY="mykeyvalue"
#export OCICONFIG_FILE=~/.oci/config
#export OCICONFIG_PROFILE=DEFAULT
#export COMPARTMENT_ID="ocid1.compartment.oc1..mycompartmentvalue"
#export OBJECTSTORAGE_NAMESPACE="myobjectstorenamespacename"
#export OBJECTSTORAGE_BUCKETNAME="myobjectstorebucketname"
#export ORDS_ENDPOINT_ANALYZE_IMAGE_OBJECTSTORE="myordsendpoint"
#export ORDS_ENDPOINT_ANALYZE_IMAGE_INLINE="myordsendpoint"
#export OCI_VISION_SERVICE_ENDPOINT="https://vision.aiservice.us-ashburn-1.oci.oraclecloud.com"

## The following are only applicable when using Kubernetes...
#export KUBECONFIG=~/.kube/config-healthai
#export DOCKER_REGISTRY=us-ashburn-1.ocir.io/oradbclouducm/gd74087885

#The following is temporary until release is available in maven and only required to be called once...
#mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar
mvn clean package ; java -Djava.security.debug="access,failure"  -jar target/oracleai-0.0.1-SNAPSHOT.jar