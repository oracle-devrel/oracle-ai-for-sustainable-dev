

## CODE BASICS FOR INTERACTIVE AI HOLOGRAM

AIHoloController.java and aiholo.html contains the majority of the logic for the app

Aside from AIApplication.java and WebConfig.java all of the functionality/dependency for interactive ai hologram program is in the `aiholo` package.

Ie, there are examples for various AI services in app but none of them are needed by ai holo, and therefore, it is not necessary to populate the value in the env.properties file.


## REQUIRED ENVIRONMENT/VARIABLES 

It is not necessary to populate the value in the env.properties file. This is for functionality in the app that the 

It is necessary to export a few values. Here's is the contents of build_and_runrun.ps1 to give the idea...
```commandline
$env:SANDBOX_API_URL = "http://129.80.247.232:8000/v1/chat/completions"
$env:SANDBOX_AUTH_TOKEN = "Bearer asdf"
$env:AIHOLO_HOST_URL = "https://aiholo.org/"
$env:AUDIO_DIR_PATH = "C:/Users/opc/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/interactive-ai-holograms/java-version/src/main/resources/static/audio-aiholo"

mvn package
java -jar .\target\oracleai-0.0.1-SNAPSHOT.jar
```

Also, DataSourceConfiguration.java has hardcoded values for database access. 
I will make the mods to get these from the env soon.


## GCP SPEECH AI PREREQ

GCP account with access to Speech AI services is required for greater multi-lingual support.

Install gcloud CLI and run init, etc. as necessary.

Run the following command to obtain new token for Speech AI (this should last 14 days but only lasts a day for me currently)
`gcloud auth application-default login`

## BUILD AND RUN

I use build_and_runrun.ps1 on windows but there shell scripts for the same. 



