import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import cytoscape from 'cytoscape';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const SidePanel = styled.div`
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  margin-top: 20px; /* Add spacing above the side panel */
`;

const ToggleButton = styled.button`
  background-color: #1abc9c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;

  &:hover {
    background-color: #16a085;
  }
`;

const GraphContainer = styled.div`
  width: 100%;
  height: 400px; /* Set a fixed height for the graph container */
  margin-top: 20px;
  border: 1px solid #444; /* Optional border for better visibility */
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the graph container */
`;

const Graph = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  useEffect(() => {
    const cy = cytoscape({
      container: document.getElementById('cy'), // Reference to the graph container
      elements: [
        { data: { id: 'a' } },
        { data: { id: 'b' } },
        { data: { id: 'ab', source: 'a', target: 'b' } }
      ],
      style: [
        { selector: 'node', style: { 'label': 'data(id)', 'background-color': '#0074D9' } },
        { selector: 'edge', style: { 'width': 2, 'line-color': '#ccc' } }
      ],
      layout: { name: 'grid' }
    });

    return () => {
      cy.destroy(); // Clean up Cytoscape instance on component unmount
    };
  }, []);

  return (
    <PageContainer>
      <h2>Detect money laundering</h2>
      <h2>Graph</h2>
      <h2>Certegy</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div>
                   <div>
                <a
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
            <h4>Financial Process:</h4>
            <ul>
              <li>Graph analysis is conducted for money laundering</li>
            </ul>
            <h4>Developer Notes:</h4>
            <ul>
              <li>Leverage Oracle Database Graph</li>
            </ul>
          </div>
        )}
      </SidePanel>

      {/* Cytoscape Graph */}
      <GraphContainer id="cy"></GraphContainer>
    </PageContainer>
  );
};

export default Graph;
