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
  margin-bottom: 20px;
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

const IframesContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  gap: 20px;
`;

const Image = styled.img`
  width: 100%;
  max-height: 50%;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid ${bankerAccent};
  background: ${bankerPanel};
`;

const TwitchEmbed = styled.div`
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const Notice = styled.div`
  width: 100%;
  color: #e67e22;
  font-weight: bold;
  margin: 12px 0 4px 0;
  text-align: center;
`;

const SpeakWithData = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);

  return (
    <PageContainer>
      <h2>Process: Speak with your financial data</h2>
      <h2>Tech: NL2SQL, Vector Search, Speech AI</h2>
      <h2>Reference: Industrial Scientific</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
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
              <h4>Differentiators:</h4>
              <ul>
                <li>Conduct SQL and Vector searches using natural language</li>
              </ul>
            </TextContent>
            <VideoWrapper>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/NODP_gee53c"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
              ></iframe>
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      <h2>Speak with Your Financial Data</h2>
      <p>
        Use natural language and speech to interact with your financial data.<br />
        Powered by Oracle's NL2SQL, Vector Search, and Speech AI technologies.
      </p>

      <IframesContainer>
        {/* Image */}
        <div style={{ width: '100%', height: 'auto' }}>
          <Image
            src="/images/aiholotwitchsnippet.png"
            alt="AI Holo Twitch Snippet"
          />
        </div>

        {/* Notice above the video player */}
        <Notice>
          THIS FUNCTIONALITY IS CURRENTLY BEING USED AT THE BIGDATA AND AI EVENT - WILL RETURN BY END OF WEEK
        </Notice>

        {/* Video Snippet */}
        <div style={{ width: '100%', height: 'auto' }}>
          <video
            src="/images/aiholotwitchsnippet.mp4"
            autoPlay
            loop
            muted
            style={{
              width: '100%',
              borderRadius: '8px',
              border: `1px solid ${bankerAccent}`,
              background: bankerPanel,
            }}
            onError={e => {
              e.target.poster = '';
              e.target.style.display = 'none';
            }}
          >
            Your browser does not support the video tag.
          </video>
        </div>

        {/* Twitch Embed */}
        <TwitchEmbed>
          <iframe
            src="https://player.twitch.tv/?channel=aiholo&parent=localhost"
            style={{ height: '100%', width: '100%' }}
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
