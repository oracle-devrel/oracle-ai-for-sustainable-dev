import React, { useState, useEffect } from 'react';
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
`;

const TableHeader = styled.th`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  text-align: left;
`;

const TableCell = styled.td`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  color: ${bankerText};
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-top: 20px;
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

const CodePanel = styled.div`
  background: ${bankerPanel};
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  padding: 20px;
  color: ${bankerText};
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  font-size: 0.98rem;
  white-space: pre-wrap;
  margin-top: 32px;
  margin-bottom: 32px;
  overflow-x: auto;
`;

const CodeTitle = styled.div`
  font-weight: bold;
  color: ${bankerAccent};
  margin-bottom: 12px;
`;

const APIs = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [accountData, setAccountData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Fetch data for the first table
    const fetchData = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/'
        );
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAccountData(data.items || []);
        setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const tableHeaders = [
    'account_id',
    'account_balance',
    'customer_id',
    'account_name',
    'account_opened_date',
    'account_other_details',
    'account_type',
  ];

  const renderCellContent = (value) => {
    if (typeof value === 'object' && value !== null) {
      return JSON.stringify(value);
    }
    return value !== null && value !== undefined ? value : 'N/A';
  };

  return (
    <PageContainer>
      <h2>Process: Publish Financial APIs</h2>
      <h2>Tech: Oracle Rest Data Services (ORDS), OpenAPI</h2>
      <h2>Reference: Sphere</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, marginRight: '20px' }}>
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
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Oracle Rest Data Services (ORDS) to expose APIs for data and processes</li>
                <li>Automatic OpenAPI is generated for seamless integration</li>
                <li>Expose data or processes in the database with a couple clicks or lines of code</li>
                <li>Automate workflows using REST APIs</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Exposes not just CRUD operations but stored procedures, workflows, event-driven flows, Vector searches, etc.</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '70%' }}>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="615"
                src="https://www.youtube.com/embed/qHVYXagpAC0?t=169"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
              ></iframe>
            </div>
          </div>
        )}
      </SidePanel>

      {/* Code Panel for API Examples */}
      <CodePanel>
        <CodeTitle>ORDS REST API Example</CodeTitle>
        <code>
{`-- Enable ORDS for table
BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'FINANCIAL',
        P_OBJECT      =>  'account_detail',
        P_OBJECT_TYPE      => 'TABLE',
        P_OBJECT_ALIAS      => 'accounts',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;

-- Use the generated REST endpoints

// Get all accounts (GET)
GET https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/

// Get a single account (GET)
GET https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/{account_id}

// Create an account (POST)
POST https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/
Content-Type: application/json
{
  "account_id": "A123",
  "account_name": "My Account",
  "account_type": "checking",
  "customer_id": "C456",
  "account_opened_date": "2024-06-11",
  "account_other_details": "details",
  "account_balance": 1000
}
`}
        </code>
      </CodePanel>

      {/* Account Data Table */}
      <h3>Account Data</h3>
      {loading ? (
        <p>Loading account data...</p>
      ) : error ? (
        <p style={{ color: 'red' }}>Error: {error}</p>
      ) : (
        <Table>
          <thead>
            <tr>
              {tableHeaders.map((header) => (
                <TableHeader key={header}>{header.replace(/_/g, ' ').toUpperCase()}</TableHeader>
              ))}
            </tr>
          </thead>
          <tbody>
            {accountData.map((account, index) => (
              <tr key={index}>
                {tableHeaders.map((header) => (
                  <TableCell key={header}>{renderCellContent(account[header])}</TableCell>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </PageContainer>
  );
};

export default APIs;
