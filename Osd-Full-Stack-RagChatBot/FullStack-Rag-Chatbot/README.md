# Integrate OCI Generative AI with LangChain
This repo contains all the work done to develop demos on the integration between [**LangChain**](https://www.langchain.com/) and Oracle [**OCI GenAI**](https://www.oracle.com/artificial-intelligence/generative-ai/large-language-models/) Service.

## OCI Generative AI Service is in Limited Availability
Consider that OCI Generative AI Service (based on Cohere models) is now (oct. 2023) in **Limited 
Availability**.

It means that:
* to test this code you need to apply for the LA
* during the LA there could be breaking changes, in the OCI SDK or in the signature of the service.
* integration with LangChain will be published officially. Plans are that it will be published when GenAI Service will be GA.

I'll try to keep the code updated.

## Documentation
The development of the proposed integration is based on the example, from LangChain, provided [here](https://python.langchain.com/docs/modules/model_io/models/llms/custom_llm)

**RAG** has been first described in the following [arXiv paper](https://arxiv.org/pdf/2005.11401.pdf)

## Features
* How-to build a complete, end-2-end RAG solution using LangChain and Oracle GenAI Service.
* How-to load multiple pdf
* How-to split pdf pages in smaller chuncks
* How-to do semantic search using Embeddings
* How-to use Cohere Embeddings
* How-to use HF Embeddings
* How-to setup a Retriever using Embeddings
* How-to add Cohere reranker to the chain
* How to integrate OCI GenAI Service with LangChain
* How to define the LangChain

## Oracle BOT
Using the script [run_oracle_bot.sh](./run_oracle_bot.sh) you can launch a simple ChatBot that showcase Oracle GenAI service. The demo is based on docs from Oracle Database pdf documentation.

You need to put in the local directory:
* oracle-database-23c-new-features-guide.pdf
* database-concepts.pdf

You can add more pdf. Edit [config_rag.py](./config_rag.py)

## Video
A Video showing Oracle Bot powered by GenAI is available in this LinkedIn [post](https://www.linkedin.com/feed/update/urn:li:activity:7127218835404795905/)




