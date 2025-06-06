#!/bin/bash

export `cat env.properties`
export SANDBOX_API_URL="http://129.80.247.232:8000/v1/chat/completions"
export AI_OPTIMZER="Bearer asdf"
export AIHOLO_HOST_URL="https://aiholo.org/"
export AUDIO_DIR_PATH="C:/Users/opc/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/interactive-ai-holograms/java-version/src/main/resources/static/audio-aiholo"
#export `cat ~/Downloads/env.properties`

mvn clean package ; java -jar target/oracleai-0.0.1-SNAPSHOT.jar
