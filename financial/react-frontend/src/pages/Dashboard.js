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
  min-height: 100vh;
  padding: 20px;
`;

const VideoContainer = styled.div`
  margin-top: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  padding: 10px;
  background-color: ${bankerPanel};
`;

const LinksContainer = styled.div`
  margin-bottom: 20px;
  a {
    color: ${bankerAccent};
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
    border: 1px solid ${bankerAccent};
    background: ${bankerPanel};
  }
`;

const CollapsibleSection = styled.div`
  margin-bottom: 24px;
  background: ${bankerPanel};
  border-radius: 8px;
  border: 1px solid ${bankerAccent};
  padding: 12px;
`;

const CollapsibleContent = styled.div`
  margin-top: 8px;
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

const DashBoard = () => {
  const [showCollapsible, setShowCollapsible] = useState(false);

  return (
    <PageContainer>
      <h2>Technical architecture and setup (includes Kubernetes, OpenTelemetry Observability, ...)</h2>
      <ul>
        <li>All pages in this app have corresponding labs in the workshop which is linked to from the page itself.</li>
        <li>Source for all pages is available in the GitHub repo which is linked to from the page itself.</li>
        <li>All pages/labs share the same schema but can be run a la carte as there are no interdependencies.</li>
        <li>All pages are microservices that can be run either in Kubernetes or standalone.</li>
      </ul>

      {/* Collapsible Section */}
      <CollapsibleSection>
        <ToggleButton
          type="button"
          onClick={() => setShowCollapsible(!showCollapsible)}
        >
          {showCollapsible ? 'Hide Developer Details' : 'Show Developer Details'}
        </ToggleButton>
        {showCollapsible && (
          <CollapsibleContent>
            <a
              href="https://oracleai-financial.org/grafana"
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: bankerAccent, textDecoration: 'underline' }}
            >
              Open Grafana Dashboard (admin/Welcome12345*)
            </a>
            <div style={{ marginTop: 24 }}>
              <iframe
                width="900"
                height="506"
                src="https://www.youtube.com/embed/Uuj9MCiYTPo"
                title="Developer Details"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}`, background: '#fff' }}
              ></iframe>
            </div>
          </CollapsibleContent>
        )}
      </CollapsibleSection>

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
      {/* tracing diagram Image */}
      <ImageContainer>
        <h2>End-to-end Tracing INTO Database</h2>
        <img
          src={`${process.env.PUBLIC_URL}/images/tracing.png`}
          alt="Technical Architecture"
        />
      </ImageContainer>
      {/* three pillars diagram Image */}
      <ImageContainer>
        <h2>"Single Pane" view across metrics, logs, and tracing</h2>
        <img
          src={`${process.env.PUBLIC_URL}/images/3pillars.png`}
          alt="Technical Architecture"
        />
      </ImageContainer>
    </PageContainer>
  );
};

export default DashBoard;
