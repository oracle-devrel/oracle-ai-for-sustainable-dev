WHENEVER SQLERROR EXIT SQL.SQLCODE

PROMPT Creating supply-chain exchange schema objects...
@@01-schema.sql
@@02-seed-data.sql
@@04-views.sql
@@05-mcp-sequence.sql
@@06-mcp-procedure.sql

PROMPT Supply-chain exchange database setup complete.
