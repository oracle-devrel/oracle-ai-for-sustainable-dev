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

const Accounts = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [formData, setFormData] = useState({
    account_id: '',
    official_name: '',
    type: '',
    current_balance: '',
  });
  const [updateFormData, setUpdateFormData] = useState({
    amount: '',
    crashAfterFirstUpdate: false,
    action: 'Use MongoDB', // Default action
  });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleUpdateChange = (e) => {
    const { name, value, type, checked } = e.target;
    setUpdateFormData({
      ...updateFormData,
      [name]: type === 'checkbox' ? checked : value,
    });
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
          official_name: '',
          type: '',
          current_balance: '',
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

  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('http://localhost:8080/financial/accounts', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updateFormData),
      });

      if (response.ok) {
        alert('Accounts updated successfully!');
        setUpdateFormData({
          amount: '',
          crashAfterFirstUpdate: false,
          action: 'Use MongoDB',
        });
        fetchAccounts(); // Refresh the accounts table
      } else {
        const errorText = await response.text();
        alert(`Failed to update accounts: ${errorText}`);
      }
    } catch (error) {
      console.error('Error updating accounts:', error);
      alert('An error occurred while updating the accounts.');
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

    // Set up interval to refresh accounts every 1 second
    const interval = setInterval(() => {
      fetchAccounts();
    }, 1000);

    // Cleanup interval on component unmount
    return () => clearInterval(interval);
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
            <div style={{ flexShrink: 0, width: '70%' }}>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="615"
                src="https://www.youtube.com/embed/bK2yP1rXxn8"
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

      {/* Forms Section */}
      <FormContainer>
        {/* Create Account Form */}
        <StyledForm onSubmit={handleSubmit}>
          <h3>Create Account</h3>
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

          <Label htmlFor="current_balance">Current Balance</Label>
          <Input
            type="number"
            id="current_balance"
            name="current_balance"
            value={formData.current_balance}
            onChange={handleChange}
            placeholder="Enter current balance"
          />

          <Button type="submit">Create Account</Button>
        </StyledForm>

        {/* Update All Accounts Form */}
        <StyledForm onSubmit={handleUpdateSubmit}>
          <h3>Update All Accounts</h3>
          <Label htmlFor="amount">Amount to add to all accounts</Label>
          <Input
            type="number"
            id="amount"
            name="amount"
            value={updateFormData.amount}
            onChange={handleUpdateChange}
            placeholder="Enter amount"
            required
          />

          <CheckboxContainer>
            <div>
              <Input
                type="checkbox"
                id="crashAfterFirstUpdate"
                name="crashAfterFirstUpdate"
                checked={updateFormData.crashAfterFirstUpdate}
                onChange={handleUpdateChange}
              />
              <CheckboxLabel htmlFor="crashAfterFirstUpdate">Crash After First Update</CheckboxLabel>
            </div>

            <RadioGroup>
              <div>
                <Input
                  type="radio"
                  id="useMongoDB"
                  name="action"
                  value="Use MongoDB"
                  checked={updateFormData.action === 'Use MongoDB'}
                  onChange={handleUpdateChange}
                />
                <RadioLabel htmlFor="useMongoDB">Use MongoDB</RadioLabel>
              </div>
              <div>
                <Input
                  type="radio"
                  id="useMongoDBWithOracleAdapter"
                  name="action"
                  value="Use MongoDB with Oracle Adapter"
                  checked={updateFormData.action === 'Use MongoDB with Oracle Adapter'}
                  onChange={handleUpdateChange}
                />
                <RadioLabel htmlFor="useMongoDBWithOracleAdapter">Use MongoDB with Oracle Adapter</RadioLabel>
              </div>
            </RadioGroup>
          </CheckboxContainer>

          <Button type="submit">Update Accounts</Button>
        </StyledForm>
      </FormContainer>

      {/* Accounts Table */}
      {loading ? (
        <p>Loading accounts...</p>
      ) : (
        <Table>
          <thead>
            <tr>
              <TableHeader>Account ID</TableHeader>
              <TableHeader>Official Name</TableHeader>
              <TableHeader>Type</TableHeader>
              <TableHeader>Current Balance</TableHeader>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.account_id}>
                <TableCell>{account.account_id}</TableCell>
                <TableCell>{account.official_name ?? 'N/A'}</TableCell>
                <TableCell>{account.type}</TableCell>
                <TableCell>{account.current_balance ?? 'N/A'}</TableCell>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </PageContainer>
  );
};

export default Accounts;
