# Gemini Enterprise to private Oracle AI Database A2A

## Outcome

The managed Oracle AI Database A2A endpoint for `paulparkdb` is protected by a
private endpoint. Gemini Enterprise's managed outbound request cannot enter
that network directly. A narrow Cloud Run relay provides the public A2A client
surface and forwards the authenticated user's request through the existing GCP
VPC and Oracle Database@Google Cloud ODB network.

```text
Gemini Enterprise
  -> HTTPS plus the user's Oracle OAuth bearer token
  -> Cloud Run private-A2A relay
  -> Direct VPC egress on the GCP default network
  -> Oracle Database@Google Cloud ODB network
  -> paulparkdb private endpoint
  -> Oracle managed A2A server
  -> ORACLE_AI_DATABASE_AGENT Select AI agent team
```

No new cross-cloud VPN is required for this database because Oracle
Database@Google Cloud already connects the ODB network to the GCP project.

## Verified network inventory

| Resource | Value |
|---|---|
| GCP project | `adb-pm-prod` |
| GCP region | `us-east4` |
| GCP VPC | `default` |
| Database | `paulparkdb` |
| Database private hostname prefix | `gy5twnrd` |
| Database private endpoint IP | `192.168.2.113` |
| Database OCID | Store in deployment configuration; do not publish in a blog |

On August 17, 2026, the private A2A hostname resolved to `192.168.2.113` from
`paulpark-instance` on the GCP `default` VPC. This proves that the ODB network
already supplies the required private route and DNS view.

The deployed relay is:

```text
https://oracle-private-a2a-relay-1093068138027.us-east4.run.app
```

An invalid bearer-token test returned Oracle error `ADB-00015 Invalid
credential`. Before the relay, the public request returned the ACL-hidden HTTP
404. The explicit credential error proves that Cloud Run reached Oracle through
the permitted private network and that Oracle OAuth is now the active boundary.

## Security boundaries

- The relay permits only `message/send`, `message/stream`, `tasks/get`, and
  `tasks/cancel`.
- Requests without an Oracle bearer token are rejected.
- Tokens are forwarded in memory and are not persisted or logged.
- The private Oracle hostname is not returned in public errors.
- Oracle Database applies database-user privileges, Select AI profile object
  lists, row filtering, and unified auditing after authentication.
- Do not replace user OAuth with an `ADMIN` password or shared administrative
  bearer token.

## Deploy

Build the container from `private-a2a-proxy/`, deploy it to Cloud Run in
`us-east4`, and attach it to the `default` network and subnet using Direct VPC
egress. Set:

```text
PRIVATE_ORACLE_A2A_URL=https://gy5twnrd.adb.us-ashburn-1.oraclecloudapps.com/adb/a2a/v1/databases/DB_OCID/agents/oracle_ai_database_agent
PUBLIC_A2A_URL=https://CLOUD_RUN_SERVICE_URL
```

The Cloud Run endpoint must be reachable by Gemini Enterprise. Application
requests remain protected by Oracle OAuth even when Cloud Run allows public
ingress. Keep the service minimal and apply Cloud Armor or an API gateway if
organizational policy requires additional edge controls.

## Configure Gemini Enterprise OAuth

Create a dedicated Oracle OAuth client as `ADMIN` with both Gemini Enterprise
callback URIs supported by the relevant registration flow. Store the client ID
and secret in Gemini Enterprise's authorization resource, not in the relay.

Register the relay's agent card as a custom A2A agent. Associate the Oracle
authorization resource so Gemini prompts each user to authenticate with their
database identity and sends the resulting bearer token to the relay.

The existing app currently has eight registered agents, which is its creation
quota. Google also prevents changing a Marketplace agent's endpoint while its
`cloud_marketplace_config` is present. Complete registration by explicitly
replacing the nonfunctional Marketplace entry or repointing the custom Select
AI fallback. Preserve the existing Oracle authorization resource when doing so.

## Validate

1. `GET /health` returns HTTP 200.
2. `GET /.well-known/agent-card.json` advertises the relay URL and Oracle OAuth.
3. A request without a bearer token returns HTTP 401.
4. A request with an invalid token reaches Oracle privately and returns an
   Oracle authentication failure rather than an ACL failure.
5. A valid `FINANCIAL` login lists and invokes `ORACLE_AI_DATABASE_AGENT`.
6. A restricted database login returns only rows permitted by its database
   grants and Deep Data Security policy.
7. `UNIFIED_AUDIT_TRAIL` records the database-side A2A activity.

## Rollback

Disable or delete the custom Gemini Enterprise relay agent, then delete the
Cloud Run relay. This does not alter the database private endpoint, ODB
network, Select AI agent team, or existing custom A2A agents.

## References

- [Oracle A2A private-endpoint security](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/security-a2a.html#network-configuration-for-private-endpoint)
- [Oracle A2A bearer tokens](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/generate-bearer-token.html)
- [Oracle A2A agent discovery](https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/list-available-agents.html)
- [Google multi-agent private networking patterns](https://docs.cloud.google.com/architecture/multi-agent-private-networking-patterns)
- [Google register and manage A2A agents](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
