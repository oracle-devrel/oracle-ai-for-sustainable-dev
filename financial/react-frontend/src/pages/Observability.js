import React from 'react';
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

const Iframe = styled.iframe`
  width: 100%;
  height: calc(50vh - 40px);
  border: none;
  margin-top: 20px;
  border-radius: 8px;
  background: ${bankerPanel};
  border: 1px solid ${bankerAccent};
`;

const Image = styled.img`
  display: block;
  width: 100%;
  max-width: 1200px;
  height: auto;
  margin: 20px auto;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background: ${bankerPanel};
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-top: 20px;
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