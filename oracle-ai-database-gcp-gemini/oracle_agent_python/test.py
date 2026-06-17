import asyncio
import json
import os
from pathlib import Path

from dotenv import load_dotenv
from a2a.client.client_factory import ClientFactory
from a2a.client.helpers import create_text_message_object

REPO_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(REPO_ROOT / ".env")

async def test_inventory_action_agent():
    url = os.environ.get("ACTION_AGENT_URL") or os.environ.get("A2A_URL") or (
        f"{os.environ.get('PUBLIC_PROTOCOL') or 'http'}://"
        f"{os.environ.get('PUBLIC_HOST') or 'localhost'}:"
        f"{os.environ.get('ACTION_AGENT_PORT') or os.environ.get('PORT') or '8080'}"
    )
    print(f"Connecting to {url}...")

    client = await ClientFactory.connect(url)

    card = await client.get_card()
    print(f"Connected to: {card.name}")
    print(f"Output modes: {card.default_output_modes}")

    print("\nSending inventory action request...")
    message = create_text_message_object(
        content=(
            "What inventory action should we take for SKU-500 given the current supply risk? "
            "Gather graph, spatial, and external evidence first."
        )
    )

    async for response in client.send_message(message):
        print(f"Task state: {response.result.status.state}")
        status_message = response.result.status.message
        if status_message:
            if getattr(status_message, "metadata", None):
                print("Message metadata:")
                print(json.dumps(status_message.metadata, indent=2))
            for part in status_message.parts:
                root = part.root
                if hasattr(root, "text"):
                    print(f"Message: {root.text}")
                if hasattr(root, "data"):
                    print("Data:")
                    print(json.dumps(root.data, indent=2))

if __name__ == "__main__":
    asyncio.run(test_inventory_action_agent())
