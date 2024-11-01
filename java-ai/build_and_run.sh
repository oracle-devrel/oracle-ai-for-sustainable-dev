#!/bin/bash

export `cat env.properties`
#export `cat ~/Downloads/env.properties`

mvn clean package ; java -jar target/oracleai-0.0.1-SNAPSHOT.jar
