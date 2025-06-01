import React, { useState, useEffect } from 'react';
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

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px; /* Add spacing between text and video */
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%; /* Set the width of the video */
`;

const StockTicker = () => {
  const [formData, setFormData] = useState({
    stock: '',
    shares: '',
    cacheOption: 'True Cache',
    customerId: '', // Will be set after fetching customerIds
  });

  const [isCollapsed, setIsCollapsed] = useState(true);
  const [stockList, setStockList] = useState([]);
  const [lastAction, setLastAction] = useState(null); // { ticker: 'AAPL', action: 'buy' | 'sell' }
  const [customerIds, setCustomerIds] = useState([]);

  useEffect(() => {
    // Fetch stock list
    fetch('https://oracleai-financial.org/financial/stockticker')
      .then(res => res.json())
      .then(data => setStockList(data))
      .catch(() => setStockList([]));

    // Fetch customer IDs from accounts endpoint
    fetch('https://oracleai-financial.org/financial/accounts')
      .then(res => res.json())
      .then(data => {
        const ids = Array.from(new Set(data.map(acc => acc.CUSTOMER_ID))).filter(Boolean);
        setCustomerIds(ids);
        // Set default customerId if not already set
        setFormData(f => ({ ...f, customerId: ids[0] || '' }));
      })
      .catch(() => setCustomerIds([]));
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleStockSubmit = (e, action) => {
    e.preventDefault();
    const payload = {
      customerId: formData.customerId,
      ticker: formData.stock,
      quantity: Number(formData.shares),
      purchasePrice: Number(formData.shares),
      action, // "buy" or "sell"
    };

    fetch('https://oracleai-financial.org/financial/stockbuyorsell', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          setLastAction({ ticker: formData.stock, action });
          // Refresh ticker after purchase/sale
          fetch('https://oracleai-financial.org/financial/stockticker')
            .then(res => res.json())
            .then(data => setStockList(data))
            .catch(() => setStockList([]));
          alert(`${action === 'buy' ? 'Purchase' : 'Sale'} successful!`);
        } else {
          alert(`${action === 'buy' ? 'Purchase' : 'Sale'} failed: ${data.message || 'Please try again.'}`);
        }
      })
      .catch((error) => {
        console.error('Error:', error);
        alert('An error occurred. Please try again.');
      });
  };

  return (
    <PageContainer>
      <h2>Process: Stock ticker and buy/sell stock</h2>
      <h2>Tech: True Cache</h2>
      <h2>Reference: NYSE</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <CollapsibleContent>
            <TextContent>
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
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Oracle Event Hub for event streaming</li>
                <li>Integrate with Kafka APIs for seamless event processing</li>
                <li>Leverage Oracle Database for advanced analytics</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Unlike Redis which has its own API, True Cache uses SQL and so no application modifications are required</li>
              </ul>
            </TextContent>
            <VideoWrapper>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/E1pOaCkd_PM"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: '1px solid #444' }}
              ></iframe>
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      <TickerContainer>
        <TickerText>
          <TickerContent>
            {stockList.map(stock => {
              let arrow = '';
              if (
                lastAction &&
                lastAction.ticker === stock.TICKER
              ) {
                arrow = lastAction.action === 'buy' ? ' ▲' : ' ▼';
              }
              return `${stock.TICKER}: $${Number(stock.CURRENT_PRICE).toFixed(2)}${arrow}`;
            }).join(' | ')}
          </TickerContent>
          <TickerContent>
            {stockList.map(stock => {
              let arrow = '';
              if (
                lastAction &&
                lastAction.ticker === stock.TICKER
              ) {
                arrow = lastAction.action === 'buy' ? ' ▲' : ' ▼';
              }
              return `${stock.TICKER}: $${Number(stock.CURRENT_PRICE).toFixed(2)}${arrow}`;
            }).join(' | ')}
          </TickerContent>
        </TickerText>
      </TickerContainer>

      <Form>
        <Label htmlFor="customerId">Customer</Label>
        <Select
          id="customerId"
          name="customerId"
          value={formData.customerId}
          onChange={handleChange}
          required
        >
          <option value="" disabled>
            Select a customer
          </option>
          {customerIds.map(id => (
            <option key={id} value={id}>
              {id}
            </option>
          ))}
        </Select>

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
          {stockList.map(stock => (
            <option key={stock.TICKER} value={stock.TICKER}>
              {stock.TICKER}
            </option>
          ))}
        </Select>

        <Label htmlFor="shares">Number Of Shares</Label>
        <Input
          type="number"
          id="shares"
          name="shares"
          value={formData.shares}
          onChange={handleChange}
          placeholder="Enter number of shares"
          required
        />

        {/* Cache Option Radio Buttons */}
        <h4>Cache Option</h4>
        <div>
          <label>
            <input
              type="radio"
              name="cacheOption"
              value="Redis"
              checked={formData.cacheOption === 'Redis'}
              onChange={handleChange}
            />
            Use Redis
          </label>
          <label style={{ marginLeft: '20px' }}>
            <input
              type="radio"
              name="cacheOption"
              value="True Cache"
              checked={formData.cacheOption === 'True Cache'}
              onChange={handleChange}
            />
            Use True Cache
          </label>
        </div>

        {/* Add space above, below, and between buttons */}
        <div style={{ margin: '24px 0 24px 0', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <Button type="button" onClick={(e) => handleStockSubmit(e, 'buy')}>
            Buy
          </Button>
          <Button type="button" onClick={(e) => handleStockSubmit(e, 'sell')}>
            Sell
          </Button>
        </div>

        <p style={{ marginTop: '10px', color: '#1abc9c', fontSize: '14px' }}>
          Buy/sell affects stock value
        </p>
      </Form>
    </PageContainer>
  );
};

export default StockTicker;
