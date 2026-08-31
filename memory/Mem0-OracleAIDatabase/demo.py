"""End-to-end Mem0 example using Oracle AI Database as the vector store."""

from __future__ import annotations

import json
import os

from dotenv import load_dotenv

from config import mem0_config


def main() -> None:
    load_dotenv()
    if not os.environ.get("OPENAI_API_KEY"):
        raise SystemExit("OPENAI_API_KEY is required for the end-to-end demo")

    from mem0 import Memory

    memory = Memory.from_config(mem0_config())
    messages = [
        {
            "role": "user",
            "content": "I use a wheelchair, prefer quiet breakfasts, and enjoy the lantern show.",
        },
        {
            "role": "assistant",
            "content": "I will remember those preferences for future itinerary planning.",
        },
    ]

    added = memory.add(
        messages,
        user_id="ava",
        metadata={"domain": "theme-park-concierge", "privacy": "private"},
    )
    recalled = memory.search(
        "What should the concierge remember when planning Ava's visit?",
        user_id="ava",
        limit=5,
    )

    print("Added memories:")
    print(json.dumps(added, indent=2, default=str))
    print("\nRecalled memories:")
    print(json.dumps(recalled, indent=2, default=str))


if __name__ == "__main__":
    main()

