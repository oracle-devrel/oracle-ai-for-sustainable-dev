import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
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

const Accounts = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <PageContainer>
      <h2>Account Management</h2>
      <h2>MongoDB/MERN stack</h2>
      <h2>Decimal Point Analytics (DPA)</h2>

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
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html?lab=financial-api"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <h4>Financial Process:</h4>
              <ul>
                <li>Create and query all accounts</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Oracle Database MongoDB adapter to insert accounts using MongoDB application/MERN stack</li>
                <li>Query the accounts using relational/SQL commands from a Java/Spring Boot stack</li>
                <li>This is possible due to the JSON Duality feature</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Only Oracle Database has the ability to read and write the same data/tables using both JSON (and MongoDB API) as well as relational/SQL</li>
              </ul>
              <h4>Contacts:</h4>
              <ul>
                <li>JSON Duality and MongoDB adapter: Julian Dontcheff, Beda Hammerschmidt</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '40%' }}>
              <h4>Walkthrough Video:</h4>
              <video
                controls
                width="100%"
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              >
                <source src="/images/financial-apis.mov" type="video/mp4" />
                Your browser does not support the video tag.
              </video>
            </div>
          </div>
        )}
      </SidePanel>
    </PageContainer>
  );
};

export default Accounts;
