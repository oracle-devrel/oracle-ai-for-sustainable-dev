# Set environment variable
$env:COMPARTMENT_ID = "ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq"
$env:PEM_PASSPHRASE = "Welcome12345"

# Authenticate OCI session
#oci session authenticate --region us-phoenix-1 --profile-name MYSPEECHAIPROFILE

# Run Python script
#python python-realtimespeech-selectai/src/RealtimeSpeechSelectAI.py
# python python-realtimespeech-selectai/src/InteractiveAIHolograms.py
# python python-realtimespeech-selectai/src/InteractiveAIHologramsRest.py this is nonssl one
python src/AIHoloRest.py