import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
`;

const TableHeader = styled.th`
  border: 1px solid #444; /* Darker border */
  padding: 8px;
  background-color: #1e1e1e; /* Darker background for the header */
  color: #ffffff; /* Light text for visibility */
  text-align: left;
`;

const TableCell = styled.td`
  border: 1px solid #444; /* Darker border */
  padding: 8px;
  color: #ffffff; /* Light text for table cells */
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

const APIs = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [accountData, setAccountData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Fetch data from the API
    const fetchData = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/'
        );
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setAccountData(data.items || []); // Assuming the data is in the `items` array
        setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Dynamically generate table headers based on the keys of the first object in the data, excluding "links"
  const tableHeaders = accountData.length > 0 
    ? Object.keys(accountData[0]).filter((key) => key !== 'links') 
    : [];

  // Helper function to safely render table cell content
  const renderCellContent = (value) => {
    if (typeof value === 'object' && value !== null) {
      return JSON.stringify(value); // Convert objects to JSON strings
    }
    return value !== null && value !== undefined ? value : 'N/A'; // Handle null/undefined values
  };

  return (
    <PageContainer>
      <h2>Publish Financial APIs</h2>
      <h2>Oracle Rest Data Services (ORDS), OpenAPI</h2>
      <h2>Sphere</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, marginRight: '20px' }}>
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
                <li>Access/use financial data or processes from APIs</li>
                <li>Display the largest changes in portfolio</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Oracle Rest Data Services (ORDS) to expose APIs for data and processes</li>
                <li>Automatic OpenAPI is generated for seamless integration</li>
                <li>Expose data or processes in the database with a couple clicks or lines of code</li>
              </ul>
              <h4>Contacts:</h4>
              <ul>
                <li>ORDS: Jeff Smith</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '70%' }}>
              <h4>Walkthrough Video:</h4>
              <video
                controls
                width="100%"
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              >
                <source src="/images/financial-apis.mp4" type="video/mp4" />
                Your browser does not support the video tag.
              </video>
            </div>
          </div>
        )}
      </SidePanel>

      {/* Account Data Table */}
      {loading ? (
        <p>Loading account data...</p>
      ) : error ? (
        <p style={{ color: 'red' }}>Error: {error}</p>
      ) : (
        <Table>
          <thead>
            <tr>
              {tableHeaders.map((header) => (
                <TableHeader key={header}>{header}</TableHeader>
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
