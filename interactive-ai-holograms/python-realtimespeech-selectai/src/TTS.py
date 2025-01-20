import oci
from oci.ai_speech import AIServiceSpeechClient
from oci.ai_speech.models import SynthesizeSpeechDetails

# setting up my OCI config file and speech client
config = oci.config.from_file("~/.oci/config", "DEFAULT")
speech_client = AIServiceSpeechClient(config)

# specify text, compartmentId, and the voice parameters
text_to_speech = SynthesizeSpeechDetails(
    text="Testing Oracle Database and OCI Text to Speech!",
    is_stream_enabled=True,
    # compartment_id=,
    # configuration=,
    # audio_config=,
)

# call the service to synthesize speech
response = speech_client.synthesize_speech(synthesize_speech_details=text_to_speech)

# save the output to a file
with open("output.wav", "wb") as audio_file:
    audio_file.write(response.data.content)

print("Speech synthesis completed and saved as output.wav")
