import React, { useState } from 'react';
import styled from 'styled-components';

const PageContainer = styled.div`
  background-color: #121212; /* Dark background */
  color: #ffffff; /* Light text */
  width: 100%;
  height: 100vh;
  padding: 20px;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column; /* Stack form elements vertically */
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid #444; /* Darker border */
  border-radius: 8px;
  background-color: #1e1e1e; /* Darker background for the form */
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #ffffff; /* Light text */
`;

const Input = styled.input`
  width: 100%;
  margin-bottom: 16px;
  padding: 8px;
  border: 1px solid #555; /* Darker border */
  border-radius: 4px;
  background-color: #2c2c2c; /* Darker input background */
  color: #ffffff; /* Light text */
`;

const RadioLabel = styled.label`
  display: block;
  margin-bottom: 8px;
  color: #ffffff; /* Light text */
`;

const Button = styled.button`
  padding: 10px;
  background-color: #1abc9c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  &:hover {
    background-color: #16a085;
  }
`;

const ATM = () => {
  const [formData, setFormData] = useState({
    amount: '',
    transactionType: '',
    language: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert(
      `Transaction submitted successfully! Type: ${formData.transactionType}, Language: ${formData.language}`
    );
  };

  return (
    <PageContainer>
      <h2>ATM</h2>
      <h2>Polyglot (Java, JS, Python, .NET, Go, Rust)</h2>

      <Form onSubmit={handleSubmit}>
        <Label htmlFor="amount">Amount</Label>
        <Input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          placeholder="Enter amount"
          required
        />

        <h4>Select a transaction type:</h4>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="deposit"
            checked={formData.transactionType === 'deposit'}
            onChange={handleChange}
          />
          Deposit
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="withdraw"
            checked={formData.transactionType === 'withdraw'}
            onChange={handleChange}
          />
          Withdraw
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="transactionType"
            value="balance"
            checked={formData.transactionType === 'balance'}
            onChange={handleChange}
          />
          Balance Inquiry
        </RadioLabel>

        <h4>Select a language for the transaction:</h4>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Java (Spring Boot)"
            checked={formData.language === 'Java (Spring Boot)'}
            onChange={handleChange}
          />
          Java (Spring Boot)
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="JS"
            checked={formData.language === 'JS'}
            onChange={handleChange}
          />
          JS
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Python"
            checked={formData.language === 'Python'}
            onChange={handleChange}
          />
          Python
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value=".NET"
            checked={formData.language === '.NET'}
            onChange={handleChange}
          />
          .NET
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Go"
            checked={formData.language === 'Go'}
            onChange={handleChange}
          />
          Go
        </RadioLabel>
        <RadioLabel>
          <input
            type="radio"
            name="language"
            value="Rust"
            checked={formData.language === 'Rust'}
            onChange={handleChange}
          />
          Rust
        </RadioLabel>

        <Button type="submit">Submit</Button>
      </Form>
    </PageContainer>
  );
};

export default ATM;
