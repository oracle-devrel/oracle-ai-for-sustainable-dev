-- Reference schema used by the Memories Are the Magic demo.
-- The Java service creates these objects only when they are missing.

CREATE TABLE AIM_DEMO_GUESTS (
  guest_id VARCHAR2(40) PRIMARY KEY,
  display_name VARCHAR2(120) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE SEQUENCE AIM_DEMO_MEMORY_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE AIM_DEMO_MEMORIES (
  memory_id NUMBER PRIMARY KEY,
  guest_id VARCHAR2(40),
  agent_id VARCHAR2(60) NOT NULL,
  memory_type VARCHAR2(20) NOT NULL,
  scope_type VARCHAR2(12) NOT NULL,
  memory_key VARCHAR2(80) NOT NULL,
  content VARCHAR2(1000) NOT NULL,
  status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
  version_no NUMBER DEFAULT 1 NOT NULL,
  valid_from TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE,
  superseded_by NUMBER,
  source VARCHAR2(60) NOT NULL,
  metadata_json CLOB CHECK (metadata_json IS JSON),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  FOREIGN KEY (guest_id) REFERENCES AIM_DEMO_GUESTS (guest_id)
);

-- The runnable service also creates AIM_DEMO_TRACES, AIM_DEMO_SKILLS,
-- AIM_DEMO_RECALL_AUDIT, and their sequences. See DatabaseSetup.java for
-- the complete idempotent setup used by the application.
