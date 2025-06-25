#!/bin/bash

#The following is temporary until release is available in maven and only required to be called once...
#mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=lib/oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar

mvn clean package
