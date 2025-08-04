"""
Modular Workflow System for Agentic AI

This package provides configurable, modular workflow handlers for different
domain-specific AI agent workflows.
"""

from .base_workflow import BaseWorkflowHandler, WorkflowRegistry

try:
    from .investment_advisor_handler import InvestmentAdvisorHandler
    INVESTMENT_HANDLER_AVAILABLE = True
except ImportError:
    INVESTMENT_HANDLER_AVAILABLE = False

try:
    from .banking_concierge_handler import BankingConciergeHandler
    BANKING_HANDLER_AVAILABLE = True
except ImportError:
    BANKING_HANDLER_AVAILABLE = False

try:
    from .spatial_digital_twins_handler import SpatialDigitalTwinsHandler
    SPATIAL_HANDLER_AVAILABLE = True
except ImportError:
    SPATIAL_HANDLER_AVAILABLE = False

__all__ = [
    'BaseWorkflowHandler',
    'WorkflowRegistry'
]

if INVESTMENT_HANDLER_AVAILABLE:
    __all__.append('InvestmentAdvisorHandler')

if BANKING_HANDLER_AVAILABLE:
    __all__.append('BankingConciergeHandler')

if SPATIAL_HANDLER_AVAILABLE:
    __all__.append('SpatialDigitalTwinsHandler')
