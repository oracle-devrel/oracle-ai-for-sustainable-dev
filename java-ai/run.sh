#!/bin/bash

export `cat env.properties`

java -jar target/oracleai-0.0.1-SNAPSHOT.jar
