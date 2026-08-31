SET PAGESIZE 100
SET LINESIZE 220

PROMPT === Durable guest memories and lifecycle state ===
SELECT memory_id, guest_id, memory_type, scope_type, memory_key,
       status, version_no, expires_at, superseded_by
  FROM aim_demo_memories
 ORDER BY memory_id;

PROMPT === Read/recall audit ===
SELECT recall_id, guest_id, memory_id, query_text, recalled_at
  FROM aim_demo_recall_audit
 ORDER BY recall_id;

PROMPT === Learning traces ===
SELECT trace_id, guest_id, was_successful, shareable_pattern, outcome
  FROM aim_demo_traces
 ORDER BY trace_id;

PROMPT === Induced skills and human approval ===
SELECT skill_id, skill_name, status, source_episode_count,
       approved_by, approved_at
  FROM aim_demo_skills
 ORDER BY skill_id;
