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

const ImageContainer = styled.div`
  margin-bottom: 20px;
  text-align: center;

  img {
    max-width: 100%;
    border-radius: 8px;
    border: 1px solid #444;
  }
`;

const DashBoard = () => {
  return (
    <PageContainer>
      <h2>Technical architecture and setup</h2>
      <ul>
        <li>All pages in this app have corresponding labs in the workshop which is linked to from the page itself.</li>
        <li>Source for all pages is available in the GitHub repo which is linked to from the page itself.</li>
        <li>All pages/labs share the same schema but can be run a la carte as there are no interdependencies.</li>
        <li>All pages are microservices that can be run either in Kubernetes or standalone.</li>
      </ul>

      {/* Links */}
      <LinksContainer>
        <a
          href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
          target="_blank"
          rel="noopener noreferrer"
        >
          Link To Source Code
        </a>
        <br />
        <br />
        <a
          href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
          target="_blank"
          rel="noopener noreferrer"
        >
          Link to Workshop
        </a>
      </LinksContainer>

      {/* Architecture Image */}
      <ImageContainer>
        <img
          src={`${process.env.PUBLIC_URL}/images/architecture.png`}
          alt="Technical Architecture"
        />
      </ImageContainer>

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
