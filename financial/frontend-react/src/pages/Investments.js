import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
  overflow-y: auto; /* Allow scrolling if content overflows */
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

const CollapsibleContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
`;

const TextContent = styled.div`
  flex: 1;
  margin-right: 20px; /* Add spacing between text and video */
`;

const VideoWrapper = styled.div`
  flex-shrink: 0;
  width: 40%; /* Set the width of the video */
`;

const IframeContainer = styled.div`
  width: 100%;
  height: 100%;
  overflow: hidden;
`;

const Iframe = styled.iframe`
  width: 100%;
  height: calc(100vh - 20px); // Adjust height to fit the layout
  border: none;
`;

const Investments = () => {
  const [isCollapsed, setIsCollapsed] = useState(true);
  const [searchText, setSearchText] = useState("advise as to my financial situation");
  const [searchResult, setSearchResult] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    setSearchResult("");
    try {
      // 1. Fetch stock info for the customer
      const stockInfoResp = await fetch("https://oracleai-financial.org/financial/stockinfoforcustid", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({}) // Add customer id if needed, e.g. { customerId }
      });
      let stockInfo = "";
      if (stockInfoResp.ok) {
        stockInfo = await stockInfoResp.text();
      }

      // 2. Append the stock info to the prompt
      const prompt =
        searchText +
        " based on vanguard projections and the list of stocks purchases I am aslo sending (assume the stock name indicates the industry etc.). don't ask me to provide any other information\n\n" +
        stockInfo;

      // 3. Query endpoint
      const response = await fetch("https://oracleai-financial.org/financial/query", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ query: prompt })
      });
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      const data = await response.json();
      setSearchResult(data.answer || "No answer found.");
    } catch (err) {
      setSearchResult("❌ Error: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageContainer>
      <h2>Process: Get personal financial insights</h2>
      <h2>Tech: Vector Search, AI Agents and MCP</h2>
      <h2>Reference: DMCC</h2>

      {/* Collapsible SidePanel */}
      <SidePanel>
        <ToggleButton onClick={() => setIsCollapsed(!isCollapsed)}>
          {isCollapsed ? 'Show Developer Details' : 'Hide Developer Details'}
        </ToggleButton>
        {!isCollapsed && (
          <CollapsibleContent>
            <TextContent>
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
              <div>
                <a
                  href="http://141.148.204.74:8080"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#1abc9c', textDecoration: 'none' }}
                >
                   AI Agents Backend
                </a>
              </div>
              <h4>Financial Process:</h4>
              <ul>
                <li>Generate financial insights using Oracle Database and AI Agents for private financial data, compliance docs, and market analysis</li>
              </ul>
              <h4>Developer Notes:</h4>
              <ul>
                <li>Uses Oracle Database for RAG with private financial</li>
                <li>Uses Oracle Database for vector searches of compliance pdfs</li>
                <li>Uses MCP for real-time market data</li>
              </ul>
              <h4>Differentiators:</h4>
              <ul>
                <li>Vector processing in the same database and with other business data (structured and unstructured)</li>
                <li>Call MCP from within the database using Java, JavaScript, or PL/SQL</li>
              </ul>
            </TextContent>
            <VideoWrapper>
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
            </VideoWrapper>
          </CollapsibleContent>
        )}
      </SidePanel>

      {/* Search Form */}
      <form
        style={{
          margin: '32px 0 24px 0',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }}
        onSubmit={handleSearch}
      >
        <label htmlFor="searchText" style={{ marginRight: '8px', fontWeight: 'bold' }}>
          Search:
        </label>
        <input
          type="text"
          id="searchText"
          name="searchText"
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          style={{
            padding: '8px',
            borderRadius: '4px',
            border: '1px solid #888',
            width: '350px',
            background: '#222',
            color: '#fff',
          }}
        />
        <button
          type="submit"
          style={{
            backgroundColor: '#1abc9c',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            padding: '8px 16px',
            cursor: 'pointer',
          }}
          disabled={loading}
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </form>
      {searchResult && (
        <div
          style={{
            background: "#181818",
            color: "#fff",
            border: "1px solid #444",
            borderRadius: "8px",
            padding: "16px",
            marginBottom: "24px",
            whiteSpace: "pre-wrap"
          }}
        >
          <strong>Result:</strong>
          <div>{searchResult}</div>
        </div>
      )}
    </PageContainer>
  );
};

export default Investments;
