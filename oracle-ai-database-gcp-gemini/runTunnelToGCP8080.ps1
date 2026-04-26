# SSH tunnel to GCP VM for accessing code-server on localhost:8080
# This creates a tunnel from local port 8080 to the VM's localhost:8080

# Path to SSH key (use Windows-style path)
$sshKeyPath = "$env:USERPROFILE\.ssh\ssh-key-2025-10-20.key"
$vmUser = "ssh-key-2025-10-20"
$vmIP = "34.48.146.146"
$localPort = "8080"
$remotePort = "8080"

Write-Host "Starting SSH tunnel to GCP VM..." -ForegroundColor Green
Write-Host "Local port $localPort -> ${vmIP}:${remotePort}" -ForegroundColor Cyan

# Run SSH tunnel (use ssh.exe to ensure Windows OpenSSH is used)
ssh.exe -i $sshKeyPath -L ${localPort}:localhost:${remotePort} -N ${vmUser}@${vmIP}
