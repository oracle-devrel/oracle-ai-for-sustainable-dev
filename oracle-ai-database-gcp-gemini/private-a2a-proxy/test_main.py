import importlib
import os

from fastapi.testclient import TestClient


os.environ["PRIVATE_ORACLE_A2A_URL"] = (
    "https://example.adb.us-ashburn-1.oraclecloudapps.com/adb/a2a/v1/"
    "databases/example/agents/oracle_ai_database_agent"
)
os.environ["PUBLIC_A2A_URL"] = "https://relay.example.test"

main = importlib.import_module("main")


def test_card_advertises_public_relay_and_oauth():
    with TestClient(main.app) as client:
        response = client.get("/.well-known/agent-card.json")
    assert response.status_code == 200
    card = response.json()
    assert card["url"] == "https://relay.example.test"
    assert card["security"][0]["oauth2"] == ["openid"]


def test_relay_requires_bearer_token():
    with TestClient(main.app) as client:
        response = client.post("/", json={"jsonrpc": "2.0", "method": "message/send", "id": "1"})
    assert response.status_code == 401


def test_relay_rejects_unapproved_method():
    with TestClient(main.app) as client:
        response = client.post(
            "/",
            headers={"Authorization": "Bearer not-a-real-token"},
            json={"jsonrpc": "2.0", "method": "admin/delete", "id": "1"},
        )
    assert response.status_code == 400
