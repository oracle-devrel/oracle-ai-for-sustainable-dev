#!/usr/bin/env python3
"""
Simple script to start the Agentic AI LangGraph backend server
"""

import sys
import os
import subprocess

def check_dependencies():
    """Check if required dependencies are installed"""
    required_packages = [
        "fastapi",
        "uvicorn",
        "langgraph",
        "langchain",
        "oracledb"
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
        except ImportError:
            missing_packages.append(package)
    
    if missing_packages:
        print("Missing required packages:")
        for package in missing_packages:
            print(f"  - {package}")
        print("\nInstall them with:")
        print(f"pip install {' '.join(missing_packages)}")
        return False
    
    return True

def start_server():
    """Start the FastAPI server"""
    if not check_dependencies():
        sys.exit(1)
    
    print("Starting Agentic AI LangGraph API server...")
    print("Server will be available at: http://localhost:8001")
    print("API docs at: http://localhost:8001/docs")
    print("Press Ctrl+C to stop the server")
    
    try:
        # Import and run uvicorn
        import uvicorn
        uvicorn.run(
            "agenticai_langgraph:app",
            host="0.0.0.0",
            port=8001,
            reload=True,
            log_level="info"
        )
    except KeyboardInterrupt:
        print("\nServer stopped by user")
    except Exception as e:
        print(f"Error starting server: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    start_server()
