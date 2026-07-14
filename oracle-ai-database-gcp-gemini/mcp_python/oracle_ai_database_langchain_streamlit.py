from dotenv import load_dotenv
from PyPDF2 import PdfReader
from langchain_text_splitters import CharacterTextSplitter
from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_community.vectorstores.oraclevs import OracleVS
from langchain_community.vectorstores.utils import DistanceStrategy
from langchain_core.documents import Document
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_google_vertexai import VertexAI, VertexAIEmbeddings
from google.cloud import aiplatform

import oracledb
import sys
#from sentence_transformers import CrossEncoder
import array
import time
import oci
import streamlit as st
import os
import vertexai
import time
import langchain

def chunks_to_docs_wrapper(row: dict) -> Document:
    """
    Converts a row from a DataFrame into a Document object suitable for ingestion into Oracle Vector Store.
    - row (dict): A dictionary representing a row of data with keys for 'id', 'link', 'source', and 'text'.
    """
    metadata = {'id': str(row['id']), 'link': row['link'], 'source': row.get('source', 'Unknown')}
    return Document(page_content=row['text'], metadata=metadata)

def main():
    # Load from parent directory .env file
    load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))

    st.set_page_config(page_title="ask question based on pdf")
    st.info("Oracle Database@Google Cloud and Google Vertex AI")
    st.header(" Ask your question to get answers based on your pdf " )

    # Load configuration from environment variables
    un = os.getenv("DB_USERNAME", "ADMIN")
    pw = os.getenv("DB_PASSWORD")
    dsn = os.getenv("DB_DSN", "paulparkdb_tp")
    wpwd = os.getenv("DB_WALLET_PASSWORD")
    wallet_dir = os.getenv("DB_WALLET_DIR", "/home/ssh-key-2025-10-20/wallet")
    
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    region = os.getenv("GCP_REGION", "us-central1")

    connection = oracledb.connect(
        config_dir=wallet_dir, 
        user=un, 
        password=pw, 
        dsn=dsn,
        wallet_location=wallet_dir,
        wallet_password=wpwd)
    
    # Initialize Vertex AI (must be done before creating embeddings)
    # Initialize Vertex AI SDK with values from environment
    vertexai.init(project=project_id, location=region)
    
    # Use Vertex AI embeddings to match what's in the database (768 dimensions)
    model_4db = VertexAIEmbeddings(model_name="text-embedding-004")
    
    # Check if table has existing data
    knowledge_base = None
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM rag_tab")
            count = cursor.fetchone()[0]
            if count > 0:
                st.info(f"✓ Found {count} existing document chunks in database")
                # Load existing knowledge base from table
                knowledge_base = OracleVS(
                    client=connection,
                    embedding_function=model_4db,
                    table_name="RAG_TAB",
                    distance_strategy=DistanceStrategy.COSINE
                )
    except Exception as e:
        st.warning(f"Could not load existing data: {e}")
    
    #upload the file
    pdf = st.file_uploader("upload your pdf (optional if data already exists)",type="pdf")

    #extract the text
    if pdf is not None:
      pdf_reader = PdfReader(pdf)

      text=""
      for page in pdf_reader.pages:
        text += page.extract_text()

      # split the text
      text_splitter = CharacterTextSplitter(separator="\n",chunk_size=1000,chunk_overlap=200,length_function=len)
      chunks = text_splitter.split_text(text)

      # Create documents using wrapper - include PDF filename in metadata
      pdf_name = pdf.name
      docs = [chunks_to_docs_wrapper({'id': page_num, 'link': f'Page {page_num}', 'source': pdf_name, 'text': text}) for page_num, text in enumerate(chunks)]

      s1time = time.time()

      #create knowledge base in Oracle.
      # Create vector store
      knowledge_base = OracleVS.from_documents(docs, model_4db, client=connection, table_name="RAG_TAB", distance_strategy=DistanceStrategy.COSINE)

      s2time =  time.time()
      st.success(f"✓ Uploaded and vectorized PDF in {round(s2time - s1time, 1)} seconds")

    # Initialize LLM after Vertex AI is initialized
    # Using stable gemini-2.0-flash-001 instead of experimental to avoid rate limits
    llm = VertexAI(
        model_name="gemini-2.0-flash-001",
        max_output_tokens=8192,
        temperature=0.7,
        top_p=0.8,
        top_k=40,
        verbose=True,
    )
      
      # ask a question
    user_question = st.text_input("Ask a question about your pdf")
    if user_question:
      if knowledge_base is None:
        st.error("⚠️ Please upload a PDF file first!")
      else:
        try:
          with st.spinner('🔍 Searching database and generating answer...'):
            s3time = time.time()
            result_chunks = knowledge_base.similarity_search(user_question, 5)
            s4time = time.time()
            
            # Define context and question dictionary
            template = """Answer the question based only on the following context:
                       {context} Question: {question} """
            prompt = PromptTemplate.from_template(template)
            retriever = knowledge_base.as_retriever(search_kwargs={"k": 10})

            chain = (
              {"context": retriever, "question": RunnablePassthrough()}
                 | prompt
                 | llm
                 | StrOutputParser()
            )
            
            s4_5time = time.time()
            response = chain.invoke(user_question)
            s5time = time.time()
            
          # Display response
          st.write("### Answer:")
          st.write(response)
          
          # Display sources
          st.write("---")
          st.write("**Sources:**")
          for i, chunk in enumerate(result_chunks, 1):
              doc_name = chunk.metadata.get('source', 'Unknown')
              page_ref = chunk.metadata.get('link', 'Unknown')
              preview = chunk.page_content[:80].replace('\n', ' ')
              st.caption(f"{i}. {doc_name} ({page_ref}) - _{preview}_...")
          
          # Display timing info
          st.write("---")
          st.caption(f":blue[Vector search duration: {round(s4time - s3time, 2)} sec]")
          st.caption(f":blue[LLM response duration: {round(s5time - s4_5time, 2)} sec]")
          st.caption(f":blue[Total query time: {round(s5time - s3time, 2)} sec]")
        
        except Exception as e:
          st.error(f"❌ Error processing query: {str(e)}")
          st.exception(e)

if __name__ == '__main__':
    main()

