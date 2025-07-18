import React, { useState, useEffect } from 'react';
import styled, { keyframes } from 'styled-components';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

const PageContainer = styled.div`
  background-color: ${bankerBg};
  color: ${bankerText};
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto;
`;

const TickerContainer = styled.div`
  width: 100%;
  background-color: ${bankerPanel};
  overflow: hidden;
  white-space: nowrap;
  border: 1px solid ${bankerAccent};
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
  color: ${bankerAccent};
  font-weight: bold;
  white-space: nowrap;
`;

const TickerContent = styled.div`
  display: inline-block;
  padding-right: 50px;
`;

const Form = styled.form`
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: ${bankerText};
`;

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
`;

const Button = styled.button`
  padding: 10px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  &:hover {
    background-color: ${bankerBg};
  }
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-bottom: 20px;
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

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px;
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%;
`;

const TwoColumnContainer = styled.div`
  display: flex;
  gap: 32px;
  width: 100%;
  @media (max-width: 900px) {
    flex-direction: column;
    gap: 0;
  }
`;

const LeftColumn = styled.div`
  flex: 1;
  min-width: 320px;
`;

const RightColumn = styled.div`
  flex: 1;
  min-width: 320px;
  background: ${bankerPanel};
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  padding: 20px;
  color: ${bankerText};
  font-family: 'Fira Mono', 'Consolas', 'Menlo', monospace;
  font-size: 0.98rem;
  white-space: pre-wrap;
  overflow-x: auto;
`;

const CodeTitle = styled.div`
  font-weight: bold;
  color: ${bankerAccent};
  margin-bottom: 12px;
`;

const StockTicker = () => {
  const [formData, setFormData] = useState({
    stock: '',
    shares: '',
    cacheOption: 'True Cache',
    customerId: '',
  });

  const [isCollapsed, setIsCollapsed] = useState(true);
  const [stockList, setStockList] = useState([]);
  const [lastAction, setLastAction] = useState(null);
  const [customerIds, setCustomerIds] = useState([]);

  useEffect(() => {
    fetch('https://oracleai-financial.org/financial/truecache/stockticker')
      .then(res => res.json())
      .then(data => setStockList(data))
      .catch(() => setStockList([]));

    fetch('https://oracleai-financial.org/financial/accounts')
      .then(res => res.json())
      .then(data => {
        const ids = Array.from(new Set(data.map(acc => acc.CUSTOMER_ID))).filter(Boolean);
        setCustomerIds(ids);
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
      action,
    };

    fetch('https://oracleai-financial.org/financial/truecache/stockbuyorsell', {
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
          fetch('https://oracleai-financial.org/financial/truecache/stockticker')
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

  const codeSnippets = {
    "Redis": `// Redis (Node.js example)
const redis = require("redis");
const client = redis.createClient();

client.set("AAPL", 189.50);
client.get("AAPL", (err, price) => {
  console.log("AAPL price:", price);
});
`,
    "True Cache": `-- True Cache (Oracle Database, SQL)
MERGE INTO stock_cache s
USING (SELECT :ticker AS ticker, :price AS price FROM dual) d
ON (s.ticker = d.ticker)
WHEN MATCHED THEN
  UPDATE SET s.price = d.price
WHEN NOT MATCHED THEN
  INSERT (ticker, price) VALUES (d.ticker, d.price);

SELECT price FROM stock_cache WHERE ticker = :ticker;
`
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
                  style={{ color: bankerAccent, textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none' }}
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
                src="https://www.youtube.com/embed/qHVYXagpAC0?t=1"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
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

      <TwoColumnContainer>
        <LeftColumn>
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

            <div style={{ margin: '24px 0 24px 0', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <Button type="button" onClick={(e) => handleStockSubmit(e, 'buy')}>
                Buy
              </Button>
              <Button type="button" onClick={(e) => handleStockSubmit(e, 'sell')}>
                Sell
              </Button>
            </div>

            <p style={{ marginTop: '10px', color: bankerAccent, fontSize: '14px' }}>
              Buy/sell affects stock value
            </p>
          </Form>
        </LeftColumn>
        <RightColumn>
          <CodeTitle>Sample Cache Source Code</CodeTitle>
          <code>
            {codeSnippets[formData.cacheOption]}
          </code>
        </RightColumn>
      </TwoColumnContainer>
    </PageContainer>
  );
};

export default StockTicker;
