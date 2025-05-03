import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import styled from 'styled-components';
import { FaTachometerAlt, FaWallet, FaExchangeAlt, FaChartPie, FaPiggyBank, FaHandHoldingUsd, FaFileInvoiceDollar, FaShieldAlt, FaUserShield, FaCogs, FaChartLine } from 'react-icons/fa';

const SidebarContainer = styled.div`
  width: 350px;
  background-color: #2c3e50;
  height: 100vh;
  position: fixed;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
`;

const SidebarMenu = styled.ul`
  list-style: none;
  padding: 0;
`;

const SidebarItem = styled.li`
  padding: 20px;
  color: #ecf0f1;
  &:hover {
    background-color: #34495e;
  }
`;

const StyledNavLink = styled(NavLink)`
  color: inherit;
  text-decoration: none;
  display: flex;
  align-items: center; /* Align icon and first line of text horizontally */
  &.active {
    background-color: #1abc9c;
  }
`;

const IconWrapper = styled.div`
  margin-right: 10px; /* Add spacing between the icon and text */
  display: flex;
  align-items: center;
  svg {
    font-size: 24px; /* Make the icons smaller */
  }
`;

const HighlightedText = styled.span`
  color: #1abc9c; /* Slightly different color for the first line */
  font-weight: bold;
  background: none; /* Ensure no background is applied */
  display: inline; /* Ensure it behaves like inline text */
`;

const TextContainer = styled.div`
  display: flex;
  flex-direction: column; /* Stack the first line and additional details vertically */
`;

const ToggleButton = styled.button`
  background-color: #1abc9c;
  color: white;
  border: none;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  margin: 10px;
  &:hover {
    background-color: #16a085;
  }
`;

const Sidebar = () => {
  const [showDetails, setShowDetails] = useState(true); // Toggle state for showing details

  return (
    <SidebarContainer>
      <SidebarMenu>
        <SidebarItem>
          <StyledNavLink to="/financialstoryboard">
            <IconWrapper>
              <FaChartLine />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Financial Storyboard</HighlightedText>
              {showDetails && <div>Details about Financial Storyboard</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/dashboard">
            <IconWrapper>
              <FaTachometerAlt />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Technical architecture and setup</HighlightedText>
              {showDetails && <div>Details about architecture and setup</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/apis">
            <IconWrapper>
              <FaShieldAlt />
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
              <FaWallet />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Create and view accounts</HighlightedText>
              {showDetails && <div>MongoDB/MERN stack<br/>
              Decimal Point Analytics (DPA)</div>}
            </TextContainer>
          </StyledNavLink>
        </SidebarItem>
        <SidebarItem>
          <StyledNavLink to="/atm">
            <IconWrapper>
              <FaExchangeAlt />
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
              <FaExchangeAlt />
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
              <FaPiggyBank />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Make purchases and detect fraud</HighlightedText>
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
              <FaCogs />
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
              <FaChartPie />
            </IconWrapper>
            <TextContainer>
              <HighlightedText>Transfer to brokerage accounts</HighlightedText>
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
              <FaFileInvoiceDollar />
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
              <FaHandHoldingUsd />
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
              <FaUserShield />
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
      <ToggleButton onClick={() => setShowDetails(!showDetails)}>
        {showDetails ? 'Hide Details' : 'Show Details'}
      </ToggleButton>
    </SidebarContainer>
  );
};

export default Sidebar;
