import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import styled from 'styled-components';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

const SidebarContainer = styled.div`
  width: 350px;
  background-color: ${bankerBg};
  height: 100vh;
  position: fixed;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
`;

const SidebarHeader = styled.div`
  padding: 20px;
  color: ${bankerText};
  font-size: 14px;
  text-align: center;
  border-bottom: 1px solid ${bankerAccent};
`;

const SidebarMenu = styled.ul`
  list-style: none;
  padding: 0;
  flex: 1;
`;

const SidebarItem = styled.li`
  padding: 20px;
  color: ${bankerText};
  &:hover {
    background-color: ${bankerPanel};
  }
`;

const StyledNavLink = styled(NavLink)`
  color: inherit;
  text-decoration: none;
  display: flex;
  align-items: center;
  &.active {
    background-color: ${bankerAccent};
  }
`;

const IconWrapper = styled.div`
  margin-right: 10px;
  display: flex;
  align-items: center;
  img {
    width: 28px;
    height: 28px;
    object-fit: contain;
  }
`;

const HighlightedText = styled.span`
  color: #e3f2fd; // Lighter blue for better visibility, still in theme
  font-weight: bold;
  background: none;
  display: inline;
`;

const TextContainer = styled.div`
  display: flex;
  flex-direction: column;
`;

const ToggleSwitchContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 10px;
  flex-shrink: 0;
  background-color: ${bankerBg};
  border-top: 1px solid ${bankerAccent};
  padding-top: 15px;
`;

const ToggleSwitchLabel = styled.label`
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
`;

const ToggleSwitchInput = styled.input`
  opacity: 0;
  width: 0;
  height: 0;

  &:checked + span {
    background-color: ${bankerAccent};
  }

  &:checked + span:before {
    transform: translateX(26px);
  }
`;

const ToggleSwitchSlider = styled.span`
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.4s;
  border-radius: 24px;

  &:before {
    position: absolute;
    content: '';
    height: 18px;
    width: 18px;
    left: 4px;
    bottom: 3px;
    background-color: white;
    transition: 0.4s;
    border-radius: 50%;
  }
`;

const Sidebar = () => {
  const [showDetails, setShowDetails] = useState(true);

  return (
    <SidebarContainer>
      <SidebarMenu style={{ marginTop: 0 }}>
        {/* "About this app" is now flush to the top */}
        <SidebarItem style={{ marginTop: 0, paddingTop: 32 }}>
          <StyledNavLink to="/financialstoryboard">
            <IconWrapper>
              <img src="/images/Side Menu Icons/About this App.svg" alt="About this app" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>About this app</HighlightedText>
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/dashboard">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Architecture and Setup.svg" alt="Architecture and setup" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Architecture and setup</HighlightedText>
              {showDetails && <div>Kubernetes, Observability, BaaS,etc.</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/apis">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Publish Financial APIs.svg" alt="Publish financial APIs" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Publish financial APIs</HighlightedText>
              {showDetails && <div>ORDS, OpenAPI <br />Sphere</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/accounts">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Create and View Accounts.svg" alt="Create and view accounts" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Create and view accounts</HighlightedText>
              {showDetails && <div>MongoDB/MERN stack, JSON Duality<br/>
              Decimal Point Analytics</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/atm">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Deposit Withdraw Money ATM.svg" alt="Deposit/withdraw money (ATM)" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Deposit/withdraw money (ATM)</HighlightedText>
              {showDetails && <div>
              Polyglot<br/>
              Java, JS, Python, .NET, Go, Rust
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/transactions">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Transfer to External Bank.svg" alt="Transfer to external bank" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Transfer to external bank</HighlightedText>
              {showDetails && <div>
                MicroTx, Lock-free reservations <br />
                University of Naples
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/creditcardpurchase">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Make Purchases and Visualize Fraud.svg" alt="Make purchases and visualize fraud" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Make purchases and visualize fraud</HighlightedText>
              {showDetails && <div>
                Globally Distributed DB, OML, Spatial<br />
                AMEX
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/graph">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Detect Money Laundering.svg" alt="Detect money laundering" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Detect money laundering</HighlightedText>
              {showDetails && <div>
                Graph
                <br />Certegy
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/messaging">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Transfer to External Bank.svg" alt="Transfer to brokerage accounts" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Assets/Inventory Management</HighlightedText>
              {showDetails && <div>
                Kafka and TxEventQ <br />
                FSGBU
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/stockticker">
            <IconWrapper>
              <img src="/images/Side Menu Icons/View Stock Ticker.svg" alt="View stock ticker and buy/sell stock" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>View stock ticker and buy/sell stock</HighlightedText>
              {showDetails && <div>
                True Cache
                <br />
                NYSE
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/investments">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Get Personal Financial Insights.svg" alt="Get personal financial insights" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Get personal financial insights</HighlightedText>
              {showDetails && <div>
                Vector Search, AI Agents and MCP <br />
                DMCC
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/speakwithdata">
            <IconWrapper>
              <img src="/images/Side Menu Icons/Speak with your Financial Data.svg" alt="Speak with your financial data" />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Speak with your financial data</HighlightedText>
              {showDetails && <div>
                NL2SQL, Vector Search, Speech AI <br />
                Industrial Scientific
              </div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
      </SidebarMenu>
      <ToggleSwitchContainer>
        <ToggleSwitchLabel>
          <ToggleSwitchInput
            type="checkbox"
            checked={showDetails}
            onChange={() => setShowDetails(!showDetails)}
          />
          <ToggleSwitchSlider />
        </ToggleSwitchLabel>
        <span style={{ marginLeft: '10px', color: bankerText }}>
          {showDetails ? 'Hide Details' : 'Show Details'}
        </span>
      </ToggleSwitchContainer>
    </SidebarContainer>
  );
};

export default Sidebar;
