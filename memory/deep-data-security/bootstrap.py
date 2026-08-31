#!/usr/bin/env python3
"""Install and verify the local Deep Data Security memory demonstration."""

from __future__ import annotations

import os

import oracledb


def quoted(value: str) -> str:
    return '"' + value.replace('"', '""') + '"'


schema = os.environ.get("DB_USERNAME", "FINANCIAL").upper()
wallet = os.environ["TNS_ADMIN"]
wallet_password = os.environ.get("DB_WALLET_PASSWORD", os.environ["DB_PASSWORD"])
ava_password = os.environ.get("DDS_AVA_PASSWORD", os.environ["DB_PASSWORD"])
leo_password = os.environ.get("DDS_LEO_PASSWORD", os.environ["DB_PASSWORD"])

connection = oracledb.connect(
    user="ADMIN",
    password=os.environ["DDS_ADMIN_PASSWORD"],
    dsn=os.environ.get("DB_SERVICE", "financialdb_high"),
    config_dir=wallet,
    wallet_location=wallet,
    wallet_password=wallet_password,
)

statements = [
    f"CREATE END USER IF NOT EXISTS \"AVA\" IDENTIFIED BY {quoted(ava_password)} SCHEMA {schema}",
    f"CREATE END USER IF NOT EXISTS \"LEO\" IDENTIFIED BY {quoted(leo_password)} SCHEMA {schema}",
    "CREATE DATA ROLE IF NOT EXISTS memory_trip_organizer",
    "CREATE DATA ROLE IF NOT EXISTS memory_trip_participant",
    "CREATE ROLE IF NOT EXISTS memory_dds_session_role",
    "GRANT CREATE SESSION TO memory_dds_session_role",
    "GRANT memory_dds_session_role TO memory_trip_organizer",
    "GRANT memory_dds_session_role TO memory_trip_participant",
    'GRANT DATA ROLE memory_trip_organizer TO "AVA"',
    'GRANT DATA ROLE memory_trip_participant TO "LEO"',
    f"""
    CREATE OR REPLACE DATA GRANT {schema}.MEMORY_ORGANIZER_ACCESS
      AS SELECT, UPDATE(METADATA)
      ON {schema}.MAGIC_PY_MEMORY
      WHERE UPPER(USER_ID) = UPPER(ORA_END_USER_CONTEXT.username)
         OR (USER_ID IS NULL AND JSON_VALUE(METADATA, '$.kind')
             IN ('experience_trace', 'procedural'))
      TO memory_trip_organizer
    """,
    f"""
    CREATE OR REPLACE DATA GRANT {schema}.MEMORY_PARTICIPANT_ACCESS
      AS SELECT (RECORD_ID, ORDER_SEQ, THREAD_ID, USER_ID, AGENT_ID,
                 SPACE_ID, MEMORY_TYPE, CONTENT, TIMESTAMP,
                 EXPIRES_AT, CREATED_AT)
      ON {schema}.MAGIC_PY_MEMORY
      WHERE UPPER(USER_ID) = UPPER(ORA_END_USER_CONTEXT.username)
         OR (USER_ID IS NULL
             AND MEMORY_TYPE = 'guideline'
             AND JSON_VALUE(METADATA, '$.status') = 'approved')
      TO memory_trip_participant
    """,
]

with connection.cursor() as cursor:
    for statement in statements:
        cursor.execute(statement)
    cursor.execute(
        """
        SELECT data_role, grantee, grantee_type
          FROM dba_data_role_grants
         WHERE data_role IN ('MEMORY_TRIP_ORGANIZER', 'MEMORY_TRIP_PARTICIPANT')
         ORDER BY data_role, grantee
        """
    )
    grants = list(cursor)
connection.commit()
connection.close()

print("Deep Data Security configured and verified:")
for data_role, grantee, grantee_type in grants:
    print(f"  {grantee}: {data_role} ({grantee_type})")
