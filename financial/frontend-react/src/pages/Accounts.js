import React, { useState, useEffect } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212;
  color: #ffffff;
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto;
`;

const Form = styled.form`
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #444;
  border-radius: 8px;
  background-color: #1e1e1e;
  color: #ffffff;
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

const BASE_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL || 'http://localhost:8080';
const BASE_MERN_BACKEND_URL =
  process.env.REACT_APP_MERN_BACKEND_SERVICE_URL || 'http://localhost:5000';

const Accounts = () => {
  const [formData, setFormData] = useState({
    _id: '', // Add _id field for accountId
    accountName: '',
    accountType: '',
    customerId: '',
    accountOpenedDate: new Date().toISOString().split('T')[0],
    accountOtherDetails: '',
    accountBalance: '',
  });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${BASE_MERN_BACKEND_URL}/api/accounts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        alert('Account created successfully!');
        setFormData({
          _id: '', // Reset _id field
          accountName: '',
          accountType: '',
          customerId: '',
          accountOpenedDate: new Date().toISOString().split('T')[0],
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

      {/* Create Account Form */}
      <Form onSubmit={handleSubmit}>
        <h3>Create Account</h3>
        <Label htmlFor="_id">Account ID</Label>
        <Input
          type="text"
          id="_id"
          name="_id"
          value={formData._id}
          onChange={handleChange}
          placeholder="Enter account ID"
          required
        />

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

        <Label htmlFor="customerId">Customer ID</Label>
        <Input
          type="text"
          id="customerId"
          name="customerId"
          value={formData.customerId}
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
      </Form>

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
              <tr key={account._id}>
                <TableCell>{account._id}</TableCell>
                <TableCell>{account.accountName || 'N/A'}</TableCell>
                <TableCell>{account.accountType || 'N/A'}</TableCell>
                <TableCell>{account.customerId || 'N/A'}</TableCell>
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
