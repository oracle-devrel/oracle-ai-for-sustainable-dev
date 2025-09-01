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
  display: flex;
  flex-direction: column;
`;

const ContentContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  /* Remove max-width and margin to allow full width usage */
  /* max-width: 1200px;
  margin: 0 auto; */
  flex: 1;
`;

const Form = styled.form`
  width: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid ${bankerAccent};
  border-radius: 8px;
  background-color: ${bankerPanel};
`;

const SidePanel = styled.div`
  width: 100%;
  border: 1px solid ${bankerAccent};
  padding: 10px;
  border-radius: 8px;
  background-color: ${bankerPanel};
  color: ${bankerText};
  font-size: 1.1rem;
  margin-bottom: 20px;
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
`;

const Select = styled.select`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background-color: #406080;
  color: ${bankerText};
`;

const RadioLabel = styled.label`
  display: block;
  margin-bottom: 8px;
  color: ${bankerText};
`;

const CheckboxLabel = styled.label`
  display: flex;
  align-items: center;
  margin-top: 10px;
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

const CollapsibleContent = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
`;

const TextContainer = styled.div`
  flex: 1;
  margin-right: 20px;
`;

const VideoContainer = styled.div`
  flex: 1;
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: ${bankerPanel};
  color: ${bankerText};
`;

const TableHeader = styled.th`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  text-align: left;
`;

const TableCell = styled.td`
  border: 1px solid ${bankerAccent};
  padding: 8px;
`;

const TwoColumnContainer = styled.div`
  display: flex;
  gap: 32px;
  width: 100%;
  flex: 1;
  align-items: stretch;
  @media (max-width: 900px) {
    flex-direction: column;
    gap: 0;
  }
`;

const LeftColumn = styled.div`
  flex: 1 1 0;
  min-width: 320px;
  display: flex;
  flex-direction: column;
`;

const RightColumn = styled.div`
  flex: 1 1 0;
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
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
`;

const CodeTitle = styled.div`
  font-weight: bold;
  color: ${bankerAccent};
  margin-bottom: 12px;
`;

const Transactions = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);

  const [formData, setFormData] = useState({
    amount: '',
    fromAccount: '',
    toAccount: '',
    crashSimulation: 'noCrash',
    sagaAction: 'complete',
    useLockFreeReservations: false,
  });

  const [fromAccounts, setFromAccounts] = useState([]);
  const [toAccounts, setToAccounts] = useState([]);
  const [allAccounts, setAllAccounts] = useState([]);

  const fetchAccounts = async () => {
    try {
      const BASE_URL = process.env.REACT_APP_MICROTX_ACCOUNT_SERVICE_URL;
      const response = await fetch(`${BASE_URL}/accounts`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      setFromAccounts(data);
      setToAccounts(data);
      setAllAccounts(data);
    } catch (error) {
      console.error('Error fetching accounts:', error);
    }
  };

  useEffect(() => {
    fetchAccounts();
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

    const BASE_URL = process.env.REACT_APP_MICROTX_TRANSFER_SERVICE_URL;
    const url = `${BASE_URL}?fromAccount=${formData.fromAccount}&toAccount=${formData.toAccount}&amount=${formData.amount}&sagaAction=${formData.sagaAction}&useLockFreeReservations=${formData.useLockFreeReservations}&crashSimulation=${formData.crashSimulation}`;

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    })
      .then((response) => {
        if (response.ok) {
          alert(`Transfer requestion complete.
        From Account: ${formData.fromAccount}, 
        To Account: ${formData.toAccount}, 
        Amount: ${formData.amount}, 
        Saga Action: ${formData.sagaAction}, 
        Lock-free Reservations: ${formData.useLockFreeReservations}, 
        Crash Simulation: ${formData.crashSimulation}`);
          fetchAccounts();
        } else {
          alert('Transfer failed. Please try again.');
        }
      })
      .catch((error) => {
        console.error('Error:', error);
        alert('An error occurred. Please try again.');
      });
  };

  const codeSnippets = {
    lockFree: `// Auto-compensating distributed transactions reduces code by 80% and insures transactional integrity

public ResponseEntity<?> deposit //...
        microTxLockFreeReservation.join(connection);

//...

public ResponseEntity<?> compensate //...
        microTxLockFreeReservation.rollback(connection);
 `,
    standard: `
// Traditional compensation logic, complicated and error-prone, requires book-keeping and extensive error handling

public ResponseEntity<?> deposit //...
        Account account = AccountTransferDAO.instance().getAccountForAccountId(accountId);
        AccountTransferDAO.instance().saveJournal(new Journal(DEPOSIT, accountId, 0, lraId,
                    AccountTransferDAO.getStatusString(ParticipantStatus.Active)));
        AccountTransferDAO.instance().saveJournal(new Journal(DEPOSIT, accountId, depositAmount, lraId,
                AccountTransferDAO.getStatusString(ParticipantStatus.Active)));

//...

public ResponseEntity<?> compensate //...
        Journal journal = AccountTransferDAO.instance().getJournalForLRAid(lraId, WITHDRAW);
        String lraState = journal.getLraState();
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Compensating));
        AccountTransferDAO.instance().saveJournal(journal);
        Account account = AccountTransferDAO.instance().getAccountForAccountId(journal.getAccountId());
        if (account != null) {
            account.setAccountBalance(account.getAccountBalance() + journal.getJournalAmount());
            AccountTransferDAO.instance().saveAccount(account);
        } else {
            journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.FailedToCompensate));
        }
        journal.setLraState(AccountTransferDAO.getStatusString(ParticipantStatus.Compensated));
        AccountTransferDAO.instance().saveJournal(journal);
`
  };

  const selectedSnippet = formData.useLockFreeReservations ? codeSnippets.lockFree : codeSnippets.standard;

  return (
    <PageContainer>
      <h2>Process: Transfer to external bank</h2>
      <h2>Tech: MicroTx, Lock-free reservations</h2>
      <h2>Reference: University of Naples</h2>
      <ContentContainer>
        <SidePanel>
          <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
            {isCollapsed ? 'Developer Details' : 'Hide Developer Details'}
          </ToggleButton>
          {!isCollapsed && (
            <CollapsibleContent>
              <TextContainer>
                <div>
                  <a
                    href="https://paulparkinson.github.io/converged/microservices-with-converged-db/workshops/freetier/index.html"
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
                  <li>Transfer funds between banks</li>
                </ul>
                <h4>Developer Notes:</h4>
                <ul>
                  <li>The only database that provides auto-compensating sagas (microservice transactions) and highest throughput for hotspots/fields</li>
                  <li>Simplified development (~80% less code)</li>
                  <li>More background on sagas can be found here: https://youtu.be/3p8X-i1y43U</li>
                </ul>
                <h4>Differentiators:</h4>
                <ul>
                  <li>Auto-compensating microservices transactions, support for multiple languages, Rest and Messaging</li>
                </ul>
              </TextContainer>
              <VideoContainer>
                <h4>Walkthrough Video:</h4>
                <iframe
                  width="100%"
                  height="315"
                  src="https://www.youtube.com/embed/qHVYXagpAC0?start=466&autoplay=0"
                  title="YouTube video player"
                  frameBorder="0"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                  style={{ borderRadius: '8px', border: `1px solid ${bankerAccent}` }}
                ></iframe>
              </VideoContainer>
            </CollapsibleContent>
          )}
        </SidePanel>
        <TwoColumnContainer>
          <LeftColumn>
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
                  <option key={account._id} value={account._id}>
                    {account._id}
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
                  <option key={account._id} value={account._id}>
                    {account._id}
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
                  name="crashSimulation"
                  value="noCrash"
                  checked={formData.crashSimulation === 'noCrash'}
                  onChange={handleChange}
                />
                No Crash
              </RadioLabel>
              <RadioLabel>
                <input
                  type="radio"
                  name="crashSimulation"
                  value="crashBeforeFirstBankCommit"
                  checked={formData.crashSimulation === 'crashBeforeFirstBankCommit'}
                  onChange={handleChange}
                />
                Crash Before First Bank Commit
              </RadioLabel>
              <RadioLabel>
                <input
                  type="radio"
                  name="crashSimulation"
                  value="crashAfterFirstBankCommit"
                  checked={formData.crashSimulation === 'crashAfterFirstBankCommit'}
                  onChange={handleChange}
                />
                Crash After First Bank Commit
              </RadioLabel>
              <RadioLabel>
                <input
                  type="radio"
                  name="crashSimulation"
                  value="crashAfterSecondBankCommit"
                  checked={formData.crashSimulation === 'crashAfterSecondBankCommit'}
                  onChange={handleChange}
                />
                Crash After Second Bank Commit
              </RadioLabel>

              <Button type="submit">Submit</Button>
            </Form>
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
                {allAccounts.map((account) => (
                  <tr key={account._id}>
                    <TableCell>{account._id}</TableCell>
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
          </LeftColumn>
          <RightColumn>
            <CodeTitle>
              {formData.useLockFreeReservations
                ? "Lock-free Reservations Example"
                : "Standard Saga Transaction Example"}
            </CodeTitle>
            <code>
              {selectedSnippet}
            </code>
          </RightColumn>
        </TwoColumnContainer>
      </ContentContainer>
    </PageContainer>
  );
};

export default Transactions;
