import React, { useState } from 'react';
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

const LoginForm = styled.form`
  background: ${bankerPanel};
  padding: 24px 28px 20px 28px;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.18);
  width: 340px;
  color: ${bankerText};
  border: 1px solid ${bankerAccent};
`;

const Input = styled.input`
  width: 100%;
  padding: 10px;
  margin: 12px 0;
  border: 1px solid ${bankerAccent};
  border-radius: 4px;
  background: #406080;
  color: ${bankerText};
  font-size: 1rem;
  &:focus {
    border-color: ${bankerAccent};
    outline: 1px solid ${bankerAccent};
  }
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
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    // Add authentication logic here
    if (username === 'DEVELOPER' && password === 'Welcome123456#') {
      login();
      navigate('/financialstoryboard');
    } else {
      alert('Invalid credentials');
    }
  };

  return (
    <LoginContainer>
      <LoginForm onSubmit={handleSubmit}>
        <h2 style={{ color: bankerAccent, textAlign: 'center', marginBottom: '18px' }}>Login</h2>
        <Input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <Input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button type="submit">Login</Button>
      </LoginForm>
    </LoginContainer>
  );
};

export default Login;