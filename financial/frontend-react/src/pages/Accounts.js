import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto;
`;

const SidePanel = styled.div`
  border: 1px solid #444; /* Darker border */
  padding: 10px;
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the side panel */
  color: #ffffff; /* Light text */
  margin-top: 20px; /* Add spacing above the side panel */
`;

const Form = styled.form`
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #444;
  border-radius: 8px;
  background-color: #1e1e1e;
  color: #ffffff;
`;

const FormContainer = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 20px; /* Add spacing between the forms */
  margin-top: 20px;
`;

const StyledForm = styled(Form)`
  flex: 1; /* Ensure both forms take equal width */
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
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
  padding: 10px 20px;
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
  border-collapse: collapse;
  margin-top: 20px;
`;

const TableHeader = styled.th`
  border: 1px solid #444;
  padding: 8px;
  background-color: #1e1e1e;
  color: #ffffff;
  text-align: left;
`;

const TableCell = styled.td`
  border: 1px solid #444;
  padding: 8px;
  color: #ffffff;
`;

const CheckboxContainer = styled.div`
  display: flex;
  flex-direction: column; /* Stack elements vertically */
  align-items: flex-start; /* Align to the left */
  margin-bottom: 16px;
`;

const RadioGroup = styled.div`
  display: flex;
  flex-direction: column; /* Stack radio buttons vertically */
  margin-top: 8px; /* Add spacing above the radio buttons */
`;

const CheckboxLabel = styled.label`
  margin-left: 8px;
  color: #ffffff;
  white-space: nowrap; /* Prevent label from wrapping to the next line */
`;

const RadioLabel = styled.label`
  color: #ffffff;
  margin-left: 24px; /* Indent radio buttons for better alignment */
`;

const BASE_URL =
  process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080'; // Use environment variable or default to localhost

console.log('BASE_URL:', BASE_URL); // Debug output to verify the value

const Accounts = () => {
  const [isCollapsed, setIsCollapsed] = useState(true); // Set to true to make the SidePanel collapsed by default
  const [formData, setFormData] = useState({
    accountName: '',
    accountType: '',
    accountCustomerId: '',
    accountOpenedDate: new Date().toISOString().split('T')[0], // Set current date as default
    accountOtherDetails: '',
    accountBalance: '',
  });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);

  const BASE_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL; // Use the same URL prefix as in Transactions.js

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${BASE_URL}/createAccountWithGivenBalance`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        alert('Account created successfully!');
        setFormData({
          accountName: '',
          accountType: '',
          accountCustomerId: '',
          accountOpenedDate: new Date().toISOString().split('T')[0], // Reset to current date
          accountOtherDetails: '',
          accountBalance: '',
        });
        fetchAccounts(); // Refresh the accounts table
      } else {
        const errorText = await response.text();
        alert(`Failed to create account: ${errorText}`);
      }
    } catch (error) {
      console.error('Error creating account:', error);
      alert('An error occurred while creating the account.');
    }
  };

  const fetchAccounts = async () => {
    try {
      const response = await fetch(`${BASE_URL}/accounts`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      setAccounts(data); // Populate the accounts table
      setLoading(false);
    } catch (error) {
      console.error('Error fetching accounts:', error);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  return (
    <PageContainer>
      <h2>Process: Create and view accounts</h2>
      <h2>Tech: MongoDB/MERN stack</h2>
      <h2>Reference: Decimal Point Analytics</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <Button onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </Button>
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
                <li>Create and query all accounts</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Use Oracle Database MongoDB adapter to insert accounts using MongoDB application/MERN stack</li>
                <li>Query the accounts using relational/SQL commands from a Java/Spring Boot stack</li>
                <li>This is possible due to the JSON Duality feature</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>JSON Duality allows the same data to be read and written to using JSON (and MongoDB API) as well as SQL</li>
                <li>MongoDB API adapter maps MongoDB operations into real Oracle Database transactions and so, even MongoDB's recently added multi-document transactions are less efficient and can leave locks</li>
                <li>MongoDB does not support Geo-distributed transactions or transactions across multiple databases, sharding, or savepoints</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '40%' }}>
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

      {/* Create Account Form */}
      <StyledForm onSubmit={handleSubmit}>
        <h3>Create Account</h3>
        <Label htmlFor="accountName">Account Name</Label>
        <Input
          type="text"
          id="accountName"
          name="accountName"
          value={formData.accountName}
          onChange={handleChange}
          placeholder="Enter account name"
          required
        />

        <Label htmlFor="accountType">Account Type</Label>
        <Input
          type="text"
          id="accountType"
          name="accountType"
          value={formData.accountType}
          onChange={handleChange}
          placeholder="Enter account type"
          required
        />

        <Label htmlFor="accountCustomerId">Customer ID</Label>
        <Input
          type="text"
          id="accountCustomerId"
          name="accountCustomerId"
          value={formData.accountCustomerId}
          onChange={handleChange}
          placeholder="Enter customer ID"
          required
        />

        <Label htmlFor="accountOpenedDate">Opened Date</Label>
        <Input
          type="date"
          id="accountOpenedDate"
          name="accountOpenedDate"
          value={formData.accountOpenedDate}
          onChange={handleChange}
          required
        />

        <Label htmlFor="accountOtherDetails">Other Details</Label>
        <Input
          type="text"
          id="accountOtherDetails"
          name="accountOtherDetails"
          value={formData.accountOtherDetails}
          onChange={handleChange}
          placeholder="Enter other details"
        />

        <Label htmlFor="accountBalance">Balance</Label>
        <Input
          type="number"
          id="accountBalance"
          name="accountBalance"
          value={formData.accountBalance}
          onChange={handleChange}
          placeholder="Enter balance"
          required
        />

        <Button type="submit">Create Account</Button>
      </StyledForm>

      {/* Table to display all accounts */}
      {loading ? (
        <p>Loading accounts...</p>
      ) : (
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
            {accounts.map((account) => (
              <tr key={account.accountId}>
                <TableCell>{account.accountId}</TableCell>
                <TableCell>{account.accountName || 'N/A'}</TableCell>
                <TableCell>{account.accountType || 'N/A'}</TableCell>
                <TableCell>{account.accountCustomerId || 'N/A'}</TableCell>
                <TableCell>{account.accountOpenedDate || 'N/A'}</TableCell>
                <TableCell>{account.accountOtherDetails || 'N/A'}</TableCell>
                <TableCell>{account.accountBalance}</TableCell>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </PageContainer>
  );
};

export default Accounts;
