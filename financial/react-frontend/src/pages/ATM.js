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
`;

const SidePanel = styled.div`
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  margin-bottom: 20px;
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

const Form = styled.form`
  display: flex;
  flex-direction: column;
  max-width: 800px;
  margin: 20px auto;
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
  font-weight: bold;
  &:hover {
    background-color: ${bankerBg};
  }
`;

const Table = styled.table`
  width: 100%;
  margin-top: 20px;
  border-collapse: collapse;
`;

const TableHeader = styled.th`
  background-color: ${bankerPanel};
  color: ${bankerText};
  padding: 10px;
  border: 1px solid ${bankerAccent};
`;

const TableCell = styled.td`
  background-color: ${bankerBg};
  color: ${bankerText};
  padding: 10px;
  border: 1px solid ${bankerAccent};
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

const ATM = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [formData, setFormData] = useState({
    accountId: '',
    amount: '',
    language: '',
  });
  const [accountIds, setAccountIds] = useState([]);
  const [accountDetails, setAccountDetails] = useState(null);

  const BASE_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL;

  useEffect(() => {
    const fetchAccountIds = async () => {
      try {
        const response = await fetch(`${BASE_URL}/accounts`);
        const data = await response.json();
        setAccountIds(Array.isArray(data) ? data : []);
        if (data.length > 0) {
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
      const response = await fetch(`${BASE_URL}/account/${accountId}`);
      if (response.ok) {
        const data = await response.json();
        setAccountDetails(data);
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
        body: JSON.stringify(parseFloat(formData.amount)),
      });

      if (response.ok) {
        alert(`Transaction successful! Account ID: ${formData.accountId}, Amount: ${formData.amount}`);
        setFormData({ ...formData, amount: '' });
        fetchAccountDetails(formData.accountId);
      } else {
        const errorText = await response.text();
        alert(`Transaction failed: ${errorText}`);
      }
    } catch (error) {
      console.error('Error submitting transaction:', error);
      alert('An error occurred. Please try again.');
    }
  };

  const codeSnippets = {
    "Java - Spring Boot (using Spring Data JPA for ORM)": `// Java (Spring Boot, Spring Data JPA)
@Transactional
public void updateBalance(Long accountId, BigDecimal amount) {
    Account account = accountRepository.findById(accountId).orElseThrow();
    account.setBalance(account.getBalance().add(amount));
    accountRepository.save(account);
}`,
    "JavaScript (Node.js)": `// JavaScript (Node.js, Express, Mongoose)
await AccountModel.findByIdAndUpdate(
  accountId,
  { $inc: { accountBalance: amount } }
);`,
    "Python (using SQLAlchemy for ORM)": `# Python (SQLAlchemy)
with Session() as session:
    account = session.query(Account).get(account_id)
    account.balance += amount
    session.commit()`,
    ".NET": `// .NET (C#, Entity Framework)
using (var context = new AccountContext()) {
    var account = context.Accounts.Find(accountId);
    account.Balance += amount;
    context.SaveChanges();
}`,
    "Go (using BunDB for ORM)": `// Go (BunDB)
account := new(Account)
err := db.NewSelect().Model(account).Where("id = ?", accountId).Scan(ctx)
account.Balance += amount
_, err = db.NewUpdate().Model(account).WherePK().Exec(ctx)`,
    "Rust": `// Rust (SQLx)
let mut tx = pool.begin().await?;
sqlx::query!("UPDATE accounts SET balance = balance + $1 WHERE id = $2", amount, account_id)
    .execute(&mut tx)
    .await?;
tx.commit().await?;`,
    "Ruby on Rails": `# Ruby on Rails (ActiveRecord)
account = Account.find(account_id)
account.balance += amount
account.save!`
  };

  // Set Java as default if nothing is selected
  const selectedLanguage =
    formData.language || "Java - Spring Boot (using Spring Data JPA for ORM)";

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
                  style={{ color: bankerAccent, textDecoration: 'none' }}
                >
                  Click here for workshop lab and further information
                </a>
              </div>
              <div>
                <a
                  href="https://github.com/paulparkinson/oracle-ai-for-sustainable-dev/tree/main/financial/atm-polyglot"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: bankerAccent, textDecoration: 'none' }}
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
                src="https://www.youtube.com/embed/rSn9cI2oRrA"
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
                checked={selectedLanguage === 'Java - Spring Boot (using Spring Data JPA for ORM)'}
                onChange={handleChange}
              />
              Java (Spring Boot)
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value="JavaScript (Node.js)"
                checked={selectedLanguage === 'JavaScript (Node.js)'}
                onChange={handleChange}
              />
              JS
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value="Python (using SQLAlchemy for ORM)" 
                checked={selectedLanguage === 'Python (using SQLAlchemy for ORM)'}
                onChange={handleChange}
              />
              Python
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value=".NET"
                checked={selectedLanguage === '.NET'}
                onChange={handleChange}
              />
              .NET
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value="Go (using BunDB for ORM)"
                checked={selectedLanguage === 'Go (using BunDB for ORM)'}
                onChange={handleChange}
              />
              Go
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value="Rust"
                checked={selectedLanguage === 'Rust'}
                onChange={handleChange}
              />
              Rust
            </RadioLabel>
            <RadioLabel>
              <input
                type="radio"
                name="language"
                value="Ruby on Rails"
                checked={selectedLanguage === 'Ruby on Rails'}
                onChange={handleChange}
              />
              Ruby on Rails
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
        </LeftColumn>
        <RightColumn>
          <CodeTitle>Sample Code for Transaction</CodeTitle>
          <code>
            {codeSnippets[selectedLanguage]}
          </code>
        </RightColumn>
      </TwoColumnContainer>
    </PageContainer>
  );
};

export default ATM;
