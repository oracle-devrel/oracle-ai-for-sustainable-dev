# Set the OCI region environment variable
$env:OCI_CLI_REGION = "us-phoenix-1"

# Define the profile name to be used
$ProfileName = "MYSPEECHAIPROFILE`r`n"

# Start the OCI session authenticate command and try to pipe in the profile name
$ProfileName | oci session authenticate --region $env:OCI_CLI_REGION
