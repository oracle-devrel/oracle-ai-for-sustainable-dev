"""Oracle Deep Data Security proof for the theme-park memory demo."""

from __future__ import annotations

import json
import os
from typing import Any

import oracledb


AVA_ID = "AVA"
LEO_ID = "LEO"
SKILL_ID = "magic-skill-rainy-evening"


class DeepDataSecurityService:
    """Query the Agent Memory table as local Deep Sec end users."""

    def __init__(self) -> None:
        self.schema = os.environ.get("DB_USERNAME", "FINANCIAL").upper()
        self.dsn = os.environ.get("DB_SERVICE", "financialdb_high")
        self.wallet = os.environ["TNS_ADMIN"]
        self.wallet_password = os.environ.get(
            "DB_WALLET_PASSWORD", os.environ["DB_PASSWORD"]
        )
        # The default is convenient for the local demo. Production deployments
        # should use individually managed credentials or IAM tokens.
        self.passwords = {
            AVA_ID: os.environ.get("DDS_AVA_PASSWORD", os.environ["DB_PASSWORD"]),
            LEO_ID: os.environ.get("DDS_LEO_PASSWORD", os.environ["DB_PASSWORD"]),
        }

    def state(self) -> dict[str, Any]:
        """Return live database-enforced visibility for both identities."""
        try:
            ava_rows = self._rows_for(AVA_ID)
            leo_rows = self._rows_for(LEO_ID)
        except oracledb.Error as error:
            return {
                "mode": "setup-required",
                "databaseEnforced": False,
                "message": (
                    "Run memory/deep-data-security/bootstrap.py once as ADMIN "
                    "to create the local end users, data roles, and data grants."
                ),
                "detail": self._safe_error(error),
                "identities": [],
            }

        ava_private = [row for row in ava_rows if row["USER_ID"] == AVA_ID]
        leo_cross_user = [row for row in leo_rows if row["USER_ID"] == AVA_ID]
        leo_shared = [row for row in leo_rows if row["USER_ID"] is None]
        return {
            "mode": "deep-data-security",
            "databaseEnforced": True,
            "message": (
                "Oracle Deep Data Security evaluated both queries inside the "
                "database. Leo received no Ava-private rows or raw traces."
            ),
            "policy": {
                "organizer": "own private rows plus shared traces and guidelines",
                "participant": "own private rows plus approved shared guidelines",
                "protectedObject": f"{self.schema}.MAGIC_PY_MEMORY",
            },
            "identities": [
                {
                    "id": AVA_ID,
                    "role": "Trip organizer",
                    "dataRole": "MEMORY_TRIP_ORGANIZER",
                    "rowCount": len(ava_rows),
                    "privateRows": len(ava_private),
                    "rows": ava_rows,
                },
                {
                    "id": LEO_ID,
                    "role": "Trip participant",
                    "dataRole": "MEMORY_TRIP_PARTICIPANT",
                    "rowCount": len(leo_rows),
                    "avaPrivateRows": len(leo_cross_user),
                    "approvedSharedRows": len(leo_shared),
                    "rows": leo_rows,
                },
            ],
            "crossUserProbe": {
                "sql": (
                    f"SELECT ... FROM {self.schema}.MAGIC_PY_MEMORY "
                    "WHERE USER_ID = 'AVA'"
                ),
                "executedAs": LEO_ID,
                "returnedRows": len(leo_cross_user),
                "blocked": len(leo_cross_user) == 0,
            },
        }

    def approve_guideline(self, approved_by: str) -> bool:
        """Update the shared guideline as Ava under the organizer data role."""
        patch = json.dumps(
            {
                "kind": "procedural",
                "status": "approved",
                "source_episode_count": 3,
                "private_guest_data_included": False,
                "approved_by": approved_by,
                "enforced_by": "Oracle Deep Data Security",
            }
        )
        try:
            with self._connect(AVA_ID) as connection, connection.cursor() as cursor:
                cursor.execute(
                    f"""
                    UPDATE {self.schema}.MAGIC_PY_MEMORY
                       SET METADATA = JSON_MERGEPATCH(METADATA, :patch RETURNING JSON)
                     WHERE RECORD_ID = :record_id
                    """,
                    patch=patch,
                    record_id=SKILL_ID,
                )
                updated = cursor.rowcount == 1
                connection.commit()
                return updated
        except oracledb.Error:
            return False

    def available(self) -> bool:
        return self.state().get("databaseEnforced", False)

    def _rows_for(self, identity: str) -> list[dict[str, Any]]:
        with self._connect(identity) as connection, connection.cursor() as cursor:
            cursor.execute(
                f"""
                SELECT RECORD_ID, USER_ID, MEMORY_TYPE,
                       DBMS_LOB.SUBSTR(CONTENT, 1000, 1) CONTENT,
                       EXPIRES_AT, CREATED_AT
                  FROM {self.schema}.MAGIC_PY_MEMORY
                 ORDER BY CREATED_AT, RECORD_ID
                """
            )
            columns = [column[0] for column in cursor.description]
            return [
                {
                    key: self._json_value(value)
                    for key, value in zip(columns, row, strict=True)
                }
                for row in cursor
            ]

    def _connect(self, identity: str) -> oracledb.Connection:
        return oracledb.connect(
            user=identity,
            password=self.passwords[identity],
            dsn=self.dsn,
            config_dir=self.wallet,
            wallet_location=self.wallet,
            wallet_password=self.wallet_password,
        )

    @staticmethod
    def _json_value(value: Any) -> Any:
        if hasattr(value, "read"):
            return value.read()
        if hasattr(value, "isoformat"):
            return value.isoformat()
        return value

    @staticmethod
    def _safe_error(error: oracledb.Error) -> str:
        message = str(error).splitlines()[0]
        password = os.environ.get("DB_PASSWORD", "")
        return message.replace(password, "<redacted>") if password else message
