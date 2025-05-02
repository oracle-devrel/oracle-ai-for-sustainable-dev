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

const Accounts = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [formData, setFormData] = useState({
    account_id: '',
    name: '',
    official_name: '',
    type: '',
    subtype: '',
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
      const response = await fetch('http://localhost:8080/financial/accounts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        alert('Account created successfully!');
        setFormData({
          account_id: '',
          name: '',
          official_name: '',
          type: '',
          subtype: '',
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
      const response = await fetch('http://localhost:8080/financial/accounts');
      const data = await response.json();
      setAccounts(data);
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
      <h2>Create and view accounts</h2>
      <h2>MongoDB/MERN stack</h2>
      <h2>Decimal Point Analytics (DPA)</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <Button onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Details' : 'Hide Details'}
        </Button>
        {!isCollapsed && (
          <div>
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
          </div>
        )}
      </SidePanel>

      {/* Form Section */}
      <Form onSubmit={handleSubmit}>
        <Label htmlFor="account_id">Account ID</Label>
        <Input
          type="text"
          id="account_id"
          name="account_id"
          value={formData.account_id}
          onChange={handleChange}
          placeholder="Enter account ID"
          required
        />

        <Label htmlFor="name">Name</Label>
        <Input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          placeholder="Enter account name"
          required
        />

        <Label htmlFor="official_name">Official Name</Label>
        <Input
          type="text"
          id="official_name"
          name="official_name"
          value={formData.official_name}
          onChange={handleChange}
          placeholder="Enter official name"
        />

        <Label htmlFor="type">Type</Label>
        <Input
          type="text"
          id="type"
          name="type"
          value={formData.type}
          onChange={handleChange}
          placeholder="Enter account type"
          required
        />

        <Label htmlFor="subtype">Subtype</Label>
        <Input
          type="text"
          id="subtype"
          name="subtype"
          value={formData.subtype}
          onChange={handleChange}
          placeholder="Enter account subtype"
        />

        <Button type="submit">Create Account</Button>
      </Form>

      {/* Accounts Table */}
      {loading ? (
        <p>Loading accounts...</p>
      ) : (
        <Table>
          <thead>
            <tr>
              <TableHeader>Account ID</TableHeader>
              <TableHeader>Name</TableHeader>
              <TableHeader>Type</TableHeader>
              <TableHeader>Subtype</TableHeader>
              <TableHeader>Official Name</TableHeader>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.account_id}>
                <TableCell>{account.account_id}</TableCell>
                <TableCell>{account.name}</TableCell>
                <TableCell>{account.type}</TableCell>
                <TableCell>{account.subtype}</TableCell>
                <TableCell>{account.official_name ?? 'N/A'}</TableCell>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </PageContainer>
  );
};

export default Accounts;
