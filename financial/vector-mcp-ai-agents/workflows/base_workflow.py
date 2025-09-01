#!/usr/bin/env python3
"""
Base Workflow Handler for Agentic AI System

This module provides a base class for workflow handlers that can be configured
through JSON files and executed dynamically.
"""

import json
import logging
import random
from datetime import datetime
from typing import Dict, Any, List, Optional
from pathlib import Path
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)

class BaseWorkflowHandler(ABC):
    """Base class for all workflow handlers"""
    
    def __init__(self, config_path: str, oracle_tools=None):
        """Initialize workflow handler with configuration
        
        Args:
            config_path: Path to the workflow configuration JSON file
            oracle_tools: Oracle MCP tools instance for database operations
        """
        self.config_path = Path(config_path)
        self.oracle_tools = oracle_tools
        self.config = self._load_config()
        self.workflow_name = list(self.config.keys())[0]
        self.workflow_config = self.config[self.workflow_name]
        
        logger.info(f"Initialized {self.workflow_name} handler")
    
    def _load_config(self) -> Dict[str, Any]:
        """Load workflow configuration from JSON file"""
        try:
            with open(self.config_path, 'r') as f:
                config = json.load(f)
            return config
        except Exception as e:
            logger.error(f"Failed to load config from {self.config_path}: {str(e)}")
            raise
    
    def handle_input_component(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Handle input components based on configuration"""
        try:
            input_config = self.workflow_config["components"]["input_handlers"].get(component_type)
            
            if not input_config:
                return self._generic_input_handler(component_type, component_source, state)
            
            return self._process_configured_input(component_type, input_config, state)
            
        except Exception as e:
            logger.error(f"Error in input handler for {component_type}: {str(e)}")
            return {"status": "error", "error": str(e)}
    
    def handle_processing_node(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Handle processing nodes based on configuration"""
        try:
            processing_config = self.workflow_config["components"]["processing_nodes"].get(component_type)
            
            if not processing_config:
                return self._generic_processing_handler(component_type, component_source, state)
            
            return self._process_configured_node(component_type, processing_config, state)
            
        except Exception as e:
            logger.error(f"Error in processing handler for {component_type}: {str(e)}")
            return {"status": "error", "error": str(e)}
    
    def handle_decision_node(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Handle decision nodes based on configuration"""
        try:
            decision_config = self.workflow_config["components"]["decision_nodes"].get(component_type)
            
            if not decision_config:
                return self._generic_decision_handler(component_type, component_source, state)
            
            return self._process_configured_decision(component_type, decision_config, state)
            
        except Exception as e:
            logger.error(f"Error in decision handler for {component_type}: {str(e)}")
            return {"status": "error", "error": str(e)}
    
    def handle_output_component(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Handle output components based on configuration"""
        try:
            output_config = self.workflow_config["components"]["output_handlers"].get(component_type)
            
            if not output_config:
                return self._generic_output_handler(component_type, component_source, state)
            
            return self._process_configured_output(component_type, output_config, state)
            
        except Exception as e:
            logger.error(f"Error in output handler for {component_type}: {str(e)}")
            return {"status": "error", "error": str(e)}
    
    def _perform_vector_search(self, query: str, collection: str = "pdf_documents", k: int = 5) -> List[Dict[str, Any]]:
        """Perform vector search using Oracle tools"""
        if self.oracle_tools:
            return self.oracle_tools.vector_search(query, collection, k)
        else:
            # Mock response
            return [{
                "content": f"Mock search result for: {query}",
                "metadata": {"source": "mock", "collection": collection},
                "score": 0.9
            }]
    
    def _simulate_data(self, data_template: Dict[str, Any]) -> Dict[str, Any]:
        """Simulate data based on template configuration"""
        simulated = {}
        
        for key, value in data_template.items():
            if isinstance(value, list) and len(value) == 2 and all(isinstance(x, (int, float)) for x in value):
                # Range values - generate random number in range
                simulated[key] = round(random.uniform(value[0], value[1]), 2)
            elif isinstance(value, list):
                # List values - pick random item
                simulated[key] = random.choice(value)
            elif isinstance(value, dict):
                # Nested dict - recursively simulate
                simulated[key] = self._simulate_data(value)
            else:
                # Direct value
                simulated[key] = value
        
        return simulated
    
    # Abstract methods to be implemented by specific workflow handlers
    @abstractmethod
    def _process_configured_input(self, component_type: str, config: Dict[str, Any], state: Any) -> Dict[str, Any]:
        """Process configured input component - implement in subclass"""
        pass
    
    @abstractmethod 
    def _process_configured_node(self, component_type: str, config: Dict[str, Any], state: Any) -> Dict[str, Any]:
        """Process configured processing node - implement in subclass"""
        pass
    
    @abstractmethod
    def _process_configured_decision(self, component_type: str, config: Dict[str, Any], state: Any) -> Dict[str, Any]:
        """Process configured decision node - implement in subclass"""
        pass
    
    @abstractmethod
    def _process_configured_output(self, component_type: str, config: Dict[str, Any], state: Any) -> Dict[str, Any]:
        """Process configured output component - implement in subclass"""
        pass
    
    # Generic fallback handlers
    def _generic_input_handler(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Generic input handler for unconfigured components"""
        return {
            "status": "success",
            "data": {
                "input_type": component_type,
                "source": component_source,
                "timestamp": datetime.now().isoformat(),
                "raw_data": state.current_data.get("input_data", {})
            }
        }
    
    def _generic_processing_handler(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Generic processing handler for unconfigured components"""
        search_query = f"{component_type} {component_source}"
        search_results = self._perform_vector_search(search_query)
        
        return {
            "status": "success", 
            "data": {
                "processed_data": state.current_data,
                "processing_type": component_source,
                "search_results": search_results[:3]
            }
        }
    
    def _generic_decision_handler(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Generic decision handler for unconfigured components"""
        return {
            "status": "success",
            "data": {
                "decision": "approved",
                "decision_type": component_source,
                "confidence": 0.8,
                "reasoning": f"Generic decision for {component_type}"
            }
        }
    
    def _generic_output_handler(self, component_type: str, component_source: str, state: Any) -> Dict[str, Any]:
        """Generic output handler for unconfigured components"""
        return {
            "status": "success",
            "data": {
                "output": state.current_data,
                "output_type": component_source,
                "final_result": True,
                "timestamp": datetime.now().isoformat()
            }
        }

class WorkflowRegistry:
    """Registry for managing workflow handlers"""
    
    def __init__(self):
        """Initialize workflow registry"""
        self.handlers = {}
        self.workflows_dir = Path(__file__).parent / "workflows"
        logger.info("Initialized workflow registry")
    
    def register_handler(self, workflow_name: str, handler_class: type, config_file: str = None):
        """Register a workflow handler
        
        Args:
            workflow_name: Name of the workflow
            handler_class: Handler class that extends BaseWorkflowHandler
            config_file: Optional config file name (defaults to {workflow_name}_config.json)
        """
        if config_file is None:
            config_file = f"{workflow_name}_config.json"
        
        config_path = self.workflows_dir / config_file
        
        if not config_path.exists():
            logger.warning(f"Config file not found: {config_path}")
            return False
        
        self.handlers[workflow_name] = {
            "handler_class": handler_class,
            "config_path": str(config_path)
        }
        
        logger.info(f"Registered workflow handler: {workflow_name}")
        return True
    
    def get_handler(self, workflow_name: str, oracle_tools=None) -> Optional[BaseWorkflowHandler]:
        """Get a workflow handler instance
        
        Args:
            workflow_name: Name of the workflow
            oracle_tools: Oracle MCP tools instance
            
        Returns:
            Workflow handler instance or None if not found
        """
        if workflow_name not in self.handlers:
            logger.warning(f"Workflow handler not found: {workflow_name}")
            return None
        
        handler_info = self.handlers[workflow_name]
        handler_class = handler_info["handler_class"]
        config_path = handler_info["config_path"]
        
        try:
            return handler_class(config_path, oracle_tools)
        except Exception as e:
            logger.error(f"Failed to create handler for {workflow_name}: {str(e)}")
            return None
    
    def list_workflows(self) -> List[str]:
        """List all registered workflows"""
        return list(self.handlers.keys())
    
    def auto_discover_workflows(self):
        """Auto-discover workflow configurations in the workflows directory"""
        if not self.workflows_dir.exists():
            logger.warning(f"Workflows directory not found: {self.workflows_dir}")
            return
        
        config_files = list(self.workflows_dir.glob("*_config.json"))
        logger.info(f"Found {len(config_files)} workflow config files")
        
        for config_file in config_files:
            workflow_name = config_file.stem.replace("_config", "")
            logger.info(f"Discovered workflow config: {workflow_name}")

# Global workflow registry
workflow_registry = WorkflowRegistry()
