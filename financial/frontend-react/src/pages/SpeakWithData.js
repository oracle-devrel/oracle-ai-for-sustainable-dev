import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const IframesContainer = styled.div`
  display: flex;
  flex-direction: column; /* Stack elements vertically */
  width: 100%;
  height: 100%; /* Use full height */
  gap: 20px; /* Space between the elements */
`;

const Image = styled.img`
  width: 100%; /* Make the image take the full width */
  max-height: 50%; /* Limit the height to half the container */
  object-fit: contain; /* Maintain aspect ratio */
  border: none;
`;

const TwitchEmbed = styled.div`
  flex: 1; /* Allow the Twitch iframe to take the remaining space */
  display: flex;
  align-items: center;
  justify-content: center;
`;

const SidePanel = styled.div`
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  margin-bottom: 20px; /* Add spacing below the side panel */
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

const SpeakWithData = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <PageContainer>

    <h2>Speak with your financial data</h2>
    <h2>NL2SQL, Vector Search, Speech AI</h2>
    <h2>Industrial Scientific</h2>
      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div>
            <h4>Personas:</h4>
            <ul>
              <li>Financial Analyst</li>
              <li>Business Executive</li>
            </ul>
            <h4>Process:</h4>
            <ul>
              <li>Query financial data using natural language</li>
              <li>Generate insights with NL2SQL and Vector Search</li>
              <li>Interact with data using Speech AI</li>
            </ul>
            <h4>Developer Notes:</h4>
            <ul>
              <li>Leverage Oracle AI for natural language processing</li>
              <li>Use Vector Search for semantic data queries</li>
              <li>Integrate Speech AI for voice-based interactions</li>
            </ul>
          </div>
        )}
      </SidePanel>

      <h2>Speak with Your Financial Data</h2>
      <p>
        Use natural language and speech to interact with your financial data.<br />
        Powered by Oracle's NL2SQL, Vector Search, and Speech AI technologies.
      </p>

      <IframesContainer>
        {/* Image */}
        <Image src="/images/aiholopage.png" alt="SpeakWithData" />

        {/* Twitch Embed */}
        <TwitchEmbed>
          <iframe
            src="https://player.twitch.tv/?channel=aiholo&parent=localhost"
            style={{ height: '100%', width: '100%' }} /* Ensure iframe fills its container */
            frameBorder="0"
            allowFullScreen={true}
            title="Twitch Stream"
          ></iframe>
        </TwitchEmbed>
      </IframesContainer>
    </PageContainer>
  );
};

export default SpeakWithData;
