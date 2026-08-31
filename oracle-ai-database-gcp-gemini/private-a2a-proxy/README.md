# Private Oracle AI Database A2A relay

This narrow relay lets Gemini Enterprise call an Oracle Autonomous AI Database
A2A agent when the database uses a private endpoint. Gemini calls the public
Cloud Run relay, while Cloud Run uses Direct VPC egress and the Oracle
Database@Google Cloud ODB network to reach the private database endpoint.

The relay does not hold a database password. Gemini Enterprise obtains an
Oracle OAuth token for the signed-in database user and sends it to the relay.
The relay forwards that token to Oracle over the private network. Oracle
Database remains responsible for authentication, authorization, Select AI
object restrictions, VPD or Deep Data Security policies, and unified auditing.

See [PRIVATE_A2A_NETWORK_RUNBOOK.md](../docs/PRIVATE_A2A_NETWORK_RUNBOOK.md) for
deployment, registration, validation, rollback, and troubleshooting steps.

Deploy from this directory after loading the ignored repository `.env`:

```bash
set -a
source ../.env
set +a
./deploy.sh
```
