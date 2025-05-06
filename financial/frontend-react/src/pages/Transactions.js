import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212;
  color: #ffffff;
  width: 100%; /* Ensure it spans the full width of the screen */
  height: 100vh;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
`;

const ContentContainer = styled.div`
  display: flex;
  flex-direction: column; /* Stack SidePanel and Form vertically */
  width: 100%; /* Use the full width of the page */
  max-width: 1200px; /* Add a maximum width for better readability */
  margin: 0 auto; /* Center the content horizontally within the page */
`;

const Form = styled.form`
  width: 100%; /* Make the Form span the full width */
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid #444;
  border-radius: 8px;
  background-color: #1e1e1e;
`;

const SidePanel = styled.div`
  width: 100%; /* Make the SidePanel span the full width */
  border: 1px solid #444;
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e;
  color: #ffffff;
  font-size: 1.1rem;
  margin-bottom: 20px; /* Add spacing between the SidePanel and the Form */
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

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2c2c2c;
  color: #ffffff;
`;

const RadioLabel = styled.label`
  display: block;
  margin-bottom: 8px;
  color: #ffffff;
`;

const CheckboxLabel = styled.label`
  display: flex;
  align-items: center;
  margin-top: 10px;
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
  flex-direction: row; /* Arrange text and video side by side */
  justify-content: space-between;
  align-items: flex-start;
  width: 100%; /* Make it span the full width of the page */
`;

const TextContainer = styled.div`
  flex: 1;
  margin-right: 20px; /* Add spacing between text and video */
`;

const VideoContainer = styled.div`
  flex: 1;
`;

const Transactions = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const [formData, setFormData] = useState({
    amount: '',
    fromAccount: '',
    toAccount: '',
    crashOption: 'noCrash', // Default to "No Crash"
    sagaAction: 'complete', // Default to "Complete/Commit"
    useLockFreeReservations: false, // Default to not using lock-free reservations
  });

  const [fromAccounts, setFromAccounts] = useState([]); // State for "From Account" dropdown
  const [toAccounts, setToAccounts] = useState([]); // State for "To Account" dropdown

  // Fetch account IDs for "From Account" dropdown
  useEffect(() => {
    const fetchFromAccounts = async () => {
      try {
        const response = await fetch(
          'https://ij1tyzir3wpwlpe-financialdb.adb.eu-frankfurt-1.oraclecloudapps.com/ords/financial/accounts/'
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
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value,
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    fetch('http://oracleai-financial.org/transfer', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(formData),
    })
      .then((response) => {
        if (response.ok) {
          alert('Transfer successful!');
        } else {
          alert('Transfer failed. Please try again.');
        }
      })
      .catch((error) => {
        console.error('Error:', error);
        alert('An error occurred. Please try again.');
      });
  };

  return (
    <PageContainer>
      <h2>Transfer to external bank</h2>
      <h2>MicroTx, Lock-free reservations</h2>
      <h2>University of Naples</h2>
      <ContentContainer>
        <SidePanel>
          <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
            {isCollapsed ? 'Show Details' : 'Hide Details'}
          </ToggleButton>
          {!isCollapsed && (
            <CollapsibleContent>
              <TextContainer>
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
                  <li>Transfer funds between banks</li>
                </ul>
                <h4>Developer Notes:</h4>
                <ul>
                  <li>The only database that provides auto-compensating sagas (microservice transactions) and highest throughput for hotspots/fields</li>
                  <li>Simplified development (~80% less code)</li>
                </ul>
                <h4>Differentiators:</h4>
                <ul>
                  <li>Auto-compensating microservices transactions, support for multiple languages, Rest and Messaging</li>
                </ul>
              </TextContainer>
              <VideoContainer>
                <iframe
                  width="100%"
                  height="315"
                  src="https://www.youtube.com/embed/3p8X-i1y43U"
                  title="YouTube video player"
                  frameBorder="0"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                ></iframe>
              </VideoContainer>
            </CollapsibleContent>
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

          <h4>Saga Action</h4>
          <RadioLabel>
            <input
              type="radio"
              name="sagaAction"
              value="complete"
              checked={formData.sagaAction === 'complete'}
              onChange={handleChange}
            />
            Complete/Commit
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="sagaAction"
              value="rollback"
              checked={formData.sagaAction === 'rollback'}
              onChange={handleChange}
            />
            Compensate/Rollback
          </RadioLabel>

          {/* Checkbox for Lock-free Reservations */}
          <CheckboxLabel>
            <input
              type="checkbox"
              name="useLockFreeReservations"
              checked={formData.useLockFreeReservations}
              onChange={handleChange}
            />
            Use Lock-free Reservations
          </CheckboxLabel>

          <h4>Crash Simulation</h4>
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
              value="crashBeforeFirstBankCommit"
              checked={formData.crashOption === 'crashBeforeFirstBankCommit'}
              onChange={handleChange}
            />
            Crash Before First Bank Commit
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="crashAfterFirstBankCommit"
              checked={formData.crashOption === 'crashAfterFirstBankCommit'}
              onChange={handleChange}
            />
            Crash After First Bank Commit
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="crashOption"
              value="crashAfterSecondBankCommit"
              checked={formData.crashOption === 'crashAfterSecondBankCommit'}
              onChange={handleChange}
            />
            Crash After Second Bank Commit
          </RadioLabel>

          <Button type="submit">Submit</Button>
        </Form>
      </ContentContainer>
    </PageContainer>
  );
};

export default Transactions;
