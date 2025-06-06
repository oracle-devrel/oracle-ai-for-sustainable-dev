import React from 'react';
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
  background-color: ${bankerAccent};
  color: ${bankerText};
`;

const TableCell = styled.td`
  border: 1px solid ${bankerAccent};
  padding: 8px;
  color: ${bankerText};
`;

const HighlightedText = styled.p`
  font-size: 1.8rem;
  font-weight: bold;
  margin-bottom: 20px;
  color: ${bankerAccent};
`;

const StyledList = styled.ul`
  list-style-type: none;
  padding: 0;
  margin: 20px 0;
  font-size: 1.5rem;
  font-weight: bold;
  color: ${bankerAccent};
`;

const StyledListItem = styled.li`
  margin-bottom: 10px;
  text-align: center;
`;

const VideoWrapper = styled.div`
  margin-top: 32px;
  text-align: center;
  iframe {
    border-radius: 8px;
    border: 1px solid ${bankerAccent};
    background: ${bankerPanel};
  }
`;

const FinancialStoryBoard = () => {
  // List of JavaScript files in the "pages" directory
  const pages = [
    { name: 'Architecture and Setup: k8s and otel', complete: '100%', notes: 'Update video, design work, finish workshop doc' },
    { name: 'API: ORDS', complete: '100%', notes: 'Update video, design work, finish workshop doc' },
    { name: 'Accounts: MERN', complete: '100%', notes: 'Update video, design work, finish workshop doc - mongodb save issue/bug being worked with dev team' },
    { name: 'ATM: Polyglot', complete: '100%', notes: 'Update video, design work, finish workshop doc, Add last few languages.' },
    { name: 'Transfer: MicroTx and Lock-free', complete: '100%', notes: 'Update video, design work, finish workshop doc' },
    { name: 'Suspicious Purchases: GDD, Spatial', complete: '100%', notes: 'Update video, show purchases, Jupyter on prod version' },
    { name: 'Circular payments: Graph', complete: '100%', notes: 'Update video, use new graph server container' },
    { name: 'Transfer to brokerage: Kafka TxEventQ', complete: '100%', notes: 'Update video, change naming and queue creation' },
    { name: 'Stock ticker: True Cache', complete: '100%', notes: 'Update video, make stock price directly updatable, ie not via stock average, and mod names' },
    { name: 'Financial Insights: AI Agents, MCP, Vector search', complete: '100%', notes: 'Update video' },
    { name: 'Speak with data: Speech AI, Select AI', complete: '100%', notes: 'Update video, design work, finish workshop doc, add region to accounting table, add option to play audio rather than rely on avatar/metahuman to say it' },
    { name: 'Nice to have on home page: Vector search of app itself', complete: '70%', notes: ' Question asked like `which page uses distributed database? ' },
    { name: 'ADD ABILITY FOR MULTIPLE USERS TO RUN APP', complete: '100%', notes: 'I currently simply ask that everyone create their own bank accounts (see Create Accounts/ MERN page) and use them.' }
  ];

  return (
    <PageContainer>
      <h2>Who is this application and workshop for?</h2>
      <StyledList>
        <StyledListItem>SEs, Architects, ... AND developers</StyledListItem>
      </StyledList>
      <h2>Is the application ready to test?</h2>
      <StyledList>
        <StyledListItem>
          The status and remaining work is listed below.
        </StyledListItem>
        <StyledListItem>
          Please create your own account(s) in the "Create Account" page for any testing you do.
        </StyledListItem>
      </StyledList>

      {/* Table for tasks */}
      <Table>
        <thead>
          <tr>
            <TableHeader>Task</TableHeader>
            <TableHeader>% Functionally Complete</TableHeader>
            <TableHeader>Notes/Remaining work</TableHeader>
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
      {/* Add YouTube video below the table */}
      <VideoWrapper>
        <iframe
          width="560"
          height="315"
          src="https://www.youtube.com/embed/-o16D-Sq-mU"
          title="YouTube video player"
          frameBorder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        ></iframe>
      </VideoWrapper>
    </PageContainer>
  );
};

export default FinancialStoryBoard;
