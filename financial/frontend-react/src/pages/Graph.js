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

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px; /* Add spacing between text and video */
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%; /* Set the width of the video */
`;

const GraphContainer = styled.div`
  width: 100%;
  height: 400px; /* Set a fixed height for the graph container */
  margin-top: 20px;
  border: 1px solid #444; /* Optional border for better visibility */
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the graph container */
`;

const GenerateButton = styled.button`
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #0074D9;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background-color: #005bb5;
  }
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

const Graph = () => {
  const [cy, setCy] = useState(null);
  const [isCollapsed, setIsCollapsed] = useState(true);

  useEffect(() => {
    const cyInstance = cytoscape({
      container: document.getElementById('cy'), // Reference to the graph container
      elements: [],
      style: [
        { selector: 'node', style: { 'label': 'data(id)', 'background-color': '#0074D9' } },
        { selector: 'edge', style: { 'width': 2, 'line-color': '#ccc' } }
      ],
      layout: { name: 'grid' }
    });

    setCy(cyInstance);

    return () => {
      cyInstance.destroy(); // Clean up Cytoscape instance on component unmount
    };
  }, []);

  const generateTransactions = async () => {
    if (!cy) return;
    await generateCircularTransfersAndGraph(cy);
  };

  // Create a transfer from srcAcctId to dstAcctId
  async function createTransfer(srcAcctId, dstAcctId, amount, description) {
    const response = await fetch('https://oracleai-financial.org/financial/transfers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        srcAcctId,
        dstAcctId,
        amount,
        description,
      }),
    });
    return response.json();
  }

  async function fetchGraphData() {
    const accounts = await fetch('https://oracleai-financial.org/financial/accounts').then(res => res.json());
    const transfers = await fetch('https://oracleai-financial.org/financial/transfers').then(res => res.json());
    return { accounts, transfers };
  }

  function updateCytoscapeGraph(cy, accounts, transfers) {
    cy.elements().remove(); // Clear old graph

    // Add nodes
    accounts.forEach(acc => {
      cy.add({ data: { id: String(acc.ACCOUNT_ID), label: acc.ACCOUNT_NAME || acc.ACCOUNT_ID } });
    });

    // Add edges
    transfers.forEach(tx => {
      cy.add({
        data: {
          id: `txn${tx.TXN_ID}`,
          source: String(tx.SRC_ACCT_ID),
          target: String(tx.DST_ACCT_ID),
          label: tx.DESCRIPTION || ''
        }
      });
    });

    cy.layout({ name: 'cose' }).run();
  }

  async function generateCircularTransfersAndGraph(cy) {
    // 1. Fetch all accounts (assume at least 20 exist)
    const accounts = await fetch('https://oracleai-financial.org/financial/accounts').then(res => res.json());
    const accountIds = accounts.slice(0, 20).map(acc => acc.ACCOUNT_ID);

    // 2. Create 20 transfers in a circle
    for (let i = 0; i < 20; i++) {
      const src = accountIds[i];
      const dst = accountIds[(i + 1) % 20];
      await createTransfer(src, dst, Math.floor(Math.random() * 1000), `Transfer ${i + 1}`);
    }

    // 3. Fetch updated graph data and update Cytoscape
    const { accounts: updatedAccounts, transfers } = await fetchGraphData();
    updateCytoscapeGraph(cy, updatedAccounts, transfers);
  }

  return (
    <PageContainer>
      <h2>Process: Detect Money Laundering</h2>
      <h2>Tech : Graph</h2>
      <h2>Reference: Certegy</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <>
            <CollapsibleContent>
              <TextContent>
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
                <h4>Differentiators:</h4>
                <ul>
                  <li>Supports PGQL, SQL, JSONPath, Rest, and Vector​</li>
                </ul>
              </TextContent>
              <VideoWrapper>
                <h4>Walkthrough Video:</h4>
                <iframe
                  width="100%"
                  height="315"
                  src="https://www.youtube.com/embed/E1pOaCkd_PM"
                  title="YouTube video player"
                  frameBorder="0"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                  style={{ borderRadius: '8px', border: '1px solid #444' }}
                ></iframe>
              </VideoWrapper>
            </CollapsibleContent>
            {/* Add Graph Studio link below collapsible content */}
            <div style={{ marginTop: '24px', width: '100%' }}>
              <h4>Oracle Graph Studio:</h4>
              <a
                href="https://IJ1TYZIR3WPWLPE-FINANCIALDB.adb.eu-frankfurt-1.oraclecloudapps.com/graphstudio/"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#1abc9c', textDecoration: 'none', fontWeight: 'bold', fontSize: '1.1em' }}
              >
                Open Oracle Graph Studio in a new tab
              </a>
            </div>
          </>
        )}
      </SidePanel>

      {/* Cytoscape Graph */}
      <GraphContainer id="cy"></GraphContainer>

      {/* Generate Transactions Button */}
      <GenerateButton onClick={generateTransactions}>Generate transactions to see corresponding graph</GenerateButton>
    </PageContainer>
  );
};

export default Graph;
