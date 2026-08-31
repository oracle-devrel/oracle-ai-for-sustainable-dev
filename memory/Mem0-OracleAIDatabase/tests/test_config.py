"""Unit tests that do not require Mem0, OpenAI, or Oracle Database."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from config import collection_name, mem0_config, oracle_connection_params


class ConfigTest(unittest.TestCase):
    def setUp(self) -> None:
        self.env = {
            "ORACLE_USER": "mem0_user",
            "ORACLE_PASSWORD": "secret",
            "ORACLE_DSN": "localhost:1521/FREEPDB1",
        }

    def test_connection_params(self) -> None:
        self.assertEqual(
            oracle_connection_params(self.env),
            {
                "user": "mem0_user",
                "password": "secret",
                "dsn": "localhost:1521/FREEPDB1",
            },
        )

    def test_wallet_parameters_are_optional(self) -> None:
        self.env["ORACLE_CONFIG_DIR"] = "/wallet"
        self.assertEqual(oracle_connection_params(self.env)["config_dir"], "/wallet")

    def test_collection_is_normalized_and_validated(self) -> None:
        self.env["MEM0_ORACLE_COLLECTION"] = "mem0_guest_memory"
        self.assertEqual(collection_name(self.env), "MEM0_GUEST_MEMORY")
        self.env["MEM0_ORACLE_COLLECTION"] = "unsafe;drop table x"
        with self.assertRaises(ValueError):
            collection_name(self.env)

    def test_builds_official_provider_config(self) -> None:
        config = mem0_config(self.env)
        self.assertEqual(config["vector_store"]["provider"], "oracledb")
        self.assertEqual(
            config["vector_store"]["config"]["collection_name"],
            "MEM0_ORACLE_MEMORIES",
        )
        self.assertEqual(
            config["vector_store"]["config"]["embedding_model_dims"],
            1536,
        )

    def test_rejects_invalid_accuracy(self) -> None:
        self.env["MEM0_VECTOR_INDEX_ACCURACY"] = "101"
        with self.assertRaises(ValueError):
            mem0_config(self.env)


if __name__ == "__main__":
    unittest.main()

