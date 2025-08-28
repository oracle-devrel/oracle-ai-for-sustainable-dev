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

const Form = styled.form`
  margin-top: 20px;
  padding: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
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
  &:focus {
    border-color: ${bankerAccent};
    outline: 1px solid ${bankerAccent};
  }
`;

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
  &:focus {
    border-color: ${bankerAccent};
    outline: 1px solid ${bankerAccent};
  }
`;

const Button = styled.button`
  padding: 10px 20px;
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

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
`;

const TableHeader = styled.th`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  text-align: left;
`;

const TableCell = styled.td`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  color: ${bankerText};
`;

const CollapsiblePanel = styled.div`
  margin-top: 20px;
  padding: 10px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
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

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-top: 20px;
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

const PanelSection = styled.div`
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid ${bankerAccent};
  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }
`;

const REACT_APP_MERN_SQL_ORACLE_SERVICE_URL =
  process.env.REACT_APP_MERN_SQL_ORACLE_SERVICE_URL || 'http://localhost:8080';
const REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL =
  process.env.REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL || 'http://localhost:5001';
const REACT_APP_MERN_MONGODB_SERVICE_URL = process.env.REACT_APP_MERN_MONGODB_SERVICE_URL || 'http://localhost:8080';

const Accounts = () => {
  const [formData, setFormData] = useState({
    _id: '',
    accountName: '',
    accountType: 'checking', // Default to "checking"
    customerId: '',
    accountOpenedDate: new Date().toISOString().split('T')[0],
    accountOtherDetails: '',
    accountBalance: '',
    writeOption: 'MongoDB API accessing Oracle Database', // Default write option
    readOption: 'SQL',  // Default read option
  });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showDeveloperDetails, setShowDeveloperDetails] = useState(true);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    let postUrl;
    if (formData.writeOption === 'SQL') {
      postUrl = `${REACT_APP_MERN_SQL_ORACLE_SERVICE_URL}/accounts/api/accounts`;
    } else if (formData.writeOption === 'MongoDB API accessing Oracle Database') {
      postUrl = `${REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL}/api/accounts`;
    } else if (formData.writeOption === 'MongoDB API') {
      postUrl = `${REACT_APP_MERN_MONGODB_SERVICE_URL}/accounts/api/accounts`;
    }

    const payload = {
      ...formData,
      accountId: formData._id,
      accountName: '1000', // Always send "1000" as the value
      accountCustomerId: formData.customerId,
      accountOpenedDate: new Date().toISOString().split('T')[0], // Always use current date
      accountOtherDetails: '', // Always send empty string
      accountBalance: 1000,    // Always send 1000
    };

    try {
      const response = await fetch(postUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        alert('Account created successfully!');
        setFormData({
          _id: '',
          accountName: '',
          accountType: 'checking', // Default to "checking"
          customerId: '',
          accountOtherDetails: '',
          accountBalance: '',
          writeOption: 'MongoDB API accessing Oracle Database',
          readOption: 'SQL',
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
    let fetchUrl;
    if (formData.readOption === 'SQL') {
      fetchUrl = `${REACT_APP_MERN_SQL_ORACLE_SERVICE_URL}/accounts/accounts`;
    } else if (formData.readOption === 'MongoDB API') {
      fetchUrl = `${REACT_APP_MERN_MONGODB_SERVICE_URL}/accounts/accounts`;
    } else if (formData.readOption === 'MongoDB API accessing Oracle Database') {
      fetchUrl = `${REACT_APP_MERN_MONGODB_JSONDUALITY_ORACLE_SERVICE_URL}/api/accounts`;
    }

    try {
      const response = await fetch(fetchUrl);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const text = await response.text();
      const data = text ? JSON.parse(text) : [];
      setAccounts(data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching accounts:', error);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
    // eslint-disable-next-line
  }, [formData.readOption]);

  // Filtered accounts based on Account ID search
  const filteredAccounts = formData._id
    ? accounts.filter(
        (account) =>
          (account.accountId || account._id || '').toString() === formData._id.toString()
      )
    : accounts;

  return (
    <PageContainer>
      <h2>Process: Create and view accounts</h2>
      <h2>Tech: MongoDB/MERN stack with JSON Duality</h2>
      <h2>Reference: Decimal Point Analytics</h2>

      {/* Collapsible Developer Details Panel */}
      <SidePanel>
        <ToggleButton onClick={() => setShowDeveloperDetails(!showDeveloperDetails)}>
          {showDeveloperDetails ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!showDeveloperDetails && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, marginRight: '20px' }}>
              <div>
                <a
                  href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier/index.html?lab=financial-account-management-mern"
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
                <li>Create and view accounts</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Uses MongoDB/MERN stack for account management</li>
                <li>Connected to Oracle JSON Duality View via MongoDB API</li>
                <li>Query the same data using SQL or MongoDB API</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Oracle Database JSON Duality provides the ability to use JSON, SQL, and Graph operations (read and write) against the same data</li>
                <li>Oracle Database can be accessed via MongoDB API by simply changing the URL to point to Oracle Database (no code changes required)</li>
              </ul>
            </div>
            <div style={{ flexShrink: 0, width: '40%' }}>
              <h4>Walkthrough Video:</h4>
              <iframe
                width="100%"
                height="315"
                src="https://www.youtube.com/embed/qHVYXagpAC0?start=371&autoplay=0"
                title="YouTube video player"
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
              ></iframe>
            </div>
          </div>
        )}
      </SidePanel>

      <TwoColumnContainer>
        <LeftColumn>
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

            <Label htmlFor="accountType">Account Type</Label>
            <Select
              id="accountType"
              name="accountType"
              value={formData.accountType}
              onChange={handleChange}
              required
            >
              <option value="" disabled>Select account type</option>
              <option value="checking">checking</option>
              <option value="savings">savings</option>
              <option value="brokerage">brokerage</option>
            </Select>

            <Label htmlFor="customerId">Customer Name / ID</Label>
            <Input
              type="text"
              id="customerId"
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              placeholder="Enter customer Name / ID"
              required
            />

            {/* Remove these lines from the form */}
            {/*
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

            <Label htmlFor="accountOtherDetails">Other Details</Label>
            <Input
              type="text"
              id="accountOtherDetails"
              name="accountOtherDetails"
              value={formData.accountOtherDetails}
              onChange={handleChange}
              placeholder="Enter other details"
            />
            */}

            {/* Write Data Using */}
            <h4>Write data using...</h4>
            <div>
              <label style={{ marginLeft: '0px' }}>
                <input
                  type="radio"
                  name="writeOption"
                  value="SQL"
                  checked={formData.writeOption === 'SQL'}
                  onChange={handleChange}
                />
                SQL
              </label>
              <label style={{ marginLeft: '20px' }}>
                <input
                  type="radio"
                  name="writeOption"
                  value="MongoDB API accessing Oracle Database"
                  checked={formData.writeOption === 'MongoDB API accessing Oracle Database'}
                  onChange={handleChange}
                />
                MongoDB API accessing Oracle Database
              </label>
            </div>

            <div style={{ height: '20px' }}></div> {/* Add space before the button */}

            <Button type="submit">Create Account</Button>

            {/* Read Data Using */}
            <h4 style={{ marginTop: '32px' }}>Read data using...</h4>
            <div>
              <label style={{ marginLeft: '0px' }}>
                <input
                  type="radio"
                  name="readOption"
                  value="SQL"
                  checked={formData.readOption === 'SQL'}
                  onChange={handleChange}
                />
                SQL
              </label>
              <label style={{ marginLeft: '20px' }}>
                <input
                  type="radio"
                  name="readOption"
                  value="MongoDB API accessing Oracle Database"
                  checked={formData.readOption === 'MongoDB API accessing Oracle Database'}
                  onChange={handleChange}
                />
                MongoDB API accessing Oracle Database
              </label>
            </div>
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
                {filteredAccounts.map((account) => (
                  <tr key={account.accountId || account._id}>
                    <TableCell>{account.accountId || account._id || 'N/A'}</TableCell>
                    <TableCell>{account.accountName || 'N/A'}</TableCell>
                    <TableCell>{account.accountType || 'N/A'}</TableCell>
                    <TableCell>{account.accountCustomerId || account.customerId || 'N/A'}</TableCell>
                    <TableCell>{account.accountOpenedDate || 'N/A'}</TableCell>
                    <TableCell>{account.accountOtherDetails || 'N/A'}</TableCell>
                    <TableCell>{account.accountBalance}</TableCell>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </LeftColumn>
        <RightColumn>
          <PanelSection>
            <CodeTitle>Write data using...</CodeTitle>
            <code>
              {formData.writeOption === 'SQL' ? (
`// SQL (Oracle)
INSERT INTO accounts (account_id, account_name, account_type, customer_id, opened_date, other_details, balance)
VALUES (:accountId, :accountName, :accountType, :customerId, :openedDate, :otherDetails, :balance);`
              ) : (
`// MongoDB API accessing Oracle Database (Node.js/Mongoose)
await AccountModel.create({
  _id: accountId,
  accountName,
  accountType,
  customerId,
  openedDate,
  otherDetails,
  balance
});`
              )}
            </code>
          </PanelSection>
          <PanelSection>
            <CodeTitle>Read data using...</CodeTitle>
            <code>
              {formData.readOption === 'SQL' ? (
`// SQL (Oracle)
SELECT * FROM accounts WHERE account_id = :accountId;`
              ) : (
`// MongoDB API accessing Oracle Database (Node.js/Mongoose)
const account = await AccountModel.findById(accountId);

// Or use JSON Duality View (Oracle)
SELECT * FROM json_duality_view_accounts WHERE account_id = :accountId;`
              )}
            </code>
          </PanelSection>
        </RightColumn>
      </TwoColumnContainer>
    </PageContainer>
  );
};

export default Accounts;
