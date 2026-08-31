"""Database-only contract smoke test for Mem0's Oracle vector-store provider."""

from __future__ import annotations

import argparse
import os
import uuid

from dotenv import load_dotenv

from config import oracle_connection_params


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--keep",
        action="store_true",
        help="Keep the uniquely named smoke-test table for inspection.",
    )
    args = parser.parse_args()
    load_dotenv()

    from mem0.vector_stores.oracledb import OracleAIVectorSearch

    table_name = f"MEM0_SMOKE_{uuid.uuid4().hex[:12].upper()}"
    store = OracleAIVectorSearch(
        connection_params=oracle_connection_params(),
        use_connection_pool=True,
        collection_name=table_name,
        embedding_model_dims=3,
        distance_metric="COSINE",
        do_create_index=False,
    )

    try:
        ids = [str(uuid.uuid4()) for _ in range(3)]
        store.insert(
            vectors=[
                [1.0, 0.0, 0.0],
                [0.9, 0.1, 0.0],
                [0.0, 1.0, 0.0],
            ],
            payloads=[
                {"user_id": "ava", "memory": "Prefers quiet breakfasts"},
                {"user_id": "ava", "memory": "Needs step-free routes"},
                {"user_id": "leo", "memory": "Enjoys roller coasters"},
            ],
            ids=ids,
        )

        hits = store.search(
            query="fixed-vector contract test",
            vectors=[1.0, 0.0, 0.0],
            top_k=5,
            filters={"user_id": "ava"},
        )
        assert [hit.id for hit in hits] == ids[:2]
        assert all(hit.payload["user_id"] == "ava" for hit in hits)

        first = store.get(ids[0])
        assert first is not None
        assert first.payload["memory"] == "Prefers quiet breakfasts"

        store.update(
            ids[0],
            payload={"user_id": "ava", "memory": "Prefers an early quiet breakfast"},
        )
        assert store.get(ids[0]).payload["memory"] == "Prefers an early quiet breakfast"

        listed = store.list(filters={"user_id": "ava"}, top_k=10)[0]
        assert len(listed) == 2

        store.delete(ids[1])
        assert store.get(ids[1]) is None

        print(f"PASS: Oracle vector-store contract validated with {table_name}")
        if args.keep:
            print(f"KEPT: {table_name}")
    finally:
        if not args.keep:
            store.delete_col()


if __name__ == "__main__":
    main()

