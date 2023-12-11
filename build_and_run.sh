#!/bin/bash

source ~/Downloads/env.properties
#source env.properties

#The following is temporary until release is available in maven and only required to be called once...
#mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=oci-java-sdk-generativeai-3.25.1-preview1-20230906.204234-1.jar
mvn clean package ; java -Djava.security.debug="access,failure"  -jar target/oracleai-0.0.1-SNAPSHOT.jar