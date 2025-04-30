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

const Image = styled.img`
  display: block;
  max-width: 90%; /* Make the images responsive */
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

const CreditCardPurchase = () => {
  return (
    <PageContainer>
      <h2>Fraud Alerts on Credit Card Purchases</h2>
      <p>
        Credit card purchases are conducted using Oracle Globally Distributed Database<br/>
        Fraud detection and visualization is conducted using OML4Py (Python) and Spatial<br/>
        Money Laundering is detected using Oracle Graph.<br/>
        Events are sent using Knative Eventing and CloudEvents.
      </p>

      {/* Process SidePanel */}
      <SidePanel>
        <h4>Personas:</h4>
        <ul>
          <li>Credit Card User</li>
          <li>Backend Financial Worker</li>
        </ul>
        <h4>Process:</h4>
        <ul>
        <li>Manager credit card transactions with Globally Distributed Database</li>
          <li>Detect suspicious credit card transactions using ML/AI and spatial</li>
          <li>Graph analysis is conducted for money laundering</li>
          <li>Generate fraud alerts in real-time using Knative Eventing and CloudEvents</li>
        </ul>
        <h4>Developer Notes:</h4>
        <ul>
          <li>Leverage Oracle Spatial and Graph for advanced analytics</li>
          <li>Use OML4Py (Python, Jupyter, etc.) for machine learning</li>
          <li>Integrate with Knative Eventing and CloudEvents for real-time event processing</li>
        </ul>
      </SidePanel>

      {/* Developers SidePanel */}

      <Image src="/images/spatial-suspicious.png" alt="Spatial Suspicious Transactions" />
      <Image src="/images/spatial-agg-03.png" alt="Spatial Aggregated Data" />
    </PageContainer>
  );
};

export default CreditCardPurchase;
