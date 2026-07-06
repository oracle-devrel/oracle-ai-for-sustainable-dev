import asyncio
import logging
import streamlit as st
from typing import Optional
import oracledb
import traceback
import sys

# LangChain & OCI
from langchain.schema.runnable import RunnablePassthrough
from langchain_community.chat_models.oci_generative_ai import ChatOCIGenAI
from langchain.storage import LocalFileStore
from langchain.vectorstores import Chroma, FAISS
from langchain.embeddings import CohereEmbeddings, CacheBackedEmbeddings
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CohereRerank
from langchain.prompts import ChatPromptTemplate
from langchain.llms import Cohere
import oci
from langchain_community.vectorstores import OracleVS
from langchain_community.vectorstores.utils import DistanceStrategy
from llama_index.embeddings.huggingface_api import HuggingFaceInferenceAPIEmbedding

# ----------------- CONFIG -----------------
from config_rag import (
    VECTOR_STORE_NAME,
    MAX_TOKENS,
    EMBED_TYPE,
    EMBED_COHERE_MODEL_NAME,
    MAX_DOCS_RETRIEVED,
    ADD_RERANKER,
    TEMPERATURE,
    EMBED_HF_MODEL_NAME,
    LLM_TYPE,
    DEBUG
)

CONFIG_PROFILE = "DEFAULT"
COMPARTMENT_OCID = "ocid1.compartment.oc1..aaaaaaaajdyhd7dqnix2avhlckbhhkkcl3cujzyuz6jzyzonadca3i66pqjq"
oci_config = oci.config.from_file("~/.oci/config", CONFIG_PROFILE)
COHERE_API_KEY = oci_config.get('security_token_file')


# ----------------- MCP CLI Classes -----------------
class OracleMCPUI:
    """Oracle Mcp Streamlit UI app."""

    def __init__(self, config_path: Optional[str] = None):
        self.orchestrator = None  # placeholder if needed

    async def start(self):
        # Placeholder for starting MCP server
        st.info("MCP server started (placeholder)")

    async def stop(self):
        # Placeholder for stopping MCP server
        st.info("MCP server stopped (placeholder)")

    async def process_prompt(self, prompt: str):
        # Placeholder for processing commands
        return {"sql": "SELECT * FROM dual;", "results": [{"DUMMY": 1}], "explanation": "Example explanation"}


# ----------------- EMBEDDINGS & VECTORSTORE -----------------
def create_cached_embedder():
    fs = LocalFileStore("./vector-cache/")
    if EMBED_TYPE == "COHERE":
        embed_model = CohereEmbeddings(model=EMBED_COHERE_MODEL_NAME, cohere_api_key=COHERE_API_KEY)
    elif EMBED_TYPE == "LOCAL":
        model_kwargs = {"device": "cpu"}
        encode_kwargs = {"normalize_embeddings": True}
        embed_model = HuggingFaceInferenceAPIEmbedding(
            model_name=EMBED_HF_MODEL_NAME,
            model_kwargs=model_kwargs,
            use_onnx=True,
            encode_kwargs=encode_kwargs,
        )
    else:
        raise ValueError("Unsupported embedding type")

    cached_embedder = CacheBackedEmbeddings.from_bytes_store(embed_model, fs, namespace=embed_model.model_name)
    return cached_embedder


def create_retriever(vectorstore):
    if vectorstore is None:
        return None
    if not ADD_RERANKER:
        return vectorstore.as_retriever(search_kwargs={"k": MAX_DOCS_RETRIEVED})
    else:
        compressor = CohereRerank(cohere_api_key=COHERE_API_KEY)
        base_retriever = vectorstore.as_retriever(search_kwargs={"k": MAX_DOCS_RETRIEVED})
        return ContextualCompressionRetriever(base_compressor=compressor, base_retriever=base_retriever)


def create_vector_store(store_type, embedder):
    vectorstore = None
    try:
        if store_type == "ORACLEDB":
            connection = oracledb.connect(
                user="admin",
                password="Password1234",
                dsn="localhost:14401/w3m00000td.osdgsmdatabases.v10101ash.oraclevcn.com"
            )
            vectorstore = OracleVS(
                client=connection,
                embedding_function=embedder,
                table_name="kb_chunk",
                distance_strategy=DistanceStrategy.DOT_PRODUCT
            )
            st.success(f"Vector Store Loaded from Table: {vectorstore.table_name}")
    except Exception as e:
        st.error(f"Failed to create vector store: {e}")
        traceback.print_exc()
    return vectorstore


def make_security_token_signer(oci_config):
    pk = oci.signer.load_private_key_from_file(oci_config.get("key_file"), None)
    with open(oci_config.get("security_token_file")) as f:
        st_string = f.read()
    return oci.auth.signers.SecurityTokenSigner(st_string, pk)


def build_llm(llm_type):
    if llm_type == "OCI":
        llm = ChatOCIGenAI(
            model_id="meta.llama-3.1-405b-instruct",
            service_endpoint="https://inference.generativeai.us-chicago-1.oci.oraclecloud.com",
            compartment_id=COMPARTMENT_OCID,
            model_kwargs={"max_tokens": 200},
            auth_type='SECURITY_TOKEN',
        )
    elif llm_type == "COHERE":
        llm = Cohere(model="command", cohere_api_key=COHERE_API_KEY, max_tokens=MAX_TOKENS, temperature=TEMPERATURE)
    else:
        raise ValueError("Unsupported LLM type")
    return llm


# ----------------- RAG CHAIN -----------------
@st.cache_resource
def initialize_rag_chain():
    embedder = create_cached_embedder()
    vectorstore = create_vector_store(VECTOR_STORE_NAME, embedder)
    if vectorstore is None:
        return None
    retriever = create_retriever(vectorstore)
    llm = build_llm(LLM_TYPE)
    template = """Answer the question based only on the following context:
{context}

Question: {question}
"""
    rag_prompt = ChatPromptTemplate.from_template(template)
    rag_chain = ({"context": retriever, "question": RunnablePassthrough()} | rag_prompt | llm)
    return rag_chain


def get_answer(rag_chain, question):
    response = rag_chain.invoke(question)
    if DEBUG:
        print(f"Question: {question}")
        print("Response:", response)
    return response.content


# ----------------- STREAMLIT APP -----------------
st.set_page_config(page_title="Oracle Mcp", page_icon="🧠", layout="wide")
st.title("🧠 Distributed Database Intelligence AI - Natural Language Database Assistant")

# Initialize MCP session state
if "Mcp" not in st.session_state:
    st.session_state.Mcp = OracleMCPUI()
    st.session_state.started = False

# Start/Stop MCP buttons
col1, col2 = st.columns([1, 1])
with col1:
    if st.button("▶️ Start MCP Server", key="mcp_start", disabled=st.session_state.started):
        asyncio.run(st.session_state.Mcp.start())
        st.session_state.started = True
        st.success("✅ MCP Server started")
with col2:
    if st.button("⏹️ Stop MCP Server", key="mcp_stop", disabled=not st.session_state.started):
        asyncio.run(st.session_state.Mcp.stop())
        st.session_state.started = False
        st.warning("🛑 MCP Server stopped")

# MCP Command Input
if st.session_state.started:
    mcp_prompt = st.text_input("💬 Enter MCP command:", placeholder="e.g. Show me current sessions", key="mcp_input")
    if mcp_prompt:
        try:
            result = asyncio.run(st.session_state.Mcp.process_prompt(mcp_prompt))
            if result.get("sql"):
                st.subheader("🧠 Generated SQL")
                st.code(result["sql"], language="sql")
            if result.get("results"):
                st.subheader("✅ Results")
                st.write(result["results"])
            if result.get("explanation"):
                st.subheader("📝 Explanation")
                st.info(result["explanation"])
        except Exception as e:
            st.error(f"MCP command error: {e}")

# Initialize RAG
if "messages" not in st.session_state:
    st.session_state.messages = []
    st.session_state.feedback_rendered = False
    st.session_state.feedback_key = 0

rag_chain = initialize_rag_chain()
if rag_chain is None:
    st.warning("RAG chain is not available because vector store failed to initialize.")
else:
    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])

    user_question = st.chat_input("Ask a question (RAG):", key="rag_input")
    if user_question:
        st.chat_message("user").markdown(user_question)
        st.session_state.messages.append({"role": "user", "content": user_question})
        try:
            answer = get_answer(rag_chain, user_question)
            st.chat_message("assistant").markdown(answer)
            st.session_state.messages.append({"role": "assistant", "content": answer})
        except Exception as e:
            st.error(f"RAG error: {e}")
