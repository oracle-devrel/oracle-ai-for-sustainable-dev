import asyncio
import os
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


def main():
    pool = oracledb.create_pool(
        user="scott",
        password="tiger",
        dsn="localhost:1521/freepdb1",
        min=1, max=2, increment=1
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

    user_id = "user_ollama_demo"
    thread = memory.create_thread(user_id=user_id)

    thread.add_messages([
        {"role": "user", "content": "My name is Ana. I prefer vegetarian meals and short morning workouts."},
        {"role": "assistant", "content": "Got it. I will keep recommendations vegetarian and concise."},
        {"role": "user", "content": "Please avoid dairy suggestions."},
    ])

    # Search typed durable records extracted by LLM
    results = memory.search(
        query="vegetarian dairy workouts",
        scope=SearchScope(user_id=user_id),
        record_types=["memory", "guideline", "fact", "preference"],
    )

    print(f"thread_id={thread.thread_id}")
    print(f"results={len(results)}")
    for r in results:
        print(f"- [{r.record.record_type}] {r.content}")


if __name__ == "__main__":
    main()
