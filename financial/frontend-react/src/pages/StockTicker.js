import React, { useState } from 'react';
import styled, { keyframes } from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const TickerContainer = styled.div`
  width: 100%;
  background-color: #1e1e1e; /* Darker background for the ticker */
  overflow: hidden;
  white-space: nowrap;
  border: 1px solid #444; /* Darker border */
  padding: 10px 0;
  position: relative;
`;

const scrollAnimation = keyframes`
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
`;

const TickerText = styled.div`
  display: flex;
  animation: ${scrollAnimation} 15s linear infinite;
  color: #1abc9c; /* Accent color for the ticker text */
  font-weight: bold;
  white-space: nowrap;
`;

const TickerContent = styled.div`
  display: inline-block;
  padding-right: 50px; /* Add space between the duplicate text */
`;

const Form = styled.form`
  max-width: 600px;
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

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555; /* Darker border */
  border-radius: 4px;
  background-color: #2c2c2c; /* Darker select background */
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

const StockTicker = () => {
  const [formData, setFormData] = useState({
    stock: '',
    shares: '',
  });

  const [isCollapsed, setIsCollapsed] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleStockSubmit = (e, action) => {
    e.preventDefault();
    fetch('http://oracleai-financial.org/stock', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ...formData, action }),
    })
      .then((response) => {
        if (response.ok) {
          alert(`${action === 'buy' ? 'Purchase' : 'Sale'} successful!`);
        } else {
          alert(`${action === 'buy' ? 'Purchase' : 'Sale'} failed. Please try again.`);
        }
      })
      .catch((error) => {
        console.error('Error:', error);
        alert('An error occurred. Please try again.');
      });
  };

  return (
    <PageContainer>
    <h2>Stock ticker and buy/sell stock</h2>
    <h2>True Cache</h2>
    <h2>NYSE</h2>
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
              <li>Stream real-time stock prices using Oracle Streaming Service</li>
              <li>Analyze stock trends with Oracle Database</li>
              <li>Visualize stock performance in real-time</li>
            </ul>
            <h4>Developer Notes:</h4>
            <ul>
              <li>Use Oracle Event Hub for event streaming</li>
              <li>Integrate with Kafka APIs for seamless event processing</li>
              <li>Leverage Oracle Database for advanced analytics</li>
            </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Unlike Redis which has it's own API, True Cache uses SQL and so no application modifications are required</li>
              </ul>
          

          </div>
        )}
      </SidePanel>


      <TickerContainer>
        <TickerText>
          <TickerContent>
            AAPL: $170.50 ▲1.25% | GOOGL: $2,850.30 ▼0.45% | AMZN: $3,450.10 ▲0.75% | MSFT: $310.20 ▲0.95% | TSLA: $1,050.00 ▼1.10%
          </TickerContent>
          <TickerContent>
            AAPL: $170.50 ▲1.25% | GOOGL: $2,850.30 ▼0.45% | AMZN: $3,450.10 ▲0.75% | MSFT: $310.20 ▲0.95% | TSLA: $1,050.00 ▼1.10%
          </TickerContent>
        </TickerText>
      </TickerContainer>

      <Form>
        <Label htmlFor="stock">Stock</Label>
        <Select
          id="stock"
          name="stock"
          value={formData.stock}
          onChange={handleChange}
          required
        >
          <option value="" disabled>
            Select a stock
          </option>
          <option value="AAPL">Apple (AAPL)</option>
          <option value="GOOGL">Google (GOOGL)</option>
          <option value="AMZN">Amazon (AMZN)</option>
          <option value="MSFT">Microsoft (MSFT)</option>
          <option value="TSLA">Tesla (TSLA)</option>
        </Select>

        <Label htmlFor="shares">Number of Shares</Label>
        <Input
          type="number"
          id="shares"
          name="shares"
          value={formData.shares}
          onChange={handleChange}
          placeholder="Enter number of shares"
          required
        />

        <Button type="button" onClick={(e) => handleStockSubmit(e, 'buy')}>
          Buy
        </Button>
        <Button type="button" onClick={(e) => handleStockSubmit(e, 'sell')}>
          Sell
        </Button>
      </Form>

    </PageContainer>
  );
};

export default StockTicker;
