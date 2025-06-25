# Set the environment variables
$env:SANDBOX_API_URL = "http://129.80.247.232:8000/v1/chat/completions"
$env:AI_OPTIMZER = "Bearer asdf"
$env:AIHOLO_HOST_URL = "https://aiholo.org/"
$env:AUDIO_DIR_PATH = "C:/Users/opc/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/interactive-ai-holograms/java-version/src/main/resources/static/audio-aiholo"

mvn package
java -jar .\target\oracleai-0.0.1-SNAPSHOT.jar
