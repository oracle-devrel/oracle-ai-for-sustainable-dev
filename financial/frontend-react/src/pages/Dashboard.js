import React from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
`;

const VideoContainer = styled.div`
  margin-top: 20px;
  border: 1px solid #444;
  border-radius: 8px;
  padding: 10px;
  background-color: #1e1e1e;
`;

const LinksContainer = styled.div`
  margin-bottom: 20px;
  a {
    color: #1abc9c;
    text-decoration: none;
    margin-right: 15px;

    &:hover {
      text-decoration: underline;
    }
  }
`;

const DashBoard = () => {
  return (
    <PageContainer>
      <h2>Technical Architecture</h2>

      {/* Links */}
      <LinksContainer>
        <a
          href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
          target="_blank"
          rel="noopener noreferrer"
        >
          Link To Source Code
        </a>
        <a
          href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
          target="_blank"
          rel="noopener noreferrer"
        >
          Link to Workshop
        </a>
      </LinksContainer>

      {/* Walkthrough Video */}
      <VideoContainer>
        <h4>Walkthrough Video:</h4>
        <video
          controls
          width="100%"
          style={{ borderRadius: '8px', border: '1px solid #444' }}
        >
          <source src="/images/financial-app-walkthrough.mov" type="video/mp4" />
          Your browser does not support the video tag.
        </video>
      </VideoContainer>
    </PageContainer>
  );
};

export default DashBoard;
