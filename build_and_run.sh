#!/bin/bash

#source ~/Downloads/env.properties
source env.properties

mvn clean package ; java -jar target/oracleai-0.0.1-SNAPSHOT.jar
