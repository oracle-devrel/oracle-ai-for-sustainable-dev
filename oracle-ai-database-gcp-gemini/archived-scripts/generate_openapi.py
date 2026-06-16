"""
Generate clean OpenAPI 3.0.3 spec for GCP Vertex AI Agents
Run this and copy the output to GCP
"""
import json

openapi_spec = {
    "openapi": "3.0.3",
    "info": {
        "title": "Oracle AI Database RAG API",
        "description": "FastAPI service for querying Oracle AI Database with Vertex AI RAG capabilities",
        "version": "1.0.0"
    },
    "servers": [
        {
            "url": "http://10.150.0.8:8501"
        }
    ],
    "paths": {
        "/query": {
            "post": {
                "summary": "Query the knowledge base",
                "description": "Submit a question to search the document knowledge base and generate an answer using RAG",
                "operationId": "query",
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {
                                "$ref": "#/components/schemas/QueryRequest"
                            }
                        }
                    },
                    "required": True
                },
                "responses": {
                    "200": {
                        "description": "Successful Response",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/QueryResponse"
                                }
                            }
                        }
                    }
                }
            }
        },
        "/status": {
            "get": {
                "summary": "Get service status",
                "description": "Returns the current status of the RAG service",
                "operationId": "get_status",
                "responses": {
                    "200": {
                        "description": "Successful Response",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/StatusResponse"
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    "components": {
        "schemas": {
            "QueryRequest": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The question to ask about the documents",
                        "example": "What are the new spatial features in Oracle Database?"
                    },
                    "top_k": {
                        "type": "integer",
                        "description": "Number of similar document chunks to retrieve",
                        "default": 5,
                        "minimum": 1,
                        "maximum": 20
                    }
                },
                "required": ["query"]
            },
            "QueryResponse": {
                "type": "object",
                "properties": {
                    "answer": {
                        "type": "string",
                        "description": "Generated answer based on retrieved context"
                    },
                    "context_chunks": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "description": "Retrieved document chunks used for the answer"
                    },
                    "vector_search_time": {
                        "type": "number",
                        "description": "Time taken for vector search in seconds"
                    },
                    "llm_response_time": {
                        "type": "number",
                        "description": "Time taken for LLM response in seconds"
                    },
                    "total_time": {
                        "type": "number",
                        "description": "Total query processing time in seconds"
                    }
                },
                "required": ["answer", "context_chunks", "vector_search_time", "llm_response_time", "total_time"]
            },
            "StatusResponse": {
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "description": "Service status"
                    },
                    "document_count": {
                        "type": "integer",
                        "description": "Number of document chunks in the knowledge base"
                    },
                    "database_connected": {
                        "type": "boolean",
                        "description": "Database connection status"
                    },
                    "models_loaded": {
                        "type": "boolean",
                        "description": "AI models initialization status"
                    }
                },
                "required": ["status", "document_count", "database_connected", "models_loaded"]
            }
        }
    }
}

# Output as formatted JSON
print(json.dumps(openapi_spec, indent=2))
