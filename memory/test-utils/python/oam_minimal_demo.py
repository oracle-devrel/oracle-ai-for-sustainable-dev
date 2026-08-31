import numpy as np
import os
import oracledb
from oracleagentmemory.core import OracleAgentMemory
#from oracleagentmemory.core.embedders.embedder import Embedder
from oracleagentmemory.apis.embedders import IEmbedder

class DbOnnxEmbedder(IEmbedder):
    def __init__(self, pool, model="allminilm"):
        self.pool = pool
        self.model = model

    def embed(self, texts, is_query=False):
        out = []
        sql = f"select vector_embedding({self.model} using :t as data) from dual"
        with self.pool.acquire() as conn, conn.cursor() as cur:
            for t in texts:
                cur.execute(sql, t=t)
                v = cur.fetchone()[0]
                out.append(v.tolist() if hasattr(v, "tolist") else list(v))
        return np.asarray(out, dtype=np.float32)

    async def embed_async(self, texts, is_query=False):
        return await asyncio.to_thread(self.embed, texts, is_query)

def main():
    pool = oracledb.create_pool(
        user="scott",
        password="tiger",
        dsn="localhost:1521/freepdb1",
        min=1,
        max=2,
        increment=1,
    )

    #didnt work
    #embedder = Embedder(model="ALLMINILM")

    # Manual mode: messages are stored, memories are added explicitly.
    memory = OracleAgentMemory(
        connection=pool,
        extract_memories=False,
        #embedder=embedder,
        embedder=DbOnnxEmbedder(pool, model="allminilm"),
        schema_policy="create_if_necessary",
    )

    thread = memory.create_thread(user_id="user_demo")
    thread.add_messages([
        {"role": "user", "content": "I like orange juice with breakfast."},
        {"role": "assistant", "content": "Noted."},
    ])
    thread.add_memory("The user likes orange juice with breakfast.")

    print("Done. thread_id =", thread.thread_id)

if __name__ == "__main__":
    main()
