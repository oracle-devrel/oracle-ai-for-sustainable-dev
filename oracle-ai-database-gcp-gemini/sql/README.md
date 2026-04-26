# Shared SQL Assets

This directory holds the shared Oracle Database setup, seed, and SQLcl helper scripts used across the Java, Python, and Go agent implementations.

Current contents:

- schema and seed SQL for the supply-chain graph demo
- schema and seed SQL for the inventory-risk and hotspot demo tables
- Select AI profile helper SQL
- SQLcl wrapper scripts that load the repo-level `.env` and run the shared SQL from one canonical location

Use this directory as the canonical home for database assets instead of treating them as Java-specific files.
