# Deep Data Security for the Memory Demo

This setup turns the Ava and Leo isolation claim into a database-enforced proof on the real `FINANCIAL.MAGIC_PY_MEMORY` table.

- Ava is a local Deep Data Security end user with the `MEMORY_TRIP_ORGANIZER` data role. She can read her private memories and shared learning material, and approve a shared guideline.
- Leo is a local Deep Data Security end user with the `MEMORY_TRIP_PARTICIPANT` data role. He can read his own records and approved shared guidelines. Ava's private records, raw traces, and pending guidelines are filtered in Oracle AI Database.
- The participant data grant also omits `METADATA`, demonstrating column-level minimization.

## Bootstrap

Start the memory app once so `MAGIC_PY_MEMORY` exists. Then load the normal database environment plus an ADMIN password and run:

```bash
cd memory/deep-data-security
export DDS_ADMIN_PASSWORD='<financial database ADMIN password>'
../.venv/bin/python bootstrap.py
```

The local demo defaults Ava and Leo's end-user passwords to `DB_PASSWORD`. Set `DDS_AVA_PASSWORD` and `DDS_LEO_PASSWORD` before bootstrap and app startup to use separate values. Production systems should use individually managed identities or IAM tokens, not shared demo passwords.

Alternatively, run `setup_local_dds.sql` as ADMIN in SQLcl.

Restart `memory/run.sh`, advance through Retain, Dream, Approve, and Next Guest, and inspect the **Deep Data Security proof** panel. The app executes the same query as each end user; filtering occurs in the database through `CREATE DATA GRANT`, not in browser code.

## Security boundary

Deep Data Security enforces row and column access and restricts who can update approval metadata. It does not prove that an input was de-identified, detect all PII, prevent poisoned learning traces, or make embeddings anonymous. The app therefore combines DDS with a minimum evidence threshold, explicit de-identification checks, human approval, TTL, and audit evidence.
