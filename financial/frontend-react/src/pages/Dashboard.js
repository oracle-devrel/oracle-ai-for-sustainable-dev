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
      <h2>Technical architecture and setup (includes Kubernetes, OpenTelemetry Observability, ...)</h2>
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
      <h2>Architecture</h2>
        <img
          src={`${process.env.PUBLIC_URL}/images/architecture.png`}
          alt="Technical Architecture"
        />
      </ImageContainer>
      {/* ER diagram Image */}
      <ImageContainer>
      <h2>Entity Diagram</h2>
        <img
          src={`${process.env.PUBLIC_URL}/images/er_diagram.png`}
          alt="Technical Architecture"
        />
      </ImageContainer>


      {/* Grafana iframe */}
      <VideoContainer>
        <h4>Grafana Dashboard (admin/Welcome12345*):</h4>
        <iframe
          src="https://oracleai-financial.org/grafana"
          title="Grafana Dashboard"
          width="100%"
          height="1000"
          style={{ border: '1px solid #444', borderRadius: '8px', background: '#fff' }}
        ></iframe>
      </VideoContainer>
    </PageContainer>
  );
};

export default DashBoard;
