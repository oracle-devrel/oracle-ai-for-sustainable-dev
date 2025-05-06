import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
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

const Form = styled.form`
  display: flex;
  flex-direction: column; /* Stack form elements vertically */
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid #444; /* Darker border */
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the form */
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #ffffff; /* Light text */
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555; /* Darker border */
  border-radius: 4px;
  background-color: #2c2c2c; /* Darker input background */
  color: #ffffff; /* Light text */
`;

const RadioLabel = styled.label`
  display: block;
  margin-bottom: 8px;
  color: #ffffff; /* Light text */
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

const ATM = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [formData, setFormData] = useState({
    amount: '',
    transactionType: '',
    language: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert(
      `Transaction submitted successfully! Type: ${formData.transactionType}, Language: ${formData.language}`
    );
  };

  return (
    <PageContainer>
    <h2>Deposit/withdraw money (ATM)</h2>
    <h2>Polyglot</h2>
    <h2>Java, JS, Python, .NET, Go, Rust</h2>
      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, marginRight: '20px' }}>
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
                <li>Deposit or withdraw money</li>
                <li>Check account balance</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Polyglot implementation: Java, JS, Python, .NET, Go, Rust</li>
                <li>Ensure secure transactions and detect/prevent with Blockchain Tables</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>---</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '70%' }}>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/8Tgmy74A4Bg"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              ></iframe>
            </div>
          </div>
        )}
      </SidePanel>

      <Form onSubmit={handleSubmit}>
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

        <h4>Select a transaction type:</h4>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="deposit"
            checked={formData.transactionType === 'deposit'}
            onChange={handleChange}
          />
          Deposit
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="withdraw"
            checked={formData.transactionType === 'withdraw'}
            onChange={handleChange}
          />
          Withdraw
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="balance"
            checked={formData.transactionType === 'balance'}
            onChange={handleChange}
          />
          Balance Inquiry
        </RadioLabel>

        <h4>Select a language for the transaction:</h4>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Java (Spring Boot)"
            checked={formData.language === 'Java (Spring Boot)'}
            onChange={handleChange}
          />
          Java (Spring Boot)
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="JS"
            checked={formData.language === 'JS'}
            onChange={handleChange}
          />
          JS
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Python"
            checked={formData.language === 'Python'}
            onChange={handleChange}
          />
          Python
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value=".NET"
            checked={formData.language === '.NET'}
            onChange={handleChange}
          />
          .NET
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Go"
            checked={formData.language === 'Go'}
            onChange={handleChange}
          />
          Go
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Rust"
            checked={formData.language === 'Rust'}
            onChange={handleChange}
          />
          Rust
        </RadioLabel>

        <Button type="submit">Submit</Button>
      </Form>
    </PageContainer>
  );
};

export default ATM;
