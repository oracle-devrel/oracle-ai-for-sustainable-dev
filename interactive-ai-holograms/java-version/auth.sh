#!/bin/bash

# token itself expires after 1 hour, but it is automatically refreshed as long as the stored credentials remain valid.
# provides long-lived authentication (~1 week) via Application Default Credentials (ADC).
gcloud auth application-default login
