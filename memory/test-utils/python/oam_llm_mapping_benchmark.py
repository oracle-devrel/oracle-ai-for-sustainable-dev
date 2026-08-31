import asyncio
import json
from pathlib import Path
import uuid

import numpy as np
import oracledb

from oracleagentmemory.core import OracleAgentMemory
from oracleagentmemory.core.llms.llm import Llm
from oracleagentmemory.apis.searchscope import SearchScope
from oracleagentmemory.apis.embedders import IEmbedder


class DbOnnxEmbedder(IEmbedder):
    def __init__(self, pool, model="allminilm"):
        self.pool = pool
        self.model = model

    def embed(self, texts: list[str], is_query: bool = False) -> np.ndarray:
        out = []
        sql = f"select vector_embedding({self.model} using :t as data) from dual"
        with self.pool.acquire() as conn, conn.cursor() as cur:
            for t in texts:
                cur.execute(sql, t=t)
                v = cur.fetchone()[0]
                out.append(v.tolist() if hasattr(v, "tolist") else list(v))
        return np.asarray(out, dtype=np.float32)

    async def embed_async(self, texts: list[str], is_query: bool = False) -> np.ndarray:
        return await asyncio.to_thread(self.embed, texts, is_query)


def load_cases():
    benchmark_file = (
        Path(__file__).parent
        / "ojdbc-agent-memory-examples"
        / "src"
        / "main"
        / "resources"
        / "longmemeval_oracle_common.json"
    )
    with benchmark_file.open() as f:
        return json.load(f)["cases"]


def main():
    pool = oracledb.create_pool(
        user="scott",
        password="tiger",
        dsn="localhost:1521/freepdb1",
        min=1,
        max=2,
        increment=1,
    )

    embedder = DbOnnxEmbedder(pool, model="allminilm")

    llm = Llm(
        model="ollama/llama3.2",
        api_base="http://localhost:11434",
        temperature=0.0,
    )

    memory = OracleAgentMemory(
        connection=pool,
        embedder=embedder,
        llm=llm,
        extract_memories=True,
        schema_policy="create_if_necessary",
    )

    run_id = uuid.uuid4().hex[:8]

    for case in load_cases():
        user_id = f'{case["user_id"]}_{run_id}'
        thread = memory.create_thread(user_id=user_id)

        thread.add_messages([
            {"role": message["role"], "content": message["content"]}
            for message in case["messages"]
        ])

        results = memory.search(
            query=case["question"],
            scope=SearchScope(user_id=user_id),
            record_types=["memory", "guideline", "fact", "preference"],
        )

        snippets = [snippet.lower() for snippet in case["expected_memory_snippets"]]
        contents = [r.content.lower() for r in results]
        hit = all(any(snippet in content for content in contents) for snippet in snippets)

        print(f'question_id={case["question_id"]}')
        print(f'question_type={case["question_type"]}')
        print(f'question={case["question"]}')
        print(f'expected_answer={case["expected_answer"]}')
        print(f'hit={hit}')
        for r in results:
            print(f'- [{r.record.record_type}] {r.content}')

        print(f"summary={thread.get_summary()}")
        print(f"context_card={thread.get_context_card()}")
        print()


if __name__ == "__main__":
    main()
