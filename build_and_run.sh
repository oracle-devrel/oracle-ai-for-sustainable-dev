#!/bin/bash

export OPENAI_KEY="sk-youropenaikeyhere-asdf"
export OCICONFIG_FILE=~/.oci/config
export OCICONFIG_PROFILE=DEFAULT
mvn clean package ; java -jar target/oracleai-0.0.1-SNAPSHOT.jar