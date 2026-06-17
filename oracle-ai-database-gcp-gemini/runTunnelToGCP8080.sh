#!/bin/bash
# SSH tunnel to GCP VM for accessing code-server on localhost:8080
# This creates a tunnel from local port 8080 to the VM's localhost:8080
ssh -i "~/.ssh/ssh-key-2025-10-20.key" -L 8081:localhost:8080 -N ssh-key-2025-10-20@34.48.146.146
