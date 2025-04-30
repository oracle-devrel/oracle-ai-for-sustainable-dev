import React from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const Iframe = styled.iframe`
  width: 100%;
  height: calc(50vh - 40px); /* Adjust height to fit within the page */
  border: none;
  margin-top: 20px;
`;

const Image = styled.img`
  display: block;
  width: 100%; /* Make the image take the full width of the container */
  max-width: 1200px; /* Limit the maximum width */
  height: auto; /* Maintain aspect ratio */
  margin: 20px auto;
  border: 1px solid #444; /* Optional border for better visibility */
  border-radius: 8px;
`;

const SidePanel = styled.div`
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  margin-top: 20px; /* Add spacing above the side panel */
`;

const Observability = () => {
  return (
    <PageContainer>
      <h2>DevOps: Kubernetes and Observability</h2>
      <h2>Oracle OpenTelemetry logs and metrics exporters and Trace exporter IN the database</h2>
      <h2>LOLC</h2>

      {/* Process SidePanel */}
      <SidePanel>
        <h4>Process:</h4>
        <ul>
          <li>Monitor Kubernetes clusters and microservices</li>
          <li>Visualize traces, logs, and metrics in Grafana</li>
          <li>Leverage Oracle OpenTelemetry exporters for seamless observability</li>
        </ul>
      </SidePanel>

      {/* Developers SidePanel */}
      <SidePanel>
        <h4>Developers:</h4>
        <ul>
          <li>Use Oracle OpenTelemetry exporters for logs, metrics, and traces</li>
          <li>The only database that provides tracing observability directly into the database</li>
          <li>Visualize observability data in Grafana with minimal setup</li>
        </ul>
      </SidePanel>

      {/* Uncomment the iframe if needed */}
      {/* <Iframe src="http://localhost:8088/grafana" title="Kubernetes and OpenTelemetry Observability" /> */}
      <Image src="/images/grafanatrace.png" alt="Grafana Trace Visualization" />
    </PageContainer>
  );
};

export default Observability;