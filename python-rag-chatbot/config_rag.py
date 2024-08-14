# configurations for the RAG

# to enable debugging info..
DEBUG = False

# book to use for augmentation

BOOK1 = "pdfFiles/sharding-adg-addshard-cookbook-3610618.pdf"
BOOK2 = "pdfFiles/globally-distributed-autonomous-database.pdf"
BOOK3 = "pdfFiles/oracle-database-23c.pdf"
BOOK4 = "pdfFiles/oracle-globally-distributed-database-guide.pdf"
BOOK5 = "pdfFiles/Oracle True cache.pdf"
BOOK6 = "pdfFiles/Trobleshooting.pdf"


BOOK_LIST = [BOOK1, BOOK2, BOOK3, BOOK4, BOOK5, BOOK6]


# to divide docs in chunks
CHUNK_SIZE = 1000
CHUNK_OVERLAP = 50


#
# Vector Store (Chrome or FAISS)
#
# VECTOR_STORE_NAME = "FAISS"
# VECTOR_STORE_NAME = "ORACLEDB"
VECTOR_STORE_NAME = "CHROME"


# type of Embedding Model. The choice has been parametrized
# Local means HF
EMBED_TYPE = "LOCAL"
# see: https://huggingface.co/spaces/mteb/leaderboard
# see also: https://github.com/FlagOpen/FlagEmbedding
# base seems to work better than small
# EMBED_HF_MODEL_NAME = "BAAI/bge-base-en-v1.5"
# EMBED_HF_MODEL_NAME = "BAAI/bge-small-en-v1.5"
EMBED_HF_MODEL_NAME = "BAAI/bge-large-en-v1.5"

# Cohere means the embed model from Cohere site API
# EMBED_TYPE = "COHERE"
EMBED_COHERE_MODEL_NAME = "embed-english-v3.0"

# number of docs to return from Retriever
MAX_DOCS_RETRIEVED = 6

# to add Cohere reranker to the QA chain
ADD_RERANKER = False

#
# LLM Config
#
# LLM_TYPE = "COHERE"
LLM_TYPE = "OCI"

# max tokens returned from LLM for single query
MAX_TOKENS = 1000
# to avoid "creativity"
TEMPERATURE = 0

#
# OCI GenAI configs
#
TIMEOUT = 30
ENDPOINT = "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com"
