import React, { useState, useEffect } from 'react';
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

const Table = styled.table`
  width: 100%;
  margin-top: 20px;
  border-collapse: collapse;
`;

const TableHeader = styled.th`
  background-color: #2c2c2c;
  color: #ffffff;
  padding: 10px;
  border: 1px solid #444;
`;

const TableCell = styled.td`
  background-color: #121212;
  color: #ffffff;
  padding: 10px;
  border: 1px solid #444;
`;

const ATM = () => {
  const [isCollapsed, setIsCollapsed] = useState(true); // Set to true to make the details box collapsed by default
  const [formData, setFormData] = useState({
    accountId: '',
    amount: '',
    language: '',
  });
  const [accountIds, setAccountIds] = useState([]);
  const [accountDetails, setAccountDetails] = useState(null); // State to store the account details

  const BASE_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL; // Use the same URL prefix as in Transactions.js

  // Fetch account IDs from the API
  useEffect(() => {
    const fetchAccountIds = async () => {
      try {
        const response = await fetch(`${BASE_URL}/accounts`);
        const data = await response.json();
        setAccountIds(Array.isArray(data) ? data : []);
        if (data.length > 0) {
          // Prepopulate with the first account's id
          setFormData((prev) => ({
            ...prev,
            accountId: data[0].accountId || data[0]._id || data[0].id || ''
          }));
        }
      } catch (error) {
        console.error('Error fetching account IDs:', error);
      }
    };

    fetchAccountIds();
  }, [BASE_URL]);

  const fetchAccountDetails = async (accountId) => {
    try {
      const response = await fetch(`${BASE_URL}/account/${accountId}`); // Fetch details for a specific account
      if (response.ok) {
        const data = await response.json();
        setAccountDetails(data); // Update the account details state
      } else {
        console.error(`Failed to fetch account details for accountId: ${accountId}`);
      }
    } catch (error) {
      console.error('Error fetching account details:', error);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const url = `${BASE_URL}/account/${formData.accountId}/balance`;

    try {
      const response = await fetch(url, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(parseFloat(formData.amount)), // Send the amount as a number
      });

      if (response.ok) {
        alert(`Transaction successful! Account ID: ${formData.accountId}, Amount: ${formData.amount}`);
        setFormData({ ...formData, amount: '' }); // Reset the amount field
        fetchAccountDetails(formData.accountId); // Refresh the account details table
      } else {
        const errorText = await response.text();
        alert(`Transaction failed: ${errorText}`);
      }
    } catch (error) {
      console.error('Error submitting transaction:', error);
      alert('An error occurred. Please try again.');
    }
  };

  return (
    <PageContainer>
      <h2>Process: Deposit/withdraw money (ATM)</h2>
      <h2>Tech: Polyglot</h2>
      <h2>Java, JS, Python, .NET, Go, Rust</h2>
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
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial/atm-polyglot"
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
                <li>Supports all languages and frameworks</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '70%' }}>
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
            </div>
          </div>
        )}
      </SidePanel>

      <Form onSubmit={handleSubmit}>
        <Label htmlFor="accountId">Account ID</Label>
        <Select
          id="accountId"
          name="accountId"
          value={formData.accountId}
          onChange={handleChange}
          required
        >
          <option value="" disabled>Select an account</option>
          {accountIds.map((account) => (
            <option key={account.accountId || account._id || account.id} value={account.accountId || account._id || account.id}>
              {account.accountId || account._id || account.id}
            </option>
          ))}
        </Select>

        <Label htmlFor="amount">Amount to deposit/withdraw</Label>
        <Input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          placeholder="Enter amount"
          required
        />

        <h4>Select a language for the transaction:</h4>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Java - Spring Boot (using Spring Data JPA for ORM)"
            checked={formData.language === 'Java - Spring Boot (using Spring Data JPA for ORM)'}
            onChange={handleChange}
          />
          Java (Spring Boot)
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="JavaScript (Node.js)"
            checked={formData.language === 'JavaScript (Node.js)'}
            onChange={handleChange}
          />
          JS
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Python (using SQLAlchemy for ORM)" 
            checked={formData.language === 'Python (using SQLAlchemy for ORM)'}
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
            value="Go (using BunDB for ORM)"
            checked={formData.language === 'Go (using BunDB for ORM)'}
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

      {/* Table to display account details */}
      {accountDetails && (
        <Table>
          <thead>
            <tr>
              <TableHeader>Account ID</TableHeader>
              <TableHeader>Account Name</TableHeader>
              <TableHeader>Account Type</TableHeader>
              <TableHeader>Customer ID</TableHeader>
              <TableHeader>Opened Date</TableHeader>
              <TableHeader>Other Details</TableHeader>
              <TableHeader>Balance</TableHeader>
            </tr>
          </thead>
          <tbody>
            <tr>
              <TableCell>
                {accountDetails.accountId || accountDetails._id || accountDetails.id || 'N/A'}
              </TableCell>
              <TableCell>{accountDetails.accountName || 'N/A'}</TableCell>
              <TableCell>{accountDetails.accountType || 'N/A'}</TableCell>
              <TableCell>{accountDetails.accountCustomerId || 'N/A'}</TableCell>
              <TableCell>{accountDetails.accountOpenedDate || 'N/A'}</TableCell>
              <TableCell>{accountDetails.accountOtherDetails || 'N/A'}</TableCell>
              <TableCell>{accountDetails.accountBalance}</TableCell>
            </tr>
          </tbody>
        </Table>
      )}
    </PageContainer>
  );
};

export default ATM;
