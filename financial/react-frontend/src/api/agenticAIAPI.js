// API integration for Agentic AI Backend
// This file provides functions to communicate with the agenticai_langgraph.py backend

const API_BASE_URL = process.env.REACT_APP_BACKEND_API_URL || 'http://localhost:8001/api';

/**
 * Execute a workflow by sending components to the backend
 * @param {string} workflowName - Name of the workflow
 * @param {Array} components - Array of workflow components
 * @param {Object} inputData - Input data for the workflow
 * @param {string} userQuery - Optional user query
 * @returns {Promise<Object>} Workflow execution response
 */
export const executeWorkflow = async (workflowName, components, inputData = null, userQuery = null) => {
  try {
    const response = await fetch(`${API_BASE_URL}/workflow/execute`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        workflow_name: workflowName,
        components: components,
        input_data: inputData,
        user_query: userQuery
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    console.log('Workflow execution started:', result);
    return result;
  } catch (error) {
    console.error('Error executing workflow:', error);
    throw error;
  }
};

/**
 * Get the status of a running workflow
 * @param {string} workflowId - ID of the workflow to check
 * @returns {Promise<Object>} Workflow status
 */
export const getWorkflowStatus = async (workflowId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/workflow/status/${workflowId}`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error getting workflow status:', error);
    throw error;
  }
};

/**
 * Get the final result of a completed workflow
 * @param {string} workflowId - ID of the workflow
 * @returns {Promise<Object>} Workflow result
 */
export const getWorkflowResult = async (workflowId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/workflow/result/${workflowId}`);
    
    if (!response.ok) {
      if (response.status === 202) {
        return { status: 'running', message: 'Workflow still running' };
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error getting workflow result:', error);
    throw error;
  }
};

/**
 * Check if the backend API is healthy
 * @returns {Promise<Object>} Health status
 */
export const checkBackendHealth = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/health`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error checking backend health:', error);
    return { 
      status: 'unhealthy', 
      error: error.message,
      oracle_db_available: false,
      llm_available: false 
    };
  }
};

/**
 * List all workflows
 * @returns {Promise<Object>} List of workflows
 */
export const listWorkflows = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/workflows`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error listing workflows:', error);
    throw error;
  }
};

/**
 * Poll workflow status until completion
 * @param {string} workflowId - ID of the workflow to poll
 * @param {number} pollInterval - Polling interval in milliseconds (default: 2000)
 * @param {number} maxAttempts - Maximum polling attempts (default: 30)
 * @returns {Promise<Object>} Final workflow result
 */
export const pollWorkflowUntilComplete = async (workflowId, pollInterval = 2000, maxAttempts = 30) => {
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    try {
      const status = await getWorkflowStatus(workflowId);
      
      if (status.status === 'completed' || status.status === 'failed') {
        // Get the final result
        const result = await getWorkflowResult(workflowId);
        return result;
      }
      
      // Wait before next poll
      await new Promise(resolve => setTimeout(resolve, pollInterval));
      attempts++;
      
    } catch (error) {
      console.error(`Error polling workflow ${workflowId}:`, error);
      attempts++;
      
      if (attempts >= maxAttempts) {
        throw new Error(`Max polling attempts reached for workflow ${workflowId}`);
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, pollInterval));
    }
  }
  
  throw new Error(`Workflow ${workflowId} did not complete within the expected time`);
};

/**
 * Execute workflow and wait for completion
 * @param {string} workflowName - Name of the workflow
 * @param {Array} components - Array of workflow components
 * @param {Object} inputData - Input data for the workflow
 * @param {string} userQuery - Optional user query
 * @returns {Promise<Object>} Final workflow result
 */
export const executeWorkflowAndWait = async (workflowName, components, inputData = null, userQuery = null) => {
  try {
    // Start the workflow
    const startResponse = await executeWorkflow(workflowName, components, inputData, userQuery);
    
    if (!startResponse.workflow_id) {
      throw new Error('No workflow ID returned from backend');
    }
    
    console.log(`Workflow ${startResponse.workflow_id} started, waiting for completion...`);
    
    // Poll until completion
    const result = await pollWorkflowUntilComplete(startResponse.workflow_id);
    
    console.log(`Workflow ${startResponse.workflow_id} completed:`, result);
    return result;
    
  } catch (error) {
    console.error('Error executing workflow and waiting:', error);
    throw error;
  }
};

// Helper function to format workflow components for the backend
export const formatComponentsForBackend = (agents) => {
  return agents.map(agent => ({
    id: agent.id,
    type: agent.type,
    source: agent.source,
    componentType: agent.componentType || 'Node',
    position: agent.position,
    config1: agent.config1 || null,
    config2: agent.config2 || null
  }));
};

// Example usage functions for common workflow patterns
export const executeInvestmentAdvisorWorkflow = async (userQuery = "Analyze investment portfolio") => {
  const components = [
    { id: 1, type: 'Market Data Input', source: 'Real-time Market Feed', componentType: 'Graph Input', position: { left: 50, top: 50 } },
    { id: 2, type: 'Risk Analysis', source: 'Risk Assessment Engine', componentType: 'Node', position: { left: 300, top: 50 } },
    { id: 3, type: 'Portfolio Strategy', source: 'AI Portfolio Optimizer', componentType: 'Decision Node', position: { left: 550, top: 50 } },
    { id: 4, type: 'Investment Recommendation', source: 'Advisory Output', componentType: 'Graph Output', position: { left: 750, top: 50 } }
  ];
  
  return await executeWorkflowAndWait('Investment Advisor Agent', components, { market_focus: 'balanced' }, userQuery);
};

export const executeBankingConciergeWorkflow = async (customerRequest = "Check account balance") => {
  const components = [
    { id: 1, type: 'Customer Request', source: 'Customer Input Channel', componentType: 'Graph Input', position: { left: 50, top: 50 } },
    { id: 2, type: 'Intent Recognition', source: 'NLP Processing', componentType: 'Node', position: { left: 300, top: 50 } },
    { id: 3, type: 'Fraud Check', source: 'Fraud Detection AI', componentType: 'Decision Node', position: { left: 550, top: 50 } },
    { id: 4, type: 'Account Services', source: 'Banking Operations', componentType: 'Node', position: { left: 300, top: 200 } },
    { id: 5, type: 'Response Output', source: 'Customer Response', componentType: 'Graph Output', position: { left: 750, top: 125 } }
  ];
  
  return await executeWorkflowAndWait('Banking Concierge', components, { priority: 'high' }, customerRequest);
};

export default {
  executeWorkflow,
  getWorkflowStatus,
  getWorkflowResult,
  checkBackendHealth,
  listWorkflows,
  pollWorkflowUntilComplete,
  executeWorkflowAndWait,
  formatComponentsForBackend,
  executeInvestmentAdvisorWorkflow, 
  executeBankingConciergeWorkflow
};
