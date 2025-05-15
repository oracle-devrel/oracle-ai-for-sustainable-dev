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

const HighlightedText = styled.p`
  font-size: 1.8rem; /* Larger font size */
  font-weight: bold;
  margin-bottom: 20px;
`;

const Dashboard = () => (
  <div>
    <h2>Who is this for?</h2>
    <HighlightedText>
      This financial application and its corresponding workshop are aimed at:
    </HighlightedText>
    <ul>
      <li>Financial systems experts   AND</li>
      <li>Developers who build these systems</li>
    </ul>
    <p>
      In order to liaise the two and have a shared understanding of the possibilities and details of both the business solutions and the development architecture involved in them.
    </p>
    <Image src="/images/financial-customer-storyboard-leadin.png" alt="Financial Storyboard" />
    <Image src="/images/financial-analyst-storyboard-leadin.png" alt="Financial Storyboard" />
    <Image src="/images/developer-storyboard-leadin.png" alt="Financial Storyboard" />
  </div>
);

export default Dashboard;
