import React, { useState, useEffect } from 'react';
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

const ContentContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 20px;
`;

const Form = styled.form`
  flex: 2;
  display: flex;
  flex-direction: column;
  max-width: 800px;
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

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
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

const RadioLabel = styled.label`
  display: block;
  margin-bottom: 8px;
  color: ${bankerText};
`;

const Button = styled.button`
  padding: 10px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
  margin-bottom: 8px;
  font-weight: bold;
  &:hover {
    background-color: ${bankerBg};
  }
`;

const Section = styled.div`
  margin-bottom: 24px;
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

const DevPanel = styled.div`
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background: ${bankerPanel};
  padding: 24px;
  margin-bottom: 32px;
  color: ${bankerText};
`;

const Messaging = () => {
  const BASE_URL = 'https://oracleai-financial.org/financial';
  const ACCOUNT_FETCH_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL || 'http://localhost:8080';

  const [formData, setFormData] = useState({
    orderId: '',
    amount: '',
    fromAccount: '',
    nftDrop: '',
    messagingOption: 'Kafka (backed by TxEventQ) with Oracle Database',
    crashOption: 'noCrash',
  });

  const [inventoryForm, setInventoryForm] = useState({
    nftDrop: '',
    amount: '',
  });

  const [fromAccounts, setFromAccounts] = useState([]);
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  const [txnCrashOption, setTxnCrashOption] = useState('');
  const [txnCrashResult, setTxnCrashResult] = useState('');
  const [orderResult, setOrderResult] = useState('');
  const [inventoryResult, setInventoryResult] = useState('');
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [topicName, setTopicName] = useState('');
  const [topicResult, setTopicResult] = useState('');

  useEffect(() => {
    const fetchFromAccounts = async () => {
      try {
        const response = await fetch(`${ACCOUNT_FETCH_URL}/accounts`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setFromAccounts(data.items || []);
      } catch (error) {
        console.error('Error fetching from accounts:', error);
      }
    };
    fetchFromAccounts();
  }, [BASE_URL]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleInventoryFormChange = (e) => {
    const { name, value } = e.target;
    setInventoryForm({ ...inventoryForm, [name]: value });
  };

  // Inventory actions (now in their own form)
  const handleInventoryAction = async (action) => {
    setInventoryResult('');
    setLoading(true);
    let endpoint = '';
    if (action === 'add') endpoint = `${BASE_URL}/inventory/add`;
    if (action === 'remove') endpoint = `${BASE_URL}/inventory/remove`;
    if (action === 'get') endpoint = `${BASE_URL}/inventory/get`;
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nftDrop: inventoryForm.nftDrop, amount: inventoryForm.amount }),
      });
      const text = await response.text();
      const data = text ? JSON.parse(text) : {};
      setInventoryResult(JSON.stringify(data));
    } catch (err) {
      setInventoryResult('❌ Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Order actions (now the main form's buttons)
  const handleOrderAction = async (action) => {
    setOrderResult('');
    setLoading(true);
    let endpoint = '';
    if (action === 'delete') endpoint = `${BASE_URL}/orders/deleteAll`;
    if (action === 'place') endpoint = `${BASE_URL}/orders/place`;
    if (action === 'show') endpoint = `${BASE_URL}/orders/show`;
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });
      const text = await response.text();
      const data = text ? JSON.parse(text) : {};
      setOrderResult(JSON.stringify(data));
    } catch (err) {
      setOrderResult('❌ Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTopic = async (e) => {
    e.preventDefault();
    setTopicResult('');
    setLoading(true);
    try {
      const response = await fetch(`${BASE_URL}/admin/create-topic`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topicName }),
      });
      const text = await response.text();
      const data = text ? JSON.parse(text) : {};
      setTopicResult(JSON.stringify(data));
    } catch (err) {
      setTopicResult('❌ Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageContainer>
      <h2>Process: Purchase Assets/NFTs</h2>
      <h2>Tech: Kafka and TxEventQ</h2>
      <h2>Reference: FSGBU</h2>

      {/* Developer Details Collapsible Panel */}
      <DevPanel>
        <ToggleButton
          type="button"
          onClick={() => setIsCollapsed(!isCollapsed)}
        >
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, marginRight: '20px' }}>
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
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial/brokerage-transfer-kafka"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Kafka and TxEventQ for messaging</li>
                <li>Ensure consistency across distributed systems with no duplicate delivery or lost messages</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Only Oracle Database has a built-in messaging engine (TxEventQ) which allows database and messaging operations in the same local transaction</li>
                <li>TxEventQ can be used via Kafka API, JMS, PL/SQL and via any language</li>
              </ul>
              <Section>
                <h4>Create Kafka Topic (one time call to setup, ie not needed if app is already running)</h4>
                <form
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    marginBottom: '16px'
                  }}
                  onSubmit={handleCreateTopic}
                >
                  <Label htmlFor="topicName" style={{ marginBottom: 0 }}>Topic Name</Label>
                  <Input
                    type="text"
                    id="topicName"
                    name="topicName"
                    value={topicName}
                    onChange={e => setTopicName(e.target.value)}
                    placeholder="Enter topic name"
                    style={{ width: 220, marginBottom: 0 }}
                    required
                  />
                  <Button type="submit" disabled={loading}>Create Topic</Button>
                </form>
                {topicResult && (
                  <div style={{
                    background: bankerPanel,
                    color: bankerText,
                    border: `1px solid ${bankerAccent}`,
                    borderRadius: "8px",
                    padding: "16px",
                    marginTop: "12px",
                    whiteSpace: "pre-wrap"
                  }}>
                    <strong>Topic Result:</strong>
                    <div>{topicResult}</div>
                  </div>
                )}
              </Section>
            </div>
            <div style={{ flex: 1, marginLeft: '20px', textAlign: 'center' }}>
              <img
                src="/images/mongopostgreskafka_vs_OracleAQ.png"
                alt="Mongo/Postgres/Kafka vs Oracle AQ"
                style={{ width: '100%', borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
              />
            </div>
          </div>
        )}
      </DevPanel>

      <ContentContainer>
        <Form>
          {/* Order Id field */}
          <Label htmlFor="orderId">Order Id</Label>
          <Input
            type="text"
            id="orderId"
            name="orderId"
            value={formData.orderId || ""}
            onChange={handleChange}
            placeholder="Enter order id"
          />

          {/* From Account dropdown */}
          <Label htmlFor="fromAccount">From Account</Label>
          <Select
            id="fromAccount"
            name="fromAccount"
            value={formData.fromAccount}
            onChange={handleChange}
            required
          >
            <option value="" disabled>
              Select an account
            </option>
            {fromAccounts.map((account) => (
              <option key={account.ACCOUNT_ID} value={account.ACCOUNT_ID}>
                {account.ACCOUNT_ID}
              </option>
            ))}
          </Select>

          {/* NFT to purchase */}
          <Label htmlFor="nftDrop">NFT to purchase</Label>
          <Select
            id="nftDrop"
            name="nftDrop"
            value={formData.nftDrop || ""}
            onChange={handleChange}
            required
          >
            <option value="" disabled>
              Select an NFT
            </option>
            <option value="real estate X">real estate X</option>
            <option value="digital art Y">digital art Y</option>
            <option value="music rights Z">music rights Z</option>
          </Select>

          {/* Amount field under NFT Drop */}
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

          <Section>
            <h4>Messaging Option</h4>
            <RadioLabel>
              <input
                type="radio"
                name="messagingOption"
                value="Kafka with MongoDB and Postgres"
                checked={formData.messagingOption === "Kafka with MongoDB and Postgres"}
                onChange={handleChange}
              />
              Use Kafka with MongoDB and Postgres
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="messagingOption"
                value="Kafka (backed by TxEventQ) with Oracle Database"
                checked={formData.messagingOption === "Kafka (backed by TxEventQ) with Oracle Database"}
                onChange={handleChange}
              />
              Use Kafka (backed by TxEventQ) with Oracle Database
            </RadioLabel>
          </Section>

          {/* Radio buttons under NFT Drop and Amount */}
          <Section>
            <h4>Transactional Exactly-Once Message Delivery Tests...</h4>
            <RadioLabel>
              <input
                type="radio"
                name="txnCrashOption"
                value="noCrash"
                checked={txnCrashOption === 'noCrash'}
                onChange={e => setTxnCrashOption(e.target.value)}
              />
              No Crash
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="txnCrashOption"
                value="crashOrderAfterInsert"
                checked={txnCrashOption === 'crashOrderAfterInsert'}
                onChange={e => setTxnCrashOption(e.target.value)}
              />
              Crash Order service after Order is inserted (before Order message is sent to Inventory service)
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="txnCrashOption"
                value="crashInventoryAfterOrderMsg"
                checked={txnCrashOption === 'crashInventoryAfterOrderMsg'}
                onChange={e => setTxnCrashOption(e.target.value)}
              />
              Crash Inventory service after Order message is received (before inventory for order is checked)
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="txnCrashOption"
                value="crashInventoryAfterChecked"
                checked={txnCrashOption === 'crashInventoryAfterChecked'}
                onChange={e => setTxnCrashOption(e.target.value)}
              />
              Crash Inventory service after inventory for order is checked (before Inventory status message is sent)
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="txnCrashOption"
                value="crashOrderAfterInventoryMsg"
                checked={txnCrashOption === 'crashOrderAfterInventoryMsg'}
                onChange={e => setTxnCrashOption(e.target.value)}
              />
              Crash Order service after Inventory message is received (before Order status is updated)
            </RadioLabel>
          </Section>

          {/* Order Actions buttons */}
          <Section>
            <h4>Order Actions</h4>
            <Button onClick={() => handleOrderAction('delete')} disabled={loading} type="button">Delete All Orders</Button>
            <Button onClick={() => handleOrderAction('place')} disabled={loading} type="button">Place Order</Button>
            <Button onClick={() => handleOrderAction('show')} disabled={loading} type="button">Show Order</Button>
            {orderResult && (
              <div style={{
                background: bankerPanel,
                color: bankerText,
                border: `1px solid ${bankerAccent}`,
                borderRadius: "8px",
                padding: "16px",
                marginTop: "12px",
                whiteSpace: "pre-wrap"
              }}>
                <strong>Order Result:</strong>
                <div>{orderResult}</div>
              </div>
            )}
          </Section>
        </Form>
      </ContentContainer>

      {/* Inventory Actions as separate form with its own NFT and amount */}
      <Section>
        <h4>Inventory Actions</h4>
        <form
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            marginBottom: '16px'
          }}
          onSubmit={e => e.preventDefault()}
        >
          <Label htmlFor="inventoryNftDrop" style={{ marginBottom: 0 }}>NFT</Label>
          <Select
            id="inventoryNftDrop"
            name="nftDrop"
            value={inventoryForm.nftDrop || ""}
            onChange={handleInventoryFormChange}
            required
            style={{ width: 220 }}
          >
            <option value="" disabled>
              Select an NFT
            </option>
            <option value="real estate X">real estate X</option>
            <option value="digital art Y">digital art Y</option>
            <option value="music rights Z">music rights Z</option>
          </Select>
          <Label htmlFor="inventoryAmount" style={{ marginBottom: 0 }}>Amount</Label>
          <Input
            type="number"
            id="inventoryAmount"
            name="amount"
            value={inventoryForm.amount}
            onChange={handleInventoryFormChange}
            placeholder="Enter amount"
            style={{ width: 120, marginBottom: 0 }}
            required
          />
          <Button onClick={() => handleInventoryAction('add')} disabled={loading} type="button">Add Inventory</Button>
          <Button onClick={() => handleInventoryAction('remove')} disabled={loading} type="button">Remove Inventory</Button>
          <Button onClick={() => handleInventoryAction('get')} disabled={loading} type="button">Get Inventory</Button>
        </form>
        {inventoryResult && (
          <div style={{
            background: bankerPanel,
            color: bankerText,
            border: `1px solid ${bankerAccent}`,
            borderRadius: "8px",
            padding: "16px",
            marginTop: "12px",
            whiteSpace: "pre-wrap"
          }}>
            <strong>Inventory Result:</strong>
            <div>{inventoryResult}</div>
          </div>
        )}
      </Section>

      {/* Main form result */}
      {result && (
        <div style={{
          background: bankerPanel,
          color: bankerText,
          border: `1px solid ${bankerAccent}`,
          borderRadius: "8px",
          padding: "16px",
          marginTop: "24px",
          whiteSpace: "pre-wrap"
        }}>
          <strong>Result:</strong>
          <div>{result}</div>
        </div>
      )}
    </PageContainer>
  );
};

export default Messaging;
