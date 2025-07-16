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
          fontSize: '1.22rem',
          marginBottom: 32,
          marginTop: 24,
          lineHeight: 1.7,
          letterSpacing: '0.01em',
          background: bankerPanel,
          borderRadius: 12,
          padding: '32px 32px 24px 32px',
          boxShadow: `0 2px 12px 0 rgba(0,0,0,0.10)`,
        }}
      >
        <div style={{ fontWeight: 'bold', color: bankerAccent, fontSize: '1.35rem', marginBottom: 16 }}>
          This workshop is aimed at developers and those wanting to know more about dev aspects of Oracle Database tech.<br />
          Each page contains the app itself and...
        </div>
        <ul style={{ marginTop: 12, marginBottom: 24, paddingLeft: 32 }}>
          <li style={{ marginBottom: 10 }}>Can be run by itself or in conjunction with other pages (ie entire app is modular and microservices architecture)</li>
          <li style={{ marginBottom: 10 }}>Can be run in Kubernetes or standalone</li>
          <li style={{ marginBottom: 10 }}>
            Contains a <b>Developer Details</b> button which when clicked, expands to show:
            <ul style={{ marginTop: 8, marginBottom: 8, paddingLeft: 28 }}>
              <li style={{ marginBottom: 6 }}>a walkthrough video</li>
              <li style={{ marginBottom: 6 }}>code and workshop links</li>
              <li style={{ marginBottom: 6 }}>developer notes and differentiators</li>
              <li style={{ marginBottom: 6 }}>links to corresponding backend dev tools/env (such as Grafana, Jupyter/OML4Py, Graph Studio, AI Agents Vector search backend)</li>
            </ul>
          </li>
          <li style={{ marginBottom: 10 }}>Contains relevant code snippets on the right hand side that change depending on form options, etc. selected</li>
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

      {/* GitHub Source Code Repos Section */}
      <div style={{ 
        marginTop: '40px', 
        textAlign: 'center',
        padding: '20px',
        background: bankerPanel,
        borderRadius: '8px',
        border: `1px solid ${bankerAccent}`
      }}>
        <h3 style={{ 
          color: bankerAccent, 
          marginBottom: '20px',
          fontSize: '1.4rem'
        }}>
          GitHub Source Code Repos for This App
        </h3>
        <img
          src={`${process.env.PUBLIC_URL}/images/bit.ly_oraclefinancialgithub.png`}
          alt="GitHub Source Code Repos"
          style={{
            maxWidth: '50%',
            height: 'auto',
            borderRadius: '8px',
            border: `1px solid ${bankerAccent}`
          }}
        />
      </div>
    </PageContainer>
  );
};

export default FinancialStoryBoard;
