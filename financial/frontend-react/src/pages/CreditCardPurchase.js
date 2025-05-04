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

const Form = styled.form`
  width: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid #444;
  border-radius: 8px;
  background-color: #1e1e1e;
  margin-bottom: 20px; /* Add spacing below the form */
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #ffffff;
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2c2c2c;
  color: #ffffff;
`;

const Button = styled.button`
  padding: 10px;
  background-color: #1abc9c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background-color: #16a085;
  }
`;

const CreditCardPurchase = () => {
  const [formData, setFormData] = useState({
    cardNumber: '',
    amount: '',
    description: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert(`Transaction submitted successfully! Card: ${formData.cardNumber}, Amount: ${formData.amount}`);
  };

  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <PageContainer>
      <h2>Make purchases and visualize fraud</h2>
      <h2>Globally Distributed DB, OML, Spatial</h2>
      <h2>AMEX</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div>
            <div>
              <a
                href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier-financial/index.html"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#1abc9c', textDecoration: 'none' }}
              >
                Click here for workshop lab and further information
              </a>
            </div>
            <div>
              <a
                href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: '#1abc9c', textDecoration: 'none' }}
              >
                Direct link to source code on GitHub
              </a>
            </div>
            <h4>Financial Process:</h4>
            <ul>
              <li>Manage credit card transactions with Globally Distributed Database</li>
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
            <h4>Contacts:</h4>
            <ul>
              <li>Globally Distributed Database: Shefali Bhargava, Pankaj Chandiramani, OML: Mark Hornick, Sherry LaMonica, Spatial: David Lapp</li>
            </ul>
          </div>
        )}
      </SidePanel>

      {/* Form Section */}
      <Form onSubmit={handleSubmit}>
        <Label htmlFor="cardNumber">Card Number</Label>
        <Input
          type="text"
          id="cardNumber"
          name="cardNumber"
          value={formData.cardNumber}
          onChange={handleChange}
          placeholder="Enter card number"
          required
        />

        <Label htmlFor="amount">Amount</Label>
        <Input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          placeholder="Enter amount"
          required
        />

        <Label htmlFor="description">Description</Label>
        <Input
          type="text"
          id="description"
          name="description"
          value={formData.description}
          onChange={handleChange}
          placeholder="Enter transaction description"
        />

        <Button type="submit">Submit</Button>
      </Form>

      {/* Images */}
      <Image src="/images/spatial-suspicious.png" alt="Spatial Suspicious Transactions" />
      <Image src="/images/spatial-agg-03.png" alt="Spatial Aggregated Data" />
    </PageContainer>
  );
};

export default CreditCardPurchase;
