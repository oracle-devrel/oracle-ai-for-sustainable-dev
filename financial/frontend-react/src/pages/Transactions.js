import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
  display: flex; /* Use flexbox for layout */
  flex-direction: column; /* Stack title and content vertically */
`;

const ContentContainer = styled.div`
  display: flex; /* Use flexbox for layout */
  flex-direction: row; /* Place SidePanel and Form side by side */
  align-items: flex-start; /* Align items at the top */
  margin-top: 20px; /* Add spacing below the title */
`;

const Form = styled.form`
  flex: 3; /* Take up more space for the form */
  display: flex;
  flex-direction: column;
  max-width: 800px;
  margin-left: 20px; /* Add spacing between the SidePanel and the form */
  padding: 20px;
  border: 1px solid #444; /* Darker border */
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the form */
`;

const SidePanel = styled.div`
  flex: 0.5; /* Reduce the space taken by the side panel */
  max-width: 400px; /* Limit the maximum width */
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  font-size: 1.1rem; /* Slightly smaller font size */
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

const Transactions = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const [formData, setFormData] = useState({
    amount: '',
    fromAccount: '',
    toAccount: '',
    messagingOption: '', // New field for messaging options
    crashOption: '', // New field for crash simulation
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
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
      <h2>Spring Boot, MicroTx, Lock-free reservations</h2>
      <h2>U of Naples</h2>
      <ContentContainer>
        <SidePanel>
          <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
            {isCollapsed ? 'Show Details' : 'Hide Details'}
          </ToggleButton>
          {!isCollapsed && (
            <div>
              <h4>Process:</h4>
              <ul>
                <li>Transfer funds between banks</li>
              </ul>
              <h4>Developers:</h4>
              <ul>
                <li>The only database that provides auto-compensating sagas (microservice transactions) and highest throughput for hotspots/fields</li>
                <li>Simplified development (~80% less code)</li>
              </ul>
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
            <option value="bank1account1">Bank 1 Account 1</option>
            <option value="bank1account2">Bank 1 Account 2</option>
            <option value="bank1account3">Bank 1 Account 3</option>
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
            <option value="bank2account1">Bank 2 Account 1</option>
            <option value="bank2account2">Bank 2 Account 2</option>
            <option value="bank2account3">Bank 2 Account 3</option>
          </Select>

          <h4>For Developers: Select a radio button to trigger chaos/crash testing and notice difference in behavior and simplified coding when using Oracle Database lock-free reservations and MicroTx sagas</h4>
          <RadioLabel>
            <input
              type="radio"
              name="messagingOption"
              value="Without MicroTx and Lock-free Reservations"
              checked={formData.messagingOption === 'Without MicroTx and Lock-free Reservations'}
              onChange={handleChange}
            />
            Without MicroTx and Lock-free Reservations
          </RadioLabel>
          <RadioLabel>
            <input
              type="radio"
              name="messagingOption"
              value="With MicroTx and Lock-free Reservations"
              checked={formData.messagingOption === 'With MicroTx and Lock-free Reservations'}
              onChange={handleChange}
            />
            With MicroTx and Lock-free Reservations
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
