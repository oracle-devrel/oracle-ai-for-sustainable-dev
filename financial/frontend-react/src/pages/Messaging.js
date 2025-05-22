import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
`;

const ContentContainer = styled.div`
  display: flex;
  flex-direction: row; /* Align form and image side by side */
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 20px;
`;

const Form = styled.form`
  flex: 2; /* Take up more space for the form */
  display: flex;
  flex-direction: column; /* Stack form elements vertically */
  max-width: 800px;
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

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555; /* Darker border */
  border-radius: 4px;
  background-color: #2c2c2c; /* Darker select background */
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

const ImageContainer = styled.div`
  flex: 1; /* Take up less space for the image */
  margin-left: 20px; /* Add spacing between the form and the image */
  text-align: center;
`;

const Messaging = () => {
  const BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080'; // Default to localhost if env variable is not set

  const [formData, setFormData] = useState({
    amount: '',
    fromAccount: '',
    toAccount: '',
    messagingOption: 'Kafka with Oracle Database', // Default to "Kafka with Oracle Database"
    crashOption: 'noCrash', // Default to "No Crash"
  });

  const [fromAccounts, setFromAccounts] = useState([]); // State for "From Account" dropdown
  const [toAccounts, setToAccounts] = useState([]); // State for "To Account" dropdown
  const [isCollapsed, setIsCollapsed] = useState(true);

  useEffect(() => {
    // Fetch account IDs for "From Account" dropdown
    const fetchFromAccounts = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/ACCOUNT_DETAIL/'
);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setFromAccounts(data.items || []); // Assuming the data is in the `items` array
      } catch (error) {
        console.error('Error fetching from accounts:', error);
      }
    };

    // Fetch account IDs for "To Account" dropdown
    const fetchToAccounts = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial2/accounts/'
);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setToAccounts(data.items || []); // Assuming the data is in the `items` array
      } catch (error) {
        console.error('Error fetching to accounts:', error);
      }
    };

    fetchFromAccounts();
    fetchToAccounts();
  }, [BASE_URL]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${BASE_URL}/kafka/transfer`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        alert(`Brokerage transfer submitted successfully!`);
      } else {
        const errorText = await response.text(); // Declare and define errorText here
        alert(`Failed to submit transaction: ${errorText}`);
      }
    } catch (error) {
      console.error('Error submitting transaction:', error);
      alert('An error occurred while submitting the transaction.');
    }
  };

  return (
    <PageContainer>
      <h2>Process: Transfer to brokerage accounts</h2>
      <h2>Tech: Kafka and TxEventQ</h2>
      <h2>Reference: FSGBU</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
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
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial/brokerage-transfer-kafka"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                  Direct link to source code on GitHub
                </a>
              </div>
              <h4>Financial Process:</h4>
              <ul>
                <li>Transfer to brokerage accounts</li>
              </ul>
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
            </div>
            <div style={{ flexShrink: 0, width: '40%' }}>
  <h4>Walkthrough Video:</h4>
  <iframe
    width="100%"
    height="315"
    src="https://www.youtube.com/embed/3p8X-i1y43U"
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

      {/* Form and Image Section */}
      <ContentContainer>
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
              <option key={account.account_id} value={account.account_id}>
                {account.account_id}
              </option>
            ))}
          </Select>

          <Label htmlFor="toAccount">To Account</Label>
          <Select
            id="toAccount"
            name="toAccount"
            value={formData.toAccount}
            onChange={handleChange}
            required
          >
            <option value="" disabled>
              Select an account
            </option>
            {toAccounts.map((account) => (
              <option key={account.account_id} value={account.account_id}>
                {account.account_id}
              </option>
            ))}
          </Select>

          <h4>For Developers: Select a radio button to trigger chaos/crash testing and notice difference in behavior between Kafka with Postgres and MongoDB and Kafka with Oracle Database</h4>
          <RadioLabel>
            <input
              type="radio"
              name="messagingOption"
              value="Kafka with Postgres and MongoDB"
              checked={formData.messagingOption === 'Kafka with Postgres and MongoDB'}
              onChange={handleChange}
            />
            Kafka with Postgres and MongoDB
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="messagingOption"
              value="Kafka with Oracle Database"
              checked={formData.messagingOption === 'Kafka with Oracle Database'}
              onChange={handleChange}
            />
            Kafka with Oracle Database
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="noCrash"
              checked={formData.crashOption === 'noCrash'}
              onChange={handleChange}
            />
            No Crash
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="crashBeforeCommit"
              checked={formData.crashOption === 'crashBeforeCommit'}
              onChange={handleChange}
            />
            Crash after message received, before brokerage updated
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="crashAfterCommitBank1"
              checked={formData.crashOption === 'crashAfterCommitBank1'}
              onChange={handleChange}
            />
            Crash after brokerage updated, before message sent
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="crashAfterCommitBank2"
              checked={formData.crashOption === 'crashAfterCommitBank2'}
              onChange={handleChange}
            />
            Crash After Commit Bank 2 (Before Return)
          </RadioLabel>

          <Button type="submit">Submit</Button>
        </Form>

        <ImageContainer>
          <img
            src="/images/mongopostgreskafka_vs_OracleAQ.png"
            alt="Mongo/Postgres/Kafka vs Oracle AQ"
            style={{ width: '100%', borderRadius: '8px', border: '1px solid #444' }}
          />
        </ImageContainer>
      </ContentContainer>
    </PageContainer>
  );
};

export default Messaging;
