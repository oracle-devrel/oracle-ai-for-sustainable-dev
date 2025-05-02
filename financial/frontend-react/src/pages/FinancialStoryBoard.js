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
    <h2>Who is this for?</h2>
    This financial application and its corresponding workshop are aimed at both financial systems experts and the developers who build these systems <br />
<br />in order to liaise the two and have a shared understanding of the possibilities and details of both the business solutions and the development architecture involved in them.

    <Image src="/images/financial-customer-storyboard-leadin.png" alt="Financial Storyboard" /><br />
    <Image src="/images/financial-analyst-storyboard-leadin.png" alt="Financial Storyboard" /><br />
    <Image src="/images/developer-storyboard-leadin.png" alt="Financial Storyboard" /><br />
  </div>
);

export default Dashboard;
