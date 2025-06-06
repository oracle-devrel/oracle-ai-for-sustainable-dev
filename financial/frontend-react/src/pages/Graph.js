import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import cytoscape from 'cytoscape';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

const PageContainer = styled.div`
  background-color: ${bankerBg};
  color: ${bankerText};
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto;
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-top: 20px;
`;

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px;
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%;
`;

const GraphContainer = styled.div`
  width: 100%;
  height: 400px;
  margin-top: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
`;

const GenerateButton = styled.button`
  margin-top: 20px;
  padding: 10px 20px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;

  &:hover {
    background-color: ${bankerBg};
  }
`;

const DangerButton = styled(GenerateButton)`
  background-color: #e74c3c;
  margin-left: 10px;

  &:hover {
    background-color: #c0392b;
  }
`;

const ToggleButton = styled.button`
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 10px;
  font-weight: bold;

  &:hover {
    background-color: ${bankerBg};
  }
`;

const Graph = () => {
  const [cy, setCy] = useState(null);
  const [isCollapsed, setIsCollapsed] = useState(true);

  useEffect(() => {
    const cyInstance = cytoscape({
      container: document.getElementById('cy'),
      elements: [],
      style: [
        { selector: 'node', style: { 'label': 'data(id)', 'background-color': bankerAccent } },
        { selector: 'edge', style: { 'width': 2, 'line-color': '#ccc' } }
      ],
      layout: { name: 'grid' }
    });

    setCy(cyInstance);

    return () => {
      cyInstance.destroy();
    };
  }, []);

  // Plot a single transfer edge
  function plotTransferEdge(cy, tx, index) {
    cy.add({
      data: {
        id: `txn${tx.TXN_ID || `${tx.src}_${tx.dst}_${index}`}`,
        source: String(tx.SRC_ACCT_ID || tx.src),
        target: String(tx.DST_ACCT_ID || tx.dst),
        label: tx.DESCRIPTION || tx.description || ''
      }
    });
  }

  // Create a transfer from srcAcctId to dstAcctId and plot it
  async function createAndPlotTransfer(cy, srcAcctId, dstAcctId, amount, description, index) {
    const response = await fetch('https://oracleai-financial.org/financial/createtransfer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        srcAcctId,
        dstAcctId,
        amount,
        description,
      }),
    });
    const result = await response.json();
    plotTransferEdge(cy, {
      src: srcAcctId,
      dst: dstAcctId,
      description,
    }, index);
  }

  function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
  }

  // Generate and plot 20 circular transfers one by one
  async function generateCircularTransfersAndGraph(cy) {
    // Clear graph
    cy.elements().remove();

    // Fetch and add nodes
    const accounts = await fetch('https://oracleai-financial.org/financial/accounts').then(res => res.json());
    const accountIds = accounts.slice(0, 20).map(acc => acc.ACCOUNT_ID);
    accounts.slice(0, 20).forEach(acc => {
      cy.add({ data: { id: String(acc.ACCOUNT_ID), label: acc.ACCOUNT_NAME || acc.ACCOUNT_ID } });
    });

    // Add random edges
    for (let i = 0; i < 20; i++) {
      let src, dst;
      do {
        src = accountIds[Math.floor(Math.random() * accountIds.length)];
        dst = accountIds[Math.floor(Math.random() * accountIds.length)];
      } while (src === dst);

      await createAndPlotTransfer(cy, src, dst, Math.floor(Math.random() * 1000), `Transfer ${i + 1}`, i);

      // Run a quick layout after each edge is added
      cy.layout({ name: 'cose', animate: true, animationDuration: 300 }).run();

      // Optional: add a small delay for visual effect
      await new Promise(res => setTimeout(res, 200));
    }

    // Only run layout once, after all nodes and edges are added
    cy.layout({ name: 'cose' }).run();
  }

  // Clear all transfers in backend and graph
  async function clearAllTransfers() {
    await fetch('https://oracleai-financial.org/financial/cleartransfers', { method: 'POST' });
    if (cy) {
      cy.elements('edge').remove();
      cy.layout({ name: 'cose' }).run();
    }
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
                    style={{ color: bankerAccent, textDecoration: 'none' }}
                  >
                    Click here for workshop lab and further information
                  </a>
                </div>
                <div>
                  <a
                    href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ color: bankerAccent, textDecoration: 'none' }}
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
                  style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
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
                style={{ color: bankerAccent, textDecoration: 'none', fontWeight: 'bold', fontSize: '1.1em' }}
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
      <GenerateButton onClick={() => generateCircularTransfersAndGraph(cy)}>
        Generate transactions to see corresponding graph
      </GenerateButton>
      <DangerButton onClick={clearAllTransfers}>
        Clear all transfer history
      </DangerButton>
    </PageContainer>
  );
};

export default Graph;
