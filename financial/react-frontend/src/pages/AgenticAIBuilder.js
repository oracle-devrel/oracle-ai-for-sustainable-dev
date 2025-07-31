import React, { useState } from 'react';
import styled from 'styled-components';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

const PageContainer = styled.div`
  background-color: ${bankerBg};
  color: ${bankerText};
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
`;

const MainContainer = styled.div`
  display: flex;
  gap: 20px;
  flex: 1;
`;

const LeftPanel = styled.div`
  flex: 2;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
  padding: 20px;
  position: relative;
  min-height: 600px;
`;

const RightPanel = styled.div`
  flex: 1;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
  padding: 20px;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

const EditButton = styled.button`
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 10px;
  font-weight: bold;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  
  &:hover {
    background-color: ${bankerBg};
  }
`;

const EditForm = styled.div`
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
  background-color: ${bankerBg};
`;

const Label = styled.label`
  font-weight: bold;
  color: ${bankerText};
  margin-bottom: 4px;
`;

const Select = styled.select`
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
  font-size: 14px;

  option {
    background-color: #406080;
    color: ${bankerText};
  }
`;

const Input = styled.input`
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
  font-size: 14px;
  width: 100%;
`;

const Button = styled.button`
  padding: 12px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  margin-top: 16px;

  &:hover {
    background-color: ${bankerBg};
  }
`;

const AgentBox = styled.div`
  position: absolute;
  width: 200px;
  height: 100px;
  border: 2px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerBg};
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: move;
  user-select: none;
  transition: ${props => props.isDragging ? 'none' : 'all 0.3s ease'};

  &:hover {
    transform: ${props => props.isDragging ? 'none' : 'translateY(-2px)'};
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }

  ${props => props.isDragging && `
    z-index: 1000;
    transform: rotate(5deg);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.6);
  `}
`;

const DecisionBox = styled.div`
  position: absolute;
  width: 120px;
  height: 120px;
  border: 2px solid ${bankerAccent};
  background-color: ${bankerBg};
  transform: rotate(45deg);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: move;
  user-select: none;
  transition: ${props => props.isDragging ? 'none' : 'all 0.3s ease'};

  &:hover {
    transform: ${props => props.isDragging ? 'rotate(45deg)' : 'rotate(45deg) translateY(-2px)'};
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }

  ${props => props.isDragging && `
    z-index: 1000;
    transform: rotate(50deg);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.6);
  `}
`;

const DecisionContent = styled.div`
  transform: rotate(-45deg);
  text-align: center;
  font-size: 12px;
  color: ${bankerText};
`;

const CircleBox = styled.div`
  position: absolute;
  width: 100px;
  height: 100px;
  border: 2px solid ${bankerAccent};
  border-radius: 50%;
  background-color: ${bankerBg};
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: move;
  user-select: none;
  transition: ${props => props.isDragging ? 'none' : 'all 0.3s ease'};
  text-align: center;
  font-size: 12px;
  color: ${bankerText};

  &:hover {
    transform: ${props => props.isDragging ? 'none' : 'translateY(-2px)'};
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  }

  ${props => props.isDragging && `
    z-index: 1000;
    transform: rotate(5deg);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.6);
  `}
`;

const EdgeLine = styled.div`
  position: absolute;
  width: 150px;
  height: 3px;
  background-color: ${bankerAccent};
  cursor: move;
  user-select: none;
  transition: ${props => props.isDragging ? 'none' : 'all 0.3s ease'};

  &::after {
    content: '';
    position: absolute;
    right: -8px;
    top: 50%;
    transform: translateY(-50%);
    width: 0;
    height: 0;
    border-left: 8px solid ${bankerAccent};
    border-top: 5px solid transparent;
    border-bottom: 5px solid transparent;
  }

  &:hover {
    transform: ${props => props.isDragging ? 'none' : 'translateY(-2px)'};
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  ${props => props.isDragging && `
    z-index: 1000;
    transform: rotate(5deg);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
  `}
`;

const AgentType = styled.div`
  font-weight: bold;
  font-size: 14px;
  color: ${bankerAccent};
  margin-bottom: 4px;
`;

const AgentSource = styled.div`
  font-size: 12px;
  color: ${bankerText};
  text-align: center;
`;

const Arrow = styled.div`
  position: absolute;
  background-color: ${bankerAccent};
  z-index: 10;
  transform-origin: 0 0;

  &::after {
    content: '';
    position: absolute;
    right: -8px;
    top: 50%;
    transform: translateY(-50%);
    width: 0;
    height: 0;
    border-left: 8px solid ${bankerAccent};
    border-top: 5px solid transparent;
    border-bottom: 5px solid transparent;
  }
`;

const AgenticAIBuilder = () => {
  const [agents, setAgents] = useState([]);
  const [formData, setFormData] = useState({
    type: '',
    source: ''
  });
  const [dragState, setDragState] = useState({
    isDragging: false,
    draggedAgentId: null,
    dragOffset: { x: 0, y: 0 }
  });
  const [editingAgent, setEditingAgent] = useState(null);
  const [editFormData, setEditFormData] = useState({});

  const componentTypes = [
    'Node',
    'Decision Node', 
    'Edge',
    'Graph Input',
    'Graph Output'
  ];

  // Dropdown options for different component types
  const componentOptions = {
    'Node': {
      types: ['Processing Node', 'Data Node', 'API Node', 'Service Node', 'Transform Node'],
      sources: ['Oracle Database', 'REST API', 'Vector Database', 'Document Store', 'Real-time Feed'],
      actions: ['Process', 'Transform', 'Validate', 'Filter', 'Aggregate']
    },
    'Decision Node': {
      types: ['If-Then', 'Switch', 'Route', 'Filter', 'Validate'],
      conditions: ['Greater Than', 'Less Than', 'Equals', 'Contains', 'Exists', 'Is Null'],
      actions: ['Continue', 'Stop', 'Redirect', 'Log', 'Alert']
    },
    'Edge': {
      types: ['Data Flow', 'Control Flow', 'Error Flow', 'Success Flow'],
      styles: ['Solid', 'Dashed', 'Dotted', 'Bold'],
      directions: ['Forward', 'Backward', 'Bidirectional']
    },
    'Graph Input': {
      types: ['User Input', 'File Input', 'API Input', 'Database Input', 'Stream Input'],
      formats: ['JSON', 'XML', 'CSV', 'Binary', 'Text'],
      validation: ['Required', 'Optional', 'Validated', 'Sanitized']
    },
    'Graph Output': {
      types: ['File Output', 'API Response', 'Database Write', 'Stream Output', 'Display'],
      formats: ['JSON', 'XML', 'CSV', 'PDF', 'HTML'],
      destinations: ['Local', 'Remote', 'Database', 'Queue', 'Display']
    }
  };

  const agentTypes = [
    'Data Analyst',
    'Financial Advisor',
    'Risk Assessor',
    'Compliance Monitor',
    'Market Researcher',
    'Customer Service',
    'Fraud Detective',
    'Portfolio Manager'
  ];

  const agentSources = [
    'Oracle Database',
    'REST API',
    'Vector Database',
    'Document Store',
    'Real-time Feed',
    'External Service',
    'Machine Learning Model',
    'Knowledge Base'
  ];

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formData.type && formData.source) {
      const newAgent = {
        id: Date.now(),
        type: formData.type,
        source: formData.source,
        position: calculatePosition(agents.length)
      };
      
      setAgents([...agents, newAgent]);
      setFormData({ type: '', source: '' });
    }
  };

  const addComponent = (componentType) => {
    const newComponent = {
      id: Date.now(),
      type: componentType,
      source: getDefaultSource(componentType),
      position: calculatePosition(agents.length),
      componentType: componentType
    };
    
    setAgents([...agents, newComponent]);
  };

  const getDefaultSource = (componentType) => {
    switch (componentType) {
      case 'Node': return 'Processing Node';
      case 'Decision Node': return 'Decision Logic';
      case 'Edge': return 'Connection';
      case 'Graph Input': return 'Input Data';
      case 'Graph Output': return 'Output Result';
      default: return 'Component';
    }
  };

  const handleEditClick = (e, agent) => {
    e.stopPropagation();
    setEditingAgent(agent);
    setEditFormData({
      type: agent.type,
      source: agent.source,
      // Add default values for new fields based on component type
      subType: componentOptions[agent.componentType || agent.type]?.types?.[0] || '',
      config1: '',
      config2: ''
    });
  };

  const handleEditFormChange = (e) => {
    const { name, value } = e.target;
    setEditFormData({
      ...editFormData,
      [name]: value
    });
  };

  const handleEditFormSubmit = (e) => {
    e.preventDefault();
    if (editingAgent) {
      setAgents(prevAgents => 
        prevAgents.map(agent => 
          agent.id === editingAgent.id 
            ? { ...agent, ...editFormData }
            : agent
        )
      );
      setEditingAgent(null);
      setEditFormData({});
    }
  };

  const cancelEdit = () => {
    setEditingAgent(null);
    setEditFormData({});
  };

  const exampleWorkflows = [
    'Investment advisor agent',
    'Banking concierge including fraud detection', 
    'Spatial digital twins and logistics optimizations',
    'Disaster response',
    'Content Management System'
  ];

  const loadExampleWorkflow = (workflowName) => {
    let exampleAgents = [];
    
    switch (workflowName) {
      case 'Investment advisor agent':
        exampleAgents = [
          { id: Date.now() + 1, type: 'Market Data Input', source: 'Real-time Market Feed', componentType: 'Graph Input', position: { left: 50, top: 50 } },
          { id: Date.now() + 2, type: 'Risk Analysis', source: 'Risk Assessment Engine', componentType: 'Node', position: { left: 300, top: 50 } },
          { id: Date.now() + 3, type: 'Portfolio Strategy', source: 'AI Portfolio Optimizer', componentType: 'Decision Node', position: { left: 550, top: 50 } },
          { id: Date.now() + 4, type: 'Investment Recommendation', source: 'Advisory Output', componentType: 'Graph Output', position: { left: 750, top: 50 } }
        ];
        break;
        
      case 'Banking concierge including fraud detection':
        exampleAgents = [
          { id: Date.now() + 1, type: 'Customer Request', source: 'Customer Input Channel', componentType: 'Graph Input', position: { left: 50, top: 50 } },
          { id: Date.now() + 2, type: 'Intent Recognition', source: 'NLP Processing', componentType: 'Node', position: { left: 300, top: 50 } },
          { id: Date.now() + 3, type: 'Fraud Check', source: 'Fraud Detection AI', componentType: 'Decision Node', position: { left: 550, top: 50 } },
          { id: Date.now() + 4, type: 'Account Services', source: 'Banking Operations', componentType: 'Node', position: { left: 300, top: 200 } },
          { id: Date.now() + 5, type: 'Response Output', source: 'Customer Response', componentType: 'Graph Output', position: { left: 750, top: 125 } }
        ];
        break;
        
      case 'Spatial digital twins and logistics optimizations':
        exampleAgents = [
          { id: Date.now() + 1, type: 'IoT Sensors', source: 'Real-time Location Data', componentType: 'Graph Input', position: { left: 50, top: 50 } },
          { id: Date.now() + 2, type: 'Digital Twin Model', source: 'Spatial Mapping Engine', componentType: 'Node', position: { left: 300, top: 50 } },
          { id: Date.now() + 3, type: 'Route Optimization', source: 'AI Path Planning', componentType: 'Decision Node', position: { left: 550, top: 50 } },
          { id: Date.now() + 4, type: 'Logistics Control', source: 'Fleet Management', componentType: 'Node', position: { left: 300, top: 200 } },
          { id: Date.now() + 5, type: 'Optimized Routes', source: 'Navigation Output', componentType: 'Graph Output', position: { left: 750, top: 125 } }
        ];
        break;
        
      case 'Disaster response':
        exampleAgents = [
          { id: Date.now() + 1, type: 'Emergency Alert', source: 'Alert System Input', componentType: 'Graph Input', position: { left: 50, top: 50 } },
          { id: Date.now() + 2, type: 'Threat Assessment', source: 'Risk Evaluation AI', componentType: 'Node', position: { left: 300, top: 50 } },
          { id: Date.now() + 3, type: 'Response Priority', source: 'Emergency Triage', componentType: 'Decision Node', position: { left: 550, top: 50 } },
          { id: Date.now() + 4, type: 'Resource Allocation', source: 'Resource Management', componentType: 'Node', position: { left: 300, top: 200 } },
          { id: Date.now() + 5, type: 'Action Plan', source: 'Response Coordination', componentType: 'Graph Output', position: { left: 750, top: 125 } }
        ];
        break;
        
      case 'Content Management System':
        exampleAgents = [
          { id: Date.now() + 1, type: 'Content Input', source: 'Document Upload', componentType: 'Graph Input', position: { left: 50, top: 50 } },
          { id: Date.now() + 2, type: 'Content Analysis', source: 'AI Content Processor', componentType: 'Node', position: { left: 300, top: 50 } },
          { id: Date.now() + 3, type: 'Approval Workflow', source: 'Content Review', componentType: 'Decision Node', position: { left: 550, top: 50 } },
          { id: Date.now() + 4, type: 'Content Storage', source: 'Database Management', componentType: 'Node', position: { left: 300, top: 200 } },
          { id: Date.now() + 5, type: 'Published Content', source: 'Content Delivery', componentType: 'Graph Output', position: { left: 750, top: 125 } }
        ];
        break;
        
      default:
        exampleAgents = [];
    }
    
    setAgents(exampleAgents);
    setEditingAgent(null);
    setEditFormData({});
  };

  const calculatePosition = (index) => {
    const cols = 3;
    const boxWidth = 200;
    const boxHeight = 100;
    const gapX = 80;
    const gapY = 120;
    
    const col = index % cols;
    const row = Math.floor(index / cols);
    
    return {
      left: col * (boxWidth + gapX) + 20,
      top: row * (boxHeight + gapY) + 20
    };
  };

  const handleMouseDown = (e, agentId) => {
    const agent = agents.find(a => a.id === agentId);
    const rect = e.currentTarget.getBoundingClientRect();
    const containerRect = e.currentTarget.parentElement.getBoundingClientRect();
    
    setDragState({
      isDragging: true,
      draggedAgentId: agentId,
      dragOffset: {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
      }
    });
    
    e.preventDefault();
  };

  const handleMouseMove = (e) => {
    if (!dragState.isDragging || !dragState.draggedAgentId) return;
    
    const containerRect = e.currentTarget.getBoundingClientRect();
    const newX = e.clientX - containerRect.left - dragState.dragOffset.x;
    const newY = e.clientY - containerRect.top - dragState.dragOffset.y;
    
    // Constrain to container bounds
    const boundedX = Math.max(0, Math.min(newX, containerRect.width - 200));
    const boundedY = Math.max(0, Math.min(newY, containerRect.height - 100));
    
    setAgents(prevAgents => 
      prevAgents.map(agent => 
        agent.id === dragState.draggedAgentId 
          ? { ...agent, position: { left: boundedX, top: boundedY } }
          : agent
      )
    );
  };

  const handleMouseUp = () => {
    setDragState({
      isDragging: false,
      draggedAgentId: null,
      dragOffset: { x: 0, y: 0 }
    });
  };

  const renderComponent = (agent) => {
    const commonProps = {
      key: agent.id,
      isDragging: dragState.draggedAgentId === agent.id,
      style: {
        left: agent.position.left,
        top: agent.position.top
      },
      onMouseDown: (e) => handleMouseDown(e, agent.id)
    };

    const editButton = (
      <EditButton onClick={(e) => handleEditClick(e, agent)}>
        ✎
      </EditButton>
    );

    switch (agent.componentType || agent.type) {
      case 'Decision Node':
        return (
          <DecisionBox {...commonProps}>
            {editButton}
            <DecisionContent>
              <div style={{ fontWeight: 'bold', fontSize: '10px' }}>{agent.type}</div>
              <div style={{ fontSize: '8px' }}>{agent.source}</div>
            </DecisionContent>
          </DecisionBox>
        );
      
      case 'Graph Input':
      case 'Graph Output':
        return (
          <CircleBox {...commonProps}>
            {editButton}
            <div>
              <div style={{ fontWeight: 'bold', fontSize: '10px' }}>{agent.type}</div>
              <div style={{ fontSize: '8px' }}>{agent.source}</div>
            </div>
          </CircleBox>
        );
      
      case 'Edge':
        return (
          <EdgeLine {...commonProps}>
            {editButton}
          </EdgeLine>
        );
      
      default: // Node and legacy agent types
        return (
          <AgentBox {...commonProps}>
            {editButton}
            <AgentType>{agent.type}</AgentType>
            <AgentSource>{agent.source}</AgentSource>
          </AgentBox>
        );
    }
  };

  const renderArrows = () => {
    if (agents.length < 2) return null;

    return agents.slice(1).map((agent, index) => {
      const fromAgent = agents[index];
      const toAgent = agent;
      
      // Calculate connection points from center of boxes
      const fromX = fromAgent.position.left + 100; // center of from box
      const fromY = fromAgent.position.top + 50;   // center of from box
      const toX = toAgent.position.left + 100;     // center of to box
      const toY = toAgent.position.top + 50;       // center of to box
      
      // Calculate arrow properties
      const deltaX = toX - fromX;
      const deltaY = toY - fromY;
      const length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      const angle = Math.atan2(deltaY, deltaX) * (180 / Math.PI);

      // Adjust start and end points to connect at box edges
      const boxRadius = 100; // approximate distance from center to edge
      const startX = fromX + (boxRadius * Math.cos(angle * Math.PI / 180));
      const startY = fromY + (boxRadius * Math.sin(angle * Math.PI / 180));
      const adjustedLength = length - (2 * boxRadius);

      return (
        <Arrow
          key={`arrow-${fromAgent.id}-${toAgent.id}`}
          style={{
            left: startX,
            top: startY,
            width: Math.max(adjustedLength, 10),
            height: '3px',
            transform: `rotate(${angle}deg)`,
            transformOrigin: '0 50%'
          }}
        />
      );
    });
  };

  return (
    <PageContainer>
      <h2>Process: Build AI Agent Workflows</h2>
      <h2>Tech: Agentic AI Builder</h2>
      <h2>Reference: Oracle AI Platform</h2>

      <MainContainer>
        <LeftPanel
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        >
          <h3 style={{ marginTop: 0, color: bankerAccent }}>Agentic AI Workflow</h3>
          {agents.length === 0 && (
            <div style={{ 
              display: 'flex', 
              justifyContent: 'center', 
              alignItems: 'center', 
              height: '200px',
              color: '#888',
              fontSize: '18px'
            }}>
              Add your first component to get started
            </div>
          )}
          
          {renderArrows()}
          
          {agents.map((agent) => renderComponent(agent))}
        </LeftPanel>

        <RightPanel>
          <h3 style={{ marginTop: 0, color: bankerAccent }}>Components</h3>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {componentTypes.map((componentType) => (
              <Button 
                key={componentType}
                type="button"
                onClick={() => addComponent(componentType)}
              >
                Add {componentType}
              </Button>
            ))}
          </div>

          {editingAgent && (
            <EditForm>
              <h4 style={{ margin: '0 0 16px 0', color: bankerAccent }}>
                Edit {editingAgent.componentType || editingAgent.type}
              </h4>
              <Form onSubmit={handleEditFormSubmit}>
                <div>
                  <Label>Component Name</Label>
                  <Input
                    name="type"
                    value={editFormData.type}
                    onChange={handleEditFormChange}
                    placeholder="Enter component name..."
                    required
                  />
                </div>

                <div>
                  <Label>Description</Label>
                  <Select
                    name="source"
                    value={editFormData.source}
                    onChange={handleEditFormChange}
                    required
                  >
                    <option value="">Select description...</option>
                    {(componentOptions[editingAgent.componentType || editingAgent.type]?.sources || 
                      componentOptions[editingAgent.componentType || editingAgent.type]?.conditions || 
                      componentOptions[editingAgent.componentType || editingAgent.type]?.styles ||
                      componentOptions[editingAgent.componentType || editingAgent.type]?.formats || 
                      ['Custom Description']).map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </Select>
                </div>

                {componentOptions[editingAgent.componentType || editingAgent.type]?.actions && (
                  <div>
                    <Label>Action</Label>
                    <Select
                      name="config1"
                      value={editFormData.config1}
                      onChange={handleEditFormChange}
                    >
                      <option value="">Select action...</option>
                      {componentOptions[editingAgent.componentType || editingAgent.type].actions.map((action) => (
                        <option key={action} value={action}>
                          {action}
                        </option>
                      ))}
                    </Select>
                  </div>
                )}

                <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                  <Button type="submit">Save Changes</Button>
                  <Button type="button" onClick={cancelEdit} style={{ backgroundColor: '#666' }}>
                    Cancel
                  </Button>
                </div>
              </Form>
            </EditForm>
          )}

          {agents.length > 0 && (
            <div style={{ marginTop: '24px', padding: '16px', border: `1px solid ${bankerAccent}`, borderRadius: '4px' }}>
              <h4 style={{ margin: '0 0 12px 0', color: bankerAccent }}>Workflow Summary</h4>
              <div style={{ fontSize: '14px' }}>
                <strong>Total Components:</strong> {agents.length}
                <br />
                <strong>Latest:</strong> {agents[agents.length - 1]?.type}
              </div>
            </div>
          )}

          <div style={{ marginTop: '24px', padding: '16px', border: `1px solid ${bankerAccent}`, borderRadius: '4px' }}>
            <h4 style={{ margin: '0 0 16px 0', color: bankerAccent }}>Examples</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {exampleWorkflows.map((workflow) => (
                <Button
                  key={workflow}
                  type="button"
                  onClick={() => loadExampleWorkflow(workflow)}
                  style={{ fontSize: '12px', padding: '8px' }}
                >
                  {workflow}
                </Button>
              ))}
            </div>
          </div>
        </RightPanel>
      </MainContainer>
    </PageContainer>
  );
};

export default AgenticAIBuilder;
