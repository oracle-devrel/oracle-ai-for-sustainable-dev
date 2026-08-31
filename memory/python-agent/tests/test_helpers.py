import sys
import unittest
from pathlib import Path

APP_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(APP_DIR))

from app import MagicMemoryService, result_to_dict, utc_iso  # noqa: E402
from ar import ArExperienceService  # noqa: E402


class FakeRecord:
    id = "memory-1"
    content = "stored content"
    record_type = "preference"
    user_id = "ava"
    agent_id = "starlight-concierge"
    thread_id = "thread-1"
    timestamp = "2026-07-28T00:00:00+00:00"
    metadata = {"kind": "semantic"}


class FakeResult:
    record = FakeRecord()
    content = "retrieved content"


class FakeTraceRecord:
    user_id = None
    metadata = {
        "privacy_safe": True,
        "contains_direct_identifiers": False,
    }


class FakeTrace:
    record = FakeTraceRecord()
    content = "Covered connector and early dinner succeeded."


class HelpersTest(unittest.TestCase):
    def test_result_serialization_preserves_scope(self):
        value = result_to_dict(FakeResult())
        self.assertEqual("ava", value["userId"])
        self.assertEqual("semantic", value["metadata"]["kind"])
        self.assertEqual("retrieved content", value["content"])

    def test_utc_iso_is_timezone_aware(self):
        self.assertIn("+00:00", utc_iso())

    def test_ar_retention_is_bounded(self):
        self.assertEqual(7, ArExperienceService._retention_days("7"))
        with self.assertRaises(ValueError):
            ArExperienceService._retention_days(31)

    def test_ar_identifiers_reject_injection_characters(self):
        self.assertEqual("ar-session_1", ArExperienceService._identifier("ar-session_1", "id"))
        with self.assertRaises(ValueError):
            ArExperienceService._identifier("ar-session' OR 1=1", "id")

    def test_privacy_gate_accepts_minimized_unscoped_trace(self):
        self.assertTrue(MagicMemoryService._trace_is_shareable(FakeTrace()))

    def test_privacy_gate_rejects_scoped_or_identifier_bearing_trace(self):
        scoped = FakeTrace()
        scoped.record = FakeTraceRecord()
        scoped.record.user_id = "AVA"
        self.assertFalse(MagicMemoryService._trace_is_shareable(scoped))

        email = FakeTrace()
        email.content = "Email ava@example.com after the covered route."
        self.assertFalse(MagicMemoryService._trace_is_shareable(email))


if __name__ == "__main__":
    unittest.main()
