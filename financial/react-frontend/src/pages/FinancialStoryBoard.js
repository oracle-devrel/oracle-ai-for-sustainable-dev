import React from 'react';
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
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: ${bankerPanel};
  color: ${bankerText};
`;

const TableHeader = styled.th`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  text-align: left;
  background-color: ${bankerAccent};
  color: ${bankerText};
`;

const TableCell = styled.td`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  color: ${bankerText};
`;

const VideoWrapper = styled.div`
  margin-top: 32px;
  text-align: center;
  iframe {
    border-radius: 8px;
    border: 1px solid ${bankerAccent};
    background: ${bankerPanel};
    width: 900px;
    height: 506px;
    max-width: 100%;
  }
`;

const FinancialStoryBoard = () => {
  // List of JavaScript files in the "pages" directory
  const pages = [
    { name: 'Architecture and Setup', notes: 'Kubernetes, OpenTelemetry Observability, Grafana, Kubeview' },
    { name: 'APIs', notes: 'ORDS' },
    { name: 'Accounts', notes: 'MERN, MongoDB API for Oracle Database and JSON Duality' },
    { name: 'ATM', notes: 'Polyglot: Java, JavaScript, Python, .NET, Go, Ruby' },
    { name: 'Transfer', notes: 'MicroTx and Lock-free' },
    { name: 'Suspicious Purchases', notes: 'Globally Distributed Database, Spatial' },
    { name: 'Circular payments', notes: 'Graph' },
    { name: 'Transfer to brokerage', notes: 'Kafka TxEventQ' },
    { name: 'Stock ticker', notes: 'True Cache' },
    { name: 'Financial Insights', notes: 'AI Agents, MCP, Vector search' },
    { name: 'Speak with data', notes: 'Speech AI, Select AI' }
  ];

  return (
    <PageContainer>
      <div
        style={{
          fontWeight: 'bold',
          color: bankerAccent,
          marginBottom: 8,
          fontSize: '1.5rem', 
        }}
      >
        Please send any feedback to devreldb_ww@oracle.com
      </div>
      <div
        style={{
          color: bankerText,
          fontSize: '1rem',
          marginBottom: 8,
        }}
      >
        Each page has:
        <ul style={{ marginTop: 8, marginBottom: 24, paddingLeft: 24 }}>
          <li>a <b>Developer Details</b> button</li>
          <li>when clicked, expands to show:</li>
          <ul>
            <li>a walkthrough video</li>
            <li>code and workshop links</li>
            <li>etc.</li>
          </ul>
        </ul>
      </div>
      <Table>
        <thead>
          <tr>
            <TableHeader>Page</TableHeader>
            <TableHeader>Tech Used</TableHeader>
          </tr>
        </thead>
        <tbody>
          {pages.map((page, index) => (
            <tr key={index}>
              <TableCell>{page.name}</TableCell>
              <TableCell>{page.notes}</TableCell>
            </tr>
          ))}
        </tbody>
      </Table>
      {/* <VideoWrapper>
        <iframe
          width="900"
          height="506"
          src="https://www.youtube.com/embed/-o16D-Sq-mU"
          title="YouTube video player"
          frameBorder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        ></iframe>
      </VideoWrapper> */}
    </PageContainer>
  );
};

export default FinancialStoryBoard;
