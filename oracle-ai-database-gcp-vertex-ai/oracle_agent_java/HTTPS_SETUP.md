# HTTPS Setup for Gemini Enterprise

This note captures the full setup we used to expose the shared Java agent runtime over trusted HTTPS from a GCP Compute Engine VM with only a public IPv4 address.

## Why This Was Needed

Gemini Enterprise rejects `http://` agent URLs and expects `https://`.

For this project, a self-signed certificate is not a good fit because the caller is Google-managed and needs to trust the certificate chain. We therefore used a publicly trusted Let's Encrypt IP certificate for the VM's public address.

Current tested public endpoints:

- Graph base URL: `https://YOUR_PUBLIC_AGENT_HOST/graph`
- Graph card URL: `https://YOUR_PUBLIC_AGENT_HOST/graph/.well-known/agent-card.json`
- Legacy root graph card URL: `https://YOUR_PUBLIC_AGENT_HOST/.well-known/agent-card.json`
- Graph alias card URL: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json`
- Spatial alias card URL: `https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json`

## Prerequisites

- The VM has a stable public IPv4 address.
- GCP firewall rules allow inbound TCP `80` during certificate issuance and renewal.
- GCP firewall rules allow inbound TCP `443` for the live agent.
- Nothing else is listening on port `80` while Certbot runs in standalone mode.
- Certbot `5.3.0` or newer is available because IP-address issuance depends on `--ip-address`.

## Install Certbot

On the VM, install Certbot in an isolated virtual environment and expose it on `PATH`:

```bash
sudo apt-get update
sudo apt-get install -y python3 python3-venv libaugeas0
sudo python3 -m venv /opt/certbot
sudo /opt/certbot/bin/pip install --upgrade pip certbot
sudo ln -sf /opt/certbot/bin/certbot /usr/local/bin/certbot
certbot --version
```

This matches the layout we used on the GCP VM:

- Certbot virtualenv: `/opt/certbot`
- Command on PATH: `/usr/local/bin/certbot`

## Issue the IP Certificate

From [`oracle_agent_java`](/path/to/repo-root/oracle-ai-database-gcp-vertex-ai/oracle_agent_java), run:

```bash
PUBLIC_HOST="YOUR_PUBLIC_AGENT_HOST" \
CERTBOT_EMAIL="you@example.com" \
./issue_ip_certificate.sh
```

That helper script:

- validates that `PUBLIC_HOST` is an IPv4 address
- checks the installed Certbot version
- runs `certbot certonly --standalone --ip-address ...`
- requests the short-lived Let's Encrypt profile

By default the certificate name is:

- `YOUR_LE_CERT_NAME`

The resulting files are:

- `/etc/letsencrypt/live/YOUR_LE_CERT_NAME/fullchain.pem`
- `/etc/letsencrypt/live/YOUR_LE_CERT_NAME/privkey.pem`

## Optional: Copy the Certificate for a Non-Root Process

If you want to run the Java process as a regular user instead of root, copy the certificate material into a user-readable private directory:

```bash
./sync_ip_certificate.sh
```

By default that creates:

- `$HOME/.config/oracle-graph-agent-certs/YOUR_LE_CERT_NAME/fullchain.pem`
- `$HOME/.config/oracle-graph-agent-certs/YOUR_LE_CERT_NAME/privkey.pem`

The helper sets directory mode `700` and file mode `600`.

## Run the Agent on Public HTTPS

For non-root HTTPS on a high port such as `8080`, use:

```bash
PUBLIC_HOST="YOUR_PUBLIC_AGENT_HOST" \
GRAPH_AGENT_PORT="8080" \
./run_public_https.sh
```

For Gemini Enterprise, standard `443` proved to be the more reliable path. The command below runs the existing `run.sh` with HTTPS enabled:

```bash
sudo env \
  PUBLIC_HOST="YOUR_PUBLIC_AGENT_HOST" \
  PUBLIC_PROTOCOL="https" \
  GRAPH_AGENT_URL="https://YOUR_PUBLIC_AGENT_HOST" \
  GRAPH_AGENT_PORT="443" \
  BIND_HOST="0.0.0.0" \
  SSL_CERTIFICATE="/etc/letsencrypt/live/YOUR_LE_CERT_NAME/fullchain.pem" \
  SSL_CERTIFICATE_PRIVATE_KEY="/etc/letsencrypt/live/YOUR_LE_CERT_NAME/privkey.pem" \
  ./run.sh
```

## Keep the 443 Service Alive After SSH Exits

Starting the agent with `nohup` from an interactive SSH session was not reliable enough. The better approach on this VM is to run it as a transient `systemd` service:

```bash
sudo systemd-run \
  --unit=oracle-graph-agent \
  --description="Oracle Graph Agent HTTPS service" \
  --working-directory="$PWD" \
  --setenv=PUBLIC_HOST="YOUR_PUBLIC_AGENT_HOST" \
  --setenv=PUBLIC_PROTOCOL="https" \
  --setenv=GRAPH_AGENT_URL="https://YOUR_PUBLIC_AGENT_HOST" \
  --setenv=GRAPH_AGENT_PORT="443" \
  --setenv=BIND_HOST="0.0.0.0" \
  --setenv=SSL_CERTIFICATE="/etc/letsencrypt/live/YOUR_LE_CERT_NAME/fullchain.pem" \
  --setenv=SSL_CERTIFICATE_PRIVATE_KEY="/etc/letsencrypt/live/YOUR_LE_CERT_NAME/privkey.pem" \
  "$PWD/run.sh"

sudo systemctl status oracle-graph-agent.service
```

## Verify the Deployment

Use the public agent card and the local test harness:

```bash
curl -sS https://YOUR_PUBLIC_AGENT_HOST/graph/.well-known/agent-card.json | python3 -m json.tool
curl -sS https://YOUR_PUBLIC_AGENT_HOST/agent-card-graph.json | python3 -m json.tool
curl -sS https://YOUR_PUBLIC_AGENT_HOST/agent-card-spatial.json | python3 -m json.tool
GRAPH_AGENT_URL="https://YOUR_PUBLIC_AGENT_HOST" ./test.sh
```

Expected outcomes:

- the agent card returns `200`
- the action call returns `status: completed`
- the PNG artifact is saved into `test-output/`

## Renewal Notes

Important operational details:

- The helper script requests the Let's Encrypt short-lived profile.
- Port `80` must still be reachable when the certificate is renewed.
- The VM currently does not have auto-renew wired up in this repo yet.

At minimum, test renewal manually:

```bash
sudo certbot renew --dry-run
```

If you later automate renewal, remember to restart or reload the graph agent after fresh certificate files are written.
