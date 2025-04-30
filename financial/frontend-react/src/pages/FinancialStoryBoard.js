import React from 'react';
import styled from 'styled-components';

const Image = styled.img`
  max-width: 100%;
  height: auto;
  margin-top: 20px;
`;

const Link = styled.a`
  display: inline-block;
  margin-bottom: 20px;
  color: #1abc9c;
  text-decoration: none;
  font-weight: bold;
  font-size: 1.5rem; /* Increase font size */
  padding: 10px 0; /* Add padding for better spacing */

  &:hover {
    text-decoration: underline;
  }
`;

const Dashboard = () => (
  <div>
    <h2>Financial Storyboard</h2>
    <Image src="/images/storyboard.png" alt="Financial Storyboard" />
 
  </div>
);

export default Dashboard;
