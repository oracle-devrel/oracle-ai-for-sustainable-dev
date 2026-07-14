# Directory Structure

This project is organized into purpose-specific directories for better maintainability:

## Directory Layout

```
oracle-ai-database-gcp-gemini/
├── .env                    # Environment configuration (database, GCP credentials)
├── .env_example            # Example environment file
├── run.sh                  # Main launcher script
├── notebooks/              # Jupyter notebooks for exploration and demos
│   └── oracle_ai_database_gemini_rag.ipynb
├── mcp_python/             # Python MCP applications and scripts
│   ├── requirements.txt    # Python package dependencies
│   ├── .streamlit/         # Streamlit configuration
│   ├── oracle_ai_database_langchain_streamlit.py
│   ├── oracle_ai_database_adk_agent.py
│   ├── oracle_ai_database_adk_mcp_agent.py
│   └── oracle_ai_database_genai_mcp.py
├── java/                   # Java applications (reserved for future use)
├── docs/                   # Documentation
├── archived-scripts/       # Legacy code
└── tools.yaml              # MCP toolbox configuration
```

## Environment File Location

All code references the `.env` file in the **parent directory** (root of oracle-ai-database-gcp-gemini):

- **Notebooks** use: `os.path.join(os.path.dirname(os.getcwd()), '.env')`
- **Python apps** use: `os.path.join(os.path.dirname(__file__), '..', '.env')`

This centralizes configuration management while organizing code by type.

## Running Applications

Use the `run.sh` script from the root directory:

```bash
./run.sh
```

The menu will present options for:
1. Streamlit RAG Application
2. ADK Agent with Custom BaseTool
3. ADK Agent with MCP Toolbox
4. Vertex AI + Oracle MCP

## Adding New Files

- **Jupyter notebooks** → `notebooks/`
- **Python MCP scripts/apps** → `mcp_python/`
- **Java applications** → `java/`
- **Documentation** → `docs/`

## Dependencies

Install Python dependencies from the mcp_python/ directory:

```bash
pip install -r mcp_python/requirements.txt
```

Or use the virtual environment setup in `run.sh`.

## Benefits of This Structure

1. **Clear separation** of notebook experiments vs production code
2. **Single .env file** in parent directory for all configurations
3. **Scalable** - easy to add more notebooks or applications
4. **Standard convention** - follows Python project best practices
5. **Future-ready** - java/ directory prepared for Spring Boot apps

## Migration Notes

All `.env` path references have been updated to use `../.env` to reference the parent directory's environment file. The original flat structure has been reorganized but all functionality remains the same.
