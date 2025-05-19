import React from 'react';
import styled from 'styled-components';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background-color: #1e1e1e;
  color: #ffffff;
`;

const TableHeader = styled.th`
  border: 1px solid #444;
  padding: 8px;
  text-align: left;
  background-color: #1abc9c;
`;

const TableCell = styled.td`
  border: 1px solid #444;
  padding: 8px;
`;

const HighlightedText = styled.p`
  font-size: 1.8rem; /* Larger font size */
  font-weight: bold;
  margin-bottom: 20px;
`;

const StyledList = styled.ul`
  list-style-type: none; /* Remove default bullet points */
  padding: 0;
  margin: 20px 0;
  font-size: 1.5rem; /* Increase font size */
  font-weight: bold; /* Make text bold */
  color: #1abc9c; /* Add a color to match the theme */
`;

const StyledListItem = styled.li`
  margin-bottom: 10px; /* Add spacing between list items */
  text-align: center; /* Center-align the text */
`;

const Dashboard = () => {
  // List of JavaScript files in the "pages" directory
  const pages = [
    { name: 'Architecture and Setup: k8s and otel', complete: '80%', notes: 'More text description and Grafana console for prod env.' },
    { name: 'API: ORDS', complete: '100%', notes: 'Update or remove 2nd video' },
    { name: 'Accounts: MERN', complete: '100%', notes: 'Add crash option back in' },
    { name: 'ATM: Polyglot', complete: '80%', notes: 'Add last few languages.' },
    { name: 'Transfer: MicroTx and Lock-free', complete: '100%', notes: 'Update video' },
    { name: 'Suspicious Purchases: GDD, Spatial', complete: '90%', notes: 'Update video, show purchases, Jupyter on prod version' },
    { name: 'Circular payments: Graph', complete: '80%', notes: 'Update video, use new graph server container' },
    { name: 'Transfer to brokerage: Kafka TxEventQ', complete: '80%', notes: 'Update video, change naming and queue creation' },
    { name: 'Stock ticker: True Cache', complete: '90%', notes: 'Update video, make stock price directly updatable, ie not via stock average, and mod names' },
    { name: 'Financial Insights: AI Agents, MCP, Vector search', complete: '70%', notes: 'Update video' },
    { name: 'Speak with data: Speech AI, Select AI', complete: '90%', notes: 'Update video, Add regin to accounting table and return audio rather than play it' },
    { name: 'Frontened design work', complete: '20%', notes: 'Incorporate design from Fernanda' },
    { name: 'ADD ABILITY FOR MULTIPLE USERS TO RUN APP', complete: '40%', notes: 'Adding reservation for multi-tenancy' }
  ];

  return (
    <div>
      <h2>Who is this application and workshop for?</h2>
      <StyledList>
        <StyledListItem>Financial systems experts AND developers who build these systems</StyledListItem>
      </StyledList>
      <h2>Is the application ready to test?</h2>
      <StyledList>
        <StyledListItem>
          The application is in beta and will be complete by end of June.
          <br /> {/* Added a new line here */}
          The following is the status and remaining work in priority order.
        </StyledListItem>
      </StyledList>

      {/* Table for tasks */}
      <Table>
        <thead>
          <tr>
            <TableHeader>Task</TableHeader>
            <TableHeader>% Complete</TableHeader>
            <TableHeader>Notes</TableHeader>
          </tr>
        </thead>
        <tbody>
          {pages.map((page, index) => (
            <tr key={index}>
              <TableCell>{page.name}</TableCell>
              <TableCell>
                {page.complete === '100%' ? '✅' : page.complete}
              </TableCell>
              <TableCell>{page.notes}</TableCell>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
};

export default Dashboard;
