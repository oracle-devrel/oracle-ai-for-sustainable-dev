import React from 'react';
import styled from 'styled-components';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';

// Banker blue theme colors
const bankerBg = "#354F64";
const bankerAccent = "#5884A7";
const bankerText = "#F9F9F9";
const bankerPanel = "#223142";

const LoginContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: ${bankerBg};
`;

const LoginPanel = styled.div`
  background: ${bankerPanel};
  padding: 24px 28px 20px 28px;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.18);
  width: 340px;
  color: ${bankerText};
  border: 1px solid ${bankerAccent};
`;

const Button = styled.button`
  width: 100%;
  padding: 10px;
  background-color: ${bankerAccent};
  color: ${bankerText};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  margin-top: 10px;
  font-size: 1rem;
  &:hover {
    background-color: ${bankerBg};
  }
`;

const Login = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleEnter = () => {
    login();
    navigate('/financialstoryboard');
  };

  return (
    <LoginContainer>
      <LoginPanel>
        <h2 style={{ color: bankerAccent, textAlign: 'center', marginBottom: '18px' }}>Financial</h2>
        <Button type="button" onClick={handleEnter}>Enter</Button>
      </LoginPanel>
    </LoginContainer>
  );
};

export default Login;
